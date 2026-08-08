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
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

/** Externalised configuration for the ordered list of multi-stage approval workflow stages. */
@Getter
@Setter
@ConfigurationProperties(prefix = "los.workflow")
public class ApprovalWorkflowProperties {

  /** Ordered approval stages. */
  private List<String> stages =
      new java.util.ArrayList<>(
          java.util.List.of("LOAN_OFFICER", "CREDIT_COMMITTEE", "BRANCH_MANAGER"));

  /**
   * Maps Fineract role names to LOS workflow stages.
   *
   * <p>Example:
   *
   * <p>loan_officer -> LOAN_OFFICER
   */
  private Map<String, String> roleMapping = new java.util.HashMap<>();

  @PostConstruct
  public void validateStagesNotEmpty() {
    if (CollectionUtils.isEmpty(stages)) {
      throw new IllegalStateException(
          "los.workflow.stages must contain at least one approval stage.");
    }
  }

  public int indexOf(final String stageName) {
    return stages.indexOf(stageName);
  }

  public boolean isFinalStage(final String stageName) {
    final int index = indexOf(stageName);
    return index >= 0 && index == stages.size() - 1;
  }

  /** Returns the configured workflow stage for a Fineract role. */
  public String stageForFineractRole(final String fineractRoleName) {
    return roleMapping.get(fineractRoleName);
  }
}
