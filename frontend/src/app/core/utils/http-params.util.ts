import { HttpParams } from '@angular/common/http';

import { PageParams } from '../models/page-response.model';

export function buildPageParams(params: PageParams = {}): HttpParams {
  let httpParams = new HttpParams();

  if (params.page !== undefined) {
    httpParams = httpParams.set('page', params.page);
  }

  if (params.size !== undefined) {
    httpParams = httpParams.set('size', params.size);
  }

  if (params.sort) {
    httpParams = httpParams.set('sort', params.sort);
  }

  return httpParams;
}
