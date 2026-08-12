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
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.bridge.DisbursementBridgeService;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for triggering the disbursement bridge — calls Fineract's {@code POST /loans} (real or
 * mocked, per {@code los.fineract.mock-enabled}) once an application is APPROVED.
 */
@Tag(
    name = "Disbursement",
    description = "Bridge an APPROVED application into a real Fineract loan")
@RestController
@RequestMapping("/api/v1/loan-applications/{applicationRef}/disburse")
@RequiredArgsConstructor
public class DisbursementController {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String DEFAULT_TENANT = "default";

  private final DisbursementBridgeService disbursementBridgeService;

  @Operation(summary = "Create the loan in Fineract and move the application to DISBURSED")
  @PostMapping
  @PreAuthorize("hasRole('STAFF')")
  public FineractLoanCreateResponse disburse(
      @RequestHeader(value = TENANT_HEADER, defaultValue = DEFAULT_TENANT) final String tenantId,
      @PathVariable final String applicationRef) {

    return disbursementBridgeService.disburse(applicationRef, tenantId);
  }
}
