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
import org.apache.fineract.los.security.MockCustomerIdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Customer-facing security chain. Matched first ({@code @Order(1)}) so any request under
   * {@code /api/v1/customer/**} authenticates against {@link MockCustomerIdentityService}
   * (producing a {@code CustomerPrincipal}) rather than the static staff account below.
   *
   * <p>CORS must be applied here explicitly — it is NOT inherited from the staff chain, since each
   * {@code SecurityFilterChain} is independently matched and configured.
   */
  @Bean
  @Order(1)
  SecurityFilterChain customerSecurityFilterChain(
      final HttpSecurity http, final MockCustomerIdentityService customerIdentityService)
      throws Exception {

    http.securityMatcher("/api/v1/customer/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        .userDetailsService(customerIdentityService)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  /** Staff-facing security chain — explicitly ordered after the customer chain. */
  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(
      final HttpSecurity http, final InMemoryUserDetailsManager staffUserDetailsService)
      throws Exception {

    http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .userDetailsService(staffUserDetailsService)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(basic -> basic.realmName("Fineract Loan Origination Service"))
        .formLogin(form -> form.disable());

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:4200"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** Explicit staff identity — replaces the auto-configured default user, which Spring Boot
   * silently disables once any custom UserDetailsService bean (like MockCustomerIdentityService)
   * is present in the context. */
  @Bean
  InMemoryUserDetailsManager staffUserDetailsService(final PasswordEncoder passwordEncoder) {
    final UserDetails staffUser =
        User.withUsername("admin")
            .password(passwordEncoder.encode("somepassword"))
            .roles("STAFF")
            .build();
    return new InMemoryUserDetailsManager(staffUser);
  }
}