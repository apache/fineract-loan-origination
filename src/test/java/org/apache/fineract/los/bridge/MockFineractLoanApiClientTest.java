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
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockFineractLoanApiClient")
class MockFineractLoanApiClientTest {

  @Mock private LoanApplicationRepository loanApplicationRepository;

  @Test
  @DisplayName("starts at 100001 when no Fineract loan ID is stored yet")
  void startsFromSeedWhenNothingStored() {
    when(loanApplicationRepository.findMaxFineractLoanId()).thenReturn(Optional.empty());

    final MockFineractLoanApiClient client =
        new MockFineractLoanApiClient(loanApplicationRepository);

    assertThat(createLoanId(client)).isEqualTo(100001L);
  }

  @Test
  @DisplayName("continues after the highest stored ID when the application restarts")
  void continuesAfterStoredIdsAfterRestart() {
    // IDs 100001 and 100002 were stored before the restart.
    when(loanApplicationRepository.findMaxFineractLoanId()).thenReturn(Optional.of(100002L));

    final MockFineractLoanApiClient restartedClient =
        new MockFineractLoanApiClient(loanApplicationRepository);

    assertThat(createLoanId(restartedClient)).isEqualTo(100003L);
  }

  @Test
  @DisplayName("keeps IDs unique when the previous ID has not been stored yet")
  void keepsIdsUniqueBeforeTheyAreStored() {
    when(loanApplicationRepository.findMaxFineractLoanId()).thenReturn(Optional.of(100005L));

    final MockFineractLoanApiClient client =
        new MockFineractLoanApiClient(loanApplicationRepository);

    assertThat(createLoanId(client)).isEqualTo(100006L);
    assertThat(createLoanId(client)).isEqualTo(100007L);
    assertThat(createLoanId(client)).isEqualTo(100008L);
  }

  private static long createLoanId(final MockFineractLoanApiClient client) {
    return client
        .createLoan(
            FineractLoanCreateRequest.builder()
                .clientId(1001L)
                .productId(1L)
                .principal(new BigDecimal("10000"))
                .build())
        .getLoanId();
  }
}
