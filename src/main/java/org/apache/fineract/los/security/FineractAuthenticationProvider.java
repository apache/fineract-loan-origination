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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.api.dto.response.FineractAuthResponse;
import org.apache.fineract.los.service.FineractCredentialValidationService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FineractAuthenticationProvider implements AuthenticationProvider {

  private final FineractCredentialValidationService validationService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    FineractAuthResponse fineractResponse = validationService.validate(username, password);

    if (fineractResponse == null) {
      throw new BadCredentialsException("Invalid Fineract credentials for user: " + username);
    }

    List<SimpleGrantedAuthority> authorities =
        fineractResponse.getPermissions() == null
            ? List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
            : fineractResponse.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

    if (authorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_STAFF"))) {
      authorities = new ArrayList<>(authorities);
      authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
    }

    // Pass null as credentials — the plaintext password must not persist in the
    // Authentication object after validation is complete (OWASP credential-in-memory mitigation).
    return new UsernamePasswordAuthenticationToken(username, null, authorities);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
