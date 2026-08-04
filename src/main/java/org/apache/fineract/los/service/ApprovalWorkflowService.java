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

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.exception.ApplicationNotFoundException;
import org.apache.fineract.los.exception.ApprovalStageMismatchException;
import org.apache.fineract.los.exception.DuplicateApprovalException;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.apache.fineract.los.exception.LosRoleNotAssignedException;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.security.LosRole;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Orchestrates the multi-stage approval workflow (LOAN_OFFICER → CREDIT_COMMITTEE → BRANCH_MANAGER,
 * configurable via {@link ApprovalWorkflowProperties}).
 *
 * <p>Both the workflow stage an application is currently awaiting a decision at, and the officer
 * recording that decision, are derived entirely server-side — from the count of prior APPROVE
 * decisions and from the authenticated {@link Authentication} principal respectively. Neither is
 * accepted from client input (see {@link ApprovalDecisionRequest}), which is what makes the
 * four-eyes / sequential-stage enforcement in {@link #validateStageMatchesRole} meaningful: a
 * caller cannot simply claim to be acting at a stage they haven't reached.
 *
 * <p>This service owns the mapping from an {@link ApprovalDecision} to the corresponding {@link
 * LoanApplicationStatus} transition, and is the <strong>only</strong> caller of {@link
 * LoanOriginationStateMachine} for decisions made during review.
 *
 * <p>Transition rules applied here:
 *
 * <ul>
 *   <li>REJECT — always terminal, regardless of stage: UNDER_REVIEW → REJECTED
 *   <li>REFER — always returns the application to the applicant: UNDER_REVIEW → REFERRED
 *   <li>APPROVE on a non-final configured stage — application remains UNDER_REVIEW, awaiting the
 *       next stage
 *   <li>APPROVE on the final configured stage — UNDER_REVIEW → APPROVED
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

  private final LoanApplicationRepository loanApplicationRepository;
  private final ApprovalStageRepository approvalStageRepository;
  private final LoanOriginationStateMachine stateMachine;
  private final ApprovalWorkflowProperties workflowProperties;

  /**
   * Records a single approval-stage decision and drives the application's lifecycle state via the
   * state machine accordingly.
   *
   * @param applicationRef human-readable application reference
   * @param tenantId institution identifier
   * @param request the decision payload (decision + optional comments only)
   * @param authentication the authenticated staff principal recording this decision
   * @return the persisted {@link ApprovalStage} record
   */
  @Transactional
  public ApprovalStage recordDecision(
      final String applicationRef,
      final String tenantId,
      final ApprovalDecisionRequest request,
      final Authentication authentication) {

    final LoanApplication application =
        loanApplicationRepository
            .findByApplicationRefAndTenantId(applicationRef, tenantId)
            .orElseThrow(() -> new ApplicationNotFoundException(applicationRef, tenantId));

    validateUnderReview(application);
    validateComments(request);

    final String currentStage = resolveCurrentStage(application);
    final String assignedOfficer = authentication.getName();
    final LosRole callerRole = resolveCallerRole(authentication, assignedOfficer);

    validateStageMatchesRole(application, currentStage, callerRole, assignedOfficer);
    validateNoDuplicateOfficerDecision(application, assignedOfficer);

    final ApprovalStage stage = new ApprovalStage();
    stage.setApplication(application);
    stage.setTenantId(tenantId);
    stage.setStageName(currentStage);
    stage.setAssignedOfficer(assignedOfficer);
    stage.setDecision(request.getDecision());
    stage.setComments(request.getComments());
    stage.setDecidedAt(LocalDateTime.now());

    final ApprovalStage savedStage = approvalStageRepository.save(stage);

    applyTransition(application, currentStage, request.getDecision());
    loanApplicationRepository.save(application);

    log.info(
        "Approval decision recorded: tenant={} applicationRef={} stage={} officer={} "
            + "decision={} newStatus={}",
        tenantId,
        applicationRef,
        currentStage,
        assignedOfficer,
        request.getDecision(),
        application.getStatus());

    return savedStage;
  }

  /**
   * Derives the workflow stage this application is currently awaiting a decision at.
   *
   * <p>The number of prior APPROVE decisions recorded is the zero-based index into the configured
   * stage sequence. REJECT ends the workflow entirely (terminal), and REFER returns the application
   * to the applicant without advancing the stage pointer — so a re-submitted, re-reviewed
   * application resumes at the same stage it was referred from.
   */
  private String resolveCurrentStage(final LoanApplication application) {
    final long approvedStagesCompleted =
        approvalStageRepository.countByApplicationAndDecision(
            application, ApprovalDecision.APPROVE);

    final List<String> stages = workflowProperties.getStages();
    final int index = (int) approvedStagesCompleted;

    if (index >= stages.size()) {
      // Defensive only — should be unreachable: the final APPROVE moves the application out of
      // UNDER_REVIEW, and validateUnderReview() above already rejects non-UNDER_REVIEW
      // applications.
      throw new IllegalStateException(
          String.format(
              "Application [%s] has %d recorded APPROVE decisions but only %d stages are "
                  + "configured, and it is still UNDER_REVIEW — workflow configuration or state "
                  + "machine invariant violated.",
              application.getApplicationRef(), approvedStagesCompleted, stages.size()));
    }

    return stages.get(index);
  }

  /**
   * Resolves the caller's LOS workflow role from their granted authorities.
   *
   * @throws LosRoleNotAssignedException if the authenticated principal has no LOS workflow role
   */
  private LosRole resolveCallerRole(final Authentication authentication, final String username) {
    return LosRole.fromAuthorities(authentication.getAuthorities())
        .orElseThrow(() -> new LosRoleNotAssignedException(username));
  }

  /**
   * Enforces that the caller's role matches the application's current stage — the sequential
   * workflow guarantee. A BRANCH_MANAGER cannot act while the application is still awaiting its
   * LOAN_OFFICER decision, and vice versa.
   */
  private void validateStageMatchesRole(
      final LoanApplication application,
      final String currentStage,
      final LosRole callerRole,
      final String assignedOfficer) {

    if (callerRole != LosRole.valueOf(currentStage)) {
      throw new ApprovalStageMismatchException(
          application.getApplicationRef(), currentStage, assignedOfficer);
    }
  }

  /**
   * Applies the correct state-machine transition for a given stage decision.
   *
   * <p>This method is the single place where {@link ApprovalDecision} is translated into a {@link
   * LoanApplicationStatus} — the only component in the codebase permitted to make that mapping.
   */
  private void applyTransition(
      final LoanApplication application, final String stageName, final ApprovalDecision decision) {

    switch (decision) {
      case REJECT -> stateMachine.transition(application, LoanApplicationStatus.REJECTED);
      case REFER -> stateMachine.transition(application, LoanApplicationStatus.REFERRED);
      case APPROVE -> {
        if (workflowProperties.isFinalStage(stageName)) {
          stateMachine.transition(application, LoanApplicationStatus.APPROVED);
        } else {
          log.info(
              "Stage [{}] approved for applicationRef={} — awaiting next stage, "
                  + "status remains UNDER_REVIEW.",
              stageName,
              application.getApplicationRef());
        }
      }
      default ->
          throw new IllegalStateException(
              String.format(
                  "Unhandled approval decision [%s] for applicationRef=%s — no state transition "
                      + "is defined for this decision type.",
                  decision, application.getApplicationRef()));
    }
  }

  private void validateUnderReview(final LoanApplication application) {
    if (application.getStatus() != LoanApplicationStatus.UNDER_REVIEW) {
      throw new IllegalStateException(
          String.format(
              LosErrorConstants.MSG_NOT_UNDER_REVIEW_TEMPLATE,
              application.getApplicationRef(),
              application.getStatus()));
    }
  }

  private void validateComments(final ApprovalDecisionRequest request) {
    if (!StringUtils.hasText(request.getComments())) {
      throw new IllegalArgumentException(LosErrorConstants.MSG_APPROVAL_COMMENTS_REQUIRED);
    }
  }

  private void validateNoDuplicateOfficerDecision(
      final LoanApplication application, final String assignedOfficer) {
    if (approvalStageRepository.existsByApplicationAndAssignedOfficer(
        application, assignedOfficer)) {
      throw new DuplicateApprovalException(application.getApplicationRef(), assignedOfficer);
    }
  }
}
