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

import org.apache.fineract.los.bridge.FineractClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link FineractClientProperties} from {@code application.yml}.
 *
 * <p>Bean selection between the real and mock {@code FineractLoanApiClient} implementations is
 * handled declaratively via {@code @ConditionalOnProperty} directly on {@code
 * RestFineractLoanApiClient} and {@code MockFineractLoanApiClient} — this class only registers the
 * properties bean.
 */
@Configuration
@EnableConfigurationProperties(FineractClientProperties.class)
public class FineractClientConfig {
  // Marker configuration class — no beans defined here.
}
