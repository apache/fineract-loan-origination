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
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.fineract.los.cucumber.CucumberSpringConfig;
import org.apache.fineract.los.domain.CustomerCredential;
import org.apache.fineract.los.repository.CustomerCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CustomerAuthSteps {

  @Autowired private CustomerCredentialRepository customerCredentialRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private CucumberSpringConfig config;

  private Response response;

  @Before
  public void setUp() {
    RestAssured.port = config.port;
    customerCredentialRepository.deleteAll();
  }

  @Given("a registered customer with username {string} and password {string}")
  public void aRegisteredCustomer(String username, String password) {
    CustomerCredential credential = new CustomerCredential();
    credential.setUsername(username);
    credential.setPasswordHash(passwordEncoder.encode(password));
    credential.setFineractClientId(1L);
    credential.setTenantId("default");
    customerCredentialRepository.save(credential);
  }

  @When("the customer logs in with username {string} and password {string}")
  public void theCustomerLogsIn(String username, String password) {
    response =
        RestAssured.given()
            .contentType("application/json")
            .body(
                """
                        {
                          "username": "%s",
                          "password": "%s",
                          "tenantId": "default"
                        }
                        """
                    .formatted(username, password))
            .post("/api/v1/auth/login");
  }

  @Then("the response status is {int}")
  public void theResponseStatusIs(int status) {
    assertThat(response.statusCode()).isEqualTo(status);
  }

  @Then("the response contains a JWT token")
  public void theResponseContainsAJwtToken() {
    String token = response.jsonPath().getString("token");
    assertThat(token).isNotNull().isNotBlank();
  }
}
