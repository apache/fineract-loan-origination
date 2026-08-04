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

package org.apache.fineract.los.exception;

/**
 * Thrown when an authenticated staff member has no LOS workflow role — their Fineract roles don't
 * map to any entry in {@code los.workflow.role-mapping}. Examples: a "Super user" or "Self Service
 * User" who isn't LOAN_OFFICER/BRANCH_MANAGER/CREDIT_COMMITTEE attempting to record an approval
 * decision.
 *
 * <p>Unchecked — the global exception handler maps this to HTTP 403 Forbidden.
 */
public class LosRoleNotAssignedException extends RuntimeException {

  public LosRoleNotAssignedException(final String username) {
    super(String.format(LosErrorConstants.MSG_NO_LOS_ROLE_TEMPLATE, username));
  }
}
