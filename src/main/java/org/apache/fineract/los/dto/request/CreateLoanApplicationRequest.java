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

package org.apache.fineract.los.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLoanApplicationRequest {

  @NotNull
  @DecimalMin(value = "0.01", message = "requestedAmount must be greater than zero")
  private BigDecimal requestedAmount;

  @NotBlank(message = "currency is required")
  @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO 4217 code")
  @Pattern(regexp = "^[A-Z]{3}$", message = "currency must contain only uppercase letters")
  private String currency;

  @Size(max = 50, message = "loanPurpose must not exceed 50 characters")
  @Pattern(
      regexp = "^(AGRICULTURE|EDUCATION|BUSINESS|PERSONAL|HOUSING|MEDICAL|OTHER)$",
      message =
          "loanPurpose must be one of: AGRICULTURE, EDUCATION, BUSINESS, PERSONAL, HOUSING, MEDICAL, OTHER")
  private String loanPurpose;

  @NotNull(message = "tenorMonths is required")
  @Min(value = 1, message = "tenorMonths must be at least 1")
  private Integer tenorMonths;

  private Long fineractLoanProductId;

  @NotNull @Valid private ApplicantDetails applicant;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ApplicantDetails {

    @NotBlank(message = "fullName is required")
    @Size(min = 2, max = 100, message = "fullName must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z\\s'-]+$",
        message = "fullName must contain only letters, spaces, hyphens, and apostrophes")
    private String fullName;

    @Size(max = 50, message = "nationalId must not exceed 50 characters")
    @Pattern(
        regexp = "^[A-Z0-9-]+$",
        message = "nationalId must contain only uppercase letters, digits, and hyphens")
    private String nationalId;

    @PositiveOrZero(message = "monthlyIncome must be zero or positive")
    private BigDecimal monthlyIncome;

    @Size(max = 30, message = "employmentStatus must not exceed 30 characters")
    @Pattern(
        regexp = "^(EMPLOYED|SELF_EMPLOYED|UNEMPLOYED|STUDENT|RETIRED)?$",
        message =
            "employmentStatus must be one of: EMPLOYED, SELF_EMPLOYED, UNEMPLOYED, STUDENT, RETIRED")
    private String employmentStatus;

    @PositiveOrZero(message = "employmentDurationMonths must be zero or positive")
    private Integer employmentDurationMonths;

    @PositiveOrZero(message = "existingLoanObligations must be zero or positive")
    private BigDecimal existingLoanObligations;

    private Long fineractClientId;
  }
}
