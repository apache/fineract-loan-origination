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

package org.apache.fineract.los.workflow;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

/**
 * Externalised configuration for the ordered list of multi-stage approval workflow stages.
 *
 * <p>Bound from {@code application.yml} under the prefix {@code los.workflow}. Order in the list is
 * significant — {@code ApprovalWorkflowService} treats it as the sequence an application must pass
 * through: LOAN_OFFICER → BRANCH_MANAGER → CREDIT_COMMITTEE by default. Institutions can add,
 * remove, or reorder stages purely via configuration, with no code change.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "los.workflow")
public class ApprovalWorkflowProperties {

  /** Ordered list of approval stage names. Defaults to the standard three-stage MFI workflow. */
  private List<String> stages = List.of("LOAN_OFFICER", "BRANCH_MANAGER", "CREDIT_COMMITTEE");

  /**
   * Validates that at least one stage is configured on startup — an empty stage list would make it
   * impossible for any application to ever reach APPROVED.
   */
  @PostConstruct
  public void validateStagesNotEmpty() {
    if (CollectionUtils.isEmpty(stages)) {
      throw new IllegalStateException(
          "los.workflow.stages must contain at least one approval stage name.");
    }
  }

  /**
   * Returns the zero-based index of the given stage name in the configured sequence.
   *
   * @param stageName stage name to look up
   * @return index in {@link #stages}, or -1 if not configured
   */
  public int indexOf(final String stageName) {
    return stages.indexOf(stageName);
  }

  /**
   * Returns true if the given stage name is the final stage in the configured sequence — i.e. an
   * APPROVE decision here completes the entire workflow.
   *
   * @param stageName stage name to check
   * @return true if this is the last configured stage
   */
  public boolean isFinalStage(final String stageName) {
    final int index = indexOf(stageName);
    return index >= 0 && index == stages.size() - 1;
  }
}
