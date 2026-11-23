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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(
    properties = {"los.deployment.environment=test", "los.fineract.mock-enabled=true"})
class MockFineractLoanIdSequenceIT {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("los_mock_ids_test")
          .withUsername("test")
          .withPassword("test");

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private MockFineractLoanApiClient adapter;

  @Autowired private LoanApplicationRepository loanApplicationRepository;

  @Test
  void concurrentAdapterAllocationsAcrossInstancesAreUniqueAndContinueAfterReconstruction()
      throws Exception {
    final MockFineractLoanApiClient secondAdapter = new MockFineractLoanApiClient(jdbcTemplate);
    final FineractLoanCreateRequest request = createRequest();
    final int allocationCount = 64;
    final List<Callable<Long>> allocations = new ArrayList<>();

    IntStream.range(0, allocationCount)
        .forEach(
            index -> {
              final MockFineractLoanApiClient selectedAdapter =
                  index % 2 == 0 ? adapter : secondAdapter;
              allocations.add(() -> selectedAdapter.createLoan(request).getLoanId());
            });

    final Long beforeConcurrentAllocations = adapter.createLoan(request).getLoanId();
    final List<Long> allocatedIds = new ArrayList<>();

    try (var executor = Executors.newFixedThreadPool(8)) {
      final List<Future<Long>> futures = executor.invokeAll(allocations);
      for (final Future<Long> future : futures) {
        allocatedIds.add(future.get());
      }

      assertThat(allocatedIds).doesNotHaveDuplicates();
      assertThat(allocatedIds).allMatch(id -> id > beforeConcurrentAllocations);
    }

    final MockFineractLoanApiClient reconstructedAdapter =
        new MockFineractLoanApiClient(jdbcTemplate);
    assertThat(reconstructedAdapter.createLoan(request).getLoanId())
        .isGreaterThan(allocatedIds.stream().mapToLong(Long::longValue).max().orElseThrow());
  }

  @Test
  void uniqueFineractLoanIdIsScopedToTenantAndAllowsNulls() {
    loanApplicationRepository.saveAndFlush(application("first", "tenant-a", 555L));

    assertThatThrownBy(
            () ->
                loanApplicationRepository.saveAndFlush(application("duplicate", "tenant-a", 555L)))
        .isInstanceOf(DataIntegrityViolationException.class);

    loanApplicationRepository.saveAndFlush(application("other-tenant", "tenant-b", 555L));
    loanApplicationRepository.saveAndFlush(application("null-one", "tenant-a", null));
    loanApplicationRepository.saveAndFlush(application("null-two", "tenant-a", null));
  }

  private static FineractLoanCreateRequest createRequest() {
    return FineractLoanCreateRequest.builder()
        .clientId(1L)
        .productId(1L)
        .principal(BigDecimal.valueOf(100))
        .build();
  }

  private static LoanApplication application(
      final String applicationRef, final String tenantId, final Long fineractLoanId) {
    final LoanApplication application = new LoanApplication();
    application.setApplicationRef(applicationRef);
    application.setStatus(LoanApplicationStatus.DRAFT);
    application.setRequestedAmount(BigDecimal.valueOf(100));
    application.setCurrency("USD");
    application.setTenantId(tenantId);
    application.setFineractLoanId(fineractLoanId);
    return application;
  }
}
