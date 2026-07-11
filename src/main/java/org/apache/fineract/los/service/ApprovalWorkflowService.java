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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.exception.DuplicateApprovalException;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Orchestrates the multi-stage approval workflow (LOAN_OFFICER → BRANCH_MANAGER → CREDIT_COMMITTEE,
 * configurable via {@link ApprovalWorkflowProperties}).
 *
 * <p>This service owns the mapping from an {@link ApprovalDecision} recorded on an {@link
 * ApprovalStage} to the corresponding {@link LoanApplicationStatus} transition, and is the
 * <strong>only</strong> caller of {@link LoanOriginationStateMachine} for decisions made during
 * review — matching the design contract that no code outside the state machine ever calls {@code
 * LoanApplication#setStatus} directly.
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
   * @param request the decision payload
   * @return the persisted {@link ApprovalStage} record
   */
  @Transactional
  public ApprovalStage recordDecision(
      final String applicationRef, final String tenantId, final ApprovalDecisionRequest request) {

    final LoanApplication application =
        loanApplicationRepository
            .findByApplicationRefAndTenantId(applicationRef, tenantId)
            .orElseThrow(
                () ->
                    new org.apache.fineract.los.exception.ApplicationNotFoundException(
                        applicationRef, tenantId));

    validateStageName(request.getStageName());
    validateUnderReview(application);
    validateCommentsForNegativeDecisions(request);
    validateNoDuplicateOfficerDecision(application, request.getAssignedOfficer());

    final ApprovalStage stage = new ApprovalStage();
    stage.setApplication(application);
    stage.setTenantId(tenantId);
    stage.setStageName(request.getStageName());
    stage.setAssignedOfficer(request.getAssignedOfficer());
    stage.setDecision(request.getDecision());
    stage.setComments(request.getComments());
    stage.setDecidedAt(LocalDateTime.now());

    final ApprovalStage savedStage = approvalStageRepository.save(stage);

    applyTransition(application, request.getStageName(), request.getDecision());
    loanApplicationRepository.save(application);

    log.info(
        "Approval decision recorded: applicationRef={} stage={} officer={} "
            + "decision={} newStatus={}",
        applicationRef,
        request.getStageName(),
        request.getAssignedOfficer(),
        request.getDecision(),
        application.getStatus());

    return savedStage;
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
      default -> throw new IllegalStateException("Unrecognised ApprovalDecision: " + decision);
    }
  }

  private void validateStageName(final String stageName) {
    if (!StringUtils.hasText(stageName) || workflowProperties.indexOf(stageName) < 0) {
      throw new IllegalArgumentException(
          String.format(
              LosErrorConstants.MSG_UNKNOWN_STAGE_TEMPLATE,
              stageName,
              workflowProperties.getStages()));
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

  private void validateCommentsForNegativeDecisions(final ApprovalDecisionRequest request) {
    final boolean requiresComments =
        request.getDecision() == ApprovalDecision.REJECT
            || request.getDecision() == ApprovalDecision.REFER;

    if (requiresComments && !StringUtils.hasText(request.getComments())) {
      throw new IllegalArgumentException(
          "Comments are mandatory when recording a REJECT or REFER decision.");
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
