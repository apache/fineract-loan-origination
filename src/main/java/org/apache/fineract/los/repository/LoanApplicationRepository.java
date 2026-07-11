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

package org.apache.fineract.los.repository;

import java.util.List;
import java.util.Optional;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link LoanApplication} aggregate root.
 *
 * <p>All query methods are scoped by {@code tenantId} — never use methods without tenant isolation
 * as they would expose data across institutions.
 *
 * <p>Naming convention follows Spring Data JPA method derivation — no manual SQL required for
 * standard queries.
 */
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

  /**
   * Finds a loan application by its internal ID and tenant.
   *
   * <p>Always use this over {@code findById} to enforce tenant isolation.
   *
   * @param id internal surrogate key
   * @param tenantId institution identifier from request header
   * @return the application if found and belongs to tenant
   */
  Optional<LoanApplication> findByIdAndTenantId(Long id, String tenantId);

  /**
   * Finds a loan application by its human-readable reference.
   *
   * <p>Used when clients provide the application reference (e.g. LOS-2026-00101) instead of the
   * internal ID.
   *
   * @param applicationRef human-readable reference
   * @param tenantId institution identifier
   * @return the application if found and belongs to tenant
   */
  Optional<LoanApplication> findByApplicationRefAndTenantId(String applicationRef, String tenantId);

  /**
   * Returns all applications for a tenant in a given status.
   *
   * <p>Used by loan officers to fetch their work queue (e.g. all SUBMITTED applications awaiting
   * review).
   *
   * @param status lifecycle status to filter by
   * @param tenantId institution identifier
   * @return list of matching applications
   */
  List<LoanApplication> findAllByStatusAndTenantId(LoanApplicationStatus status, String tenantId);

  /**
   * Returns a paginated list of all applications for a tenant.
   *
   * <p>Used for the main application dashboard. Pagination is mandatory — never load all
   * applications at once.
   *
   * @param tenantId institution identifier
   * @param pageable pagination and sort parameters
   * @return paginated applications
   */
  Page<LoanApplication> findAllByTenantId(String tenantId, Pageable pageable);

  /**
   * Returns a paginated list of applications filtered by status.
   *
   * <p>Used for role-specific dashboards — loan officers see UNDER_REVIEW, branch managers see
   * APPROVED etc.
   *
   * @param status lifecycle status to filter by
   * @param tenantId institution identifier
   * @param pageable pagination and sort parameters
   * @return paginated applications in the given status
   */
  Page<LoanApplication> findAllByStatusAndTenantId(
      LoanApplicationStatus status, String tenantId, Pageable pageable);

  /**
   * Checks whether an application reference already exists for a given tenant.
   *
   * <p>Used by the service layer before generating a new reference to guarantee uniqueness within a
   * tenant.
   *
   * @param applicationRef reference to check
   * @param tenantId institution identifier
   * @return true if the reference already exists
   */
  boolean existsByApplicationRefAndTenantId(String applicationRef, String tenantId);

  /**
   * Counts applications in a given status for a tenant.
   *
   * <p>Used for dashboard metrics and reporting.
   *
   * @param status lifecycle status to count
   * @param tenantId institution identifier
   * @return count of matching applications
   */
  long countByStatusAndTenantId(LoanApplicationStatus status, String tenantId);

  /**
   * Counts the total number of applications ever created for a tenant, regardless of status.
   *
   * <p>Used by {@code LoanApplicationService} as the seed value when generating the next
   * human-readable application reference (LOS-{YEAR}-{SEQUENCE}). Collisions are still checked via
   * {@link #existsByApplicationRefAndTenantId} — this count is a starting point, not a guarantee of
   * uniqueness.
   *
   * @param tenantId institution identifier
   * @return total number of applications for the tenant
   */
  long countByTenantId(String tenantId);
}
