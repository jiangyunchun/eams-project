import request from '@/utils/request'

// ==================== 登录 ====================
export function loginApi(data)          { return request.post('/login', data) }
export function logoutApi()             { return request.post('/logout') }
export function getUserInfoApi()        { return request.get('/user/info') }

// ==================== 用户管理 ====================
export function listUsers(params)       { return request.get('/system/user/list', { params }) }
export function addUser(data)           { return request.post('/system/user/add', data) }
export function editUser(data)          { return request.put('/system/user/edit', data) }
export function deleteUser(id)          { return request.delete(`/system/user/delete/${id}`) }
export function resetPassword(id)       { return request.put(`/system/user/reset-pwd/${id}`) }
export function toggleUserStatus(data)  { return request.put('/system/user/status', data) }
export function getUserDetail(id)       { return request.get(`/system/user/detail/${id}`) }
export function unlockUser(username)    { return request.put(`/system/user/unlock?username=${username}`) }

// ==================== 角色管理 ====================
export function listRoles(params)       { return request.get('/system/role/list', { params }) }
export function allRoles()              { return request.get('/system/role/all') }
export function addRole(data)           { return request.post('/system/role/add', data) }
export function editRole(data)          { return request.put('/system/role/edit', data) }
export function deleteRole(id)          { return request.delete(`/system/role/delete/${id}`) }
export function assignPermission(data)  { return request.put('/system/role/permission', data) }
export function getMenuIds(roleId)      { return request.get(`/system/role/menu-ids/${roleId}`) }

// ==================== 部门管理 ====================
export function getDeptTree()           { return request.get('/system/dept/tree') }
export function getDeptDetail(id)       { return request.get(`/system/dept/detail/${id}`) }
export function addDept(data)           { return request.post('/system/dept/add', data) }
export function editDept(data)          { return request.put('/system/dept/edit', data) }
export function deleteDept(id)          { return request.delete(`/system/dept/delete/${id}`) }

// ==================== 字典管理 ====================
export function getAllDict()            { return request.get('/system/dict/all') }
export function listDictTypes(params)   { return request.get('/system/dict/type/list', { params }) }
export function addDictType(data)       { return request.post('/system/dict/type/add', data) }
export function editDictType(data)      { return request.put('/system/dict/type/edit', data) }
export function deleteDictType(id)      { return request.delete(`/system/dict/type/delete/${id}`) }
export function listDictItems(dictCode) { return request.get('/system/dict/item/list', { params: { dictCode } }) }
export function addDictItem(data)       { return request.post('/system/dict/item/add', data) }
export function editDictItem(data)      { return request.put('/system/dict/item/edit', data) }
export function deleteDictItem(id)      { return request.delete(`/system/dict/item/delete/${id}`) }

// ==================== 系统参数 ====================
export function listConfigs(params)     { return request.get('/system/config/list', { params }) }
export function addConfig(data)         { return request.post('/system/config/add', data) }
export function editConfig(data)        { return request.put('/system/config/edit', data) }
export function deleteConfig(id)        { return request.delete(`/system/config/delete/${id}`) }
export function resetConfig(id)         { return request.put(`/system/config/reset/${id}`) }
export function getConfigHistory(id)    { return request.get(`/system/config/history/${id}`) }

// ==================== 操作日志 ====================
export function listLogs(params)        { return request.get('/system/log/list', { params }) }
export function getLogDetail(id)        { return request.get(`/system/log/detail/${id}`) }

// ==================== 菜单 ====================
export function getMenuTree()           { return request.get('/system/menu/tree') }
