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

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import org.apache.fineract.los.bridge.MockFineractLoanApiClient;
import org.apache.fineract.los.bridge.dto.FineractLoanCreateRequest;
import org.apache.fineract.los.domain.LoanApplication;
import org.apache.fineract.los.domain.enums.LoanApplicationStatus;
import org.apache.fineract.los.repository.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

public class FineractLoanIdSteps {

  @Autowired private LoanApplicationRepository loanApplicationRepository;

  private Long createdLoanId;

  private RuntimeException linkFailure;

  @Given("application {string} in tenant {string} is linked to Fineract loan {long}")
  public void applicationIsLinkedToFineractLoan(String ref, String tenantId, long loanId) {
    loanApplicationRepository.saveAndFlush(applicationLinkedTo(ref, tenantId, loanId));
  }

  @When("another application {string} in tenant {string} is linked to Fineract loan {long}")
  public void tryToLinkApplicationToFineractLoan(String ref, String tenantId, long loanId) {
    try {
      loanApplicationRepository.saveAndFlush(applicationLinkedTo(ref, tenantId, loanId));
    } catch (DataIntegrityViolationException ex) {
      linkFailure = ex;
    }
  }

  @When("a freshly started mock Fineract adapter creates a loan")
  public void aFreshlyStartedMockAdapterCreatesALoan() {
    // A new instance has a fresh in-memory counter, the same as after an application restart.
    MockFineractLoanApiClient client = new MockFineractLoanApiClient(loanApplicationRepository);
    createdLoanId =
        client
            .createLoan(
                FineractLoanCreateRequest.builder()
                    .clientId(1001L)
                    .productId(1L)
                    .principal(new BigDecimal("10000.00"))
                    .build())
            .getLoanId();
  }

  @Then("the new Fineract loan ID is {long}")
  public void theNewFineractLoanIdIs(long expected) {
    assertThat(createdLoanId).isEqualTo(expected);
  }

  @Then("the link is rejected as a duplicate")
  public void theLinkIsRejectedAsADuplicate() {
    assertThat(linkFailure).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Then("the link is saved")
  public void theLinkIsSaved() {
    assertThat(linkFailure).isNull();
  }

  private LoanApplication applicationLinkedTo(String ref, String tenantId, long loanId) {
    LoanApplication app = new LoanApplication();
    app.setApplicationRef(ref);
    app.setTenantId(tenantId);
    app.setStatus(LoanApplicationStatus.APPROVED);
    app.setRequestedAmount(new BigDecimal("10000.00"));
    app.setCurrency("USD");
    app.setLoanPurpose("Test loan for Cucumber scenario");
    app.setTenorMonths(12);
    app.setFineractLoanId(loanId);
    return app;
  }
}
