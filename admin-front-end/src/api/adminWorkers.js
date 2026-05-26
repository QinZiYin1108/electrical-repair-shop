import request from './request';

export function fetchAdminWorkerList(params) {
  return request({
    url: '/admin/workers',
    method: 'get',
    params
  });
}

export function fetchAdminWorkerDetail(id) {
  return request({
    url: `/admin/workers/${id}`,
    method: 'get'
  });
}

export function updateAdminWorkerInfo(id, data) {
  return request({
    url: `/admin/workers/${id}/update`,
    method: 'post',
    data
  });
}

export function updateAdminWorkerStatus(id, accountStatus) {
  return request({
    url: `/admin/workers/${id}/status`,
    method: 'post',
    data: {
      accountStatus
    }
  });
}

export function updateAdminWorkerVisitFeePolicies(id, policies) {
  return request({
    url: `/admin/workers/${id}/visit-fee-policies`,
    method: 'post',
    data: {
      policies: policies || []
    }
  });
}

export function updateAdminWorkerWorkTimes(id, workTimes) {
  return request({
    url: `/admin/workers/${id}/work-times`,
    method: 'post',
    data: {
      workTimes: workTimes || []
    }
  });
}

export function uploadAdminWorkerAvatar(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/workers/${id}/avatar`,
    method: 'post',
    data: formData
  });
}

export function fetchAdminWorkerSkills(id) {
  return request({
    url: `/admin/workers/${id}/skills`,
    method: 'get'
  });
}

export function fetchAdminWorkerSkillCategoryTree(id, params) {
  return request({
    url: `/admin/workers/${id}/skills/available/categories`,
    method: 'get',
    params
  });
}

export function fetchAdminWorkerSkillServiceTypes(id, params) {
  return request({
    url: `/admin/workers/${id}/skills/available/service-types`,
    method: 'get',
    params
  });
}

export function batchAddAdminWorkerSkills(id, serviceTypeIds) {
  return request({
    url: `/admin/workers/${id}/skills`,
    method: 'post',
    data: {
      serviceTypeIds: serviceTypeIds || []
    }
  });
}

export function removeAdminWorkerSkill(id, serviceTypeId) {
  return request({
    url: `/admin/workers/${id}/skills/remove`,
    method: 'post',
    data: {
      serviceTypeId
    }
  });
}

export function fetchAdminWorkerPerformance(params) {
  return request({
    url: '/admin/workers/stats/performance',
    method: 'get',
    params
  });
}
