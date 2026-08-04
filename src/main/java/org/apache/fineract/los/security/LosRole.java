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
package org.apache.fineract.los.security;

import java.util.Collection;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;

/**
 * LOS workflow roles, one per configured approval stage.
 *
 * <p>Enum constant names are intentionally identical to the stage-name strings configured under
 * {@code los.workflow.stages} (e.g. {@code "LOAN_OFFICER"}), so a stage name resolved by {@link
 * org.apache.fineract.los.workflow.ApprovalWorkflowProperties#stageForFineractRole(String)} can be
 * converted directly via {@link #valueOf(String)} — adding a fourth workflow stage only requires
 * adding a fourth constant here plus a fourth entry in {@code los.workflow.stages} and {@code
 * los.workflow.role-mapping}, no other code changes.
 */
public enum LosRole {
  LOAN_OFFICER,
  BRANCH_MANAGER,
  CREDIT_COMMITTEE;

  private static final String ROLE_PREFIX = "ROLE_";

  /**
   * Resolves the first {@code ROLE_<LosRole>} authority present on an authenticated principal.
   *
   * <p>Staff principals carry exactly one such authority (see {@link
   * FineractAuthenticationProvider}), alongside the unconditional {@code ROLE_STAFF}. Authorities
   * that aren't a recognised {@link LosRole} name (e.g. {@code ROLE_STAFF} itself) are ignored
   * rather than throwing, since a principal is expected to carry authorities this enum doesn't
   * model.
   *
   * @param authorities authorities granted to the authenticated principal
   * @return the matching LOS role, or empty if none of the principal's authorities map to one
   */
  public static Optional<LosRole> fromAuthorities(
      final Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith(ROLE_PREFIX))
        .map(authority -> authority.substring(ROLE_PREFIX.length()))
        .flatMap(name -> tryParse(name).stream())
        .findFirst();
  }

  private static Optional<LosRole> tryParse(final String name) {
    try {
      return Optional.of(LosRole.valueOf(name));
    } catch (final IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
