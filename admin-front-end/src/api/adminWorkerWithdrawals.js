import request from './request';

export function fetchWithdrawals(params) {
  return request({ url: '/admin/worker-withdrawals/list', method: 'get', params });
}

export function reviewWithdrawal(id, data) {
  return request({ url: `/admin/worker-withdrawals/${id}/review`, method: 'post', data });
}

export function markWithdrawalPaid(id, data) {
  return request({ url: `/admin/worker-withdrawals/${id}/paid`, method: 'post', data });
}

export function markWithdrawalFailed(id, data) {
  return request({ url: `/admin/worker-withdrawals/${id}/failed`, method: 'post', data });
}

export function confirmWithdrawalReview(id) {
  return request({ url: `/admin/worker-withdrawals/${id}/confirm-review`, method: 'post' });
}

export function interceptWithdrawal(id, data) {
  return request({ url: `/admin/worker-withdrawals/${id}/intercept`, method: 'post', data });
}

export function uninterceptWithdrawal(id) {
  return request({ url: `/admin/worker-withdrawals/${id}/unintercept`, method: 'post' });
}
