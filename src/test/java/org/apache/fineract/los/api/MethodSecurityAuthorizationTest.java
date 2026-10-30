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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.fineract.los.bridge.DisbursementBridgeService;
import org.apache.fineract.los.exception.GlobalExceptionHandler;
import org.apache.fineract.los.exception.LosErrorConstants;
import org.apache.fineract.los.repository.ApprovalStageRepository;
import org.apache.fineract.los.repository.CustomerCredentialRepository;
import org.apache.fineract.los.security.CustomerPrincipal;
import org.apache.fineract.los.security.StaffPrincipal;
import org.apache.fineract.los.service.ApprovalWorkflowService;
import org.apache.fineract.los.service.CreditScoringService;
import org.apache.fineract.los.service.LoanApplicationService;
import org.apache.fineract.los.service.StaffManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP-level tests for {@code @PreAuthorize} denials.
 *
 * <p>Controllers are wrapped with Spring Security's real {@code @PreAuthorize} interceptor, so the
 * annotations are evaluated as in the running application, and {@link GlobalExceptionHandler} maps
 * the resulting denial.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Method security authorization failures")
class MethodSecurityAuthorizationTest {

  private static final String TENANT_HEADER = "X-Fineract-Platform-TenantId";
  private static final String TENANT_ID = "default";
  private static final String APPLICATION_REF = "LOS-2026-00001";

  @Mock private DisbursementBridgeService disbursementBridgeService;
  @Mock private LoanApplicationService loanApplicationService;
  @Mock private CreditScoringService creditScoringService;
  @Mock private ApprovalWorkflowService approvalWorkflowService;
  @Mock private StaffManagementService staffManagementService;
  @Mock private ApprovalStageRepository approvalStageRepository;
  @Mock private CustomerCredentialRepository customerCredentialRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                withMethodSecurity(new DisbursementController(disbursementBridgeService)),
                withMethodSecurity(
                    new LoanApplicationController(
                        loanApplicationService, creditScoringService, approvalWorkflowService)),
                withMethodSecurity(new AdminStaffController(staffManagementService)),
                withMethodSecurity(
                    new ApprovalController(
                        approvalWorkflowService, approvalStageRepository, loanApplicationService)),
                withMethodSecurity(
                    new AdminCustomerController(customerCredentialRepository, passwordEncoder)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Customer calling disburse gets 403 and no disbursement runs")
  void customerCannotDisburse() throws Exception {
    loginAs(customer());

    mockMvc
        .perform(
            post("/api/v1/loan-applications/{ref}/disburse", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.error").value("Forbidden"))
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_ACCESS_DENIED));

    verifyNoInteractions(disbursementBridgeService);
  }

  @Test
  @DisplayName("Customer calling start-review gets 403 and the application is not moved")
  void customerCannotStartReview() throws Exception {
    loginAs(customer());

    mockMvc
        .perform(
            post("/api/v1/loan-applications/{ref}/start-review", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_ACCESS_DENIED));

    verify(loanApplicationService, never()).moveToUnderReview(anyString(), anyString());
  }

  @Test
  @DisplayName("Loan officer calling an admin-only endpoint gets 403")
  void staffWithoutAdminRoleCannotListStaff() throws Exception {
    loginAs(loanOfficer());

    mockMvc
        .perform(get("/api/v1/admin/staff").param("tenantId", TENANT_ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_ACCESS_DENIED));

    verifyNoInteractions(staffManagementService);
  }

  @Test
  @DisplayName("Customer reading approval history gets 403 and no history is loaded")
  void customerCannotReadApprovalHistory() throws Exception {
    loginAs(customer());

    mockMvc
        .perform(
            get("/api/v1/loan-applications/{ref}/approval-decisions", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_ACCESS_DENIED));

    verifyNoInteractions(approvalStageRepository, loanApplicationService);
  }

  @Test
  @DisplayName("Loan officer registering a customer account gets 403 and nothing is saved")
  void staffWithoutAdminRoleCannotRegisterCustomer() throws Exception {
    loginAs(loanOfficer());

    mockMvc
        .perform(
            post("/api/v1/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"new.customer","password":"a-long-password",\
                    "fineractClientId":42,"tenantId":"default"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(LosErrorConstants.MSG_ACCESS_DENIED));

    verifyNoInteractions(customerCredentialRepository, passwordEncoder);
  }

  @Test
  @DisplayName("Staff calling disburse still reaches the disbursement service")
  void staffCanDisburse() throws Exception {
    when(disbursementBridgeService.executeFullDisbursement(APPLICATION_REF, TENANT_ID))
        .thenReturn(100001L);
    loginAs(loanOfficer());

    mockMvc
        .perform(
            post("/api/v1/loan-applications/{ref}/disburse", APPLICATION_REF)
                .header(TENANT_HEADER, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(content().string("100001"));

    verify(disbursementBridgeService).executeFullDisbursement(any(), any());
  }

  @SuppressWarnings("unchecked")
  private static <T> T withMethodSecurity(final T controller) {
    final ProxyFactory proxyFactory = new ProxyFactory(controller);
    proxyFactory.setProxyTargetClass(true);
    proxyFactory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
    return (T) proxyFactory.getProxy();
  }

  private static CustomerPrincipal customer() {
    return new CustomerPrincipal("customer1", null, 101L, TENANT_ID, "Customer");
  }

  private static StaffPrincipal loanOfficer() {
    return new StaffPrincipal("officer1", TENANT_ID, "ROLE_LOAN_OFFICER");
  }

  private static void loginAs(final UserDetails principal) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
