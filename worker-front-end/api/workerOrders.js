import request, { BASE_URL, createRequestError, resolveUploadResponse } from './request';

export function fetchWorkerHomeOrders() {
  return request({
    url: '/worker/orders/home',
    method: 'GET'
  });
}

export function fetchWorkerHistoryOrders() {
  return request({
    url: '/worker/orders/history',
    method: 'GET'
  });
}

export function fetchWorkerOrderDetail(orderId) {
  return request({
    url: `/worker/orders/${orderId}`,
    method: 'GET'
  });
}

export function acceptWorkerOrder(orderId) {
  return request({
    url: `/worker/orders/${orderId}/accept`,
    method: 'POST'
  });
}

export function advanceWorkerOrderStatus(orderId) {
  return request({
    url: `/worker/orders/${orderId}/next-status`,
    method: 'POST'
  });
}

export function consumeWorkerDoorQr(token) {
  return request({
    url: '/worker/orders/door-qr/consume',
    method: 'POST',
    data: { token }
  });
}

export function submitWorkerInspection(orderId, data) {
  return request({
    url: `/worker/orders/${orderId}/inspection`,
    method: 'POST',
    data: data || {}
  });
}

export function updateWorkerInspectionFees(orderId, data) {
  return request({
    url: `/worker/orders/${orderId}/inspection/fees`,
    method: 'POST',
    data: data || {}
  });
}

export function uploadWorkerInspectionMedia(filePath, mediaType) {
  const token = uni.getStorageSync('workerToken');
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/worker/orders/inspection/upload-media',
      filePath,
      name: 'file',
      formData: {
        mediaType: mediaType || ''
      },
      header: {
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        try {
          resolve(resolveUploadResponse(res, mediaType === 'video' ? '视频上传失败' : '图片上传失败'));
        } catch (error) {
          reject(error);
        }
      },
      fail(error) {
        reject(createRequestError(error?.errMsg || '上传失败，请稍后重试', error));
      }
    });
  });
}
