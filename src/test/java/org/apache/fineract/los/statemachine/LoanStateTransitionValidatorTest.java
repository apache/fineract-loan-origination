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

import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link LoanStateTransitionValidator}.
 *
 * <p>Uses JUnit 5 parameterized tests to exhaustively verify the transition map without repetitive
 * boilerplate. Each {@code @CsvSource} row represents one transition and its expected validity.
 */
@DisplayName("LoanStateTransitionValidator")
class LoanStateTransitionValidatorTest {

  private LoanStateTransitionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new LoanStateTransitionValidator();
  }

  @ParameterizedTest(name = "{0} → {1} should be valid={2}")
  @DisplayName("Transition map — all cases")
  @CsvSource({
    // Valid transitions
    "DRAFT,        SUBMITTED,    true",
    "SUBMITTED,    UNDER_REVIEW, true",
    "UNDER_REVIEW, APPROVED,     true",
    "UNDER_REVIEW, REJECTED,     true",
    "UNDER_REVIEW, REFERRED,     true",
    "REFERRED,     SUBMITTED,    true",
    "REFERRED,     UNDER_REVIEW, true",
    "APPROVED,     DISBURSED,    true",
    // Invalid transitions — skip states
    "DRAFT,        UNDER_REVIEW, false",
    "DRAFT,        APPROVED,     false",
    "DRAFT,        REJECTED,     false",
    "DRAFT,        DISBURSED,    false",
    // Invalid transitions — backward
    "SUBMITTED,    DRAFT,        false",
    "UNDER_REVIEW, SUBMITTED,    false",
    "APPROVED,     UNDER_REVIEW, false",
    // Invalid transitions — terminal
    "REJECTED,     UNDER_REVIEW, false",
    "REJECTED,     SUBMITTED,    false",
    "DISBURSED,    APPROVED,     false",
    "DISBURSED,    DRAFT,        false",
  })
  void transitionMap(final String from, final String to, final boolean expected) {
    final LoanApplicationStatus fromStatus = LoanApplicationStatus.valueOf(from.trim());
    final LoanApplicationStatus toStatus = LoanApplicationStatus.valueOf(to.trim());
    assertThat(validator.isValid(fromStatus, toStatus)).isEqualTo(expected);
  }

  @Test
  @DisplayName("permittedTransitions for terminal REJECTED is empty")
  void permittedTransitionsRejectedEmpty() {
    assertThat(validator.permittedTransitions(LoanApplicationStatus.REJECTED)).isEmpty();
  }

  @Test
  @DisplayName("permittedTransitions for terminal DISBURSED is empty")
  void permittedTransitionsDisbursedEmpty() {
    assertThat(validator.permittedTransitions(LoanApplicationStatus.DISBURSED)).isEmpty();
  }

  @Test
  @DisplayName("permittedTransitions for UNDER_REVIEW has 3 options")
  void permittedTransitionsUnderReview() {
    assertThat(validator.permittedTransitions(LoanApplicationStatus.UNDER_REVIEW))
        .hasSize(3)
        .containsExactlyInAnyOrder(
            LoanApplicationStatus.APPROVED,
            LoanApplicationStatus.REJECTED,
            LoanApplicationStatus.REFERRED);
  }
}
