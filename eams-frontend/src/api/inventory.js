import request from '@/utils/request'

// ==================== 6.4.1 盘点任务管理 ====================
/** GET /api/inventory/task/list — 盘点任务列表（分页） */
export function listTasks(params) {
  return request.get('/inventory/task/list', { params })
}

/** GET /api/inventory/task/detail/{id} — 盘点任务详情 */
export function getTaskDetail(id) {
  return request.get(`/inventory/task/detail/${id}`)
}

/** POST /api/inventory/task/add — 创建盘点任务 */
export function createTask(data) {
  return request.post('/inventory/task/add', data)
}

/** PUT /api/inventory/task/cancel/{id} — 取消盘点任务 */
export function cancelTask(id) {
  return request.put(`/inventory/task/cancel/${id}`)
}

/** PUT /api/inventory/task/complete/{id} — 完成盘点 */
export function completeTask(id) {
  return request.put(`/inventory/task/complete/${id}`)
}

/** GET /api/inventory/task/export — 导出盘点任务Excel */
export function exportTasks(params) {
  return request.get('/inventory/task/export', {
    params,
    responseType: 'blob',
    showLoading: true,
  })
}

// ==================== 6.4.2 执行盘点 ====================
/** GET /api/inventory/execute/details/{taskId} — 获取盘点明细列表 */
export function getTaskDetails(taskId) {
  return request.get(`/inventory/execute/details/${taskId}`)
}

/** PUT /api/inventory/execute/confirm — 确认盘点明细（逐项/批量） */
export function confirmDetails(data) {
  return request.put('/inventory/execute/confirm', data)
}

/** POST /api/inventory/execute/surplus-asset — 盘盈资产登记入库 */
export function registerSurplusAsset(data) {
  return request.post('/inventory/execute/surplus-asset', data)
}

// ==================== 6.4.3 盘点差异记录 ====================
/** GET /api/inventory/difference/list — 盘点差异列表（分页） */
export function listDifferences(params) {
  return request.get('/inventory/difference/list', { params })
}

/** GET /api/inventory/difference/detail/{id} — 差异详情 */
export function getDifferenceDetail(id) {
  return request.get(`/inventory/difference/detail/${id}`)
}

/** PUT /api/inventory/difference/handle — 批量标记差异已处理 */
export function handleDifferences(data) {
  return request.put('/inventory/difference/handle', data)
}

/** GET /api/inventory/difference/export — 导出差异记录Excel */
export function exportDifferences(params) {
  return request.get('/inventory/difference/export', {
    params,
    responseType: 'blob',
    showLoading: true,
  })
}
