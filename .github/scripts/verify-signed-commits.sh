#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# LOCAL pre-push helper only.
#
# NOTE: This script is NOT the CI gate. CI uses GitHub's own API to check
# commit.verification.verified. A runner holds no contributor public-keys,
# so local GPG checks here cannot give the same answer. Use this script to
# catch obvious omissions (no signature at all) before pushing; GitHub's
# check is authoritative.
#
# Usage: bash scripts/verify-signed-commits.sh [<base-ref>]
#   base-ref defaults to origin/main

set -euo pipefail

BASE_REF="${1:-origin/main}"
MERGE_BASE=$(git merge-base "$BASE_REF" HEAD)

echo "Checking commits: $(git rev-parse --short "$MERGE_BASE")..HEAD against $BASE_REF"
echo ""

FAIL=0
while IFS=" " read -r SHA STATUS; do
  MSG=$(git log -1 --format="%s" "$SHA")
  SHORT="${SHA:0:8}"
  if [ "$STATUS" = "N" ]; then
    echo "UNSIGNED: $SHORT $MSG"
    FAIL=1
  else
    echo "SIGNED:   $SHORT $MSG  [%G?=$STATUS]"
  fi
done < <(git log --format="%H %G?" "$MERGE_BASE"..HEAD)

echo ""
if [ "$FAIL" -ne 0 ]; then
  echo "One or more commits are unsigned. Sign them with:"
  echo ""
  echo "  git rebase --exec 'git commit --amend --no-edit -S' $BASE_REF"
  echo "  git push --force-with-lease"
  echo ""
  echo "Then verify again with: bash scripts/verify-signed-commits.sh"
  exit 1
fi

echo "All commits signed (local check)."
echo "Note: GitHub's API verification is still authoritative."
