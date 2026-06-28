import request from '@/utils/request'

// ==================== 供应商管理 ====================

/** 供应商分页列表 */
export function listSupplier(params)       { return request.get('/procurement-supplier/list', { params }) }

/** 所有启用供应商（下拉选择） */
export function getAllEnabledSupplier()    { return request.get('/procurement-supplier/all') }

/** 新增供应商 */
export function addSupplier(data)          { return request.post('/procurement-supplier/add', data) }

/** 编辑供应商 */
export function editSupplier(data)         { return request.put('/procurement-supplier/edit', data) }

/** 删除供应商 */
export function deleteSupplier(id)         { return request.delete(`/procurement-supplier/delete/${id}`) }

// ==================== 采购记录管理 ====================

/** 采购记录分页列表 */
export function listProcurement(params)    { return request.get('/procurement/record/list', { params }) }

/** 采购记录详情 */
export function getProcurementDetail(id)   { return request.get(`/procurement/record/detail/${id}`) }

/** 新增采购记录（采购登记） */
export function addProcurement(data)       { return request.post('/procurement/add', data) }

/** 编辑采购记录 */
export function editProcurement(data)      { return request.put('/procurement/edit', data) }

/** 删除采购记录 */
export function deleteProcurement(id)      { return request.delete(`/procurement/delete/${id}`) }
