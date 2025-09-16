import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';

// 创建axios实例
const apiClient: AxiosInstance = axios.create({
  // 在开发环境使用相对路径，通过Vite代理
  // 在生产环境使用环境变量中的完整URL
  baseURL: import.meta.env.PROD ? import.meta.env.VITE_API_BASE_URL : '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 可以在这里添加token等认证信息
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers = config.headers || {};
    //   config.headers['Authorization'] = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // 统一处理响应数据
    return response;
  },
  (error) => {
    // 统一处理错误
    if (error.response) {
      // 服务器响应错误
      console.error('API Error:', error.response.status, error.response.data);
      
      // 可以根据状态码进行不同的处理
      switch (error.response.status) {
        case 401:
          // 未授权，可以跳转到登录页
          break;
        case 403:
          // 禁止访问
          break;
        case 404:
          // 资源不存在
          break;
        case 500:
          // 服务器错误
          break;
        default:
          // 其他错误
          break;
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('Network Error:', error.message);
    } else {
      // 请求配置错误
      console.error('Request Error:', error.message);
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;