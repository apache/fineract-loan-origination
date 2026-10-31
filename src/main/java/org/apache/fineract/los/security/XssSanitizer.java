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
 * Uses OWASP Java Encoder to escape HTML, JavaScript, and other potentially dangerous content
 * before storing in database or rendering in responses.
 *
 * Usage: Call {@link #sanitizeForHtml(String)} on all user-provided text fields
 * before persistence (comments, names, addresses, etc.) to prevent stored XSS attacks.
 *
 * Defense-in-depth: This is a secondary defense layer. The primary defense is
 * proper output encoding in the frontend (Angular's default sanitization). However, sanitizing on
 * the backend ensures data is safe even if consumed by other clients or displayed in admin panels.
 */
@Component
public class XssSanitizer {

  /**
   * Sanitizes a string for safe HTML output by encoding dangerous characters.
   *
   * Converts characters like {@code <, >, &, ", '} into their HTML entity equivalents:
   *
   * 
   *   {@code <script>alert('XSS')</script>} → {@code
   *       &lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;}
   *   {@code <img src=x onerror=alert(1)>} → {@code &lt;img src=x onerror=alert(1)&gt;}
   * 
   *
   * The encoded string is safe to store in the database and render in HTML without executing
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
   * More strict than {@link #sanitizeForHtml(String)} because attribute context allows fewer
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
   * Use when embedding user input into JavaScript code (avoid this pattern if possible).
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
   * Percent-encodes special characters to prevent URL injection attacks.
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
