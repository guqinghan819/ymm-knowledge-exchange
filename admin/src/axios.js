import axios from "axios";
import { showMessage } from "@/utils/utils";
import { getToken, removeToken } from "@/utils/auth";
// import { useUserStore } from "@/stores/user";

const MAX_SAFE_INTEGER_DIGITS = 15;

function protectLargeIntegerPrecision(data) {
    if (typeof data !== 'string' || data.length === 0) {
        return data;
    }

    let result = '';
    let inString = false;
    let escaped = false;
    let i = 0;

    while (i < data.length) {
        const ch = data[i];

        if (inString) {
            result += ch;
            if (escaped) {
                escaped = false;
            } else if (ch === '\\') {
                escaped = true;
            } else if (ch === '"') {
                inString = false;
            }
            i++;
            continue;
        }

        if (ch === '"') {
            inString = true;
            result += ch;
            i++;
            continue;
        }

        if (ch === '-' || (ch >= '0' && ch <= '9')) {
            const start = i;
            if (ch === '-') {
                i++;
            }
            const digitStart = i;
            while (i < data.length && data[i] >= '0' && data[i] <= '9') {
                i++;
            }

            const digitLength = i - digitStart;
            const isDecimalOrExponent = data[i] === '.' || data[i] === 'e' || data[i] === 'E';
            if (isDecimalOrExponent) {
                while (i < data.length && /[0-9eE+\-.]/.test(data[i])) {
                    i++;
                }
                result += data.slice(start, i);
                continue;
            }

            const numberText = data.slice(start, i);
            result += digitLength > MAX_SAFE_INTEGER_DIGITS ? `"${numberText}"` : numberText;
            continue;
        }

        result += ch;
        i++;
    }

    return result;
}

// 创建 Axios 实例
const instance = axios.create({
    baseURL: '/api',
    timeout: 15000, // 请求超时时间
    transformResponse: [function (data) {
        try {
            return JSON.parse(protectLargeIntegerPrecision(data));
        } catch {
            return data;
        }
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
    // 对响应数据做点什么
    return response.data;
}, function (error) {
    
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    const status = error.response?.status
    const errorCode = error.response?.data?.errorCode

    if (status === 401 || errorCode === 401) {
        console.log("====================== 401")
        removeToken()
        showMessage('登录状态已失效，请重新登录');

        if (window.location.pathname !== '/admin/login') {
            window.location.replace('/admin/login')
        }
    } else if (status === 403) {
        showMessage('没有执行此操作的权限');
        window.dispatchEvent(new Event('admin-authorization-changed'));
    } else {
        // 显示错误信息
        const msg = error.response?.data?.message || '请求失败';
        showMessage(msg);
    }
    
    return Promise.reject(error);
});

// 暴露出去
export default instance;
