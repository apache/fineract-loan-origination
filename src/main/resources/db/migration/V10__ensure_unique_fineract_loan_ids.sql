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

-- Fail before changing schema or data if legacy duplicate links exist. Resolve these records
-- manually after verifying them against Fineract; this migration never rewrites loan links.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM loan_application
        WHERE fineract_loan_id IS NOT NULL
        GROUP BY tenant_id, fineract_loan_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add unique Fineract loan ID constraint: duplicate (tenant_id, fineract_loan_id) records exist. Reconcile them manually before retrying migration V10.';
    END IF;
END $$;

CREATE SEQUENCE mock_fineract_loan_id_seq START WITH 100001;

SELECT setval(
    'mock_fineract_loan_id_seq',
    GREATEST(100000, COALESCE(MAX(fineract_loan_id), 100000))
)
FROM loan_application;

ALTER TABLE loan_application
    ADD CONSTRAINT uq_loan_application_tenant_fineract_loan_id
        UNIQUE (tenant_id, fineract_loan_id);
