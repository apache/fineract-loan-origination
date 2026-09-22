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

package org.apache.fineract.los.api;

import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.los.crypto.RsaKeyPairProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the RSA public key that Angular uses to wrap the per-login AES-GCM session key.
 *
 * <p>This endpoint is intentionally public (no authentication required) — the public key is not a
 * secret. Only the matching private key, which never leaves the JVM, can decrypt.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CryptoKeyController {

  private final RsaKeyPairProvider keyPairProvider;

  /**
   * Returns the backend RSA public key as a Base64-encoded DER (SubjectPublicKeyInfo) string, which
   * the Web Crypto API can import directly via {@code importKey("spki", ...)}.
   *
   * @return {@code { "publicKey": "<base64-spki>" }}
   */
  @GetMapping("/public-key")
  public ResponseEntity<PublicKeyResponse> getPublicKey() {
    final byte[] spki = keyPairProvider.getPublicKey().getEncoded(); // X.509 / SPKI format
    final String base64Key = Base64.getEncoder().encodeToString(spki);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .header(HttpHeaders.EXPIRES, "0")
        .body(new PublicKeyResponse(base64Key));
  }

  public record PublicKeyResponse(String publicKey) {}
}
