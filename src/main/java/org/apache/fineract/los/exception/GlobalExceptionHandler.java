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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.statemachine.LoanStateTransitionException;
import org.apache.fineract.los.validation.PasswordValidationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised mapping from LOS exceptions to HTTP responses.
 *
 * <p>Every custom exception in {@code org.apache.fineract.los.exception} is handled here — no
 * controller should need its own try/catch for these. Anything unmapped falls through to a generic
 * 500 with the exception message, which is acceptable for a POC but should be tightened (message
 * scrubbing) before any production use.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApplicationNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleApplicationNotFound(
      final ApplicationNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ApplicantProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleProfileNotFound(
      final ApplicantProfileNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DisbursementNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleDisbursementNotAllowed(
      final DisbursementNotAllowedException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(DuplicateApprovalException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateApproval(
      final DuplicateApprovalException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(LoanStateTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
      final LoanStateTransitionException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(ApprovalStageMismatchException.class)
  public ResponseEntity<ErrorResponse> handleStageMismatch(
      final ApprovalStageMismatchException ex) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(LosRoleNotAssignedException.class)
  public ResponseEntity<ErrorResponse> handleNoLosRole(final LosRoleNotAssignedException ex) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  /**
   * Covers method-security denials ({@code @PreAuthorize}), which Spring Security raises as {@link
   * org.springframework.security.authorization.AuthorizationDeniedException}, a subclass of {@link
   * AccessDeniedException}. A fixed message is returned so authorization details are not exposed.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(final AccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, LosErrorConstants.MSG_ACCESS_DENIED);
  }

  @ExceptionHandler(WeakPasswordException.class)
  public ResponseEntity<ErrorResponse> handleWeakPassword(final WeakPasswordException ex) {
    final List<String> details =
        ex.getErrors().stream().map(PasswordValidationError::getMessage).toList();

    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), details);
  }

  @ExceptionHandler(FineractIntegrationException.class)
  public ResponseEntity<ErrorResponse> handleFineractIntegration(
      final FineractIntegrationException ex) {
    log.error("Fineract integration failure", ex);
    return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidationError(
      final MethodArgumentNotValidException ex) {
    final List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();

    return build(HttpStatus.BAD_REQUEST, LosErrorConstants.MSG_REQUEST_VALIDATION_FAILED, details);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(final IllegalStateException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(
      final org.springframework.web.server.ResponseStatusException ex) {
    // Pass through the exact HTTP status — do NOT let these fall to the generic 500 handler
    final HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    final HttpStatus effective = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    return build(effective, ex.getReason() != null ? ex.getReason() : effective.getReasonPhrase());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      final HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, LosErrorConstants.MSG_REQUEST_BODY_UNREADABLE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(final Exception ex) {
    log.error("Unhandled exception", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  private ResponseEntity<ErrorResponse> build(
      final HttpStatus status, final String message, final List<String> details) {

    return ResponseEntity.status(status)
        .body(
            ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .details(List.copyOf(details))
                .build());
  }

  private ResponseEntity<ErrorResponse> build(final HttpStatus status, final String message) {
    return build(status, message, List.of());
  }
}
