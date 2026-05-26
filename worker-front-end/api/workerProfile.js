import request, { BASE_URL, createRequestError, resolveUploadResponse } from './request';

export function getWorkerProfile() {
  return request({
    url: '/worker/profile/me',
    method: 'GET'
  });
}

export function updateWorkerProfile(data) {
  return request({
    url: '/worker/profile/me',
    method: 'POST',
    data: data || {}
  });
}

export function submitWorkerCertification(data) {
  return request({
    url: '/worker/profile/certification',
    method: 'POST',
    data: data || {}
  });
}

export function uploadWorkerAvatar(filePath) {
  const token = uni.getStorageSync('workerToken');
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/worker/profile/avatar',
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        try {
          resolve(resolveUploadResponse(res, '头像上传失败'));
        } catch (e) {
          reject(e);
        }
      },
      fail(err) {
        reject(createRequestError(err?.errMsg || '头像上传失败', err));
      }
    });
  });
}

export function getWorkerReviews() {
  return request({
    url: '/worker/reviews',
    method: 'GET'
  });
}

export function replyWorkerReview(id, data) {
  return request({
    url: `/worker/reviews/${id}/reply`,
    method: 'POST',
    data: data || {}
  });
}
