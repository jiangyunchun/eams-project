import request from '@/utils/request'

// ==================== 6.9.1 报废申请 ====================
/** POST /api/scrap/apply — 提交报废申请 */
export function applyScrap(data) {
  return request.post('/scrap/apply', data)
}

// ==================== 6.9.2 报废审批 ====================
/** GET /api/scrap/approval/list — 审批列表（待初审/待终审） */
export function listApproval(params) {
  return request.get('/scrap/approval/list', { params })
}

/** PUT /api/scrap/approve — 审批通过/驳回 */
export function approveScrap(data) {
  return request.put('/scrap/approve', data)
}

// ==================== 6.9.3 报废处置登记 ====================
/** GET /api/scrap/disposal/list — 待处置列表 */
export function listDisposal(params) {
  return request.get('/scrap/disposal/list', { params })
}

/** PUT /api/scrap/disposal — 处置登记 */
export function disposalScrap(data) {
  return request.put('/scrap/disposal', data)
}

// ==================== 6.9.4 报废记录 ====================
/** GET /api/scrap/record/list — 报废记录列表 */
export function listRecords(params) {
  return request.get('/scrap/record/list', { params })
}

/** GET /api/scrap/record/detail/{id} — 报废单详情 */
export function getDetail(id) {
  return request.get(`/scrap/record/detail/${id}`)
}

/** GET /api/scrap/record/export — 导出报废记录Excel */
export function exportRecords(params) {
  return request.get('/scrap/record/export', {
    params,
    responseType: 'blob',
    showLoading: true,
  })
}
