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

package org.apache.fineract.los.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.exception.GlobalExceptionHandler;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.service.ApprovalWorkflowService;
import org.apache.fineract.los.service.CreditScoringService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.statemachine.LoanStateTransitionValidator;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP-level tests for lifecycle actions on {@link LoanApplicationController}.
 *
 * <p>Uses the real service and state machine with mocked repositories, so the response status
 * reflects how {@link GlobalExceptionHandler} maps errors raised by the state machine.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationController lifecycle actions")
class LoanApplicationControllerTest {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String TENANT_ID = "default";
  private static final String APPLICATION_REF = "LOS-2026-00001";

  @Mock private LoanApplicationRepository loanApplicationRepository;
  @Mock private ApplicantProfileRepository applicantProfileRepository;
  @Mock private ApprovalStageRepository approvalStageRepository;
  @Mock private CreditScoringService creditScoringService;
  @Mock private ApprovalWorkflowService approvalWorkflowService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    final LoanOriginationStateMachine stateMachine =
        new LoanOriginationStateMachine(new LoanStateTransitionValidator());
    final LoanApplicationService loanApplicationService =
        new LoanApplicationService(
            loanApplicationRepository,
            applicantProfileRepository,
            stateMachine,
            creditScoringService,
            approvalStageRepository,
            new ApprovalWorkflowProperties());
    final LoanApplicationController controller =
        new LoanApplicationController(
            loanApplicationService, creditScoringService, approvalWorkflowService);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("Submitting an APPROVED application returns 409 and leaves it APPROVED")
  void submitApprovedApplicationReturnsConflict() throws Exception {
    final LoanApplication application = existingApplication(LoanApplicationStatus.APPROVED);

    mockMvc
        .perform(
            post("/api/v1/loan-applications/{ref}/submit", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message").value(containsString("[APPROVED] to [SUBMITTED]")));

    assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.APPROVED);
    verify(loanApplicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("Starting review on a DRAFT application returns 409 and does not score it")
  void startReviewOnDraftApplicationReturnsConflict() throws Exception {
    final LoanApplication application = existingApplication(LoanApplicationStatus.DRAFT);

    mockMvc
        .perform(
            post("/api/v1/loan-applications/{ref}/start-review", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value(containsString("[DRAFT] to [UNDER_REVIEW]")));

    assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.DRAFT);
    verify(loanApplicationRepository, never()).save(any());
    verify(creditScoringService, never()).computeAndPersist(any());
  }

  @Test
  @DisplayName("Submitting a DRAFT application still succeeds")
  void submitDraftApplicationSucceeds() throws Exception {
    final LoanApplication application = existingApplication(LoanApplicationStatus.DRAFT);
    when(loanApplicationRepository.save(any(LoanApplication.class))).then(returnsFirstArg());

    mockMvc
        .perform(
            post("/api/v1/loan-applications/{ref}/submit", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));

    assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.SUBMITTED);
  }

  private LoanApplication existingApplication(final LoanApplicationStatus status) {
    final LoanApplication application = new LoanApplication();
    application.setApplicationRef(APPLICATION_REF);
    application.setTenantId(TENANT_ID);
    application.setStatus(status);
    when(loanApplicationRepository.findByApplicationRefAndTenantId(APPLICATION_REF, TENANT_ID))
        .thenReturn(Optional.of(application));
    return application;
  }
}
