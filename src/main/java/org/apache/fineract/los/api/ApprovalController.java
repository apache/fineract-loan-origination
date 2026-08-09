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
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.service.ApprovalWorkflowService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for recording multi-stage approval decisions against a loan application.
 *
 * <p>The workflow stage and assigned officer are never read from the request. Both are derived by
 * {@link ApprovalWorkflowService} from the authenticated {@link Authentication} principal and the
 * application's current position in the configured approval workflow.
 *
 * <p>A single endpoint accepts APPROVE / REJECT / REFER via {@link
 * ApprovalDecisionRequest#getDecision()}, keeping all workflow validation and state transitions
 * inside {@link ApprovalWorkflowService}.
 */
@Tag(
    name = "Approval Workflow",
    description = "Record loan officer / branch manager / credit committee decisions")
@RestController
@RequestMapping("/api/v1/loan-applications/{applicationRef}/approval-decisions")
@RequiredArgsConstructor
public class ApprovalController {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String DEFAULT_TENANT = "default";

  private final ApprovalWorkflowService approvalWorkflowService;
  private final ApprovalStageRepository approvalStageRepository;
  private final LoanApplicationService loanApplicationService;

  @Operation(summary = "List approval history for an application")
  @GetMapping
  @PreAuthorize("hasRole('STAFF')")
  public List<ApprovalStage> getHistory(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef) {

    final LoanApplication application =
        loanApplicationService.getApplicationOrThrow(applicationRef, tenantId);

    return approvalStageRepository.findAllByApplicationOrderByCreatedAtAsc(application);
  }

  @Operation(
      summary =
          "Record an APPROVE, REJECT, or REFER decision for the application's current workflow "
              + "stage as the authenticated staff member")
  @PostMapping
  @PreAuthorize("hasRole('STAFF')")
  public ResponseEntity<ApprovalStage> recordDecision(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef,
      @Valid @RequestBody final ApprovalDecisionRequest request,
      final Authentication authentication) {

    final ApprovalStage stage =
        approvalWorkflowService.recordDecision(applicationRef, tenantId, request, authentication);

    return ResponseEntity.status(HttpStatus.CREATED).body(stage);
  }
}
