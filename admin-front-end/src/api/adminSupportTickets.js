import request from './request';

export function fetchSupportTickets(params) {
  return request({ url: '/admin/support-tickets/list', method: 'get', params });
}

export function fetchSupportTicketDetail(id) {
  return request({ url: `/admin/support-tickets/${id}`, method: 'get' });
}

export function createSupportTicket(data) {
  return request({ url: '/admin/support-tickets', method: 'post', data });
}

export function assignSupportTicket(id, data) {
  return request({ url: `/admin/support-tickets/${id}/assign`, method: 'post', data });
}

export function changeSupportTicketPriority(id, data) {
  return request({ url: `/admin/support-tickets/${id}/priority`, method: 'post', data });
}

export function changeSupportTicketStatus(id, data) {
  return request({ url: `/admin/support-tickets/${id}/status`, method: 'post', data });
}

export function commentSupportTicket(id, data) {
  return request({ url: `/admin/support-tickets/${id}/comment`, method: 'post', data });
}

export function approveSupportTicket(id, data) {
  return request({ url: `/admin/support-tickets/${id}/approve`, method: 'post', data });
}

export function rejectSupportTicket(id, data) {
  return request({ url: `/admin/support-tickets/${id}/reject`, method: 'post', data });
}
