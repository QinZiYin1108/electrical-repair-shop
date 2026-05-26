const DEFAULT_UPLOAD_ERROR = '上传失败，请稍后重试';

export function getUploadErrorMessage(error, fallback = DEFAULT_UPLOAD_ERROR) {
  if (typeof error === 'string' && error.trim()) {
    return error.trim();
  }
  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message.trim();
  }
  if (typeof error?.response?.data?.message === 'string' && error.response.data.message.trim()) {
    return error.response.data.message.trim();
  }
  return fallback;
}

export function showUploadErrorModal(error, options = {}) {
  const title = options.title || '上传失败';
  const fallback = options.fallback || DEFAULT_UPLOAD_ERROR;
  const content = getUploadErrorMessage(error, fallback) || fallback;
  uni.showModal({
    title,
    content,
    showCancel: false,
    confirmText: '知道了'
  });
}
