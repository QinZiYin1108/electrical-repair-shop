import request from './request';

export function getWorkerBindingStatus() {
  return request({ url: '/worker/binding/status', method: 'get' });
}

export function acceptBinding() {
  return request({ url: '/worker/binding/accept', method: 'post' });
}

export function rejectBinding() {
  return request({ url: '/worker/binding/reject', method: 'post' });
}

export function requestUnbind() {
  return request({ url: '/worker/binding/request-unbind', method: 'post' });
}
