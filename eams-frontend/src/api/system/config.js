import request from '@/utils/request'

export function listConfigs(params)   { return request.get('/system/config/list', { params }) }
export function addConfig(data)       { return request.post('/system/config/add', data) }
export function editConfig(data)      { return request.put('/system/config/edit', data) }
export function deleteConfig(id)      { return request.delete(`/system/config/delete/${id}`) }
export function resetConfig(id)       { return request.put(`/system/config/reset/${id}`) }
export function getConfigHistory(id)  { return request.get(`/system/config/history/${id}`) }
