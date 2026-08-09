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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.api.dto.response.CreditScoreResponse;
import org.apache.fineract.los.api.dto.response.LoanApplicationResponse;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.dto.request.CreateLoanApplicationRequest;
import org.apache.fineract.los.service.CreditScoringService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the loan application lifecycle: create, submit, move to review, and read.
 *
 * <p>Multi-tenancy follows Fineract's own convention — every endpoint reads {@code
 * X-Fineract-Platform-TenantId} and defaults to {@code "default"} if the header is absent, which is
 * convenient for local demos with a single tenant.
 */
@Tag(name = "Loan Applications", description = "Create, submit, and review loan applications")
@RestController
@RequestMapping("/api/v1/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String DEFAULT_TENANT = "default";

  private final LoanApplicationService loanApplicationService;
  private final CreditScoringService creditScoringService;

  @Operation(summary = "Create a new loan application in DRAFT status")
  @PostMapping
  public ResponseEntity<LoanApplicationResponse> create(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @Valid @RequestBody final CreateLoanApplicationRequest request) {

    final LoanApplication application = loanApplicationService.createApplication(request, tenantId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(LoanApplicationResponse.from(application));
  }

  @Operation(summary = "Retrieve all loan applications for the tenant")
  @GetMapping
  public List<LoanApplicationResponse> getAll(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId) {

    return loanApplicationService.getAllApplications(tenantId).stream()
        .map(LoanApplicationResponse::from)
        .toList();
  }

  @Operation(summary = "Retrieve a loan application by its reference")
  @GetMapping("/{applicationRef}")
  public LoanApplicationResponse getByRef(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef) {

    return LoanApplicationResponse.from(
        loanApplicationService.getApplicationOrThrow(applicationRef, tenantId));
  }

  @Operation(summary = "Submit a DRAFT application: DRAFT -> SUBMITTED")
  @PostMapping("/{applicationRef}/submit")
  public LoanApplicationResponse submit(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef) {

    return LoanApplicationResponse.from(loanApplicationService.submit(applicationRef, tenantId));
  }

  @Operation(
      summary =
          "Move a SUBMITTED or REFERRED application into review: -> UNDER_REVIEW, "
              + "triggers credit scoring")
  @PostMapping("/{applicationRef}/start-review")
  @PreAuthorize("hasRole('STAFF')")
  public LoanApplicationResponse startReview(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef) {

    return LoanApplicationResponse.from(
        loanApplicationService.moveToUnderReview(applicationRef, tenantId));
  }

  @Operation(summary = "Retrieve the computed credit score for an application")
  @GetMapping("/{applicationRef}/credit-score")
  public CreditScoreResponse getCreditScore(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef) {

    final LoanApplication application =
        loanApplicationService.getApplicationOrThrow(applicationRef, tenantId);

    return creditScoringService
        .findExistingScore(application)
        .map(CreditScoreResponse::from)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No credit score computed yet for application ["
                        + applicationRef
                        + "] — call /start-review first."));
  }
}
