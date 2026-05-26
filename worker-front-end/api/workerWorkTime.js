import request from './request';

export function getWorkerWorkTimes() {
  return request({
    url: '/worker/work-times',
    method: 'GET'
  });
}

export function updateWorkerWorkTimes(workTimes) {
  return request({
    url: '/worker/work-times',
    method: 'POST',
    data: {
      workTimes: workTimes || []
    }
  });
}
