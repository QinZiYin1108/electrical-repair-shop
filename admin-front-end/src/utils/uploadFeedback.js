import { ElMessageBox } from 'element-plus';

const DEFAULT_UPLOAD_ERROR = '上传失败，请稍后重试';

export function getErrorMessage(error, fallback = '') {
  if (typeof error === 'string' && error.trim()) {
    return error.trim();
  }
  if (typeof error?.response?.data?.message === 'string' && error.response.data.message.trim()) {
    return error.response.data.message.trim();
  }
  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message.trim();
  }
  return fallback;
}

export function isUploadRelatedError(error, fallback = '') {
  const message = getErrorMessage(error, fallback);
  if (!message) {
    return false;
  }
  return ['上传', '图片', '视频', '图标', '头像', '素材', '附件', '文件'].some(keyword => message.includes(keyword));
}

export function showUploadErrorDialog(error, fallback = DEFAULT_UPLOAD_ERROR, title = '上传失败') {
  const message = getErrorMessage(error, fallback) || fallback;
  return ElMessageBox.alert(message, title, {
    type: 'error',
    confirmButtonText: '知道了'
  }).catch(() => {});
}

export function showUploadLimitDialog(message, title = '文件不符合要求') {
  const content = getErrorMessage(message, '文件不符合要求') || '文件不符合要求';
  return ElMessageBox.alert(content, title, {
    type: 'warning',
    confirmButtonText: '知道了'
  }).catch(() => {});
}
