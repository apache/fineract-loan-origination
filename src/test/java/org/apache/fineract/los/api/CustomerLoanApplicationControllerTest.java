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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.CreditScore;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.exception.GlobalExceptionHandler;
import org.apache.fineract.los.repository.ApplicantProfileRepository;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.security.CustomerPrincipal;
import org.apache.fineract.los.service.CreditScoringService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.apache.fineract.los.statemachine.LoanOriginationStateMachine;
import org.apache.fineract.los.statemachine.LoanStateTransitionValidator;
import org.apache.fineract.los.workflow.ApprovalWorkflowProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP-level tests for ownership checks on {@link CustomerLoanApplicationController}.
 *
 * <p>A customer requesting an application owned by someone else must get the same 404 as for an
 * application that does not exist, so references belonging to other customers cannot be probed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerLoanApplicationController ownership checks")
class CustomerLoanApplicationControllerTest {

  private static final String BASE_PATH = "/api/v1/customer/loan-applications";
  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String TENANT_ID = "default";
  private static final String APPLICATION_REF = "LOS-2026-00001";
  private static final String MISSING_REF = "LOS-2026-09999";
  private static final Long OWNER_CLIENT_ID = 101L;
  private static final Long OTHER_CLIENT_ID = 202L;

  @Mock private LoanApplicationRepository loanApplicationRepository;
  @Mock private ApplicantProfileRepository applicantProfileRepository;
  @Mock private ApprovalStageRepository approvalStageRepository;
  @Mock private CreditScoringService creditScoringService;

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
    final CustomerLoanApplicationController controller =
        new CustomerLoanApplicationController(
            loanApplicationService, creditScoringService, approvalStageRepository);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Owner can view their own application")
  void ownerCanViewApplication() throws Exception {
    existingApplicationOwnedBy(OWNER_CLIENT_ID);
    loginAs(OWNER_CLIENT_ID);

    mockMvc
        .perform(get(BASE_PATH + "/{ref}", APPLICATION_REF).header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.applicationRef").value(APPLICATION_REF));
  }

  @Test
  @DisplayName("Another customer gets 404 and no application data when viewing it")
  void nonOwnerGetsNotFoundForApplication() throws Exception {
    existingApplicationOwnedBy(OWNER_CLIENT_ID);
    loginAs(OTHER_CLIENT_ID);

    mockMvc
        .perform(get(BASE_PATH + "/{ref}", APPLICATION_REF).header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.applicationRef").doesNotExist());
  }

  @Test
  @DisplayName("Owner can view their own credit score")
  void ownerCanViewCreditScore() throws Exception {
    final LoanApplication application = existingApplicationOwnedBy(OWNER_CLIENT_ID);
    final CreditScore creditScore = new CreditScore();
    creditScore.setScore(72);
    creditScore.setIncomeRatioScore(22);
    creditScore.setDebtBurdenScore(18);
    creditScore.setEmploymentScore(15);
    creditScore.setRepaymentHistoryScore(10);
    creditScore.setLoanPurposeScore(7);
    when(creditScoringService.findExistingScore(application)).thenReturn(Optional.of(creditScore));
    loginAs(OWNER_CLIENT_ID);

    mockMvc
        .perform(
            get(BASE_PATH + "/{ref}/credit-score", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(72));
  }

  @Test
  @DisplayName("Another customer gets 404 when viewing the credit score, and no score is read")
  void nonOwnerGetsNotFoundForCreditScore() throws Exception {
    existingApplicationOwnedBy(OWNER_CLIENT_ID);
    loginAs(OTHER_CLIENT_ID);

    mockMvc
        .perform(
            get(BASE_PATH + "/{ref}/credit-score", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.score").doesNotExist());

    verify(creditScoringService, never()).findExistingScore(any());
  }

  @Test
  @DisplayName("Another customer gets 404 when submitting it, and it stays a DRAFT")
  void nonOwnerCannotSubmitApplication() throws Exception {
    final LoanApplication application = existingApplicationOwnedBy(OWNER_CLIENT_ID);
    loginAs(OTHER_CLIENT_ID);

    mockMvc
        .perform(
            post(BASE_PATH + "/{ref}/submit", APPLICATION_REF).header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isNotFound());

    assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.DRAFT);
    verify(loanApplicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("Another customer's application looks the same as one that does not exist")
  void nonOwnerResponseMatchesMissingApplication() throws Exception {
    existingApplicationOwnedBy(OWNER_CLIENT_ID);
    when(loanApplicationRepository.findByApplicationRefAndTenantId(MISSING_REF, TENANT_ID))
        .thenReturn(Optional.empty());
    loginAs(OTHER_CLIENT_ID);

    final String nonOwnerMessage =
        mockMvc
            .perform(get(BASE_PATH + "/{ref}", APPLICATION_REF).header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final String missingMessage =
        mockMvc
            .perform(get(BASE_PATH + "/{ref}", MISSING_REF).header(TENANT_HEADER, TENANT_ID))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(messageWithoutRef(nonOwnerMessage, APPLICATION_REF))
        .isEqualTo(messageWithoutRef(missingMessage, MISSING_REF));
  }

  private LoanApplication existingApplicationOwnedBy(final Long clientId) {
    final LoanApplication application = new LoanApplication();
    application.setApplicationRef(APPLICATION_REF);
    application.setTenantId(TENANT_ID);
    application.setStatus(LoanApplicationStatus.DRAFT);

    final ApplicantProfile profile = new ApplicantProfile();
    profile.setFineractClientId(clientId);

    when(loanApplicationRepository.findByApplicationRefAndTenantId(APPLICATION_REF, TENANT_ID))
        .thenReturn(Optional.of(application));
    when(applicantProfileRepository.findByApplication(application))
        .thenReturn(Optional.of(profile));
    return application;
  }

  private void loginAs(final Long clientId) {
    final CustomerPrincipal principal =
        new CustomerPrincipal("customer-" + clientId, null, clientId, TENANT_ID, "Customer");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  /** Strips the per-request reference and timestamp so two error bodies can be compared. */
  private static String messageWithoutRef(final String body, final String applicationRef) {
    return body.replace(applicationRef, "<ref>").replaceAll("\"timestamp\":\"[^\"]*\"", "");
  }
}
