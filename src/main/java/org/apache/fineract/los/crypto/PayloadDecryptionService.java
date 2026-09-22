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

import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Decrypts the AES-GCM encrypted login payload sent by the Angular frontend.
 *
 * <p>Protocol (matches the Angular {@code PayloadEncryptionService}):
 *
 * <ol>
 *   <li>Angular generates a fresh 256-bit AES-GCM key per login.
 *   <li>Angular encrypts the JSON payload with AES-GCM (96-bit IV prepended to ciphertext).
 *   <li>Angular wraps the AES key with the backend's RSA-OAEP public key.
 *   <li>Both wrapped key and ciphertext are sent as Base64 strings in the request body.
 *   <li>This service unwraps the AES key using the RSA private key, then decrypts the payload.
 * </ol>
 *
 * <p>No plaintext credential is ever logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayloadDecryptionService {

  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;

  private final RsaKeyPairProvider keyPairProvider;

  /**
   * Decrypts an encrypted login envelope.
   *
   * @param wrappedKeyB64 Base64-encoded RSA-OAEP wrapped AES key
   * @param ciphertextB64 Base64-encoded IV-prefixed AES-GCM ciphertext
   * @return plaintext JSON string of the original login payload
   * @throws IllegalArgumentException if decryption fails (bad key, tampered data, etc.)
   */
  public String decrypt(final String wrappedKeyB64, final String ciphertextB64) {
    try {
      // 1. Unwrap the AES key using RSA-OAEP with SHA-256 for both hash and MGF1
      //    (matches Web Crypto API default when hash='SHA-256' is specified)
      final byte[] wrappedKey = Base64.getDecoder().decode(wrappedKeyB64);
      final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
      final OAEPParameterSpec oaepSpec =
          new OAEPParameterSpec(
              "SHA-256", // hash function
              "MGF1", // mask generation function
              MGF1ParameterSpec.SHA256, // MGF1 uses SHA-256 (not SHA-1 default)
              PSource.PSpecified.DEFAULT // no label
              );
      rsaCipher.init(Cipher.UNWRAP_MODE, keyPairProvider.getPrivateKey(), oaepSpec);
      final SecretKey aesKey = (SecretKey) rsaCipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

      // 2. Decode IV-prefixed AES-GCM ciphertext
      final byte[] ivAndCiphertext = Base64.getDecoder().decode(ciphertextB64);
      final byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      final byte[] ciphertext = new byte[ivAndCiphertext.length - GCM_IV_LENGTH_BYTES];
      System.arraycopy(ivAndCiphertext, 0, iv, 0, GCM_IV_LENGTH_BYTES);
      System.arraycopy(ivAndCiphertext, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

      // 3. Decrypt with AES-GCM — authentication tag is verified automatically
      final Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
      aesCipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(aesKey.getEncoded(), "AES"),
          new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      final byte[] plaintext = aesCipher.doFinal(ciphertext);

      return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);

    } catch (Exception ex) {
      // Log exception type and message for debugging, but never payload contents
      log.warn(
          "Payload decryption failed: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
      throw new IllegalArgumentException("Invalid encrypted payload", ex);
    }
  }
}
