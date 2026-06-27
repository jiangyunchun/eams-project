import request from '@/utils/request'

export function listRoles(params)       { return request.get('/system/role/list', { params }) }
export function allRoles()              { return request.get('/system/role/all') }
export function addRole(data)           { return request.post('/system/role/add', data) }
export function editRole(data)          { return request.put('/system/role/edit', data) }
export function deleteRole(id)          { return request.delete(`/system/role/delete/${id}`) }
export function assignPermission(data)  { return request.put('/system/role/permission', data) }
export function getMenuIds(roleId)      { return request.get(`/system/role/menu-ids/${roleId}`) }
