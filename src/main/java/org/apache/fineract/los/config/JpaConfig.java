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

package org.apache.fineract.los.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for the Loan Origination Service.
 *
 * Enables JPA auditing so that {@code @CreatedDate} and {@code @LastModifiedDate} fields on all
 * entities are automatically populated by Spring Data on every save.
 *
 * Enables JPA repositories scanning from the root package ensuring all repository interfaces are
 * discovered.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "org.apache.fineract.los.repository")
public class JpaConfig {
  // Spring handles all configuration via annotations.
  // No bean definitions required.
}
