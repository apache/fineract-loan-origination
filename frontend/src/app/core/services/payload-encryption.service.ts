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

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EncryptedEnvelope {
  wrappedKey: string; // Base64: RSA-OAEP wrapped AES-256 key
  ciphertext: string; // Base64: 12-byte IV || AES-GCM ciphertext+tag
}

/**
 * Encrypts login payloads in the browser before they are sent over the network.
 *
 * Protocol:
 *  1. Fetch the backend RSA-2048 public key (SPKI/Base64) from /api/v1/auth/public-key.
 *  2. Generate a fresh AES-256-GCM key (non-extractable after wrapping).
 *  3. Encode the plaintext JSON payload as UTF-8 bytes.
 *  4. Encrypt with AES-GCM using a random 96-bit IV; prepend IV to ciphertext.
 *  5. Wrap (encrypt) the AES key with RSA-OAEP SHA-256.
 *  6. Return { wrappedKey, ciphertext } as Base64 strings.
 *
 * The backend holds the matching RSA private key in JVM memory only.
 * It unwraps the AES key, decrypts the payload, then runs normal auth.
 */
@Injectable({ providedIn: 'root' })
export class PayloadEncryptionService {
  private readonly http = inject(HttpClient);
  private readonly subtle = window.crypto.subtle;

  /**
   * Encrypts a login payload object and returns an EncryptedEnvelope.
   * Fetches a fresh public key every call with cache-busting to ensure
   * we always use the key matching the backend's current in-memory private key.
   */
  async encrypt(payload: object): Promise<EncryptedEnvelope> {
    try {
      // 1. Fetch backend public key (Base64 SPKI) with cache-busting timestamp
      const cacheBuster = `_=${Date.now()}`;
      const { publicKey: spkiBase64 } = await firstValueFrom(
        this.http.get<{ publicKey: string }>(
          `${environment.losApiUrl}/auth/public-key?${cacheBuster}`,
        ),
      );

      if (!spkiBase64) {
        throw new Error('Backend returned empty public key');
      }

      const spkiBytes = this.b64ToBytes(spkiBase64);

      // 2. Import RSA-OAEP public key
      const rsaPublicKey = await this.subtle.importKey(
        'spki',
        spkiBytes,
        { name: 'RSA-OAEP', hash: 'SHA-256' },
        false, // not extractable
        ['wrapKey'], // usage: wrap only
      );

      // 3. Generate a fresh AES-256-GCM key per login
      const aesKey = await this.subtle.generateKey(
        { name: 'AES-GCM', length: 256 },
        true, // must be extractable to wrap it
        ['encrypt'],
      );

      // 4. Encrypt the payload with AES-GCM
      const iv = window.crypto.getRandomValues(new Uint8Array(12)); // 96-bit IV
      const plaintext = new TextEncoder().encode(JSON.stringify(payload));
      const gcmCiphertext = await this.subtle.encrypt({ name: 'AES-GCM', iv }, aesKey, plaintext);

      // 5. Prepend IV to ciphertext (matches backend expectation)
      const ivAndCiphertext = new Uint8Array(iv.byteLength + gcmCiphertext.byteLength);
      ivAndCiphertext.set(iv, 0);
      ivAndCiphertext.set(new Uint8Array(gcmCiphertext), iv.byteLength);

      // 6. Wrap the AES key with RSA-OAEP
      const wrappedKeyBytes = await this.subtle.wrapKey('raw', aesKey, rsaPublicKey, {
        name: 'RSA-OAEP',
      });

      return {
        wrappedKey: this.bytesToB64(new Uint8Array(wrappedKeyBytes)),
        ciphertext: this.bytesToB64(ivAndCiphertext),
      };
    } catch (error) {
      console.error('Payload encryption failed:', error);
      throw new Error('Unable to encrypt login payload. Please try again.', { cause: error });
    }
  }

  private b64ToBytes(b64: string): ArrayBuffer {
    const binary = atob(b64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes.buffer;
  }

  private bytesToB64(bytes: Uint8Array | ArrayBuffer): string {
    const view = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
    let binary = '';
    for (const b of view) binary += String.fromCharCode(b);
    return btoa(binary);
  }
}
