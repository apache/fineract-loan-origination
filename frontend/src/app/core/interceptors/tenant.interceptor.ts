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

import { HttpInterceptorFn } from '@angular/common/http';

const TENANT_HEADER = 'X-Fineract-Platform-TenantId';
const DEFAULT_TENANT = 'default';

/** Every controller defaults to "default" tenant if this header is absent — sending it
 *  explicitly keeps behavior obvious rather than relying on the server-side default. */
export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ setHeaders: { [TENANT_HEADER]: DEFAULT_TENANT } }));
};
