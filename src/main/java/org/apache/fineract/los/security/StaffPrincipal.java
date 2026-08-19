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
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security principal for staff users authenticated via JWT.
 *
 * <p>Staff users are Fineract users (not linked to a client) who access the LOS backoffice portal.
 * They are assigned workflow roles (LOAN_OFFICER, CREDIT_COMMITTEE, BRANCH_MANAGER) that determine
 * which approval stages they can act on.
 */
@Getter
public class StaffPrincipal implements UserDetails {

  private final String username;
  private final String tenantId;
  private final String losRole; // e.g., "ROLE_LOAN_OFFICER"

  public StaffPrincipal(final String username, final String tenantId, final String losRole) {
    this.username = username;
    this.tenantId = tenantId;
    this.losRole = losRole;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(losRole), new SimpleGrantedAuthority("ROLE_STAFF"));
  }

  @Override
  public String getPassword() {
    return null; // Password validation happens in Fineract
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
