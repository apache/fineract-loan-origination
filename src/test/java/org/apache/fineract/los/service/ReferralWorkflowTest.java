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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.statemachine.LoanStateTransitionValidator;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Tests for referral workflow behavior, focusing on approval stage preservation when applications
 * are referred back to customers and then resubmitted.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Referral Workflow")
class ReferralWorkflowTest {

  @Mock private LoanApplicationRepository loanApplicationRepository;
  @Mock private ApprovalStageRepository approvalStageRepository;

  private ApprovalWorkflowService approvalWorkflowService;

  @BeforeEach
  void setUp() {
    final ApprovalWorkflowProperties workflowProperties = new ApprovalWorkflowProperties();
    workflowProperties.setStages(List.of("LOAN_OFFICER", "CREDIT_COMMITTEE", "BRANCH_MANAGER"));

    final LoanOriginationStateMachine stateMachine =
        new LoanOriginationStateMachine(new LoanStateTransitionValidator());

    approvalWorkflowService =
        new ApprovalWorkflowService(
            loanApplicationRepository, approvalStageRepository, stateMachine, workflowProperties);
  }

  private LoanApplication createApplication() {
    final LoanApplication app = new LoanApplication();
    app.setApplicationRef("LOS-2026-00001");
    app.setTenantId("default");
    app.setStatus(LoanApplicationStatus.SUBMITTED);
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

    when(loanApplicationRepository.save(app)).thenReturn(app);

    when(approvalStageRepository.save(any(ApprovalStage.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("referral after first stage approval preserves stage on resubmission")
  void referralAfterFirstStagePreservesStage() {
    final LoanApplication app = createApplication();
    stubLookup(app);

    // Step 1: Loan officer approves (stage 1)
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    approvalWorkflowService.recordDecision(
        app.getApplicationRef(),
        app.getTenantId(),
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.APPROVE)
            .comments("Loan officer approved")
            .build(),
        authAs("officer1", "LOAN_OFFICER"));

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);

    // Step 2: Credit committee refers back
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(1L);

    approvalWorkflowService.recordDecision(
        app.getApplicationRef(),
        app.getTenantId(),
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.REFER)
            .comments("Need updated employment verification")
            .build(),
        authAs("officer2", "CREDIT_COMMITTEE"));

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);

    // Step 3: After customer resubmits, verify current stage is still CREDIT_COMMITTEE (stage 2)
    // because we already have 1 APPROVE decision
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(1L);

    final String currentStage = approvalWorkflowService.getCurrentStageOrNull(app);
    assertThat(currentStage).isEqualTo("CREDIT_COMMITTEE");

    // Verify that both approval and referral decisions were saved
    verify(approvalStageRepository, times(2)).save(any(ApprovalStage.class));
  }

  @Test
  @DisplayName("referral at second stage preserves both previous approvals")
  void referralAtSecondStagePreservesPreviousApprovals() {
    final LoanApplication app = createApplication();
    stubLookup(app);

    // Simulate: Loan officer approved, credit committee approved
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(2L);

    // Branch manager refers
    approvalWorkflowService.recordDecision(
        app.getApplicationRef(),
        app.getTenantId(),
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.REFER)
            .comments("Need additional collateral documentation")
            .build(),
        authAs("officer3", "BRANCH_MANAGER"));

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);

    // After customer resubmits, verify current stage is BRANCH_MANAGER (final stage)
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(2L);

    final String currentStage = approvalWorkflowService.getCurrentStageOrNull(app);
    assertThat(currentStage).isEqualTo("BRANCH_MANAGER");
  }

  @Test
  @DisplayName("referral before any approval starts from first stage on resubmission")
  void referralBeforeAnyApprovalStartsFromFirstStage() {
    final LoanApplication app = createApplication();
    stubLookup(app);

    // Loan officer refers immediately without approval
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    approvalWorkflowService.recordDecision(
        app.getApplicationRef(),
        app.getTenantId(),
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.REFER)
            .comments("Incomplete application")
            .build(),
        authAs("officer1", "LOAN_OFFICER"));

    assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);

    // After customer resubmits, verify current stage is still LOAN_OFFICER (first stage)
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);

    final String currentStage = approvalWorkflowService.getCurrentStageOrNull(app);
    assertThat(currentStage).isEqualTo("LOAN_OFFICER");
  }

  @Test
  @DisplayName("getCurrentStageOrNull returns null for non-UNDER_REVIEW applications")
  void getCurrentStageOrNullReturnsNullForNonUnderReview() {
    final LoanApplication app = createApplication();
    app.setStatus(LoanApplicationStatus.APPROVED);

    final String currentStage = approvalWorkflowService.getCurrentStageOrNull(app);
    assertThat(currentStage).isNull();
  }

  @Test
  @DisplayName("getCurrentStageOrNull calculates correct stage based on approval count")
  void getCurrentStageOrNullCalculatesCorrectStage() {
    final LoanApplication app = createApplication();
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);

    // No approvals yet - first stage
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(0L);
    assertThat(approvalWorkflowService.getCurrentStageOrNull(app)).isEqualTo("LOAN_OFFICER");

    // One approval - second stage
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(1L);
    assertThat(approvalWorkflowService.getCurrentStageOrNull(app)).isEqualTo("CREDIT_COMMITTEE");

    // Two approvals - third stage
    when(approvalStageRepository.countByApplicationAndDecision(app, ApprovalDecision.APPROVE))
        .thenReturn(2L);
    assertThat(approvalWorkflowService.getCurrentStageOrNull(app)).isEqualTo("BRANCH_MANAGER");
  }
}
