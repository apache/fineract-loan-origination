/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0.
 */

package org.apache.fineract.los.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

  private String currency;

  private String loanPurpose;

  @Min(1)
  private Integer tenorMonths;

  private Long fineractLoanProductId;

  @NotNull @Valid private ApplicantDetails applicant;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ApplicantDetails {

    @NotBlank private String fullName;

    private String nationalId;

    @PositiveOrZero private BigDecimal monthlyIncome;

    private String employmentStatus;

    @PositiveOrZero private Integer employmentDurationMonths;

    @PositiveOrZero private BigDecimal existingLoanObligations;

    private Long fineractClientId;
  }
}
