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

CREATE TABLE customer_credentials (
    id                 BIGSERIAL PRIMARY KEY,
    username           VARCHAR(100) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    fineract_client_id BIGINT       NOT NULL,
    tenant_id          VARCHAR(100) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_credentials_username ON customer_credentials(username);
CREATE INDEX idx_customer_credentials_client   ON customer_credentials(fineract_client_id, tenant_id);
