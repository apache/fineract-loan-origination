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

package org.apache.fineract.los.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Generates and holds an in-memory RSA-2048 key pair for the demo payload encryption feature.
 *
 * <p>The key pair is created once at application startup and lives only in JVM memory — it is never
 * persisted to disk or committed to the repository. On restart a new pair is generated,
 * invalidating any in-flight login that fetched the previous public key (this is acceptable for a
 * demo scenario).
 *
 * <p>The public key is served to Angular via {@code GET /api/v1/auth/public-key} so the frontend
 * can wrap a freshly generated AES-GCM key using RSA-OAEP. The private key never leaves the JVM.
 */
@Slf4j
@Component
public class RsaKeyPairProvider {

  @Getter private final RSAPublicKey publicKey;
  @Getter private final RSAPrivateKey privateKey;

  public RsaKeyPairProvider() {
    try {
      final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(2048);
      final KeyPair pair = gen.generateKeyPair();
      this.publicKey = (RSAPublicKey) pair.getPublic();
      this.privateKey = (RSAPrivateKey) pair.getPrivate();
      log.info("Demo payload-encryption RSA-2048 key pair generated (in-memory only).");
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate RSA key pair for payload encryption", ex);
    }
  }
}
