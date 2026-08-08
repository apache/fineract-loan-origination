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
 * KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.los.infrastructure.fineract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FineractAuthResponse {

  private Long userId;

  private String username;

  private String base64EncodedAuthenticationKey;

  private boolean authenticated;

  /**
   * Fineract role objects assigned to this user. Each role has an id, name (e.g. "loan_officer"),
   * and description.
   */
  private List<FineractRole> roles;

  /**
   * Fineract raw permission codes (e.g. "CREATE_CLIENT"). These are action-level permissions, NOT
   * role names — do not use for role mapping.
   */
  private List<String> permissions;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FineractRole {
    private Long id;
    private String name;
    private String description;
    private boolean disabled;
  }
}
