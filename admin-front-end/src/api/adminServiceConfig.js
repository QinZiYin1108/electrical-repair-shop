import request from './request';

// Service categories
export function fetchServiceCategories() {
  return request({
    url: '/admin/config/services/categories',
    method: 'get'
  });
}

export function createServiceCategory(data) {
  return request({
    url: '/admin/config/services/categories/create',
    method: 'post',
    data
  });
}

export function updateServiceCategory(id, data) {
  return request({
    url: `/admin/config/services/categories/${id}/update`,
    method: 'post',
    data
  });
}

export function deleteServiceCategory(id) {
  return request({
    url: `/admin/config/services/categories/${id}/delete`,
    method: 'post'
  });
}

export function uploadServiceCategoryIcon(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/config/services/categories/${id}/icon`,
    method: 'post',
    data: formData
  });
}

// Service types
export function fetchServiceTypes() {
  return request({
    url: '/admin/config/services/types',
    method: 'get'
  });
}

export function createServiceType(data) {
  return request({
    url: '/admin/config/services/types/create',
    method: 'post',
    data
  });
}

export function copyServiceTypes(data) {
  return request({
    url: '/admin/config/services/types/copy',
    method: 'post',
    data
  });
}

export function updateServiceType(id, data) {
  return request({
    url: `/admin/config/services/types/${id}/update`,
    method: 'post',
    data
  });
}

export function deleteServiceType(id) {
  return request({
    url: `/admin/config/services/types/${id}/delete`,
    method: 'post'
  });
}

// Fault phenomena
export function fetchFaultPhenomena(params) {
  return request({
    url: '/admin/config/services/faults',
    method: 'get',
    params
  });
}

export function createFaultPhenomenon(data) {
  return request({
    url: '/admin/config/services/faults/create',
    method: 'post',
    data
  });
}

export function copyFaultPhenomena(data) {
  return request({
    url: '/admin/config/services/faults/copy',
    method: 'post',
    data
  });
}

export function updateFaultPhenomenon(id, data) {
  return request({
    url: `/admin/config/services/faults/${id}/update`,
    method: 'post',
    data
  });
}

export function deleteFaultPhenomenon(id) {
  return request({
    url: `/admin/config/services/faults/${id}/delete`,
    method: 'post'
  });
}
