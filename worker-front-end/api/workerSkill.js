import request from './request';

export function getWorkerSkills() {
  return request({
    url: '/worker/skills',
    method: 'GET'
  });
}

export function getWorkerAvailableSkillCategoryTree(params = {}) {
  return request({
    url: '/worker/skills/available-category-tree',
    method: 'GET',
    data: params
  });
}

export function getWorkerAvailableSkillServiceTypes(params = {}) {
  return request({
    url: '/worker/skills/available-service-types',
    method: 'GET',
    data: params
  });
}

export function addWorkerSkill(serviceTypeId) {
  return request({
    url: '/worker/skills/add',
    method: 'POST',
    data: {
      serviceTypeId
    }
  });
}

export function batchAddWorkerSkills(serviceTypeIds = []) {
  return request({
    url: '/worker/skills/batch-add',
    method: 'POST',
    data: {
      serviceTypeIds
    }
  });
}

export function deleteWorkerSkill(serviceTypeId) {
  return request({
    url: '/worker/skills/delete',
    method: 'POST',
    data: {
      serviceTypeId
    }
  });
}
