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

package org.apache.fineract.los.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {"los.deployment.environment=test", "spring.flyway.target=9"})
class MockFineractLoanIdMigrationIT {

  private static final long EXISTING_FINERACT_LOAN_ID = 123456L;

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("los_mock_id_migration_test")
          .withUsername("test")
          .withPassword("test");

  @Autowired private DataSource dataSource;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private LoanApplicationRepository loanApplicationRepository;

  @Test
  void v10StartsSequenceAfterExistingFineractLoanIds() {
    loanApplicationRepository.saveAndFlush(application());

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .schemas("public")
        .defaultSchema("public")
        .load()
        .migrate();

    assertThat(
            jdbcTemplate.queryForObject("SELECT nextval('mock_fineract_loan_id_seq')", Long.class))
        .isEqualTo(EXISTING_FINERACT_LOAN_ID + 1);
  }

  private static LoanApplication application() {
    final LoanApplication application = new LoanApplication();
    application.setApplicationRef("existing-loan-before-v10");
    application.setStatus(LoanApplicationStatus.DISBURSED);
    application.setRequestedAmount(new BigDecimal("100.00"));
    application.setCurrency("USD");
    application.setTenantId("tenant-a");
    application.setFineractLoanId(EXISTING_FINERACT_LOAN_ID);
    return application;
  }
}
