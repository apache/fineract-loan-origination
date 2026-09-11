/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Fails a pull request whose commits GitHub does not report as **Verified**, and explains on
 * the pull request itself what to do about it.
 *
 * ## Why this replaced a local git check
 *
 * The previous gate ran `scripts/verify-signed-commits.sh` on the runner and passed anything
 * whose `%G?` was not `N`. That asks "is a signature attached?". GitHub asks something
 * stricter: "was this made by a key registered to a GitHub account, whose verified email
 * matches the committer?" A commit passes the first and fails the second routinely — a
 * signature made with a key GitHub has never seen, or a `user.email` that is not an address
 * on any account, both produce a signed-looking commit that shows as Unverified.
 *
 * A runner holds no contributor public keys, so it cannot answer the second question itself.
 * It does not need to: GitHub already computed the answer and hands it over per commit as
 * `commit.verification`. This asks for that and believes it.
 *
 * ## Why `pull_request_target`, and what makes it safe here
 *
 * Under `pull_request`, the workflow *and* the scripts it calls come from the merge ref —
 * the pull request's own copy. A contributor can therefore edit the gate that is meant to
 * gate them, which is not a hypothetical: it is how a signed-commit check came to accept any
 * `gpgsig` header.
 *
 * `pull_request_target` runs the base repository's copy of the workflow and carries a token
 * that can comment, which a fork's `pull_request` run deliberately does not. The hazard of
 * that trigger — "pwn request" — is checking out or executing the pull request's code while
 * holding that token. This does neither: the calling workflow's checkout is ref-less and
 * sparse (`.github/scripts` from the base branch), and the only input read here is the API
 * response, never the diff, never the commit tree.
 *
 * Commit *messages* are attacker-controlled and do reach the comment body, so subjects are
 * truncated and their `@` and `#` references defused before they are posted.
 *
 * Two consequences of the trigger, both worth knowing:
 *
 *   - Changes to this file take effect only once merged. A pull request editing it cannot
 *     test it — which is the entire point.
 *   - This is early, explanatory feedback, not the last line of defence. Branch protection's
 *     "Require signed commits" is enforced by GitHub itself and cannot be edited by any
 *     workflow; this check exists so a contributor learns *why* before they hit that wall.
 */

const MARKER = '<!-- signed-commits -->';

/** GitHub caps `listCommits` at 250; past that the check reports what it could see. */
const MAX_COMMITS = 250;

const CONTRIBUTING =
  'https://github.com/apache/fineract-loan-origination/blob/main/CONTRIBUTING.md#commit-signing';

/**
 * What each `verification.reason` actually means for the contributor, and what fixes it.
 *
 * These are not interchangeable, which is why a blanket "please sign your commits" has such a
 * poor hit rate. `no_user` in particular is *not* a signing problem — the signature is fine
 * and the identity is wrong — so telling someone to set up signing sends them to re-do the
 * one part they already got right.
 */
const REASONS = {
  unsigned: {
    summary: 'These commits carry no signature at all.',
    fix: `Set up commit signing, then re-sign the commits already on this branch. [CONTRIBUTING.md](${CONTRIBUTING}) has the setup.`,
  },
  no_user: {
    summary:
      'These commits **are signed**, but the committer email is not an address GitHub can match to any account — so it never gets as far as checking the key. Note that the commits are not attributed to your GitHub profile either, which is the same cause.',
    fix: 'This is an identity problem, not a signing one — your signing setup is fine. Check `git config user.email` and set it to an address that is **verified on your GitHub account** (Settings → Emails), then re-sign.',
  },
  unverified_email: {
    summary:
      'The committer email is on your GitHub account, but that address has not been verified.',
    fix: 'Verify the address under Settings → Emails, then re-sign. No change to your signing key is needed.',
  },
  unknown_key: {
    summary: 'The signing key is not registered on your GitHub account.',
    fix: 'Add the public half of your signing key under Settings → SSH and GPG keys, as a **signing** key, then re-sign.',
  },
  unknown_signature_type: {
    summary: 'GitHub does not recognise this signature type.',
    fix: 'Sign with GPG or SSH — see [CONTRIBUTING.md](' + CONTRIBUTING + ').',
  },
  expired_key: {
    summary: 'The signing key has expired.',
    fix: 'Extend or replace the key, upload the new public key to GitHub, then re-sign.',
  },
  not_signing_key: {
    summary: 'The key is on your account, but not marked as a signing key.',
    fix: 'Add it under Settings → SSH and GPG keys as a signing key, then re-sign.',
  },
  bad_signature: {
    summary: 'The signature does not match the commit contents.',
    fix: 'Re-sign the commits.',
  },
  malformed_signature: {
    summary: 'The signature could not be parsed.',
    fix: 'Re-sign the commits.',
  },
};

const FALLBACK = {
  summary: 'GitHub could not verify the signature on these commits.',
  fix: `See [CONTRIBUTING.md](${CONTRIBUTING}) for the expected signing setup.`,
};

/**
 * Commit subjects are written by the contributor and land in a comment this repository posts.
 * Keeping the characters but breaking the reference with a zero-width space means a subject
 * cannot turn into a notification for an unrelated person or a cross-link to someone's issue.
 */
function defuseReferences(text) {
  return text.replace(/@(?=[A-Za-z0-9])/g, '@​').replace(/#(?=\d)/g, '#​');
}

function subjectOf(commit) {
  const subject = (commit.commit?.message ?? '').split('\n', 1)[0];
  const clipped = subject.length > 72 ? `${subject.slice(0, 71)}…` : subject;
  return defuseReferences(clipped);
}

/** Groups the unverified commits by reason, so each remedy is stated once. */
function groupByReason(unverified) {
  const groups = new Map();
  for (const commit of unverified) {
    const reason = commit.commit?.verification?.reason ?? 'unknown';
    if (!groups.has(reason)) groups.set(reason, []);
    groups.get(reason).push(commit);
  }
  return groups;
}

function buildBody({ unverified, total, truncated }) {
  const groups = groupByReason(unverified);
  const plural = unverified.length === 1 ? 'commit is' : 'commits are';

  const sections = [...groups.entries()].map(([reason, commits]) => {
    const { summary, fix } = REASONS[reason] ?? FALLBACK;
    const list = commits.map((c) => `- \`${c.sha.slice(0, 8)}\` ${subjectOf(c)}`).join('\n');
    return `### \`${reason}\`\n\n${summary}\n\n${list}\n\n**How to fix:** ${fix}`;
  });

  const resign = [
    '```bash',
    '# only for the identity reasons above (no_user / unverified_email):',
    'git config user.email "you@example.com"',
    '',
    '# re-sign every commit on this branch:',
    "git rebase --exec 'git commit --amend --no-edit --reset-author -S' origin/main",
    'git push --force-with-lease',
    '```',
  ].join('\n');

  return (
    [
      MARKER,
      '',
      '## Commits on this pull request are not verified',
      '',
      `${unverified.length} of ${total} ${plural} not showing as **Verified** on GitHub, so this pull request cannot be merged into \`main\` yet.`,
      truncated
        ? `\n> This pull request has more than ${MAX_COMMITS} commits; only the first ${MAX_COMMITS} were checked.\n`
        : null,
      '',
      ...sections.flatMap((section) => [section, '']),
      '### Re-signing',
      '',
      resign,
      '',
      'Force-pushing is expected here — re-signing rewrites the commits, so their hashes change.',
      '',
      '---',
      '',
      'This comment is posted automatically and updates itself when you push; it disappears once every commit verifies. ' +
        'If you believe this is wrong, say so on the pull request — a maintainer can check.',
    ]
      .filter((line) => line !== null)
      .join('\n')
  );
}

async function syncComment({ github, context, issueNumber, body }) {
  const { owner, repo } = context.repo;
  const comments = await github.paginate(github.rest.issues.listComments, {
    owner,
    repo,
    issue_number: issueNumber,
    per_page: 100,
  });
  const existing = comments.find(
    (comment) => comment.user?.type === 'Bot' && comment.body?.includes(MARKER),
  );

  if (!body) {
    if (existing) {
      await github.rest.issues.deleteComment({ owner, repo, comment_id: existing.id });
      return 'deleted';
    }
    return 'noop';
  }

  if (existing) {
    await github.rest.issues.updateComment({ owner, repo, comment_id: existing.id, body });
    return 'updated';
  }
  await github.rest.issues.createComment({ owner, repo, issue_number: issueNumber, body });
  return 'created';
}

module.exports = async ({ github, context, core }) => {
  const log = core?.info ?? console.log;
  const { owner, repo } = context.repo;
  const pull = context.payload.pull_request;

  if (!pull) {
    core?.setFailed?.('No pull request in the event payload.');
    return;
  }

  const commits = await github.paginate(github.rest.pulls.listCommits, {
    owner,
    repo,
    pull_number: pull.number,
    per_page: 100,
  });

  const checked = commits.slice(0, MAX_COMMITS);
  const truncated = commits.length > MAX_COMMITS;
  const unverified = checked.filter((c) => c.commit?.verification?.verified !== true);

  for (const commit of checked) {
    const { verified, reason } = commit.commit?.verification ?? {};
    log(`${verified ? '✅' : '❌'} ${commit.sha.slice(0, 8)} ${reason ?? 'unknown'}`);
  }

  if (unverified.length === 0) {
    const action = await syncComment({ github, context, issueNumber: pull.number, body: null });
    log(`All ${checked.length} commit(s) verified. Comment: ${action}.`);
    return;
  }

  for (const commit of unverified) {
    core?.error?.(
      `Commit ${commit.sha.slice(0, 8)} is not verified by GitHub ` +
        `(${commit.commit?.verification?.reason ?? 'unknown'}).`,
      { title: 'Unverified commit' },
    );
  }

  const body = buildBody({ unverified, total: checked.length, truncated });
  const action = await syncComment({ github, context, issueNumber: pull.number, body });
  log(`Comment ${action}.`);

  core?.setFailed?.(
    `${unverified.length} of ${checked.length} commit(s) are not verified by GitHub. ` +
      'See the comment on the pull request.',
  );
};

// Exported for the unit tests; not used by the workflow.
module.exports.buildBody = buildBody;
module.exports.groupByReason = groupByReason;
module.exports.defuseReferences = defuseReferences;
module.exports.MARKER = MARKER;
module.exports.REASONS = REASONS;