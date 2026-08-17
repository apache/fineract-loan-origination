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
 * Authenticated principal representing a logged-in customer (loan applicant), as opposed to
 * internal staff (loan officers, branch managers) who authenticate through the default security
 * chain.
 *
 * <p>Carries {@code clientId} — the Fineract client identifier — which every customer-facing
 * endpoint uses to scope data access to only what that customer owns.
 *
 * <p>Also carries {@code tenantId} from the JWT claim, validated against the request header to
 * prevent cross-tenant data access.
 */
@Getter
public class CustomerPrincipal implements UserDetails {

  private final String username;
  private final String password;
  private final Long clientId;
  private final String tenantId;
  private final String displayName;

  public CustomerPrincipal(
      String username, String password, Long clientId, String tenantId, String displayName) {
    this.username = username;
    this.password = password;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.displayName = displayName;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  @Override
  public String getPassword() {
    return password;
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