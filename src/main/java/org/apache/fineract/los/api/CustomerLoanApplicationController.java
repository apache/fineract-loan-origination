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

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.api.dto.response.CreditScoreResponse;
import org.apache.fineract.los.api.dto.response.LoanApplicationResponse;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.dto.request.CreateLoanApplicationRequest;
import org.apache.fineract.los.security.CustomerPrincipal;
import org.apache.fineract.los.service.CreditScoringService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing loan application endpoints. Every method scopes results to the authenticated
 * {@link CustomerPrincipal}'s own {@code clientId} — this is the endpoint the Angular customer app
 * calls, distinct from the staff-facing {@link LoanApplicationController}.
 *
 * <p>Customers may create, view, and submit their own applications. Back-office actions
 * (start-review, approval decisions, disbursement) remain staff-only and live exclusively in {@link
 * LoanApplicationController}.
 */
@RestController
@RequestMapping("/api/v1/customer/loan-applications")
@RequiredArgsConstructor
public class CustomerLoanApplicationController {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String DEFAULT_TENANT = "default";

  private final LoanApplicationService loanApplicationService;
  private final CreditScoringService creditScoringService;

  /** Creates a new application, forcing the applicant's clientId to the caller's own identity. */
  @PostMapping
  public ResponseEntity<LoanApplicationResponse> create(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @AuthenticationPrincipal final CustomerPrincipal principal,
      @Valid @RequestBody final CreateLoanApplicationRequest request) {

    final LoanApplication application =
        loanApplicationService.createApplicationForCustomer(
            request, tenantId, principal.getClientId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(LoanApplicationResponse.from(application));
  }

  /** Returns only applications owned by the authenticated customer's clientId. */
  @GetMapping
  public List<LoanApplicationResponse> myApplications(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @AuthenticationPrincipal final CustomerPrincipal principal) {

    return loanApplicationService
        .getApplicationsForFineractClient(principal.getClientId(), tenantId)
        .stream()
        .map(LoanApplicationResponse::from)
        .toList();
  }

  /** Retrieves a single application by reference — only if it belongs to the caller. */
  @GetMapping("/{applicationRef}")
  public LoanApplicationResponse getByRef(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef,
      @AuthenticationPrincipal final CustomerPrincipal principal) {

    final LoanApplication application =
        loanApplicationService.getApplicationOrThrow(applicationRef, tenantId);

    assertOwnedByCaller(application, principal);

    return LoanApplicationResponse.from(application);
  }

  /** Submits the caller's own DRAFT application: DRAFT -> SUBMITTED. */
  @PostMapping("/{applicationRef}/submit")
  public LoanApplicationResponse submit(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef,
      @AuthenticationPrincipal final CustomerPrincipal principal) {

    final LoanApplication application =
        loanApplicationService.getApplicationOrThrow(applicationRef, tenantId);

    assertOwnedByCaller(application, principal);

    return LoanApplicationResponse.from(loanApplicationService.submit(applicationRef, tenantId));
  }

  @GetMapping("/{applicationRef}/credit-score")
  public CreditScoreResponse creditScore(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef,
      @AuthenticationPrincipal final CustomerPrincipal principal) {

    final LoanApplication application =
        loanApplicationService.getApplicationOrThrow(applicationRef, tenantId);

    assertOwnedByCaller(application, principal);

    return creditScoringService
        .findExistingScore(application)
        .map(CreditScoreResponse::from)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No credit score computed yet for application [" + applicationRef + "]."));
  }

  private void assertOwnedByCaller(
      final LoanApplication application, final CustomerPrincipal principal) {

    final Long ownerClientId = loanApplicationService.getFineractClientIdOrThrow(application);
    if (!ownerClientId.equals(principal.getClientId())) {
      throw new AccessDeniedException("Application does not belong to this customer");
    }
  }
}
