import request from './request';

export function fetchAdminProductCategories() {
  return request({
    url: '/admin/products/categories',
    method: 'get'
  });
}

export function createAdminProductCategory(data) {
  return request({
    url: '/admin/products/categories/create',
    method: 'post',
    data
  });
}

export function updateAdminProductCategory(id, data) {
  return request({
    url: `/admin/products/categories/${id}/update`,
    method: 'post',
    data
  });
}

export function deleteAdminProductCategory(id) {
  return request({
    url: `/admin/products/categories/${id}/delete`,
    method: 'post'
  });
}

export function uploadAdminProductCategoryIcon(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/products/categories/${id}/icon`,
    method: 'post',
    data: formData
  });
}

export function fetchAdminProducts(typeKey, params) {
  return request({
    url: `/admin/products/${typeKey}`,
    method: 'get',
    params
  });
}

export function createAdminProduct(typeKey, data) {
  return request({
    url: `/admin/products/${typeKey}/create`,
    method: 'post',
    data
  });
}

export function updateAdminProduct(typeKey, id, data) {
  return request({
    url: `/admin/products/${typeKey}/${id}/update`,
    method: 'post',
    data
  });
}

export function deleteAdminProduct(typeKey, id) {
  return request({
    url: `/admin/products/${typeKey}/${id}/delete`,
    method: 'post'
  });
}

export function uploadAdminProductMedia(file, mediaType) {
  const formData = new FormData();
  formData.append('file', file);
  if (mediaType) {
    formData.append('mediaType', mediaType);
  }
  return request({
    url: '/admin/products/upload-media',
    method: 'post',
    data: formData
  });
}
