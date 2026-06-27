import request from '@/utils/request'

export function listUsers(params)   { return request.get('/system/user/list', { params }) }
export function addUser(data)      { return request.post('/system/user/add', data) }
export function editUser(data)     { return request.put('/system/user/edit', data) }
export function deleteUser(id)     { return request.delete(`/system/user/delete/${id}`) }
export function resetPassword(id)  { return request.put(`/system/user/reset-pwd/${id}`) }
export function toggleStatus(data) { return request.put('/system/user/status', data) }
export function getUserDetail(id)  { return request.get(`/system/user/detail/${id}`) }
