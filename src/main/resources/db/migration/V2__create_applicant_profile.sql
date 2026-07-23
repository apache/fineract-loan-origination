-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.

CREATE TABLE applicant_profile (
    id                              BIGSERIAL PRIMARY KEY,
    application_id                  BIGINT NOT NULL UNIQUE REFERENCES loan_application(id),
    full_name                       VARCHAR(200) NOT NULL,
    national_id                     VARCHAR(100),
    monthly_income                  NUMERIC(19,2),
    employment_status               VARCHAR(50),
    employment_duration_months      INTEGER,
    existing_loan_obligations       NUMERIC(19,2) DEFAULT 0,
    tenant_id                       VARCHAR(100) NOT NULL,
    created_at                      TIMESTAMP NOT NULL DEFAULT NOW()
);