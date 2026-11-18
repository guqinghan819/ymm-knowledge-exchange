import axios from "axios";
import { showMessage } from "@/utils/utils";
import { getToken } from "@/utils/auth";
import JSONBigInt from 'json-bigint';
// import { useUserStore } from "@/stores/user";

// 创建 Axios 实例
const instance = axios.create({
    baseURL: '/api',
    timeout: 60000, // 请求超时时间 (1分钟)
    // 解决雪花算法生成的 Long 类型 ID 到前端后精度丢失的问题
    transformResponse: [function (data) {
        if (typeof data === 'string') {
            try {
                // 使用 json-bigint 将超出 JS 精度范围的数字转换为字符串
                return JSONBigInt({ storeAsString: true }).parse(data);
            } catch (e) {
                return data;
            }
        }
        return data;
    }],
});



// 添加请求拦截器
instance.interceptors.request.use(function (config) {
    // 从 localStorage 获取 token
    //const userStore = useUserStore();
    //const token = userStore.token;
    const token = getToken();
    // 如果存在 token，则添加到请求头
    if (token) {
        // 添加请求头, key 为 Authorization，value 值的前缀为 'Bearer '
        config.headers['Ymm-Sa-Token'] = `Bearer ${token}`;
    }

    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加响应拦截器
instance.interceptors.response.use(function (response) {
    // 2xx 范围内的状态码都会触发该函数。
    const res = response.data;
    
    // 处理后端统一响应格式: { success: boolean, message: string, errorCode: string, data: any }
    // 项目全局要求：只要后端响应的 success 为 false，就使用 error 提示并打断后续 then 的执行
    if (res && res.success === false) {
        showMessage(res.message || '操作失败', 'error');
        // 直接抛出错误，阻止代码进入具体请求的成功回调，方便统一进行 catch 块处理回滚等逻辑
        const businessError = new Error(res.message || '操作失败');
        businessError.errorCode = res.errorCode;
        businessError.responseData = res;
        return Promise.reject(businessError);
    }
    
    // 成功则始终返回解析后的数据给调用方处理
    return res;
}, function (error) {
    
    // 处理网络错误或超时
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        showMessage('请求超时，请稍后重试');
        return Promise.reject(error);
    }
    
    // 检查是否有 response
    if (!error.response) {
        showMessage('网络异常，请检查网络连接');
        return Promise.reject(error);
    }

    // 超出 2xx 范围的状态码都会触发该函数。
    // 尝试从后端标准格式中获取 errorCode 或使用 HTTP 状态码
    let status = error.response.data?.errorCode || error.response.status;

    if (status === 401 || status === '401') {
        console.log("====================== 401")
        showMessage('登录已过期，请重新登录');
        
        // 获取 store 实例并清除登录状态
        // const userStore = useUserStore();
        // userStore.logout();
    } else {
        // 优先显示后端返回的 message
        const msg = error.response.data?.message || '请求失败，请稍后重试';
        showMessage(msg);
    }
    
    return Promise.reject(error);
});

// 暴露出去
export default instance;
