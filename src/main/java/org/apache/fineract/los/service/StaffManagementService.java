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

package org.apache.fineract.los.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.los.domain.StaffCredential;
import org.apache.fineract.los.repository.StaffCredentialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for creating and managing LOS staff accounts.
 *
 * <p>Staff accounts are stored locally in the {@code staff_credentials} table and authenticated via
 * BCrypt password hashing — independent of Fineract's user store.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffManagementService {

  private final StaffCredentialRepository staffCredentialRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Creates a new staff account.
   *
   * @param username unique login name
   * @param plainPassword raw password (will be BCrypt-hashed before persistence)
   * @param email staff email address
   * @param role role string, e.g. {@code ROLE_STAFF} or {@code ROLE_ADMIN}
   * @param tenantId institution identifier
   * @return the persisted staff credential
   */
  @Transactional
  public StaffCredential createStaff(
      final String username,
      final String plainPassword,
      final String email,
      final String role,
      final String tenantId) {

    if (staffCredentialRepository.existsByUsernameAndTenantId(username, tenantId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Staff username already exists for this tenant");
    }

    final StaffCredential staff =
        new StaffCredential(username, passwordEncoder.encode(plainPassword), email, role, tenantId);

    final StaffCredential saved = staffCredentialRepository.save(staff);
    log.info("Staff account created: username={} role={} tenantId={}", username, role, tenantId);
    return saved;
  }

  /**
   * Returns all staff accounts for a given tenant.
   *
   * @param tenantId institution identifier
   * @return list of all staff credentials (active and inactive)
   */
  @Transactional(readOnly = true)
  public List<StaffCredential> listStaff(final String tenantId) {
    return staffCredentialRepository.findAll().stream()
        .filter(s -> s.getTenantId().equals(tenantId))
        .toList();
  }

  /**
   * Updates an existing staff member's email, role, and/or password.
   *
   * <p>Fields that are {@code null} in the request are left unchanged.
   *
   * @param id staff credential primary key
   * @param email new email, or {@code null} to leave unchanged
   * @param role new role, or {@code null} to leave unchanged
   * @param plainPassword new password in plain text, or {@code null} to leave unchanged
   * @return the updated staff credential
   */
  @Transactional
  public StaffCredential updateStaff(
      final Long id, final String email, final String role, final String plainPassword) {

    final StaffCredential staff = getOrThrow(id);

    if (email != null) staff.setEmail(email);
    if (role != null) staff.setRole(role);
    if (plainPassword != null) staff.setPasswordHash(passwordEncoder.encode(plainPassword));

    staff.setUpdatedAt(LocalDateTime.now());

    log.info("Staff account updated: id={} username={}", id, staff.getUsername());
    return staffCredentialRepository.save(staff);
  }

  /**
   * Deactivates a staff account, preventing future logins without deleting the record.
   *
   * @param id staff credential primary key
   */
  @Transactional
  public void deactivateStaff(final Long id) {
    final StaffCredential staff = getOrThrow(id);
    staff.setActive(false);
    staff.setUpdatedAt(LocalDateTime.now());
    staffCredentialRepository.save(staff);
    log.info("Staff account deactivated: id={} username={}", id, staff.getUsername());
  }

  /**
   * Reactivates a previously deactivated staff account.
   *
   * @param id staff credential primary key
   */
  @Transactional
  public void reactivateStaff(final Long id) {
    final StaffCredential staff = getOrThrow(id);
    staff.setActive(true);
    staff.setUpdatedAt(LocalDateTime.now());
    staffCredentialRepository.save(staff);
    log.info("Staff account reactivated: id={} username={}", id, staff.getUsername());
  }

  private StaffCredential getOrThrow(final Long id) {
    return staffCredentialRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Staff account not found with id: " + id));
  }
}
