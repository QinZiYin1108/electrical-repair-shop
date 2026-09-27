import request from './request';

export function fetchReconciliationBatches(params) {
  return request({ url: '/admin/reconciliation/batches', method: 'get', params });
}

export function fetchReconciliationIssues(batchId) {
  return request({ url: `/admin/reconciliation/batches/${batchId}/issues`, method: 'get' });
}

export function handleReconciliationIssue(issueId, data) {
  return request({ url: `/admin/reconciliation/issues/${issueId}/handle`, method: 'post', data });
}

export function runReconciliation(params) {
  return request({ url: '/admin/reconciliation/run', method: 'post', params });
}
