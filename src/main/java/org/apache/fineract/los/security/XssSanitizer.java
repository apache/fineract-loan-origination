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

import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;

/**
 * Utility class for sanitizing user input to prevent XSS (Cross-Site Scripting) attacks.
 *
 * <p>Uses OWASP Java Encoder to escape HTML, JavaScript, and other potentially dangerous content
 * before storing in database or rendering in responses.
 *
 * <p><strong>Usage:</strong> Call {@link #sanitizeForHtml(String)} on all user-provided text fields
 * before persistence (comments, names, addresses, etc.) to prevent stored XSS attacks.
 *
 * <p><strong>Defense-in-depth:</strong> This is a secondary defense layer. The primary defense is
 * proper output encoding in the frontend (Angular's default sanitization). However, sanitizing on
 * the backend ensures data is safe even if consumed by other clients or displayed in admin panels.
 */
@Component
public class XssSanitizer {

  /**
   * Sanitizes a string for safe HTML output by encoding dangerous characters.
   *
   * <p>Converts characters like {@code <, >, &, ", '} into their HTML entity equivalents:
   *
   * <ul>
   *   <li>{@code <script>alert('XSS')</script>} → {@code
   *       &lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;}
   *   <li>{@code <img src=x onerror=alert(1)>} → {@code &lt;img src=x onerror=alert(1)&gt;}
   * </ul>
   *
   * <p>The encoded string is safe to store in the database and render in HTML without executing
   * malicious scripts.
   *
   * @param input raw user input that may contain XSS payloads
   * @return HTML-encoded safe string, or null if input was null
   */
  public String sanitizeForHtml(final String input) {
    if (input == null) {
      return null;
    }
    return Encode.forHtml(input);
  }

  /**
   * Sanitizes a string for safe use in HTML attributes.
   *
   * <p>More strict than {@link #sanitizeForHtml(String)} because attribute context allows fewer
   * characters.
   *
   * @param input raw user input
   * @return HTML attribute-encoded safe string, or null if input was null
   */
  public String sanitizeForHtmlAttribute(final String input) {
    if (input == null) {
      return null;
    }
    return Encode.forHtmlAttribute(input);
  }

  /**
   * Sanitizes a string for safe use in JavaScript strings.
   *
   * <p>Use when embedding user input into JavaScript code (avoid this pattern if possible).
   *
   * @param input raw user input
   * @return JavaScript-encoded safe string, or null if input was null
   */
  public String sanitizeForJavaScript(final String input) {
    if (input == null) {
      return null;
    }
    return Encode.forJavaScript(input);
  }

  /**
   * Sanitizes a string for safe use in URLs.
   *
   * <p>Percent-encodes special characters to prevent URL injection attacks.
   *
   * @param input raw user input
   * @return URL-encoded safe string, or null if input was null
   */
  public String sanitizeForUrl(final String input) {
    if (input == null) {
      return null;
    }
    return Encode.forUriComponent(input);
  }
}
