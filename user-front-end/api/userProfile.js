const request = require('./request');
const { API_BASE_URL } = require('./config');

const getUserProfileDetail = () => {
  return request({
    url: '/user/profile/me',
    method: 'GET'
  });
};

const updateUserProfile = (data) => {
  return request({
    url: '/user/profile/update',
    method: 'POST',
    data
  });
};

const uploadUserAvatar = (filePath) => {
  const token = wx.getStorageSync('userToken');
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: API_BASE_URL + '/user/profile/avatar',
      filePath,
      name: 'file',
      header: {
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        try {
          resolve(request.resolveUploadResponse(res, '头像上传失败'));
        } catch (error) {
          reject(error);
        }
      },
      fail(err) {
        reject(request.createRequestError((err && err.errMsg) || '头像上传失败', err));
      }
    });
  });
};

const fetchAccountCancelStatus = () => {
  return request({
    url: '/user/profile/cancel/status',
    method: 'GET'
  });
};

const applyAccountCancel = (reason) => {
  return request({
    url: '/user/profile/cancel/apply',
    method: 'POST',
    data: {
      reason: reason || ''
    }
  });
};

const revokeAccountCancel = () => {
  return request({
    url: '/user/profile/cancel/revoke',
    method: 'POST',
    data: {}
  });
};

module.exports = {
  getUserProfileDetail,
  updateUserProfile,
  uploadUserAvatar,
  fetchAccountCancelStatus,
  applyAccountCancel,
  revokeAccountCancel
};
