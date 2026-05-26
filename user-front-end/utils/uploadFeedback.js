const DEFAULT_UPLOAD_ERROR = '上传失败，请稍后重试';

function getUploadErrorMessage(error, fallback = DEFAULT_UPLOAD_ERROR) {
  if (typeof error === 'string' && error.trim()) {
    return error.trim();
  }
  if (error && typeof error.message === 'string' && error.message.trim()) {
    return error.message.trim();
  }
  if (
    error
    && error.response
    && error.response.data
    && typeof error.response.data.message === 'string'
    && error.response.data.message.trim()
  ) {
    return error.response.data.message.trim();
  }
  return fallback;
}

function showUploadErrorModal(error, options = {}) {
  const title = options.title || '上传失败';
  const fallback = options.fallback || DEFAULT_UPLOAD_ERROR;
  const content = getUploadErrorMessage(error, fallback) || fallback;
  wx.showModal({
    title,
    content,
    showCancel: false,
    confirmText: '知道了'
  });
}

module.exports = {
  DEFAULT_UPLOAD_ERROR,
  getUploadErrorMessage,
  showUploadErrorModal
};
