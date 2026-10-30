# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0

Feature: Authorization of staff-only endpoints

  These scenarios use a real customer JWT and confirm staff-only endpoints return 403 without
  exposing applicant data.

  Scenario: Customer cannot disburse a loan application
    Given a registered customer with username "john" and password "john123"
    When the customer logs in with username "john" and password "john123"
    And the customer calls POST "/api/v1/loan-applications/LOS-2026-00001/disburse" with their token
    Then the response status is 403
    And the response body contains no applicant data

  Scenario: Customer cannot start a review
    Given a registered customer with username "john" and password "john123"
    When the customer logs in with username "john" and password "john123"
    And the customer calls POST "/api/v1/loan-applications/LOS-2026-00001/start-review" with their token
    Then the response status is 403
    And the response body contains no applicant data

  Scenario: Customer cannot list staff accounts
    Given a registered customer with username "john" and password "john123"
    When the customer logs in with username "john" and password "john123"
    And the customer calls GET "/api/v1/admin/staff?tenantId=default" with their token
    Then the response status is 403
    And the response message is "You do not have permission to perform this action."
