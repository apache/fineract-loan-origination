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

package org.apache.fineract.los.exception;

/**
 * Centralised error codes and message constants for the Loan Origination Service.
 *
 * <p>All exception messages must reference constants from this class — no hardcoded strings in
 * business logic. This ensures consistent error responses across all layers and makes message
 * changes a single-point update.
 */
public final class LosErrorConstants {

  private LosErrorConstants() {
    // Utility class — prevent instantiation
  }

  // ─────────────────────────────────────────────────────────
  // Error Codes
  // ─────────────────────────────────────────────────────────

  /** Error code prefix for all LOS validation errors. */
  public static final String ERR_VALIDATION = "los.error.validation";

  /** Error code for invalid state transition attempts. */
  public static final String ERR_INVALID_TRANSITION = "los.error.state.invalid-transition";

  /** Error code for terminal state transition attempts. */
  public static final String ERR_TERMINAL_STATE = "los.error.state.terminal";

  /** Error code for null input violations. */
  public static final String ERR_NULL_INPUT = "los.error.input.null";

  /** Error code for uninitialised entity state. */
  public static final String ERR_UNINITIALISED_STATUS = "los.error.entity.uninitialised-status";

  /** Error code for application-not-found lookups. */
  public static final String ERR_APPLICATION_NOT_FOUND = "los.error.application.not-found";

  /** Error code for applicant-profile-not-found lookups. */
  public static final String ERR_PROFILE_NOT_FOUND = "los.error.profile.not-found";

  /** Error code for disbursement attempted on a non-APPROVED application. */
  public static final String ERR_DISBURSEMENT_NOT_ALLOWED = "los.error.disbursement.not-allowed";

  /** Error code for failures calling the Fineract core API. */
  public static final String ERR_FINERACT_INTEGRATION = "los.error.fineract.integration-failure";

  /** Error code for a decision recorded twice by the same officer on the same application. */
  public static final String ERR_DUPLICATE_APPROVAL = "los.error.approval.duplicate";

  /** Error code for an unrecognised approval workflow stage name. */
  public static final String ERR_UNKNOWN_STAGE = "los.error.approval.unknown-stage";

  /** Error code for an officer acting on the wrong workflow stage. */
  public static final String ERR_STAGE_MISMATCH = "los.error.approval.stage-mismatch";

  /** Error code for a staff member with no configured LOS workflow role. */
  public static final String ERR_NO_LOS_ROLE = "los.error.approval.no-los-role";

  /** Message when approval comments are missing. */
  public static final String MSG_APPROVAL_COMMENTS_REQUIRED =
      "Comments are mandatory when recording an approval decision.";

  // ─────────────────────────────────────────────────────────
  // State Machine Messages
  // ─────────────────────────────────────────────────────────

  /** Message when application argument is null. */
  public static final String MSG_APPLICATION_NULL = "Application must not be null";

  /** Message when target status argument is null. */
  public static final String MSG_TARGET_STATUS_NULL = "Target status must not be null";

  /** Message when application status is uninitialised. */
  public static final String MSG_STATUS_UNINITIALISED =
      "Application status must not be null — " + "ensure entity is properly initialised";

  // ─────────────────────────────────────────────────────────
  // Transition Message Templates
  // ─────────────────────────────────────────────────────────

  /** Template for invalid transition error message. Parameters: fromStatus, toStatus. */
  public static final String MSG_INVALID_TRANSITION_TEMPLATE =
      "Invalid state transition: cannot move from [%s] to [%s]. "
          + "Check LoanOriginationStateMachine "
          + "for valid transitions.";

  /** Template for terminal state error message. Parameters: applicationRef, currentStatus. */
  public static final String MSG_TERMINAL_STATE_TEMPLATE =
      "Application [%s] is in terminal state [%s]. " + "No further transitions are permitted.";

  // ─────────────────────────────────────────────────────────
  // Service Layer Message Templates
  // ─────────────────────────────────────────────────────────

  /** Template for application-not-found error message. Parameters: applicationRef, tenantId. */
  public static final String MSG_APPLICATION_NOT_FOUND_TEMPLATE =
      "No loan application found with reference [%s] for tenant [%s].";

  /** Template for profile-not-found error message. Parameters: applicationRef, tenantId. */
  public static final String MSG_PROFILE_NOT_FOUND_TEMPLATE =
      "No applicant profile found for application [%s] in tenant [%s].";

  /** Template for reference generation failure. Parameters: tenantId, attempts. */
  public static final String MSG_REFERENCE_GENERATION_FAILED_TEMPLATE =
      "Unable to generate a unique application reference for tenant [%s] "
          + "after %d attempts. This indicates unexpectedly high contention "
          + "or a reference-format collision — investigate before retrying.";

  /** Template for disbursement-not-allowed error. Parameters: applicationRef, currentStatus. */
  public static final String MSG_DISBURSEMENT_NOT_ALLOWED_TEMPLATE =
      "Cannot disburse application [%s] — current status is [%s] but "
          + "disbursement requires status APPROVED.";

  /** Message when the applicant profile is missing a Fineract clientId at disbursement time. */
  public static final String MSG_MISSING_FINERACT_CLIENT_ID_TEMPLATE =
      "Cannot disburse application [%s] — applicant profile has no "
          + "fineractClientId. The applicant must be linked to an existing "
          + "Fineract client before the disbursement bridge can run.";

  /**
   * Message when the application is missing a Fineract loan product mapping at disbursement time.
   */
  public static final String MSG_MISSING_LOAN_PRODUCT_TEMPLATE =
      "Cannot disburse application [%s] — no fineractLoanProductId is set. "
          + "Configure a product mapping for loan purpose [%s] before "
          + "approval.";

  /** Template for Fineract integration failure. Parameters: applicationRef, root cause message. */
  public static final String MSG_FINERACT_CALL_FAILED_TEMPLATE =
      "Call to Fineract POST /loans failed for application [%s]: %s";

  /** Template for duplicate approval decision. Parameters: applicationRef, assignedOfficer. */
  public static final String MSG_DUPLICATE_APPROVAL_TEMPLATE =
      "Officer [%s] has already recorded a decision on application [%s]. "
          + "The four-eyes principle requires a different officer for each "
          + "approval stage.";

  /** Template for unknown workflow stage. Parameters: stageName, configuredStages. */
  public static final String MSG_UNKNOWN_STAGE_TEMPLATE =
      "Stage [%s] is not a configured approval stage. Configured stages: %s.";

  /** Template for a decision recorded on an application that is not UNDER_REVIEW. */
  public static final String MSG_NOT_UNDER_REVIEW_TEMPLATE =
      "Cannot record an approval decision for application [%s] — current "
          + "status is [%s] but decisions may only be recorded while the "
          + "application is UNDER_REVIEW.";

  /**
   * Template for stage-mismatch error. Parameters: assignedOfficer, applicationRef, expectedStage.
   */
  public static final String MSG_STAGE_MISMATCH_TEMPLATE =
      "Officer [%s] cannot record a decision on application [%s] — the application "
          + "is currently awaiting a decision at stage [%s], which does not match "
          + "this officer's assigned workflow role.";

  /** Template for a staff member with no configured LOS workflow role. Parameters: username. */
  public static final String MSG_NO_LOS_ROLE_TEMPLATE =
      "User [%s] is authenticated but has no configured LOS workflow role. "
          + "Configure los.workflow.role-mapping to map the user's Fineract role "
          + "to one of the configured approval workflow stages before recording "
          + "approval decisions.";
}
