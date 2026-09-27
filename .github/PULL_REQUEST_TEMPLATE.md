<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->

<!-- Commits must be signed to merge; see docs/development/gpg-commit-signing.adoc. -->

## Description

<!-- What changed and why. Link the GitHub issue this PR resolves. -->

Closes #

## Verification

<!-- List what you ran locally and the result. Write "Not applicable" with a reason if a check does not apply. -->

- [ ] `./mvnw spotless:apply`
- [ ] `./mvnw clean verify`
- [ ] `./mvnw apache-rat:check`
- [ ] Frontend changes: `npm run lint`, `npm run format:check`, `npm test` (in `frontend/`)

## AI assistance (optional)

<!-- If generative AI materially assisted this contribution, you may state the tool/model and how it
was used. Disclosure here is optional and does not affect review. You remain responsible for every
line of the change. -->

- Tool / model:
- How it was used:

## Checklist

- [ ] This PR is one logical change, not a code dump.
- [ ] I added or updated tests, or explained why tests are not needed.
- [ ] New files carry the ASF license header.
- [ ] Docs (and `AGENTS.md`, if commands, paths, or architecture rules changed) are updated.
- [ ] All commits are signed.
- [ ] I followed the [AI Policy](https://github.com/apache/fineract/blob/develop/CONTRIBUTING.md#ai-policy) and the [ASF Generative Tooling Guidance](https://www.apache.org/legal/generative-tooling.html).
