import request from './request';

export function getAdminProfileDetail() {
  return request({
    url: '/admin/profile/me',
    method: 'get'
  });
}

export function updateAdminProfile(data) {
  return request({
    url: '/admin/profile/update',
    method: 'post',
    data
  });
}

export function uploadAdminAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: '/admin/profile/avatar',
    method: 'post',
    data: formData
  });
}

