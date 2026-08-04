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
import org.apache.fineract.los.api.dto.response.StaffProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API exposing information about the currently authenticated staff member.
 *
 * <p>The returned LOS workflow role is derived from the authenticated Spring Security principal
 * after Fineract authorities have been mapped into LOS workflow roles. This endpoint is primarily
 * intended for frontend applications that need to determine which approval actions should be
 * available to the logged-in staff member.
 */
@Tag(name = "Staff", description = "Authenticated staff information")
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

  @Operation(summary = "Return the authenticated staff member's LOS workflow role")
  @GetMapping("/me")
  public StaffProfileResponse getCurrentStaff(final Authentication authentication) {

    final String losRole =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(
                grantedAuthority ->
                    grantedAuthority.startsWith("ROLE_") && !grantedAuthority.equals("ROLE_STAFF"))
            .map(grantedAuthority -> grantedAuthority.substring("ROLE_".length()))
            .findFirst()
            .orElse(null);

    return StaffProfileResponse.builder()
        .username(authentication.getName())
        .losRole(losRole)
        .build();
  }
}
