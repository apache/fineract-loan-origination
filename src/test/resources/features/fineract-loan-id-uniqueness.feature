# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0

Feature: Unique Fineract loan IDs

  These scenarios run against the real database, so they check both the unique constraint on
  (tenant_id, fineract_loan_id) and how the mock Fineract adapter picks IDs after a restart.

  Scenario: Mock adapter does not reuse a stored loan ID after a restart
    Given application "LOS-UNIQ-001" in tenant "default" is linked to Fineract loan 100001
    And application "LOS-UNIQ-002" in tenant "default" is linked to Fineract loan 100002
    When a freshly started mock Fineract adapter creates a loan
    Then the new Fineract loan ID is 100003

  Scenario: The same Fineract loan ID cannot be linked twice in one tenant
    Given application "LOS-UNIQ-001" in tenant "default" is linked to Fineract loan 100001
    When another application "LOS-UNIQ-002" in tenant "default" is linked to Fineract loan 100001
    Then the link is rejected as a duplicate

  Scenario: The same Fineract loan ID can be used by different tenants
    Given application "LOS-UNIQ-001" in tenant "default" is linked to Fineract loan 100001
    When another application "LOS-UNIQ-002" in tenant "other-tenant" is linked to Fineract loan 100001
    Then the link is saved
