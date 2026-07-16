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
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.exception.DuplicateApprovalException;
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
    workflowProperties.setStages(java.util.List.of("LOAN_OFFICER", "BRANCH_MANAGER"));

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
    @DisplayName("approving a non-final stage keeps status UNDER_REVIEW")
    void approvingNonFinalStageStaysUnderReview() {
      final LoanApplication app = underReviewApp();
      stubLookup(app);

      final ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .stageName("LOAN_OFFICER")
              .assignedOfficer("officer1")
              .decision(ApprovalDecision.APPROVE)
              .build();

      service.recordDecision(app.getApplicationRef(), app.getTenantId(), request);

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("approving the final configured stage moves to APPROVED")
    void approvingFinalStageMovesToApproved() {
      final LoanApplication app = underReviewApp();
      stubLookup(app);

      final ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .stageName("BRANCH_MANAGER")
              .assignedOfficer("officer2")
              .decision(ApprovalDecision.APPROVE)
              .build();

      service.recordDecision(app.getApplicationRef(), app.getTenantId(), request);

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);
    }
  }

  @Test
  @DisplayName("REJECT decision moves application to REJECTED regardless of stage")
  void rejectMovesToRejected() {
    final LoanApplication app = underReviewApp();
    stubLookup(app);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .stageName("LOAN_OFFICER")
            .assignedOfficer("officer1")
            .decision(ApprovalDecision.REJECT)
            .comments("Insufficient income documentation")
            .build();

    service.recordDecision(app.getApplicationRef(), app.getTenantId(), request);

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);
  }

  @Test
  @DisplayName("REFER decision moves application to REFERRED")
  void referMovesToReferred() {
    final LoanApplication app = underReviewApp();
    stubLookup(app);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .stageName("LOAN_OFFICER")
            .assignedOfficer("officer1")
            .decision(ApprovalDecision.REFER)
            .comments("Need updated bank statement")
            .build();

    service.recordDecision(app.getApplicationRef(), app.getTenantId(), request);

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);
  }

  @Test
  @DisplayName("REJECT without comments throws")
  void rejectWithoutCommentsThrows() {
    final LoanApplication app = underReviewApp();
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .stageName("LOAN_OFFICER")
            .assignedOfficer("officer1")
            .decision(ApprovalDecision.REJECT)
            .build();

    assertThatThrownBy(
            () -> service.recordDecision(app.getApplicationRef(), app.getTenantId(), request))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("same officer acting twice on the same application throws")
  void duplicateOfficerThrows() {
    final LoanApplication app = underReviewApp();
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));
    when(approvalStageRepository.existsByApplicationAndAssignedOfficer(app, "officer1"))
        .thenReturn(true);

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .stageName("LOAN_OFFICER")
            .assignedOfficer("officer1")
            .decision(ApprovalDecision.APPROVE)
            .build();

    assertThatThrownBy(
            () -> service.recordDecision(app.getApplicationRef(), app.getTenantId(), request))
        .isInstanceOf(DuplicateApprovalException.class);
  }

  @Test
  @DisplayName("unknown stage name throws")
  void unknownStageThrows() {
    final LoanApplication app = underReviewApp();
    when(loanApplicationRepository.findByApplicationRefAndTenantId(
            app.getApplicationRef(), app.getTenantId()))
        .thenReturn(Optional.of(app));

    final ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .stageName("REGIONAL_DIRECTOR")
            .assignedOfficer("officer1")
            .decision(ApprovalDecision.APPROVE)
            .build();

    assertThatThrownBy(
            () -> service.recordDecision(app.getApplicationRef(), app.getTenantId(), request))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
