import request, { BASE_URL, createRequestError, resolveUploadResponse } from './request';

export function fetchWorkerConversations() {
  return request({
    url: '/worker/messages/sessions',
    method: 'GET'
  });
}

export function fetchWorkerUnreadFlag() {
  return request({
    url: '/worker/messages/unread-flag',
    method: 'GET'
  });
}

export function fetchWorkerSystemMessages() {
  return request({
    url: '/worker/messages/system',
    method: 'GET'
  });
}

export function markWorkerSystemAllRead() {
  return request({
    url: '/worker/messages/system/mark-all-read',
    method: 'GET'
  });
}

export function fetchWorkerChatMessages(sessionId) {
  return request({
    url: '/worker/messages/chat',
    method: 'GET',
    data: { sessionId }
  });
}

export function sendWorkerChatMessage(data) {
  return request({
    url: '/worker/messages/chat/send',
    method: 'POST',
    data: data || {}
  });
}

export function uploadWorkerChatMedia(filePath, mediaType) {
  const token = uni.getStorageSync('workerToken');
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/worker/messages/chat/upload-media',
      filePath,
      name: 'file',
      formData: {
        mediaType: mediaType || ''
      },
      header: {
        ...(token ? { Authorization: 'Bearer ' + token } : {} )
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
