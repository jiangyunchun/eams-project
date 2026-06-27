import request from '@/utils/request'

/** 获取全量字典缓存（无需登录） */
export function getAllDict()               { return request.get('/system/dict/all') }

/** 字典类型 */
export function listDictTypes(params)     { return request.get('/system/dict/type/list', { params }) }
export function addDictType(data)         { return request.post('/system/dict/type/add', data) }
export function editDictType(data)        { return request.put('/system/dict/type/edit', data) }
export function deleteDictType(id)        { return request.delete(`/system/dict/type/delete/${id}`) }

/** 字典项 */
export function listDictItems(dictCode)   { return request.get('/system/dict/item/list', { params: { dictCode } }) }
export function addDictItem(data)         { return request.post('/system/dict/item/add', data) }
export function editDictItem(data)        { return request.put('/system/dict/item/edit', data) }
export function deleteDictItem(id)        { return request.delete(`/system/dict/item/delete/${id}`) }
export function getDictItemsCached(dictCode) { return request.get('/system/dict/item/cached', { params: { dictCode } }) }
