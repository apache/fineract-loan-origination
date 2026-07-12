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

package org.apache.fineract.los.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Loan Origination Service.
 *
 * <p>This service is a stateless REST API secured via the {@code X-Fineract-Platform-TenantId}
 * header and Basic authentication. CSRF protection is intentionally disabled because:
 *
 * <ul>
 *   <li>The API is stateless — no session cookies are used
 *   <li>All clients are server-side (Angular uses Authorization header, not cookies)
 *   <li>CSRF attacks require cookie-based session state which this service does not maintain
 * </ul>
 *
 * <p>This follows the standard practice for REST APIs as documented in the Spring Security
 * reference:
 * https://docs.spring.io/spring-security/reference/features/exploits/csrf.html#csrf-when-to-use
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {

    http
        // CSRF disabled intentionally — stateless REST API,
        // no session cookies, clients use Authorization header.
        // See class-level Javadoc for full rationale.
        .csrf(csrf -> csrf.disable())
        // Stateless session — no HttpSession created or used
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Health and info endpoints are public
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    // All other requests require authentication
                    .anyRequest()
                    .authenticated())
        .httpBasic(basic -> basic.realmName("Fineract Loan Origination Service"))
        .formLogin(form -> form.disable());

    return http.build();
  }
}
