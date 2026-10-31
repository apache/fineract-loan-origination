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

package org.apache.fineract.los.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LoanOriginationStateMachine}.
 *
 * Tests are structured using nested classes grouped by starting state — each nested class sets
 * up an application in that state and tests all valid and invalid transitions from it.
 *
 * No Spring context is loaded — pure unit tests using plain Java instantiation. All tests run in
 * milliseconds.
 *
 * Coverage targets:
 *
 * 
 *   All 7 valid transitions — must succeed
 *   All representative invalid transitions — must throw
 *   Terminal state guard — must throw on any attempt
 *   Null input guards — must throw IllegalArgumentException
 * 
 */
@DisplayName("LoanOriginationStateMachine")
class LoanOriginationStateMachineTest {

  private LoanOriginationStateMachine stateMachine;

  @BeforeEach
  void setUp() {
    final LoanStateTransitionValidator validator = new LoanStateTransitionValidator();
    stateMachine = new LoanOriginationStateMachine(validator);
  }

  // ─────────────────────────────────────────────────────────
  // Helper
  // ─────────────────────────────────────────────────────────

  /**
   * Creates a minimal LoanApplication in the given status. Only fields required by the state
   * machine are populated.
   */
  private LoanApplication applicationInStatus(final LoanApplicationStatus status) {
    final LoanApplication app = new LoanApplication();
    app.setApplicationRef("LOS-TEST-00001");
    app.setTenantId("test-tenant");
    app.setStatus(status);
    return app;
  }

  // ─────────────────────────────────────────────────────────
  // Valid Transitions
  // ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Valid transitions")
  class ValidTransitions {

    @Test
    @DisplayName("DRAFT → SUBMITTED")
    void draftToSubmitted() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      stateMachine.transition(app, LoanApplicationStatus.SUBMITTED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("SUBMITTED → UNDER_REVIEW")
    void submittedToUnderReview() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.SUBMITTED);
      stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("UNDER_REVIEW → APPROVED")
    void underReviewToApproved() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.UNDER_REVIEW);
      stateMachine.transition(app, LoanApplicationStatus.APPROVED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("UNDER_REVIEW → REJECTED")
    void underReviewToRejected() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.UNDER_REVIEW);
      stateMachine.transition(app, LoanApplicationStatus.REJECTED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);
    }

    @Test
    @DisplayName("UNDER_REVIEW → REFERRED")
    void underReviewToReferred() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.UNDER_REVIEW);
      stateMachine.transition(app, LoanApplicationStatus.REFERRED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);
    }

    @Test
    @DisplayName("REFERRED → UNDER_REVIEW")
    void referredToUnderReview() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.REFERRED);
      stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("APPROVED → DISBURSED")
    void approvedToDisbursed() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.APPROVED);
      stateMachine.transition(app, LoanApplicationStatus.DISBURSED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.DISBURSED);
    }
  }

  // ─────────────────────────────────────────────────────────
  // Invalid Transitions
  // ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Invalid transitions — must throw LoanStateTransitionException")
  class InvalidTransitions {

    @Test
    @DisplayName("DRAFT → DISBURSED is not permitted")
    void draftToDisbursed() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.DISBURSED))
          .isInstanceOf(LoanStateTransitionException.class)
          .hasMessageContaining("DRAFT")
          .hasMessageContaining("DISBURSED");
    }

    @Test
    @DisplayName("DRAFT → APPROVED is not permitted")
    void draftToApproved() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.APPROVED))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("DRAFT → UNDER_REVIEW is not permitted")
    void draftToUnderReview() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("SUBMITTED → APPROVED is not permitted")
    void submittedToApproved() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.SUBMITTED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.APPROVED))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("SUBMITTED → DISBURSED is not permitted")
    void submittedToDisbursed() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.SUBMITTED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.DISBURSED))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("APPROVED → REJECTED is not permitted")
    void approvedToRejected() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.APPROVED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.REJECTED))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("APPROVED → UNDER_REVIEW is not permitted")
    void approvedToUnderReview() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.APPROVED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("REFERRED → APPROVED is not permitted")
    void referredToApproved() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.REFERRED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.APPROVED))
          .isInstanceOf(LoanStateTransitionException.class);
    }

    @Test
    @DisplayName("REFERRED → DISBURSED is not permitted")
    void referredToDisbursed() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.REFERRED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.DISBURSED))
          .isInstanceOf(LoanStateTransitionException.class);
    }
  }

  // ─────────────────────────────────────────────────────────
  // Terminal State Guard
  // ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Terminal state guard")
  class TerminalStateGuard {

    @Test
    @DisplayName("REJECTED → any status throws IllegalStateException")
    void rejectedIsTerminal() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.REJECTED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("terminal");
    }

    @Test
    @DisplayName("DISBURSED → any status throws IllegalStateException")
    void disbursedIsTerminal() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DISBURSED);
      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.APPROVED))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("terminal");
    }

    @Test
    @DisplayName("isTerminal returns true for REJECTED")
    void isTerminalRejected() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.REJECTED);
      assertThat(stateMachine.isTerminal(app)).isTrue();
    }

    @Test
    @DisplayName("isTerminal returns true for DISBURSED")
    void isTerminalDisbursed() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DISBURSED);
      assertThat(stateMachine.isTerminal(app)).isTrue();
    }

    @Test
    @DisplayName("isTerminal returns false for DRAFT")
    void isTerminalDraft() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThat(stateMachine.isTerminal(app)).isFalse();
    }

    @Test
    @DisplayName("isTerminal returns false for APPROVED")
    void isTerminalApproved() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.APPROVED);
      assertThat(stateMachine.isTerminal(app)).isFalse();
    }
  }

  // ─────────────────────────────────────────────────────────
  // Permitted Transitions Query
  // ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("permittedTransitions query")
  class PermittedTransitionsQuery {

    @Test
    @DisplayName("DRAFT permits only SUBMITTED")
    void draftPermittedTransitions() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThat(stateMachine.permittedTransitions(app))
          .containsExactly(LoanApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("UNDER_REVIEW permits APPROVED, REJECTED, REFERRED")
    void underReviewPermittedTransitions() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.UNDER_REVIEW);
      assertThat(stateMachine.permittedTransitions(app))
          .containsExactlyInAnyOrder(
              LoanApplicationStatus.APPROVED,
              LoanApplicationStatus.REJECTED,
              LoanApplicationStatus.REFERRED);
    }

    @Test
    @DisplayName("REJECTED permits nothing — terminal state")
    void rejectedPermitsNothing() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.REJECTED);
      assertThat(stateMachine.permittedTransitions(app)).isEmpty();
    }

    @Test
    @DisplayName("DISBURSED permits nothing — terminal state")
    void disbursedPermitsNothing() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DISBURSED);
      assertThat(stateMachine.permittedTransitions(app)).isEmpty();
    }
  }

  // ─────────────────────────────────────────────────────────
  // Null Input Guards
  // ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Null input guards")
  class NullInputGuards {

    @Test
    @DisplayName("null application throws IllegalArgumentException")
    void nullApplicationThrows() {
      assertThatThrownBy(() -> stateMachine.transition(null, LoanApplicationStatus.SUBMITTED))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Application must not be null");
    }

    @Test
    @DisplayName("null targetStatus throws IllegalArgumentException")
    void nullTargetStatusThrows() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThatThrownBy(() -> stateMachine.transition(app, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Target status must not be null");
    }

    @Test
    @DisplayName("null application in permittedTransitions throws")
    void nullApplicationInPermittedTransitions() {
      assertThatThrownBy(() -> stateMachine.permittedTransitions(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null application in isTerminal throws")
    void nullApplicationInIsTerminal() {
      assertThatThrownBy(() -> stateMachine.isTerminal(null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // ─────────────────────────────────────────────────────────
  // Status Mutation Verification
  // ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Status mutation verification")
  class StatusMutationVerification {

    @Test
    @DisplayName("Status is mutated in place on the entity")
    void statusMutatedInPlace() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.DRAFT);

      stateMachine.transition(app, LoanApplicationStatus.SUBMITTED);

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Failed transition does not mutate status")
    void failedTransitionDoesNotMutate() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);

      assertThatThrownBy(() -> stateMachine.transition(app, LoanApplicationStatus.DISBURSED))
          .isInstanceOf(LoanStateTransitionException.class);

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.DRAFT);
    }

    @Test
    @DisplayName("Multiple sequential valid transitions work correctly")
    void multipleSequentialTransitions() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.DRAFT);

      stateMachine.transition(app, LoanApplicationStatus.SUBMITTED);
      stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW);
      stateMachine.transition(app, LoanApplicationStatus.APPROVED);
      stateMachine.transition(app, LoanApplicationStatus.DISBURSED);

      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.DISBURSED);
      assertThat(stateMachine.isTerminal(app)).isTrue();
    }

    @Test
    @DisplayName("REFER and re-review cycle works correctly")
    void referAndReReviewCycle() {
      final LoanApplication app = applicationInStatus(LoanApplicationStatus.UNDER_REVIEW);

      stateMachine.transition(app, LoanApplicationStatus.REFERRED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.REFERRED);

      stateMachine.transition(app, LoanApplicationStatus.UNDER_REVIEW);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.UNDER_REVIEW);

      stateMachine.transition(app, LoanApplicationStatus.APPROVED);
      assertThat(app.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);
    }
  }
}
