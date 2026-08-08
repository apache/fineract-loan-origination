/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.los.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(
    properties = {
      "los.fineract.mock-enabled=true",
      "los.workflow.stages[0]=LOAN_OFFICER",
      "los.workflow.stages[1]=CREDIT_COMMITTEE",
      "los.workflow.stages[2]=BRANCH_MANAGER",
      "los.workflow.final-stage=BRANCH_MANAGER",
      "los.workflow.role-mapping.loan_officer=LOAN_OFFICER",
      "los.workflow.role-mapping.credit_committee=CREDIT_COMMITTEE",
      "los.workflow.role-mapping.branch_manager=BRANCH_MANAGER"
    })
public class CucumberSpringConfig {

  @LocalServerPort public int port;

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("los_test")
          .withUsername("test")
          .withPassword("test");
}
