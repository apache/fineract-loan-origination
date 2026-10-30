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

package org.apache.fineract.los.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.los.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "los.fineract.mock-enabled=true",
      "los.workflow.stages[0]=LOAN_OFFICER",
      "los.workflow.stages[1]=CREDIT_COMMITTEE",
      "los.workflow.stages[2]=BRANCH_MANAGER",
      "los.workflow.final-stage=BRANCH_MANAGER",
      "los.workflow.role-mapping.loan_officer=LOAN_OFFICER",
      "los.workflow.role-mapping.credit_committee=CREDIT_COMMITTEE",
      "los.workflow.role-mapping.branch_manager=BRANCH_MANAGER"
    })
@DisplayName("Loan application endpoint security")
class LoanApplicationSecurityIT {

  private static final String TENANT_ID = "default";
  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final Long CLIENT_ID = 900001L;

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("los_test")
          .withUsername("test")
          .withPassword("test");

  @LocalServerPort private int port;
  @Autowired private JwtService jwtService;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  private String applicationRef;
  private String customerToken;

  @BeforeEach
  void createApplication() throws Exception {
    customerToken = jwtService.generateToken("security-test-customer", CLIENT_ID, TENANT_ID);
    final Response response =
        given()
            .port(port)
            .header("Authorization", bearer(customerToken))
            .header(TENANT_HEADER, TENANT_ID)
            .contentType("application/json")
            .body(
                """
                        {
                          "requestedAmount": 1000,
                          "currency": "USD",
                          "loanPurpose": "PERSONAL",
                          "tenorMonths": 12,
                          "applicant": {
                            "fullName": "Security Test Applicant",
                            "nationalId": "TEST-ONLY-ID",
                            "monthlyIncome": 5000,
                            "employmentStatus": "EMPLOYED",
                            "employmentDurationMonths": 24,
                            "existingLoanObligations": 0
                          }
                        }
                """)
            .post("/api/v1/customer/loan-applications");
    Assertions.assertEquals(201, response.statusCode());
    applicationRef = response.jsonPath().getString("applicationRef");
  }

  @Test
  @DisplayName("Customer JWT cannot read staff detail or applicant data")
  void customerTokenOnStaffDetailReturns403WithoutBodyData() throws Exception {
    final Response response = get(staffDetailPath(), customerToken);
    Assertions.assertEquals(403, response.statusCode());

    assertNoSensitiveData(response.getBody().asString());
  }

  @Test
  @DisplayName("Loan officer and branch manager JWTs can read staff detail and list")
  void loanOfficerAndBranchManagerCanReadStaffDetailAndList() throws Exception {
    for (String role : List.of("LOAN_OFFICER", "BRANCH_MANAGER")) {
      final String token = jwtService.generateStaffToken("security-test-staff", TENANT_ID, role);
      Assertions.assertEquals(200, get(staffDetailPath(), token).statusCode());
      Assertions.assertEquals(200, get("/api/v1/loan-applications", token).statusCode());
    }
  }

  @Test
  @DisplayName("Customer JWT cannot list staff applications")
  void customerTokenOnStaffListReturns403() throws Exception {
    Assertions.assertEquals(403, get("/api/v1/loan-applications", customerToken).statusCode());
  }

  @Test
  @DisplayName("Customer JWT is denied on every LoanApplicationController mapping")
  void customerTokenIsDeniedOnEveryLoanApplicationControllerMapping() throws Exception {
    final List<HttpMethodAndPath> requests = new ArrayList<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (mapping, handler) -> {
              if (handler.getBeanType().equals(LoanApplicationController.class)) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                  for (String pattern : mapping.getPatternValues()) {
                    final String path = pattern.replace("{applicationRef}", applicationRef);
                    requests.add(new HttpMethodAndPath(HttpMethod.valueOf(method.name()), path));
                  }
                }
              }
            });

    assertThat(requests).hasSize(7);
    for (HttpMethodAndPath request : requests) {
      Assertions.assertEquals(
          403, request(request.method(), request.path(), customerToken).statusCode());
    }
  }

  @Test
  @DisplayName("Anonymous staff-detail request is rejected without applicant data")
  void anonymousStaffDetailRequestIsRejectedWithoutData() throws Exception {
    final Response response =
        given().port(port).header(TENANT_HEADER, TENANT_ID).get(staffDetailPath());

    assertThat(response.statusCode()).isIn(401, 403);
    assertNoSensitiveData(response.getBody().asString());
  }

  private String staffDetailPath() {
    return "/api/v1/loan-applications/" + applicationRef + "/staff-detail";
  }

  private Response get(String path, String token) {
    return given()
        .port(port)
        .header("Authorization", bearer(token))
        .header(TENANT_HEADER, TENANT_ID)
        .get(path);
  }

  private Response request(HttpMethod method, String path, String token) {
    return given()
        .port(port)
        .header("Authorization", bearer(token))
        .header(TENANT_HEADER, TENANT_ID)
        .request(method.name(), path);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static void assertNoSensitiveData(String body) {
    assertThat(body).doesNotContain("applicantName", "nationalId", "creditScore", "monthlyIncome");
  }

  private record HttpMethodAndPath(HttpMethod method, String path) {}
}
