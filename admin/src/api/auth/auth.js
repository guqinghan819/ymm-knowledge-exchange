import axios from "@/axios";

// 接口前缀
const API_PREFIX = '/auth'

// 获取登录验证码
export function getVerificationCode(phone, verificationType) {
    return axios.post(`${API_PREFIX}/verification/code/send`, {phone, verificationType})
}

// 登录接口
export function login(loginRequest) {
    return axios.post(`${API_PREFIX}/loginOrRegister`, loginRequest)
}

// 退出登录接口
export function logout() {
    return axios.get(`${API_PREFIX}/logout`)
}

// 更新密码
export function updatePassword(phone, code, newPassword) {
    return axios.post(`${API_PREFIX}/update/password`, { phone, code, newPassword })
}
