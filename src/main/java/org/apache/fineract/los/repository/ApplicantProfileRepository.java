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

import java.util.Optional;
import org.apache.fineract.los.domain.ApplicantProfile;
import org.apache.fineract.los.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ApplicantProfile} entity.
 *
 * <p>ApplicantProfile has a one-to-one relationship with {@link LoanApplication}. All queries are
 * scoped by {@code tenantId} for tenant isolation.
 */
@Repository
public interface ApplicantProfileRepository extends JpaRepository<ApplicantProfile, Long> {

  /**
   * Finds the applicant profile for a given application.
   *
   * <p>Primary access pattern — profile is always looked up via its parent application.
   *
   * @param application the parent loan application
   * @return the applicant profile if it exists
   */
  /**
   * Finds all applicant profiles belonging to a given Fineract client, scoped to a tenant.
   *
   * <p>Used by the customer-facing controller to scope "my applications" to only what the
   * authenticated customer's clientId actually owns.
   *
   * @param fineractClientId the Fineract client identifier of the logged-in customer
   * @param tenantId institution identifier
   * @return all applicant profiles for that client within the tenant
   */
  java.util.List<ApplicantProfile> findAllByFineractClientIdAndTenantId(
      Long fineractClientId, String tenantId);

  Optional<ApplicantProfile> findByApplication(LoanApplication application);

  /**
   * Finds the applicant profile by application ID and tenant.
   *
   * <p>Used when only the application ID is available without loading the full application entity
   * first. Traverses the join to {@link LoanApplication} for tenant isolation.
   *
   * @param applicationId internal application ID
   * @param tenantId institution identifier
   * @return the applicant profile if found
   */
  Optional<ApplicantProfile> findByApplication_IdAndApplication_TenantId(
      Long applicationId, String tenantId);

  /**
   * Checks whether a profile already exists for an application.
   *
   * <p>Used by the service layer to prevent duplicate profile creation for the same application.
   *
   * @param application the parent loan application
   * @return true if a profile already exists
   */
  boolean existsByApplication(LoanApplication application);
}
