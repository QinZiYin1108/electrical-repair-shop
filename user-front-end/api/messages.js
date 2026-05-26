const request = require('./request');
const { API_BASE_URL } = require('./config');

const fetchUserConversations = () => {
  return request({
    url: '/user/messages/sessions',
    method: 'GET'
  });
};

const fetchUserUnreadFlag = () => {
  return request({
    url: '/user/messages/unread-flag',
    method: 'GET'
  });
};

const fetchSystemMessages = () => {
  return request({
    url: '/user/messages/system',
    method: 'GET'
  });
};

const markAllSystemRead = () => {
  return request({
    url: '/user/messages/system/mark-all-read',
    method: 'GET'
  });
};

const fetchChatMessages = (sessionId) => {
  return request({
    url: '/user/messages/chat',
    method: 'GET',
    data: {
      sessionId
    }
  });
};

const sendUserChatMessage = (data) => {
  return request({
    url: '/user/messages/chat/send',
    method: 'POST',
    data: data || {}
  });
};

const uploadUserChatMedia = (filePath, mediaType) => {
  const token = wx.getStorageSync('userToken');
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: API_BASE_URL + '/user/messages/chat/upload-media',
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
          resolve(request.resolveUploadResponse(res, mediaType === 'video' ? '视频上传失败' : '图片上传失败'));
        } catch (error) {
          reject(error);
        }
      },
      fail(error) {
        reject(request.createRequestError((error && error.errMsg) || '上传失败，请稍后重试', error));
      }
    });
  });
};

module.exports = {
  fetchUserConversations,
  fetchUserUnreadFlag,
  fetchSystemMessages,
  markAllSystemRead,
  fetchChatMessages,
  sendUserChatMessage,
  uploadUserChatMedia
};
