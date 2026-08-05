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

package org.apache.fineract.los.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_credentials")
@Getter
@Setter
@NoArgsConstructor
public class CustomerCredential {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "fineract_client_id", nullable = false)
  private Long fineractClientId;

  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public CustomerCredential(
      final String username,
      final String passwordHash,
      final Long fineractClientId,
      final String tenantId) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.fineractClientId = fineractClientId;
    this.tenantId = tenantId;
  }
}
