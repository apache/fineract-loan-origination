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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.apache.fineract.los.api.ApprovalController;
import org.apache.fineract.los.api.CustomerLoanApplicationController;
import org.apache.fineract.los.api.LoanApplicationController;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.CreateLoanApplicationRequest;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.service.ApprovalWorkflowService;
import org.apache.fineract.los.service.CreditScoringService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.apache.fineract.los.statemachine.LoanStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private static final String APPLICATIONS = "/api/v1/loan-applications";
  private static final String APPLICATION_REF = "LOS-2026-00001";
  private static final String VALID_REQUEST =
      """
      {
        "requestedAmount": 10000,
        "currency": "USD",
        "loanPurpose": "BUSINESS",
        "tenorMonths": 12,
        "fineractLoanProductId": 1,
        "applicant": {
          "fullName": "Test Applicant",
          "nationalId": "TEST-001",
          "monthlyIncome": 5000
        }
      }
      """;

  @Mock private LoanApplicationService loanApplicationService;
  @Mock private CreditScoringService creditScoringService;
  @Mock private ApprovalWorkflowService approvalWorkflowService;
  @Mock private ApprovalStageRepository approvalStageRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new LoanApplicationController(
                    loanApplicationService, creditScoringService, approvalWorkflowService),
                new CustomerLoanApplicationController(
                    loanApplicationService, creditScoringService, approvalStageRepository),
                new ApprovalController(
                    approvalWorkflowService, approvalStageRepository, loanApplicationService),
                new InvalidReturnValueController())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @ParameterizedTest(name = "{index}: {1}")
  @MethodSource("invalidLoanRequests")
  void beanValidationReturnsBadRequest(final String body, final String expectedMessage)
      throws Exception {
    expectBadRequest(APPLICATIONS, body)
        .andExpect(jsonPath("$.message", containsString(expectedMessage)))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(MethodArgumentNotValidException.class));

    verifyNoInteractions(loanApplicationService);
  }

  static Stream<Arguments> invalidLoanRequests() {
    return Stream.of(
        invalidLoanRequest("\"requestedAmount\": 10000,", "", "requestedAmount: must not be null"),
        invalidLoanRequest("10000", "null", "requestedAmount: must not be null"),
        invalidLoanRequest(
            "10000", "-1", "requestedAmount: requestedAmount must be greater than zero"),
        invalidLoanRequest(
            "10000", "0", "requestedAmount: requestedAmount must be greater than zero"),
        invalidLoanRequest("\"USD\"", "null", "currency: currency is required"),
        invalidLoanRequest("\"USD\"", "\"\"", "currency: currency is required"),
        invalidLoanRequest("USD", "US", "currency: currency must be a 3-letter ISO 4217 code"),
        invalidLoanRequest("USD", "usd", "currency: currency must contain only uppercase letters"),
        invalidLoanRequest(
            "\"tenorMonths\": 12", "\"tenorMonths\": null", "tenorMonths: tenorMonths is required"),
        invalidLoanRequest(
            "\"tenorMonths\": 12",
            "\"tenorMonths\": 0",
            "tenorMonths: tenorMonths must be at least 1"),
        invalidLoanRequest(
            "\"fullName\": \"Test Applicant\",", "", "applicant.fullName: fullName is required"),
        invalidLoanRequest(
            "\"Test Applicant\"", "null", "applicant.fullName: fullName is required"),
        invalidLoanRequest("Test Applicant", " ", "applicant.fullName: fullName is required"),
        invalidLoanRequest(
            "Test Applicant",
            "12345",
            "applicant.fullName: fullName must contain only letters, spaces, hyphens, and apostrophes"),
        invalidLoanRequest(
            "Test Applicant",
            "A".repeat(101),
            "applicant.fullName: fullName must be between 2 and 100 characters"),
        invalidLoanRequest(
            "TEST-001",
            "A".repeat(51),
            "applicant.nationalId: nationalId must not exceed 50 characters"),
        invalidLoanRequest(
            "5000", "-1", "applicant.monthlyIncome: monthlyIncome must be zero or positive"),
        Arguments.of(
            """
            {"requestedAmount": 10000, "currency": "USD", "tenorMonths": 12}
            """,
            "applicant: must not be null"),
        Arguments.of(
            """
            {"requestedAmount": 10000, "currency": "USD", "tenorMonths": 12, "applicant": null}
            """,
            "applicant: must not be null"));
  }

  private static Arguments invalidLoanRequest(
      final String original, final String replacement, final String expectedMessage) {
    return Arguments.of(VALID_REQUEST.replace(original, replacement), expectedMessage);
  }

  @Test
  void reportsAllInvalidFields() throws Exception {
    expectBadRequest(APPLICATIONS, "{}")
        .andExpect(jsonPath("$.message", containsString("requestedAmount: must not be null")))
        .andExpect(jsonPath("$.message", containsString("currency: currency is required")))
        .andExpect(jsonPath("$.message", containsString("tenorMonths: tenorMonths is required")))
        .andExpect(jsonPath("$.message", containsString("applicant: must not be null")));

    verifyNoInteractions(loanApplicationService);
  }

  @ParameterizedTest
  @MethodSource("unreadableRequests")
  void unreadableBodyReturnsBadRequest(final String body) throws Exception {
    expectBadRequest(APPLICATIONS, body)
        .andExpect(jsonPath("$.message").value("Request body is missing or contains invalid JSON."))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(HttpMessageNotReadableException.class));

    verifyNoInteractions(loanApplicationService);
  }

  static Stream<String> unreadableRequests() {
    return Stream.of(
        VALID_REQUEST.replace(
            "\"fineractLoanProductId\": 1", "\"fineractLoanProductId\": \"invalid\""),
        VALID_REQUEST.replace(
            "\"fineractLoanProductId\": 1", "\"fineractLoanProductId\": 9223372036854775808"),
        VALID_REQUEST.replace("10000", "\"invalid\""),
        VALID_REQUEST.replace("\"tenorMonths\": 12", "\"tenorMonths\": \"invalid\""),
        "{\"requestedAmount\":",
        "[]",
        "null",
        "");
  }

  @Test
  void customerRequestIsRejectedBeforeServiceInvocation() throws Exception {
    expectBadRequest("/api/v1/customer/loan-applications", "{}")
        .andExpect(jsonPath("$.message", containsString("requestedAmount: must not be null")));

    verifyNoInteractions(loanApplicationService);
  }

  @Test
  void methodValidationIncludesNestedBodyErrors() throws Exception {
    expectBadRequest(
            APPLICATIONS + "/" + APPLICATION_REF + "/approval-decisions",
            """
            {"decision": "APPROVE", "comments": "short"}
            """)
        .andExpect(
            jsonPath(
                "$.message",
                containsString("comments: comments must be between 10 and 2000 characters")))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(HandlerMethodValidationException.class));

    verifyNoInteractions(approvalWorkflowService, loanApplicationService, approvalStageRepository);
  }

  @Test
  void methodValidationIncludesInvalidParameter() throws Exception {
    mockMvc
        .perform(get(APPLICATIONS + "/invalid-reference"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(
            jsonPath(
                "$.message", containsString("applicationRef: applicationRef must follow format")))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(HandlerMethodValidationException.class));

    verifyNoInteractions(loanApplicationService);
  }

  @Test
  void validRequestStillCreatesApplication() throws Exception {
    final LoanApplication application = new LoanApplication();
    application.setApplicationRef(APPLICATION_REF);
    application.setStatus(LoanApplicationStatus.DRAFT);
    application.setRequestedAmount(new BigDecimal("10000"));
    application.setCurrency("USD");
    when(loanApplicationService.createApplication(
            any(CreateLoanApplicationRequest.class), eq("default")))
        .thenReturn(application);

    mockMvc
        .perform(post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.applicationRef").value(APPLICATION_REF))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.requestedAmount").value(10000))
        .andExpect(jsonPath("$.currency").value("USD"));

    final ArgumentCaptor<CreateLoanApplicationRequest> request =
        ArgumentCaptor.forClass(CreateLoanApplicationRequest.class);
    verify(loanApplicationService).createApplication(request.capture(), eq("default"));
    assertThat(request.getValue().getRequestedAmount()).isEqualByComparingTo("10000");
    assertThat(request.getValue().getFineractLoanProductId()).isEqualTo(1L);
    assertThat(request.getValue().getApplicant().getFullName()).isEqualTo("Test Applicant");
  }

  @Test
  void unexpectedServiceFailureRemainsInternalServerError() throws Exception {
    when(loanApplicationService.createApplication(
            any(CreateLoanApplicationRequest.class), eq("default")))
        .thenThrow(new NullPointerException("Internal implementation detail"));

    mockMvc
        .perform(post(APPLICATIONS).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
  }

  @Test
  void returnValueValidationRemainsInternalServerError() throws Exception {
    mockMvc
        .perform(get("/test/invalid-return-value"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
        .andExpect(
            result ->
                assertThat(result.getResolvedException())
                    .isInstanceOf(HandlerMethodValidationException.class));
  }

  @Test
  void missingApplicationRemainsNotFound() throws Exception {
    final ApplicationNotFoundException exception =
        new ApplicationNotFoundException(APPLICATION_REF, "default");
    when(loanApplicationService.getApplicationOrThrow(APPLICATION_REF, "default"))
        .thenThrow(exception);

    mockMvc
        .perform(get(APPLICATIONS + "/" + APPLICATION_REF))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value(exception.getMessage()));
  }

  @ParameterizedTest
  @EnumSource(
      value = HttpStatus.class,
      names = {"NOT_FOUND", "UNAUTHORIZED", "FORBIDDEN", "CONFLICT"})
  void responseStatusIsPreserved(final HttpStatus expectedStatus) throws Exception {
    when(loanApplicationService.getApplicationOrThrow(APPLICATION_REF, "default"))
        .thenThrow(new ResponseStatusException(expectedStatus, "Existing error reason"));

    mockMvc
        .perform(get(APPLICATIONS + "/" + APPLICATION_REF))
        .andExpect(status().is(expectedStatus.value()))
        .andExpect(jsonPath("$.status").value(expectedStatus.value()))
        .andExpect(jsonPath("$.error").value(expectedStatus.getReasonPhrase()))
        .andExpect(jsonPath("$.message").value("Existing error reason"));
  }

  @Test
  @DisplayName("LoanStateTransitionException maps to 409 Conflict with the transition message")
  void invalidStateTransitionReturnsConflict() {
    final LoanStateTransitionException ex =
        new LoanStateTransitionException(
            LoanApplicationStatus.APPROVED, LoanApplicationStatus.SUBMITTED);

    final ResponseEntity<ErrorResponse> response =
        new GlobalExceptionHandler().handleInvalidStateTransition(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(response.getBody().getError()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
    assertThat(response.getBody().getMessage())
        .isEqualTo(ex.getMessage())
        .contains("APPROVED", "SUBMITTED");
  }

  private ResultActions expectBadRequest(final String path, final String body) throws Exception {
    return mockMvc
        .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.*", hasSize(4)));
  }

  @RestController
  static class InvalidReturnValueController {

    @NotNull
    @GetMapping("/test/invalid-return-value")
    public String invalidReturnValue() {
      return null;
    }
  }
}
