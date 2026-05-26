import request from './request';

export function fetchAdminUserList(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  });
}

export function fetchAdminUserDetail(id) {
  return request({
    url: `/admin/users/${id}`,
    method: 'get'
  });
}

export function fetchAdminUserAddresses(id) {
  return request({
    url: `/admin/users/${id}/addresses`,
    method: 'get'
  });
}

export function setAdminUserDefaultAddress(userId, addressId) {
  return request({
    url: `/admin/users/${userId}/addresses/${addressId}/set-default`,
    method: 'post'
  });
}

export function updateAdminUserAddress(userId, addressId, data) {
  return request({
    url: `/admin/users/${userId}/addresses/${addressId}/update`,
    method: 'post',
    data
  });
}

export function deleteAdminUserAddress(userId, addressId) {
  return request({
    url: `/admin/users/${userId}/addresses/${addressId}/delete`,
    method: 'post'
  });
}

export function updateAdminUserInfo(id, data) {
  return request({
    url: `/admin/users/${id}/update`,
    method: 'post',
    data
  });
}

export function updateAdminUserStatus(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'post',
    data: {
      status
    }
  });
}

export function initAdminUserPassword(id) {
  return request({
    url: `/admin/users/${id}/password/init`,
    method: 'post'
  });
}

export function uploadAdminUserAvatar(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/users/${id}/avatar`,
    method: 'post',
    data: formData
  });
}
