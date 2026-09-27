import request from './request';

export function fetchFinanceSnapshots(params) {
  return request({ url: '/admin/finance/snapshots', method: 'get', params });
}

export function rebuildFinanceSnapshot(params) {
  return request({ url: '/admin/finance/snapshots/rebuild', method: 'post', params });
}

export function fetchFinanceReport(params) {
  return request({ url: '/admin/finance/report', method: 'get', params });
}
