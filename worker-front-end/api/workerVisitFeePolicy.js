import request from './request';

export function getWorkerVisitFeePolicies() {
  return request({
    url: '/worker/visit-fee-policies',
    method: 'GET'
  });
}

export function updateWorkerVisitFeePolicies(policies) {
  return request({
    url: '/worker/visit-fee-policies',
    method: 'POST',
    data: {
      policies: policies || []
    }
  });
}
