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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.los.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Prevents the mock Fineract adapter from starting outside dev and test environments. */
@Component
public class FineractMockProfileValidator implements BeanFactoryPostProcessor, EnvironmentAware {

  private static final Set<String> ALLOWED_PROFILES = Set.of("dev", "test");
  private static final Set<String> ALLOWED_DEPLOYMENT_ENVIRONMENTS = Set.of("development", "test");
  private static final String PRODUCTION_ENVIRONMENT = "production";
  private static final String DEPLOYMENT_ENVIRONMENT_PROPERTY = "los.deployment.environment";

  private Environment environment;

  @Override
  public void setEnvironment(final Environment environment) {
    this.environment = environment;
  }

  @Override
  public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
    final String mockEnabled = environment.getProperty("los.fineract.mock-enabled");
    if (mockEnabled == null) {
      return;
    }
    if (!"true".equalsIgnoreCase(mockEnabled) && !"false".equalsIgnoreCase(mockEnabled)) {
      throw new IllegalStateException(
          "los.fineract.mock-enabled must be the literal true or false (case-insensitive); "
              + "configured value: '"
              + mockEnabled
              + "'");
    }
    if ("false".equalsIgnoreCase(mockEnabled)) {
      return;
    }

    final String deploymentEnvironment =
        environment
            .getProperty(DEPLOYMENT_ENVIRONMENT_PROPERTY, String.class, PRODUCTION_ENVIRONMENT)
            .toLowerCase(Locale.ROOT);
    if (PRODUCTION_ENVIRONMENT.equals(deploymentEnvironment)) {
      throw new IllegalStateException(
          "los.fineract.mock-enabled=true is forbidden in the production deployment environment");
    }
    if (!ALLOWED_DEPLOYMENT_ENVIRONMENTS.contains(deploymentEnvironment)) {
      throw new IllegalStateException(
          "los.fineract.mock-enabled=true requires los.deployment.environment to be "
              + "development or test; configured value: "
              + deploymentEnvironment);
    }

    final Set<String> activeProfiles =
        Arrays.stream(environment.getActiveProfiles())
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
    if (activeProfiles.isEmpty() || !ALLOWED_PROFILES.containsAll(activeProfiles)) {
      throw new IllegalStateException(
          "los.fineract.mock-enabled=true is allowed only when all active Spring profiles are "
              + "dev or test; active profiles: "
              + activeProfiles);
    }
  }
}
