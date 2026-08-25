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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.apache.fineract.los.bridge.dto.FineractLoanResponse;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.FineractIntegrationStatus;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.exception.DisbursementNotAllowedException;
import org.apache.fineract.los.exception.FineractIntegrationException;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.statemachine.LoanStateTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementBridgeService")
class DisbursementBridgeServiceTest {

  @Mock private LoanApplicationRepository loanApplicationRepository;
  @Mock private ApplicantProfileRepository applicantProfileRepository;
  @Mock private FineractLoanApiClient fineractLoanApiClient;

  private DisbursementBridgeService service;

  @BeforeEach
  void setUp() {
    final FineractClientProperties properties = new FineractClientProperties();
    properties.setDefaultAmortizationType(1);
    properties.setDefaultInterestType(0);
    properties.setDefaultInterestCalculationPeriodType(1);

    final LoanOriginationStateMachine stateMachine =
        new LoanOriginationStateMachine(new LoanStateTransitionValidator());

    service =
        new DisbursementBridgeService(
            loanApplicationRepository,
            applicantProfileRepository,
            fineractLoanApiClient,
            properties,
            stateMachine);
  }

  private LoanApplication approvedApp() {
    final LoanApplication app = new LoanApplication();
    app.setApplicationRef("LOS-2026-00001");
    app.setTenantId("default");
    app.setStatus(LoanApplicationStatus.APPROVED);
    app.setRequestedAmount(new BigDecimal("10000"));
    app.setTenorMonths(12);
    app.setFineractLoanProductId(1L);
    return app;
  }

  private ApplicantProfile profileWithClientId(final Long clientId) {
    final ApplicantProfile profile = new ApplicantProfile();
    profile.setFineractClientId(clientId);
    return profile;
  }

  private void stubLookup(final LoanApplication app, final ApplicantProfile profile) {
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));
    when(applicantProfileRepository.findByApplication(app)).thenReturn(Optional.of(profile));
    when(loanApplicationRepository.save(app)).thenReturn(app);
  }

  @Nested
  @DisplayName("Complete Fineract Lifecycle")
  class CompleteFineractLifecycle {

    @Test
    @DisplayName("executes create→approve→disburse and transitions to DISBURSED")
    void executesFullLifecycle() {
      final LoanApplication app = approvedApp();
      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      when(fineractLoanApiClient.createLoan(any()))
          .thenReturn(
              FineractLoanCreateResponse.builder()
                  .officeId(1L)
                  .clientId(42L)
                  .loanId(999L)
                  .resourceId(999L)
                  .build());

      when(fineractLoanApiClient.approveLoan(eq(999L), any()))
          .thenReturn(FineractLoanResponse.builder().loanId(999L).resourceId(999L).build());

      when(fineractLoanApiClient.disburseLoan(eq(999L), any()))
          .thenReturn(FineractLoanResponse.builder().loanId(999L).resourceId(999L).build());

      final Long loanId =
          service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId());

      assertThat(loanId).isEqualTo(999L);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.DISBURSED);
      assertThat(app.getFineractLoanId()).isEqualTo(999L);
      assertThat(app.getFineractIntegrationStatus())
          .isEqualTo(FineractIntegrationStatus.LOAN_DISBURSED);

      verify(fineractLoanApiClient).createLoan(any());
      verify(fineractLoanApiClient).approveLoan(eq(999L), any());
      verify(fineractLoanApiClient).disburseLoan(eq(999L), any());
      verify(loanApplicationRepository, times(4)).save(app); // create, approve, disburse, status
    }

    @Test
    @DisplayName("idempotent - skips create if loan already created")
    void idempotentCreate() {
      final LoanApplication app = approvedApp();
      app.setFineractLoanId(999L);
      app.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_CREATED);

      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      when(fineractLoanApiClient.approveLoan(eq(999L), any()))
          .thenReturn(FineractLoanResponse.builder().loanId(999L).resourceId(999L).build());

      when(fineractLoanApiClient.disburseLoan(eq(999L), any()))
          .thenReturn(FineractLoanResponse.builder().loanId(999L).resourceId(999L).build());

      service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId());

      verify(fineractLoanApiClient, never()).createLoan(any());
      verify(fineractLoanApiClient).approveLoan(eq(999L), any());
      verify(fineractLoanApiClient).disburseLoan(eq(999L), any());
    }

    @Test
    @DisplayName("idempotent - skips create and approve if already approved")
    void idempotentApprove() {
      final LoanApplication app = approvedApp();
      app.setFineractLoanId(999L);
      app.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_APPROVED);

      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      when(fineractLoanApiClient.disburseLoan(eq(999L), any()))
          .thenReturn(FineractLoanResponse.builder().loanId(999L).resourceId(999L).build());

      service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId());

      verify(fineractLoanApiClient, never()).createLoan(any());
      verify(fineractLoanApiClient, never()).approveLoan(any(), any());
      verify(fineractLoanApiClient).disburseLoan(eq(999L), any());
    }

    @Test
    @DisplayName("idempotent - completes immediately if already disbursed")
    void idempotentDisburse() {
      final LoanApplication app = approvedApp();
      app.setFineractLoanId(999L);
      app.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_DISBURSED);
      // Note: Status is still APPROVED, not DISBURSED - we check integration status, not app status

      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      final Long loanId =
          service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId());

      assertThat(loanId).isEqualTo(999L);
      // Verify the app status transitions to DISBURSED
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.DISBURSED);
      verify(fineractLoanApiClient, never()).createLoan(any());
      verify(fineractLoanApiClient, never()).approveLoan(any(), any());
      verify(fineractLoanApiClient, never()).disburseLoan(any(), any());
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("create failure sets FAILED status")
    void createFailureSetsFailedStatus() {
      final LoanApplication app = approvedApp();
      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      when(fineractLoanApiClient.createLoan(any()))
          .thenThrow(new RestClientException("Network error"));

      assertThatThrownBy(
              () -> service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId()))
          .isInstanceOf(FineractIntegrationException.class);

      assertThat(app.getFineractIntegrationStatus()).isEqualTo(FineractIntegrationStatus.FAILED);
      verify(loanApplicationRepository).save(app);
    }

    @Test
    @DisplayName("approve failure sets FAILED status")
    void approveFailureSetsFailedStatus() {
      final LoanApplication app = approvedApp();
      app.setFineractLoanId(999L);
      app.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_CREATED);

      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      when(fineractLoanApiClient.approveLoan(eq(999L), any()))
          .thenThrow(new RestClientException("Approval rejected"));

      assertThatThrownBy(
              () -> service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId()))
          .isInstanceOf(FineractIntegrationException.class);

      assertThat(app.getFineractIntegrationStatus()).isEqualTo(FineractIntegrationStatus.FAILED);
    }

    @Test
    @DisplayName("disburse failure sets FAILED status")
    void disburseFailureSetsFailedStatus() {
      final LoanApplication app = approvedApp();
      app.setFineractLoanId(999L);
      app.setFineractIntegrationStatus(FineractIntegrationStatus.LOAN_APPROVED);

      final ApplicantProfile profile = profileWithClientId(42L);
      stubLookup(app, profile);

      when(fineractLoanApiClient.disburseLoan(eq(999L), any()))
          .thenThrow(new RestClientException("Insufficient balance"));

      assertThatThrownBy(
              () -> service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId()))
          .isInstanceOf(FineractIntegrationException.class);

      assertThat(app.getFineractIntegrationStatus()).isEqualTo(FineractIntegrationStatus.FAILED);
    }
  }

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("throws when application is not APPROVED")
    void throwsWhenNotApproved() {
      final LoanApplication app = approvedApp();
      app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
      when(loanApplicationRepository.findByApplicationRefAndTenantId(
              app.getApplicationRef(), app.getTenantId()))
          .thenReturn(Optional.of(app));

      assertThatThrownBy(
              () -> service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId()))
          .isInstanceOf(DisbursementNotAllowedException.class);
    }

    @Test
    @DisplayName("throws when applicant has no Fineract clientId")
    void throwsWhenNoClientId() {
      final LoanApplication app = approvedApp();
      when(loanApplicationRepository.findByApplicationRefAndTenantId(
              app.getApplicationRef(), app.getTenantId()))
          .thenReturn(Optional.of(app));
      when(applicantProfileRepository.findByApplication(app))
          .thenReturn(Optional.of(profileWithClientId(null)));

      assertThatThrownBy(
              () -> service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId()))
          .isInstanceOf(DisbursementNotAllowedException.class);
    }

    @Test
    @DisplayName("throws when application has no loan product ID")
    void throwsWhenNoLoanProductId() {
      final LoanApplication app = approvedApp();
      app.setFineractLoanProductId(null);
      when(loanApplicationRepository.findByApplicationRefAndTenantId(
              app.getApplicationRef(), app.getTenantId()))
          .thenReturn(Optional.of(app));
      when(applicantProfileRepository.findByApplication(app))
          .thenReturn(Optional.of(profileWithClientId(42L)));

      assertThatThrownBy(
              () -> service.executeFullDisbursement(app.getApplicationRef(), app.getTenantId()))
          .isInstanceOf(DisbursementNotAllowedException.class);
    }
  }
}
