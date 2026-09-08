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
import org.apache.fineract.los.domain.CreditScore;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@link CreditScore} entity.
 *
 * <p>A credit score has a one-to-one relationship with {@link LoanApplication}. It is computed once
 * when the application enters UNDER_REVIEW and is immutable thereafter.
 */
@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {

  /**
   * Finds the credit score for a given application.
   *
   * @param application the parent loan application
   * @return the credit score if it has been computed
   */
  Optional<CreditScore> findByApplication(LoanApplication application);

  /**
   * Finds the credit score by application ID and tenant.
   *
   * <p>Used when only the application ID is available, avoiding a load of the full application
   * entity. Traverses the join to {@link LoanApplication} to enforce tenant isolation.
   *
   * @param applicationId internal application ID
   * @param tenantId institution identifier
   * @return the credit score if found
   */
  Optional<CreditScore> findByApplication_IdAndApplication_TenantId(
      Long applicationId, String tenantId);

  /**
   * Checks whether a credit score exists for an application.
   *
   * <p>Used by the service layer to prevent duplicate score computation for the same application.
   *
   * @param application the parent loan application
   * @return true if a score has already been computed
   */
  boolean existsByApplication(LoanApplication application);

  /**
   * Counts credit scores by risk category for a tenant.
   *
   * <p>Used for portfolio-level risk reporting — how many HIGH/MEDIUM/LOW risk applications exist
   * across the institution's portfolio. Traverses the join to {@link LoanApplication} for tenant
   * scoping.
   *
   * @param riskCategory risk classification to count
   * @param tenantId institution identifier
   * @return count of scores in the risk category for the tenant
   */
  long countByRiskCategoryAndApplication_TenantId(RiskCategory riskCategory, String tenantId);
}
