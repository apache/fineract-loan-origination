# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0

Feature: Multi-Stage Loan Approval Workflow

  Background:
    Given a loan application with reference "LOS-TEST-001" exists in UNDER_REVIEW status

  Scenario: Loan Officer approves — application stays UNDER_REVIEW
    When the "LOAN_OFFICER" officer "lo.officer" approves application "LOS-TEST-001"
    Then the application "LOS-TEST-001" status is "UNDER_REVIEW"

  Scenario: Credit Committee approves — application stays UNDER_REVIEW
    When the "CREDIT_COMMITTEE" officer "cc.officer" approves application "LOS-TEST-001"
    Then the application "LOS-TEST-001" status is "UNDER_REVIEW"

  Scenario: Branch Manager approves final stage — application moves to APPROVED
    When the "BRANCH_MANAGER" officer "bm.officer" approves application "LOS-TEST-001"
    Then the application "LOS-TEST-001" status is "APPROVED"

  Scenario: Any stage rejects — application moves to REJECTED
    When the "LOAN_OFFICER" officer "lo.officer" rejects application "LOS-TEST-001" with comment "Insufficient income"
    Then the application "LOS-TEST-001" status is "REJECTED"