import request from './request';

export function fetchAdminSystemSettings() {
  return request({
    url: '/admin/system/settings',
    method: 'get'
  });
}

export function updateAdminSystemSettings(data) {
  return request({
    url: '/admin/system/settings/update',
    method: 'post',
    data
  });
}

export function fetchAdminProtocols() {
  return request({
    url: '/admin/system/settings/protocols',
    method: 'get'
  });
}

export function uploadAdminProtocol(type, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/system/settings/protocols/${type}/upload`,
    method: 'post',
    data: formData
  });
}

export function fetchAdminEmailTemplates() {
  return request({
    url: '/admin/system/settings/email-templates',
    method: 'get'
  });
}

export function uploadAdminEmailTemplate(type, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/system/settings/email-templates/${type}/upload`,
    method: 'post',
    data: formData
  });
}

export function fetchPublicProtocol(type) {
  return request({
    url: `/pass/protocols/${type}`,
    method: 'get'
  });
}