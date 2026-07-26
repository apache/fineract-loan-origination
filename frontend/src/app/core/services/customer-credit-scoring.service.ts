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
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreditScore } from '../models';

const BASE_URL = `${environment.losApiUrl}/loan-applications`;

@Injectable({ providedIn: 'root' })
export class CustomerCreditScoringService {
  private readonly http = inject(HttpClient);

  getForApplication(applicationRef: string): Observable<CreditScore> {
    return this.http.get<CreditScore>(`${BASE_URL}/${applicationRef}/credit-score`);
  }
}
