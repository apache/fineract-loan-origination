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

package org.apache.fineract.los.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.apache.fineract.los.domain.enums.ApprovalDecision;
import org.apache.fineract.los.dto.request.ApprovalDecisionRequest;
import org.apache.fineract.los.dto.request.CreateLoanApplicationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for input validation constraints on request DTOs.
 *
 * <p>Verifies that Bean Validation annotations (@Size, @Pattern, @NotBlank, etc.) correctly reject
 * invalid input and prevent:
 *
 * <ul>
 *   <li>SQL injection via malformed strings
 *   <li>XSS attacks via script tags in user input
 *   <li>Path traversal via directory navigation sequences
 *   <li>DoS attacks via unbounded string lengths
 *   <li>Data integrity issues via malformed enums and formats
 * </ul>
 */
@DisplayName("Input Validation Security Tests")
class InputValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Nested
  @DisplayName("CreateLoanApplicationRequest Validation")
  class CreateLoanApplicationRequestValidation {

    @Test
    @DisplayName("Valid request passes validation")
    void validRequestPassesValidation() {
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          CreateLoanApplicationRequest.ApplicantDetails.builder()
              .fullName("John Doe")
              .nationalId("AB123456")
              .monthlyIncome(new BigDecimal("5000.00"))
              .employmentStatus("EMPLOYED")
              .employmentDurationMonths(24)
              .existingLoanObligations(BigDecimal.ZERO)
              .fineractClientId(1L)
              .build();

      CreateLoanApplicationRequest request =
          CreateLoanApplicationRequest.builder()
              .requestedAmount(new BigDecimal("10000.00"))
              .currency("USD")
              .loanPurpose("BUSINESS")
              .tenorMonths(12)
              .fineractLoanProductId(1L)
              .applicant(applicant)
              .build();

      Set<ConstraintViolation<CreateLoanApplicationRequest>> violations =
          validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Currency with invalid ISO 4217 format is rejected")
    void invalidCurrencyFormatRejected() {
      CreateLoanApplicationRequest request =
          buildValidRequest().currency("us").build(); // Must be 3 uppercase letters

      Set<ConstraintViolation<CreateLoanApplicationRequest>> violations =
          validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    @DisplayName("Currency with lowercase letters is rejected")
    void lowercaseCurrencyRejected() {
      CreateLoanApplicationRequest request = buildValidRequest().currency("usd").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest>> violations =
          validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    @DisplayName("Invalid loan purpose enum is rejected")
    void invalidLoanPurposeRejected() {
      CreateLoanApplicationRequest request =
          buildValidRequest().loanPurpose("INVALID_PURPOSE").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest>> violations =
          validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("loanPurpose"));
    }

    @Test
    @DisplayName("XSS payload in loan purpose is rejected")
    void xssInLoanPurposeRejected() {
      CreateLoanApplicationRequest request =
          buildValidRequest().loanPurpose("<script>alert('XSS')</script>").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest>> violations =
          validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("loanPurpose"));
    }

    @Test
    @DisplayName("Full name exceeding 100 characters is rejected")
    void longFullNameRejected() {
      String tooLongName = "A".repeat(101);
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          buildValidApplicant().fullName(tooLongName).build();

      Set<ConstraintViolation<CreateLoanApplicationRequest.ApplicantDetails>> violations =
          validator.validate(applicant);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    @DisplayName("Full name with special characters (SQL injection attempt) is rejected")
    void sqlInjectionInFullNameRejected() {
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          buildValidApplicant().fullName("John'; DROP TABLE users; --").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest.ApplicantDetails>> violations =
          validator.validate(applicant);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    @DisplayName("Full name with only allowed special characters passes")
    void fullNameWithHyphenAndApostropheAccepted() {
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          buildValidApplicant().fullName("Mary-Jane O'Brien").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest.ApplicantDetails>> violations =
          validator.validate(applicant);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("National ID with lowercase letters is rejected")
    void lowercaseNationalIdRejected() {
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          buildValidApplicant().nationalId("ab123456").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest.ApplicantDetails>> violations =
          validator.validate(applicant);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("nationalId"));
    }

    @Test
    @DisplayName("National ID exceeding 50 characters is rejected")
    void longNationalIdRejected() {
      String tooLongId = "A".repeat(51);
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          buildValidApplicant().nationalId(tooLongId).build();

      Set<ConstraintViolation<CreateLoanApplicationRequest.ApplicantDetails>> violations =
          validator.validate(applicant);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("nationalId"));
    }

    @Test
    @DisplayName("Invalid employment status enum is rejected")
    void invalidEmploymentStatusRejected() {
      CreateLoanApplicationRequest.ApplicantDetails applicant =
          buildValidApplicant().employmentStatus("INVALID_STATUS").build();

      Set<ConstraintViolation<CreateLoanApplicationRequest.ApplicantDetails>> violations =
          validator.validate(applicant);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("employmentStatus"));
    }

    private CreateLoanApplicationRequest.CreateLoanApplicationRequestBuilder buildValidRequest() {
      return CreateLoanApplicationRequest.builder()
          .requestedAmount(new BigDecimal("10000.00"))
          .currency("USD")
          .loanPurpose("BUSINESS")
          .tenorMonths(12)
          .fineractLoanProductId(1L)
          .applicant(buildValidApplicant().build());
    }

    private CreateLoanApplicationRequest.ApplicantDetails.ApplicantDetailsBuilder
        buildValidApplicant() {
      return CreateLoanApplicationRequest.ApplicantDetails.builder()
          .fullName("John Doe")
          .nationalId("AB123456")
          .monthlyIncome(new BigDecimal("5000.00"))
          .employmentStatus("EMPLOYED")
          .employmentDurationMonths(24)
          .existingLoanObligations(BigDecimal.ZERO)
          .fineractClientId(1L);
    }
  }

  @Nested
  @DisplayName("ApprovalDecisionRequest Validation")
  class ApprovalDecisionRequestValidation {

    @Test
    @DisplayName("Valid approval decision passes validation")
    void validApprovalDecisionPasses() {
      ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .decision(ApprovalDecision.APPROVE)
              .comments("Application meets all credit criteria. Approved for disbursement.")
              .build();

      Set<ConstraintViolation<ApprovalDecisionRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Comments shorter than 10 characters are rejected")
    void shortCommentsRejected() {
      ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .decision(ApprovalDecision.APPROVE)
              .comments("OK")
              .build();

      Set<ConstraintViolation<ApprovalDecisionRequest>> violations = validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("comments"));
    }

    @Test
    @DisplayName("Comments exceeding 2000 characters are rejected (DoS prevention)")
    void longCommentsRejected() {
      String tooLongComments = "A".repeat(2001);
      ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .decision(ApprovalDecision.APPROVE)
              .comments(tooLongComments)
              .build();

      Set<ConstraintViolation<ApprovalDecisionRequest>> violations = validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("comments"));
    }

    @Test
    @DisplayName("Empty comments are rejected")
    void emptyCommentsRejected() {
      ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder().decision(ApprovalDecision.APPROVE).comments("").build();

      Set<ConstraintViolation<ApprovalDecisionRequest>> violations = validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("comments"));
    }

    @Test
    @DisplayName("Null decision is rejected")
    void nullDecisionRejected() {
      ApprovalDecisionRequest request =
          ApprovalDecisionRequest.builder()
              .decision(null)
              .comments("This should fail due to null decision")
              .build();

      Set<ConstraintViolation<ApprovalDecisionRequest>> violations = validator.validate(request);

      assertThat(violations)
          .isNotEmpty()
          .anyMatch(v -> v.getPropertyPath().toString().equals("decision"));
    }
  }

  @Nested
  @DisplayName("ValidApplicationRef Annotation Tests")
  class ValidApplicationRefTests {

    @Test
    @DisplayName("Valid application ref LOS-2026-00001 passes")
    void validApplicationRefPasses() {
      TestObject obj = new TestObject();
      obj.applicationRef = "LOS-2026-00001";

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Application ref with wrong format is rejected")
    void wrongFormatRejected() {
      TestObject obj = new TestObject();
      obj.applicationRef = "APP-2026-00001"; // Wrong prefix

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Application ref with 3-digit year is rejected")
    void threeDigitYearRejected() {
      TestObject obj = new TestObject();
      obj.applicationRef = "LOS-026-00001";

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Application ref with 4-digit sequence is rejected")
    void fourDigitSequenceRejected() {
      TestObject obj = new TestObject();
      obj.applicationRef = "LOS-2026-0001";

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Path traversal attempt is rejected")
    void pathTraversalRejected() {
      TestObject obj = new TestObject();
      obj.applicationRef = "../../../etc/passwd";

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("XSS payload is rejected")
    void xssPayloadRejected() {
      TestObject obj = new TestObject();
      obj.applicationRef = "<script>alert(1)</script>";

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("SQL injection attempt is rejected")
    void sqlInjectionRejected() {
      TestObject obj = new TestObject();
      obj.applicationRef = "LOS-2026-00001'; DROP TABLE loan_application; --";

      Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

      assertThat(violations).isNotEmpty();
    }

    // Test helper class
    private static class TestObject {
      @ValidApplicationRef public String applicationRef;
    }
  }
}
