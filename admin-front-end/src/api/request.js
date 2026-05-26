import axios from 'axios';
import { getToken } from '../utils/auth';

const apiBaseURL =
  process.env.VUE_APP_API_BASE_URL || (process.env.NODE_ENV === 'development' ? 'http://localhost:8080/api' : '/api');

const service = axios.create({
  baseURL: apiBaseURL,
  timeout: 10000
});

service.interceptors.request.use(
  config => {
    const token = getToken();
    if (token) {
      if (!config.headers) {
        config.headers = {};
      }
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    const responseMessage = error?.response?.data?.message;
    const status = Number(error?.response?.status || 0);
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      error.message = responseMessage.trim();
    } else if (status === 413) {
      error.message = '上传文件过大，请检查图片或视频大小是否超出限制';
    } else if (!error?.message) {
      error.message = '请求失败，请稍后重试';
    }
    return Promise.reject(error);
  }
);

export default service;
