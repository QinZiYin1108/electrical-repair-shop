import request from './request';

export function fetchInvoices(params) {
  return request({ url: '/admin/invoices/list', method: 'get', params });
}

export function issueInvoice(id, data) {
  return request({ url: `/admin/invoices/${id}/issue`, method: 'post', data });
}

export function rejectInvoice(id, data) {
  return request({ url: `/admin/invoices/${id}/reject`, method: 'post', data });
}

export function redInvoice(id, data) {
  return request({ url: `/admin/invoices/${id}/red`, method: 'post', data });
}
