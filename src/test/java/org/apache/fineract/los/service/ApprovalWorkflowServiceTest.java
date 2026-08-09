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

package org.apache.fineract.los.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.exception.ApprovalStageMismatchException;
import org.apache.fineract.los.exception.DuplicateApprovalException;
import org.apache.fineract.los.exception.LosRoleNotAssignedException;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.statemachine.LoanStateTransitionValidator;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalWorkflowService")
class ApprovalWorkflowServiceTest {

  @Mock private LoanApplicationRepository loanApplicationRepository;
  @Mock private ApprovalStageRepository approvalStageRepository;

  private ApprovalWorkflowService service;
  private ApprovalWorkflowProperties workflowProperties;

  @BeforeEach
  void setUp() {
    workflowProperties = new ApprovalWorkflowProperties();
    workflowProperties.setStages(List.of("LOAN_OFFICER", "BRANCH_MANAGER"));

    final LoanOriginationStateMachine realStateMachine =
        new LoanOriginationStateMachine(new LoanStateTransitionValidator());

    service =
        new ApprovalWorkflowService(
            loanApplicationRepository,
            approvalStageRepository,
            realStateMachine,
            workflowProperties);
  }

  private LoanApplication underReviewApp() {
    final LoanApplication app = new LoanApplication();
    app.setApplicationRef("LOS-2026-00001");
    app.setTenantId("default");
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    return app;
  }

  private Authentication authAs(final String username, final String losRole) {
    return new UsernamePasswordAuthenticationToken(
        username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + losRole)));
  }

  private void stubLookup(final LoanApplication app) {
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    when(approvalStageRepository.save(any(ApprovalStage.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("APPROVE decisions")
  class ApproveDecisions {

    @Test
    @DisplayName("approving stage 1 as LOAN_OFFICER keeps status UNDER_REVIEW")
    void approvingNonFinalStageStaysUnderReview() {

      final LoanApplication app = underReviewApp();
      stubLookup(app);

      when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
          .thenReturn(0L);

      final ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .decision(ApprovalDecision.APPROVE)
              .comments("Approved after review")
              .build();

      service.recordDecision(
          app.getApplicationRef(), app.getTenantId(), request, authAs("officer1", "LOAN_OFFICER"));

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);

      verify(approvalStageRepository).save(any(ApprovalStage.class));
      verify(loanApplicationRepository).save(app);
    }

    @Test
    @DisplayName("approving the final configured stage moves to APPROVED")
    void approvingFinalStageMovesToApproved() {

      final LoanApplication app = underReviewApp();
      stubLookup(app);

      when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
          .thenReturn(1L);

      final ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .decision(ApprovalDecision.APPROVE)
              .comments("Final approval granted")
              .build();

      service.recordDecision(
          app.getApplicationRef(),
          app.getTenantId(),
          request,
          authAs("officer2", "BRANCH_MANAGER"));

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);

      verify(approvalStageRepository).save(any(ApprovalStage.class));
      verify(loanApplicationRepository).save(app);
    }
  }

  @Test
  @DisplayName("officer acting out of sequence throws ApprovalStageMismatchException")
  void wrongStageThrowsMismatch() {

    final LoanApplication app = underReviewApp();
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.APPROVE)
            .comments("Attempted approval")
            .build();
    assertThatThrownBy(
            () ->
                service.recordDecision(
                    app.getApplicationRef(),
                    app.getTenantId(),
                    request,
                    authAs("officer2", "BRANCH_MANAGER")))
        .isInstanceOf(ApprovalStageMismatchException.class);
  }

  @Test
  @DisplayName("authenticated staff with no LOS role throws LosRoleNotAssignedException")
  void noLosRoleThrows() {

    final LoanApplication app = underReviewApp();
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.APPROVE)
            .comments("Approval requested")
            .build();

    final Authentication noRoleAuth =
        new UsernamePasswordAuthenticationToken(
            "superuser", "n/a", List.of(new SimpleGrantedAuthority("ROLE_STAFF")));

    assertThatThrownBy(
            () ->
                service.recordDecision(
                    app.getApplicationRef(), app.getTenantId(), request, noRoleAuth))
        .isInstanceOf(LosRoleNotAssignedException.class);
  }

  @Test
  @DisplayName("REJECT decision moves application to REJECTED regardless of stage")
  void rejectMovesToRejected() {

    final LoanApplication app = underReviewApp();
    stubLookup(app);

    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.REJECT)
            .comments("Insufficient income documentation")
            .build();

    service.recordDecision(
        app.getApplicationRef(), app.getTenantId(), request, authAs("officer1", "LOAN_OFFICER"));

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);

    verify(approvalStageRepository).save(any(ApprovalStage.class));
    verify(loanApplicationRepository).save(app);
  }

  @Test
  @DisplayName("REFER decision moves application to REFERRED")
  void referMovesToReferred() {

    final LoanApplication app = underReviewApp();
    stubLookup(app);

    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.REFER)
            .comments("Need updated bank statement")
            .build();

    service.recordDecision(
        app.getApplicationRef(), app.getTenantId(), request, authAs("officer1", "LOAN_OFFICER"));

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);

    verify(approvalStageRepository).save(any(ApprovalStage.class));
    verify(loanApplicationRepository).save(app);
  }

  @Test
  @DisplayName("decision without comments throws")
  void decisionWithoutCommentsThrows() {

    final LoanApplication app = underReviewApp();
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder().decision(ApprovalDecision.APPROVE).build();

    assertThatThrownBy(
            () ->
                service.recordDecision(
                    app.getApplicationRef(),
                    app.getTenantId(),
                    request,
                    authAs("officer1", "LOAN_OFFICER")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("same officer acting twice on the same application throws")
  void duplicateOfficerThrows() {

    final LoanApplication app = underReviewApp();

    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    when(approvalStageRepository.existsByApplicationAndAssignedOfficer(app, "officer1"))
        .thenReturn(true);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.APPROVE)
            .comments("Duplicate approval attempt")
            .build();

    assertThatThrownBy(
            () ->
                service.recordDecision(
                    app.getApplicationRef(),
                    app.getTenantId(),
                    request,
                    authAs("officer1", "LOAN_OFFICER")))
        .isInstanceOf(DuplicateApprovalException.class);
  }
}
