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

CREATE TABLE staff_credentials (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'ROLE_STAFF',
    tenant_id     VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

CREATE INDEX idx_staff_credentials_username ON staff_credentials(username);
CREATE INDEX idx_staff_credentials_tenant   ON staff_credentials(tenant_id);
CREATE INDEX idx_staff_credentials_active   ON staff_credentials(active);

-- Insert default admin user
-- Username: admin  |  Password: Admin@123
-- BCrypt hash generated with strength 10
INSERT INTO staff_credentials (username, password_hash, email, role, tenant_id, active, created_at)
VALUES ('admin', '$2a$10$hN8JrDCYZWTEgpwXeznAJuH66UOUhCJ6hEq5zKBSRd.aFQW8CfPHW', 'admin@localhost', 'ROLE_ADMIN', 'default', TRUE, NOW());

-- Insert default staff (loan officer) user
-- Username: staff  |  Password: Staff@123
INSERT INTO staff_credentials (username, password_hash, email, role, tenant_id, active, created_at)
VALUES ('staff', '$2a$10$z/AbcPt.ifHvynJMAieqgOiSwgQP3fH6X9WzIPh0bb6mntWjAi/ca', 'staff@localhost', 'ROLE_STAFF', 'default', TRUE, NOW());

