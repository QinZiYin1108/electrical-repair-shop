import request from './request';

export function fetchAnnouncements(channel) {
  return request({
    url: '/admin/system/announcements',
    method: 'get',
    params: { channel }
  });
}

export function createAnnouncement(data) {
  return request({
    url: '/admin/system/announcements/create',
    method: 'post',
    data
  });
}

export function updateAnnouncement(id, data) {
  return request({
    url: `/admin/system/announcements/${id}/update`,
    method: 'post',
    data
  });
}

export function deleteAnnouncement(id) {
  return request({
    url: `/admin/system/announcements/${id}/delete`,
    method: 'post'
  });
}

export function uploadAnnouncementImage(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/system/announcements/${id}/image`,
    method: 'post',
    data: formData
  });
}

