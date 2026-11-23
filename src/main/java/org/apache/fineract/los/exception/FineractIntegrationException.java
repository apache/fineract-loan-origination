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

package org.apache.fineract.los.exception;

/**
 * Thrown when a call from the disbursement bridge to Apache Fineract's core REST API fails —
 * network error, non-2xx response, or malformed response body.
 *
 * <p>Unchecked — the global exception handler maps this to HTTP 502 Bad Gateway, signalling that
 * the failure originated in a downstream dependency rather than in the LOS request itself.
 */
public class FineractIntegrationException extends RuntimeException {

  /** Fineract endpoint operation performed by the disbursement bridge. */
  public enum Operation {
    CREATE_LOAN("POST /loans"),
    APPROVE_LOAN("POST /loans/{id}?command=approve"),
    DISBURSE_LOAN("POST /loans/{id}?command=disburse");

    private final String endpoint;

    Operation(final String endpoint) {
      this.endpoint = endpoint;
    }

    public String getEndpoint() {
      return endpoint;
    }
  }

  public FineractIntegrationException(
      final String applicationRef, final Operation operation, final Throwable cause) {
    super(
        String.format(
            LosErrorConstants.MSG_FINERACT_CALL_FAILED_TEMPLATE,
            operation.getEndpoint(),
            applicationRef,
            cause.getMessage()),
        cause);
  }

  /** Retained for callers compiled against the original create-loan exception constructor. */
  public FineractIntegrationException(final String applicationRef, final Throwable cause) {
    this(applicationRef, Operation.CREATE_LOAN, cause);
  }
}
