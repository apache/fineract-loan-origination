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
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.domain.ApprovalStage;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.service.ApprovalWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for recording multi-stage approval decisions against a loan application.
 *
 * <p>A single endpoint accepts APPROVE / REJECT / REFER via {@link ApprovalDecisionRequest#getDecision()}
 * rather than three separate endpoints — keeps the state-transition logic entirely inside {@code
 * ApprovalWorkflowService}, with the controller as a thin pass-through.
 */
@Tag(name = "Approval Workflow", description = "Record loan officer / branch manager / credit committee decisions")
@RestController
@RequestMapping("/api/v1/loan-applications/{applicationRef}/approval-decisions")
@RequiredArgsConstructor
public class ApprovalController {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String DEFAULT_TENANT = "default";

  private final ApprovalWorkflowService approvalWorkflowService;

  @Operation(summary = "Record an APPROVE, REJECT, or REFER decision for the current stage")
  @PostMapping
  public ResponseEntity<ApprovalStage> recordDecision(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef,
      @Valid @RequestBody final ApprovalDecisionRequest request) {

    final ApprovalStage stage =
        approvalWorkflowService.recordDecision(applicationRef, tenantId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(stage);
  }
}