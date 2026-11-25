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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class MockFineractLoanApiClientTest {

  private static final String NEXT_ID_SQL = "SELECT nextval('mock_fineract_loan_id_seq')";

  @Test
  void createLoanRequestsAnIdFromJdbcTemplateAndReturnsIt() {
    final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    when(jdbcTemplate.queryForObject(NEXT_ID_SQL, Long.class))
        .thenReturn(100001L, 100002L, 100003L);
    final FineractLoanCreateRequest request =
        FineractLoanCreateRequest.builder()
            .clientId(1L)
            .productId(1L)
            .principal(BigDecimal.valueOf(100))
            .build();

    final MockFineractLoanApiClient firstInstance = new MockFineractLoanApiClient(jdbcTemplate);
    final MockFineractLoanApiClient restartedInstance = new MockFineractLoanApiClient(jdbcTemplate);

    assertThat(firstInstance.createLoan(request).getLoanId()).isEqualTo(100001L);
    assertThat(firstInstance.createLoan(request).getLoanId()).isEqualTo(100002L);
    assertThat(restartedInstance.createLoan(request).getLoanId()).isEqualTo(100003L);
    verify(jdbcTemplate, org.mockito.Mockito.times(3)).queryForObject(NEXT_ID_SQL, Long.class);
  }
}
