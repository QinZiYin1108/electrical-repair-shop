import request from './request';

export function fetchAppointmentClosures(params) {
  return request({ url: '/admin/appointment-capacity/closures', method: 'get', params });
}

export function createAppointmentClosure(data) {
  return request({ url: '/admin/appointment-capacity/closures', method: 'post', data });
}

export function deleteAppointmentClosure(id) {
  return request({ url: `/admin/appointment-capacity/closures/${id}/delete`, method: 'post' });
}

export function fetchAppointmentReservations(params) {
  return request({ url: '/admin/appointment-capacity/reservations', method: 'get', params });
}
