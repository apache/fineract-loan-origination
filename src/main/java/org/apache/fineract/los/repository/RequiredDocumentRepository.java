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
import org.apache.fineract.los.domain.RequiredDocument;
import org.apache.fineract.los.domain.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link RequiredDocument} entity.
 *
 * RequiredDocument has a many-to-one relationship with {@link LoanApplication}. One application
 * can have multiple required documents across different document types.
 *
 * The state machine uses this repository to check whether all required documents are verified
 * before allowing application state transitions.
 */
@Repository
public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {

  /**
   * Returns all documents for a given application.
   *
   * Primary access pattern — documents are always retrieved in context of their parent
   * application.
   *
   * @param application the parent loan application
   * @return all documents belonging to the application
   */
  List<RequiredDocument> findAllByApplication(LoanApplication application);

  /**
   * Returns all documents for an application in a specific status.
   *
   * Used by the state machine to verify all required documents are VERIFIED before allowing
   * transition from SUBMITTED to UNDER_REVIEW.
   *
   * @param application the parent loan application
   * @param documentStatus status to filter by
   * @return documents in the given status
   */
  List<RequiredDocument> findAllByApplicationAndDocumentStatus(
      LoanApplication application, DocumentStatus documentStatus);

  /**
   * Finds a specific document by ID scoped to a tenant.
   *
   * Used for document-level operations (verify, reject, delete) with tenant isolation enforced.
   *
   * @param id document internal ID
   * @param tenantId institution identifier
   * @return the document if found and belongs to tenant
   */
  Optional<RequiredDocument> findByIdAndTenantId(Long id, String tenantId);

  /**
   * Checks whether any unverified documents exist for an application.
   *
   * Used by the state machine before allowing transition to UNDER_REVIEW. If this returns true,
   * the transition is blocked until all documents are verified.
   *
   * @param application the parent loan application
   * @param documentStatus status to check for existence
   * @return true if at least one document is in that status
   */
  boolean existsByApplicationAndDocumentStatus(
      LoanApplication application, DocumentStatus documentStatus);

  /**
   * Counts documents by status for a given application.
   *
   * Used for document completion tracking in the applicant-facing status view.
   *
   * @param application the parent loan application
   * @param documentStatus status to count
   * @return count of documents in the given status
   */
  long countByApplicationAndDocumentStatus(
      LoanApplication application, DocumentStatus documentStatus);
}
