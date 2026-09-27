import request from './request';

export function fetchNotificationOutbox(params) {
  return request({ url: '/admin/notification-outbox/list', method: 'get', params });
}

export function retryNotificationOutbox(id) {
  return request({ url: `/admin/notification-outbox/${id}/retry`, method: 'post' });
}
