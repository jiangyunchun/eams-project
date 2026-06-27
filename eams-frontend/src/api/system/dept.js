import request from '@/utils/request'

export function getDeptTree()    { return request.get('/system/dept/tree') }
export function addDept(data)    { return request.post('/system/dept/add', data) }
export function editDept(data)   { return request.put('/system/dept/edit', data) }
export function deleteDept(id)   { return request.delete(`/system/dept/delete/${id}`) }
