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

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * TEMPORARY POC identity source for customer logins.
 *
 * <p>This hardcoded map exists only so the customer-facing security chain has something real to
 * authenticate against while the project doesn't yet have a persisted customer-credential table.
 * Replace with a DB-backed {@link UserDetailsService} (e.g. querying by {@code fineractClientId} or
 * a dedicated login table) before this is used anywhere beyond local development.
 */
@Service
@Profile({"dev", "test"})
public class MockCustomerIdentityService implements UserDetailsService {

  private final Map<String, CustomerRecord> customers;

  public MockCustomerIdentityService(PasswordEncoder passwordEncoder) {
    this.customers =
        Map.of(
            "customer1",
            new CustomerRecord(passwordEncoder.encode("password1"), 968751L, "Sujan"),
            "customer2",
            new CustomerRecord(passwordEncoder.encode("password2"), 200145L, "Aman"));
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    final CustomerRecord record = customers.get(username);
    if (record == null) {
      throw new UsernameNotFoundException("No customer found: " + username);
    }
    return new CustomerPrincipal(
        username, record.encodedPassword(), record.clientId(), "default", record.displayName());
  }

  private record CustomerRecord(String encodedPassword, Long clientId, String displayName) {}
}
