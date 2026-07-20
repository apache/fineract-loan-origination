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