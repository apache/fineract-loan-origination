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

import org.apache.fineract.los.scoring.ScoringWeightsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link ScoringWeightsProperties} from {@code application.yml} into a managed
 * Spring bean.
 *
 * Without this registration, {@code @ConfigurationProperties} classes are not automatically
 * picked up unless component scanned via {@code @Component} — this explicit registration is the
 * Spring Boot recommended approach for properties classes that live outside the main application
 * package scan root.
 */
@Configuration
@EnableConfigurationProperties(ScoringWeightsProperties.class)
public class ScoringConfig {
  // Marker configuration class — no beans defined here.
}
