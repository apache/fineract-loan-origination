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

ALTER TABLE loan_application
    ADD COLUMN fineract_loan_product_id BIGINT,
    ADD COLUMN fineract_loan_id         BIGINT;

ALTER TABLE applicant_profile
    ADD COLUMN fineract_client_id BIGINT;

CREATE INDEX idx_loan_application_fineract_loan_id
    ON loan_application(fineract_loan_id);