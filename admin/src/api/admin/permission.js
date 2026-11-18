import axios from '@/axios'
import { authorizationChanged } from '@/utils/authorization'
const prefix = '/user/permission'
export const getPermissionList = () => axios.get(`${prefix}/get`)
export const createPermission = data => axios.post(`${prefix}/create`, data).then(authorizationChanged)
export const updatePermission = data => axios.post(`${prefix}/update`, data).then(authorizationChanged)
export const deletePermission = id => axios.post(`${prefix}/delete`, { id }).then(authorizationChanged)
