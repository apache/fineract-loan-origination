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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rate limiting filter using token bucket algorithm to prevent brute-force attacks and API abuse.
 *
 * <p>Applies different rate limits based on endpoint sensitivity:
 *
 * <ul>
 *   <li><strong>Auth endpoints</strong> (/api/v1/auth/**): 5 requests per 15 minutes per IP —
 *       prevents credential stuffing and brute-force attacks
 *   <li><strong>Admin endpoints</strong> (/api/v1/admin/**): 20 requests per minute per IP —
 *       prevents abuse of privileged operations
 *   <li><strong>Other endpoints</strong>: 100 requests per minute per IP — prevents general API
 *       abuse and DoS
 * </ul>
 *
 * <p>Rate limits are tracked per client IP address using an in-memory cache that expires entries
 * after 15 minutes of inactivity.
 *
 * <p>When rate limit is exceeded, returns HTTP 429 (Too Many Requests) with Retry-After header.
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

  // Auth endpoints: Strict limit to prevent brute-force attacks
  private static final int AUTH_MAX_REQUESTS = 5;
  private static final Duration AUTH_WINDOW = Duration.ofMinutes(15);

  // Admin endpoints: Moderate limit for privileged operations
  private static final int ADMIN_MAX_REQUESTS = 20;
  private static final Duration ADMIN_WINDOW = Duration.ofMinutes(1);

  // General endpoints: Liberal limit for normal usage
  private static final int GENERAL_MAX_REQUESTS = 100;
  private static final Duration GENERAL_WINDOW = Duration.ofMinutes(1);

  // Cache for storing rate limit buckets per IP address
  private final Cache<String, Bucket> authBuckets =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10000).build();

  private final Cache<String, Bucket> adminBuckets =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10000).build();

  private final Cache<String, Bucket> generalBuckets =
      Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10000).build();

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    final String clientIp = getClientIp(httpRequest);
    final String requestUri = httpRequest.getRequestURI();

    Bucket bucket;
    long retryAfterSeconds;

    // Determine which rate limit to apply based on endpoint
    if (requestUri.startsWith("/api/v1/auth/")) {
      bucket = authBuckets.get(clientIp, k -> createAuthBucket());
      retryAfterSeconds = AUTH_WINDOW.toSeconds();
    } else if (requestUri.startsWith("/api/v1/admin/")) {
      bucket = adminBuckets.get(clientIp, k -> createAdminBucket());
      retryAfterSeconds = ADMIN_WINDOW.toSeconds();
    } else {
      bucket = generalBuckets.get(clientIp, k -> createGeneralBucket());
      retryAfterSeconds = GENERAL_WINDOW.toSeconds();
    }

    // Try to consume one token from the bucket
    if (bucket.tryConsume(1)) {
      // Request allowed - proceed
      chain.doFilter(request, response);
    } else {
      // Rate limit exceeded - reject request
      log.warn("Rate limit exceeded for IP [{}] on endpoint [{}]", clientIp, requestUri);

      httpResponse.setStatus(429); // Too Many Requests
      httpResponse.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
      httpResponse.setContentType("application/json");
      httpResponse
          .getWriter()
          .write(
              "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfterSeconds\":"
                  + retryAfterSeconds
                  + "}");
    }
  }

  /**
   * Creates a bucket for authentication endpoints with strict limits.
   *
   * <p>Allows 5 requests per 15 minutes with greedy refill strategy.
   */
  private Bucket createAuthBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.classic(AUTH_MAX_REQUESTS, Refill.greedy(AUTH_MAX_REQUESTS, AUTH_WINDOW)))
        .build();
  }

  /**
   * Creates a bucket for admin endpoints with moderate limits.
   *
   * <p>Allows 20 requests per minute with greedy refill strategy.
   */
  private Bucket createAdminBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.classic(ADMIN_MAX_REQUESTS, Refill.greedy(ADMIN_MAX_REQUESTS, ADMIN_WINDOW)))
        .build();
  }

  /**
   * Creates a bucket for general endpoints with liberal limits.
   *
   * <p>Allows 100 requests per minute with greedy refill strategy.
   */
  private Bucket createGeneralBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.classic(
                GENERAL_MAX_REQUESTS, Refill.greedy(GENERAL_MAX_REQUESTS, GENERAL_WINDOW)))
        .build();
  }

  /**
   * Extracts the client IP address from the request.
   *
   * <p>Checks X-Forwarded-For header first (for requests behind proxies/load balancers), then falls
   * back to remote address.
   */
  private String getClientIp(final HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isEmpty()) {
      // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
      // Take the first one which is the original client
      ip = ip.split(",")[0].trim();
    } else {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
