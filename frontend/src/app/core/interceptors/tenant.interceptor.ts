import { HttpInterceptorFn } from '@angular/common/http';

const TENANT_HEADER = 'X-Fineract-Platform-TenantId';
const DEFAULT_TENANT = 'default';

/** Every controller defaults to "default" tenant if this header is absent — sending it
 *  explicitly keeps behavior obvious rather than relying on the server-side default. */
export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ setHeaders: { [TENANT_HEADER]: DEFAULT_TENANT } }));
};