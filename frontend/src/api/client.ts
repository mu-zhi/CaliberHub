import axios, { AxiosInstance, AxiosResponse } from 'axios'

// API 响应类型
export interface ApiResponse<T = unknown> {
    code: string
    message: string
    data?: T
    detail?: string
    traceId?: string
}

// 创建 axios 实例
const apiClient: AxiosInstance = axios.create({
    baseURL: '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
})

// 请求拦截器：添加 X-User header
apiClient.interceptors.request.use(
    (config) => {
        // 从 localStorage 获取用户名，默认 demo_user
        const user = localStorage.getItem('caliberhub_user') || 'demo_user'
        config.headers['X-User'] = user
        return config
    },
    (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误
apiClient.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
        return response
    },
    (error) => {
        if (error.response) {
            const { code, message, detail } = error.response.data as ApiResponse
            console.error(`API Error [${code}]: ${message}`, detail)
        }
        return Promise.reject(error)
    }
)

export default apiClient
