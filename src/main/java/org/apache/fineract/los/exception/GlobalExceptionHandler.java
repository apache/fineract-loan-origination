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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @ExceptionHandler(FineractIntegrationException.class)
  public ResponseEntity<ErrorResponse> handleFineractIntegration(
      final FineractIntegrationException ex) {
    log.error("Fineract integration failure", ex);
    return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(final IllegalStateException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(final Exception ex) {
    log.error("Unhandled exception", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  private ResponseEntity<ErrorResponse> build(final HttpStatus status, final String message) {
    return ResponseEntity.status(status)
        .body(
            ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build());
  }
}