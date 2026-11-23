/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.fineract.los.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.fineract.los.bridge.FineractLoanApiClient;
import org.apache.fineract.los.bridge.MockFineractLoanApiClient;
import org.apache.fineract.los.bridge.RestFineractLoanApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class FineractAdapterConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              FineractClientConfig.class,
              MockFineractLoanApiClient.class,
              RestFineractLoanApiClient.class,
              FineractMockProfileValidator.class,
              AdapterInjectionConfiguration.class);

  @Test
  void selectsRestAdapterWhenMockPropertyIsMissing() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(FineractLoanApiClient.class);
          assertThat(context.getBean(FineractLoanApiClient.class))
              .isInstanceOf(RestFineractLoanApiClient.class);
        });
  }

  @Test
  void selectsRestAdapterWhenMockIsExplicitlyDisabled() {
    contextRunner
        .withPropertyValues("los.fineract.mock-enabled=false")
        .run(
            context -> {
              assertThat(context).hasSingleBean(FineractLoanApiClient.class);
              assertThat(context.getBean(FineractLoanApiClient.class))
                  .isInstanceOf(RestFineractLoanApiClient.class);
            });
  }

  @Test
  void acceptsUppercaseFalseAndSelectsRestAdapter() {
    contextRunner
        .withPropertyValues("los.fineract.mock-enabled=FALSE")
        .run(
            context -> {
              assertThat(context).hasSingleBean(FineractLoanApiClient.class);
              assertThat(context.getBean(FineractLoanApiClient.class))
                  .isInstanceOf(RestFineractLoanApiClient.class);
            });
  }

  @Test
  void allowsMockAdapterInDevProfile() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
        .withPropertyValues(
            "los.deployment.environment=development", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(FineractLoanApiClient.class);
              assertThat(context.getBean(FineractLoanApiClient.class))
                  .isInstanceOf(MockFineractLoanApiClient.class);
            });
  }

  @Test
  void acceptsUppercaseTrueAndSelectsMockAdapterInDevelopment() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
        .withPropertyValues(
            "los.deployment.environment=development", "los.fineract.mock-enabled=TRUE")
        .run(
            context -> {
              assertThat(context).hasSingleBean(FineractLoanApiClient.class);
              assertThat(context.getBean(FineractLoanApiClient.class))
                  .isInstanceOf(MockFineractLoanApiClient.class);
            });
  }

  @Test
  void allowsMockAdapterInTestProfile() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
        .withPropertyValues("los.deployment.environment=test", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(FineractLoanApiClient.class);
              assertThat(context.getBean(FineractLoanApiClient.class))
                  .isInstanceOf(MockFineractLoanApiClient.class);
            });
  }

  @Test
  void rejectsMockAdapterInProductionProfile() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
        .withPropertyValues(
            "los.deployment.environment=development", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .isInstanceOf(IllegalStateException.class)
                  .hasStackTraceContaining("all active Spring profiles are dev or test");
            });
  }

  @Test
  void rejectsMockAdapterWhenProductionIdentityHasDevProfile() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
        .withPropertyValues(
            "los.deployment.environment=production", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("forbidden in the production deployment environment");
            });
  }

  @Test
  void rejectsMockAdapterWhenProductionIdentityHasTestProfile() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
        .withPropertyValues(
            "los.deployment.environment=production", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("forbidden in the production deployment environment");
            });
  }

  @Test
  void rejectsMockAdapterWhenProductionIdentityHasNoActiveProfile() {
    contextRunner
        .withPropertyValues(
            "los.deployment.environment=production", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("forbidden in the production deployment environment");
            });
  }

  @Test
  void rejectsMockAdapterWhenAnUnsupportedProfileIsAlsoActive() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev", "prod"))
        .withPropertyValues(
            "los.deployment.environment=development", "los.fineract.mock-enabled=true")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure()).isInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  void rejectsNonLiteralMockPropertyValues() {
    for (final String value : new String[] {"no", "off", "yes", "on", "", "invalid"}) {
      contextRunner
          .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
          .withPropertyValues(
              "los.deployment.environment=development", "los.fineract.mock-enabled=" + value)
          .run(
              context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                        "los.fineract.mock-enabled must be the literal true or false");
              });
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class AdapterInjectionConfiguration {

    @Bean
    AdapterConsumer adapterConsumer(final FineractLoanApiClient adapter) {
      return new AdapterConsumer(adapter);
    }
  }

  static class AdapterConsumer {

    private final FineractLoanApiClient adapter;

    AdapterConsumer(final FineractLoanApiClient adapter) {
      this.adapter = adapter;
    }
  }
}
