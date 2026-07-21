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

CREATE TABLE loan_application (
    id                       BIGSERIAL PRIMARY KEY,
    application_ref          VARCHAR(50) NOT NULL UNIQUE,
    status                   VARCHAR(30) NOT NULL,
    requested_amount         NUMERIC(19,2) NOT NULL,
    currency                 VARCHAR(10) NOT NULL DEFAULT 'USD',
    loan_purpose             VARCHAR(100),
    tenor_months             INTEGER,
    tenant_id                VARCHAR(100) NOT NULL,
    version                  BIGINT NOT NULL DEFAULT 0,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loan_application_tenant_status
    ON loan_application(tenant_id, status);