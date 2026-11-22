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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.statemachine.LoanStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Unit tests for {@link GlobalExceptionHandler}. */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private MockMvc mockMvc;

  @BeforeEach
  void setUpMvc() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ExceptionMappingTestController())
            .setControllerAdvice(handler)
            .build();
  }

  @Test
  @DisplayName("LoanStateTransitionException maps to 409 Conflict with the transition message")
  void invalidStateTransitionReturnsConflict() {
    final LoanStateTransitionException ex =
        new LoanStateTransitionException(
            LoanApplicationStatus.APPROVED, LoanApplicationStatus.SUBMITTED);

    final ResponseEntity<ErrorResponse> response = handler.handleInvalidStateTransition(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(response.getBody().getError()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
    assertThat(response.getBody().getMessage())
        .isEqualTo(ex.getMessage())
        .contains("APPROVED", "SUBMITTED");
  }

  @Test
  @DisplayName("Fineract integration errors retain the 502 response contract")
  void fineractIntegrationErrorResponseRemainsCompatible() {
    final FineractIntegrationException ex =
        new FineractIntegrationException(
            "LOS-1",
            FineractIntegrationException.Operation.APPROVE_LOAN,
            new IllegalStateException("Fineract rejected approval"));

    final ResponseEntity<ErrorResponse> response = handler.handleFineractIntegration(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(502);
    assertThat(response.getBody().getError()).isEqualTo("Bad Gateway");
    assertThat(response.getBody().getMessage())
        .contains("POST /loans/{id}?command=approve", "LOS-1", "Fineract rejected approval");
    assertThat(response.getBody().getDetails()).isEmpty();
    assertThat(response.getBody().getTimestamp()).isNotNull();
    assertThat(LosErrorConstants.ERR_FINERACT_INTEGRATION)
        .isEqualTo("los.error.fineract.integration-failure");
  }

  @Test
  @DisplayName("AuthorizationDeniedException maps to 403 Forbidden with a fixed message")
  void authorizationDeniedReturnsForbidden() {
    final ResponseEntity<ErrorResponse> response =
        handler.handleAccessDenied(new AuthorizationDeniedException("Access Denied"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getBody().getError()).isEqualTo(HttpStatus.FORBIDDEN.getReasonPhrase());
    assertThat(response.getBody().getMessage()).isEqualTo(LosErrorConstants.MSG_ACCESS_DENIED);
  }

  @Test
  @DisplayName("AccessDeniedException does not expose its own message")
  void accessDeniedDoesNotExposeMessage() {
    final ResponseEntity<ErrorResponse> response =
        handler.handleAccessDenied(new AccessDeniedException("internal detail"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo(LosErrorConstants.MSG_ACCESS_DENIED)
        .doesNotContain("internal detail");
  }

  @Test
  @DisplayName("Request parameter conversion failures map to 400")
  void requestConversionFailuresReturnBadRequest() throws Exception {
    mockMvc
        .perform(get("/required-param"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("name: is required"));

    mockMvc
        .perform(get("/numeric/{id}", "not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("id: has an invalid value"));
  }

  @Test
  @DisplayName("Model attribute binding validation maps BindException to 400")
  void bindExceptionFromModelAttributeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/bound"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("name: must not be blank"));
  }

  @Test
  @DisplayName("Internal conversion and constraint failures remain 500")
  void internalValidationAndConversionFailuresReturnServerError() throws Exception {
    mockMvc
        .perform(get("/internal-conversion"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."));

    mockMvc
        .perform(get("/internal-constraint"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
  }

  @Test
  @DisplayName("Null validation messages use a meaningful fallback")
  void nullValidationMessagesAreReplaced() throws Exception {
    final BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "field", null));
    final ResponseEntity<ErrorResponse> bindResponse =
        handler.handleBindException(new BindException(bindingResult));

    assertThat(bindResponse.getBody().getDetails()).containsExactly("field: is invalid");
    assertThat(bindResponse.getBody().getDetails()).doesNotContain("field: null");

    final Method bodyMethod =
        ExceptionMappingTestController.class.getDeclaredMethod("json", String.class);
    final BeanPropertyBindingResult bodyBindingResult =
        new BeanPropertyBindingResult(new Object(), "body");
    bodyBindingResult.addError(new FieldError("body", "field", null));
    final ResponseEntity<ErrorResponse> bodyResponse =
        handler.handleBeanValidationError(
            new MethodArgumentNotValidException(
                new MethodParameter(bodyMethod, 0), bodyBindingResult));
    assertThat(bodyResponse.getBody().getDetails()).containsExactly("field: is invalid");

    final MethodValidationResult validationResult = mock(MethodValidationResult.class);
    final ParameterValidationResult parameterResult = mock(ParameterValidationResult.class);
    final Method method =
        ExceptionMappingTestController.class.getDeclaredMethod("required", String.class);
    final DefaultMessageSourceResolvable nullMessage =
        new DefaultMessageSourceResolvable(null, null, null);
    when(validationResult.isForReturnValue()).thenReturn(false);
    when(validationResult.getParameterValidationResults()).thenReturn(List.of(parameterResult));
    doReturn(List.of(nullMessage)).when(validationResult).getAllErrors();
    when(parameterResult.getMethodParameter()).thenReturn(new MethodParameter(method, 0));
    when(parameterResult.getResolvableErrors()).thenReturn(List.of(nullMessage));

    final ResponseEntity<ErrorResponse> methodResponse =
        handler.handleMethodValidation(new HandlerMethodValidationException(validationResult));
    assertThat(methodResponse.getBody().getDetails()).containsExactly("value: is invalid");
    assertThat(methodResponse.getBody().getDetails()).doesNotContain("value: null");
  }

  @Test
  @DisplayName("Return-value method validation remains HTTP 500")
  void returnValueValidationRemainsServerError() throws Exception {
    mockMvc
        .perform(get("/invalid-return"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
  }

  @Test
  @DisplayName("Unsupported content types retain HTTP 415 semantics")
  void unsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(post("/json").contentType(MediaType.TEXT_PLAIN).content("not-json"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415));
  }

  @RestController
  static class ExceptionMappingTestController {

    @GetMapping("/required-param")
    String required(@RequestParam("name") String value) {
      return value;
    }

    @GetMapping("/numeric/{id}")
    String numeric(@PathVariable("id") Long id) {
      return id.toString();
    }

    @NotBlank
    @GetMapping("/invalid-return")
    String invalidReturn() {
      return "";
    }

    @GetMapping("/bound")
    String bound(@Valid @ModelAttribute BoundRequest request) {
      return request.name;
    }

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    String json(@RequestBody String body) {
      return body;
    }

    @GetMapping("/internal-conversion")
    String internalConversion() {
      throw new ConversionNotSupportedException(
          "internal", Long.class, new IllegalStateException("no converter"));
    }

    @GetMapping("/internal-constraint")
    String internalConstraint() {
      throw new jakarta.validation.ConstraintViolationException("internal validation", Set.of());
    }
  }

  static class BoundRequest {

    @NotBlank String name;
  }
}
