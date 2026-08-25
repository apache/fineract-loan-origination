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

package org.apache.fineract.los.repository;

import java.util.List;
import java.util.Optional;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ApprovalStage} entity.
 *
 * <p>ApprovalStage has a many-to-one relationship with {@link LoanApplication}. Each record is an
 * immutable audit entry for one officer decision at one workflow stage.
 *
 * <p>Results are always ordered by {@code createdAt} ascending to reconstruct the chronological
 * approval history.
 */
@Repository
public interface ApprovalStageRepository extends JpaRepository<ApprovalStage, Long> {

  /**
   * Returns the full approval history for an application in chronological order.
   *
   * <p>Used to render the approval timeline in the loan officer dashboard and audit reports.
   *
   * @param application the parent loan application
   * @return all stages ordered oldest to newest
   */
  List<ApprovalStage> findAllByApplicationOrderByCreatedAtAsc(LoanApplication application);

  /**
   * Returns all approval stages for an application at a specific named stage.
   *
   * <p>Used to check whether a particular stage (e.g. BRANCH_MANAGER) has already been acted upon.
   *
   * @param application the parent loan application
   * @param stageName name of the workflow stage
   * @return stages matching the name
   */
  List<ApprovalStage> findAllByApplicationAndStageName(
      LoanApplication application, String stageName);

  /**
   * Finds the most recent approval stage for an application.
   *
   * <p>Used by the workflow engine to determine which stage the application is currently at and
   * what action was last taken.
   *
   * @param application the parent loan application
   * @return the most recently created stage if present
   */
  Optional<ApprovalStage> findFirstByApplicationOrderByCreatedAtDesc(LoanApplication application);

  /**
   * Returns all stages with a specific decision for an application.
   *
   * <p>Used to check if any REJECT decision exists in the history — which would block re-approval.
   *
   * @param application the parent loan application
   * @param decision the decision to filter by
   * @return stages with the given decision
   */
  List<ApprovalStage> findAllByApplicationAndDecision(
      LoanApplication application, ApprovalDecision decision);

  /**
   * Checks whether an approval stage exists for a specific officer on a specific application at a
   * specific stage name.
   *
   * <p>Used to prevent the same officer from deciding twice on the same stage in the same review
   * cycle — while allowing them to act again after a referral resubmission at a different cycle.
   *
   * @param application the parent loan application
   * @param assignedOfficer officer identifier
   * @param stageName the workflow stage name
   * @return true if this officer has already decided at this stage
   */
  boolean existsByApplicationAndAssignedOfficerAndStageName(
      LoanApplication application, String assignedOfficer, String stageName);

  /**
   * Checks whether an approval stage exists for a specific officer on a specific application.
   *
   * <p>Used to prevent the same officer from approving multiple stages of the same application —
   * four-eyes principle enforcement.
   *
   * @param application the parent loan application
   * @param assignedOfficer officer identifier
   * @return true if this officer has already acted
   */
  boolean existsByApplicationAndAssignedOfficer(
      LoanApplication application, String assignedOfficer);

  /**
   * Counts total approval stages for an application.
   *
   * <p>Used for reporting and audit purposes.
   *
   * @param application the parent loan application
   * @return total number of approval stage records
   */
  long countByApplication(LoanApplication application);

  /**
   * Counts approval-stage records with a specific decision for an application.
   *
   * <p>Used by {@code ApprovalWorkflowService} to derive the application's current workflow stage —
   * the number of APPROVE decisions recorded so far is the zero-based index into {@code
   * ApprovalWorkflowProperties#getStages()} for the next stage awaiting a decision.
   *
   * @param application the parent loan application
   * @param decision the decision to count
   * @return count of stage records with the given decision
   */
  long countByApplicationAndDecision(LoanApplication application, ApprovalDecision decision);
}
