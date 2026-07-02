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

package org.apache.fineract.los.domain.enums;

/**
 * Represents the declared purpose of a loan application.
 *
 * <p>Used as an input to the loan purpose risk scoring factor
 * (10% weight per CGAP guidelines). Purposes with more
 * predictable income generation score higher.
 */
public enum LoanPurpose {

    /** Agricultural input, equipment, or working capital. */
    AGRICULTURE,

    /** School fees, training, or skills development. */
    EDUCATION,

    /** Small business working capital or equipment. */
    BUSINESS,

    /** Home repairs or improvements. */
    HOME_IMPROVEMENT,

    /** General consumer goods or services. */
    CONSUMER,

    /** Medical expenses or healthcare. */
    MEDICAL,

    /** High-risk speculative investment. */
    SPECULATION,

    /** Any purpose not covered by the above categories. */
    OTHER
}