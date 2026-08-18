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

import java.util.List;
import org.apache.fineract.los.security.FineractAuthenticationProvider;
import org.apache.fineract.los.security.JwtAuthFilter;
import org.apache.fineract.los.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Customer-facing chain (Order 1). Covers customer endpoints only. Authentication is handled via
   * LOS-issued JWT tokens — credentials are validated locally against the {@code
   * customer_credentials} table; Fineract is never consulted for customer login.
   *
   * <p>Note: `/api/v1/loan-applications/**` is intentionally EXCLUDED here so that staff can access
   * these endpoints via Basic Auth (handled by Order 2 chain).
   */
  @Bean
  @Order(1)
  SecurityFilterChain jwtSecurityFilterChain(final HttpSecurity http, final JwtService jwtService)
      throws Exception {

    http.securityMatcher("/api/v1/customer/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/customer/**"))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .headers(
            headers ->
                headers
                    // Prevents MIME sniffing attacks
                    .contentTypeOptions(contentType -> contentType.disable())
                    // Prevents clickjacking by disallowing iframe embedding
                    .frameOptions(frame -> frame.deny())
                    // Enforces HTTPS (only in production with HTTPS enabled)
                    .httpStrictTransportSecurity(
                        hsts ->
                            hsts.includeSubDomains(true)
                                .maxAgeInSeconds(31536000) // 1 year
                                .preload(true))
                    // Content Security Policy - restricts resource loading
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' 'unsafe-inline'; "
                                    + "style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self' data: https:; "
                                    + "font-src 'self' data:; "
                                    + "connect-src 'self'; "
                                    + "frame-ancestors 'none'; "
                                    + "base-uri 'self'; "
                                    + "form-action 'self'"))
                    // Referrer policy - controls referrer information
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    // Permissions policy - controls browser features
                    .addHeaderWriter(
                        new StaticHeadersWriter(
                            "Permissions-Policy",
                            "geolocation=(), microphone=(), camera=(), payment=()")));

    return http.build();
  }

  /**
   * Staff (Order 2). Covers all remaining endpoints including admin, approval, disbursement, and
   * actuator routes.
   *
   * <p>Authentication supports both JWT tokens (from staff login) and Basic Auth (delegated to
   * Fineract). JWT tokens are validated locally against the staff_credentials table. Basic Auth
   * credentials are forwarded to Fineract's {@code /api/v1/authentication} endpoint via {@link
   * FineractAuthenticationProvider} for backward compatibility.
   */
  @Bean
  @Order(2)
  SecurityFilterChain staffSecurityFilterChain(
      final HttpSecurity http,
      final FineractAuthenticationProvider fineractAuthenticationProvider,
      final JwtService jwtService)
      throws Exception {

    http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(fineractAuthenticationProvider)
        .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api/v1/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(basic -> basic.realmName("Fineract Loan Origination Service"))
        .formLogin(AbstractHttpConfigurer::disable)
        .headers(
            headers ->
                headers
                    .contentTypeOptions(contentType -> contentType.disable())
                    .frameOptions(frame -> frame.deny())
                    .httpStrictTransportSecurity(
                        hsts ->
                            hsts.includeSubDomains(true).maxAgeInSeconds(31536000).preload(true))
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' 'unsafe-inline'; "
                                    + "style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self' data: https:; "
                                    + "font-src 'self' data:; "
                                    + "connect-src 'self'; "
                                    + "frame-ancestors 'none'; "
                                    + "base-uri 'self'; "
                                    + "form-action 'self'"))
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .addHeaderWriter(
                        new StaticHeadersWriter(
                            "Permissions-Policy",
                            "geolocation=(), microphone=(), camera=(), payment=()")));

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    // localhost:4200 / 4201 = LOS customer Angular app
    // localhost:60506 / 49954 = Fineract Backoffice UI
    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:60506",
            "http://localhost:49954"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * Password encoder used for both customer and staff credential hashing. Staff passwords are
   * stored locally in LOS. Fineract Basic Auth (for backward compatibility) still delegates to
   * Fineract.
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
