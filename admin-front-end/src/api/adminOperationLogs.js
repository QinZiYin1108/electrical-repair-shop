import request from './request';

export function fetchOperationLogList(params) {
  return request({
    url: '/admin/system/operation-logs',
    method: 'get',
    params
  });
}

export function fetchOperationLogDetail(id) {
  return request({
    url: `/admin/system/operation-logs/${id}`,
    method: 'get'
  });
}

