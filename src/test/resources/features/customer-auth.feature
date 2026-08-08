# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0

Feature: Customer Authentication

  Scenario: Customer logs in with valid credentials
    Given a registered customer with username "john" and password "john123"
    When the customer logs in with username "john" and password "john123"
    Then the response status is 200
    And the response contains a JWT token

  Scenario: Customer logs in with wrong password
    Given a registered customer with username "john" and password "john123"
    When the customer logs in with username "john" and password "wrongpass"
    Then the response status is 401

  Scenario: Customer logs in with unknown username
    When the customer logs in with username "nobody" and password "pass123"
    Then the response status is 401