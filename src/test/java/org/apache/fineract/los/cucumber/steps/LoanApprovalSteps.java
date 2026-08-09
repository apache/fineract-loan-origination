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

package org.apache.fineract.los.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.apache.fineract.los.service.ApprovalWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

public class LoanApprovalSteps {

  @Autowired private LoanApplicationRepository loanApplicationRepository;

  @Autowired private ApprovalStageRepository approvalStageRepository;

  @Autowired private ApprovalWorkflowService approvalWorkflowService;

  @Before
  public void setUp() {
    approvalStageRepository.deleteAll();
    loanApplicationRepository.deleteAll();
  }

  @Given("a loan application with reference {string} exists in UNDER_REVIEW status")
  public void aLoanApplicationExists(String ref) {
    LoanApplication app = new LoanApplication();
    app.setApplicationRef(ref);
    app.setTenantId("default");
    app.setStatus(LoanApplicationStatus.UNDER_REVIEW);
    app.setRequestedAmount(new BigDecimal("10000.00"));
    app.setCurrency("USD");
    app.setLoanPurpose("Test loan for Cucumber scenario");
    app.setTenorMonths(12);
    loanApplicationRepository.save(app);
  }

  @When("the {string} officer {string} approves application {string}")
  public void officerApprovesApplication(String stage, String officer, String ref) {
    ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.APPROVE)
            .comments("Approved")
            .build();
    approvalWorkflowService.recordDecision(
        ref, "default", request, authenticationFor(stage, officer));
  }

  @When("the {string} officer {string} rejects application {string} with comment {string}")
  public void officerRejectsApplication(String stage, String officer, String ref, String comment) {
    ApprovalDecisionRequest request =
        ApprovalDecisionRequest.builder()
            .decision(ApprovalDecision.REJECT)
            .comments(comment)
            .build();
    approvalWorkflowService.recordDecision(
        ref, "default", request, authenticationFor(stage, officer));
  }

  @Then("the application {string} status is {string}")
  public void theApplicationStatusIs(String ref, String expectedStatus) {
    LoanApplication app =
        loanApplicationRepository.findByApplicationRefAndTenantId(ref, "default").orElseThrow();
    assertThat(app.getStatus().name()).isEqualTo(expectedStatus);
  }

  /**
   * Builds a test {@link Authentication} whose principal name is {@code officer} and whose sole
   * granted authority is {@code ROLE_<stage>}, matching what {@link
   * org.apache.fineract.los.security.LosRole#fromAuthorities} expects to resolve the caller's LOS
   * workflow role.
   */
  private Authentication authenticationFor(String stage, String officer) {
    return new TestingAuthenticationToken(officer, null, "ROLE_" + stage);
  }
}
