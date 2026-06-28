import request from '@/utils/request'

// ==================== 领用申请（PRD 6.3.1） ====================

/** 提交领用申请 */
export function applyRequisition(data)       { return request.post('/requisition/apply', data) }

/** 我的申请列表 */
export function listMyApply(params)          { return request.get('/requisition/apply/list', { params }) }

// ==================== 审批管理（PRD 6.3.2） ====================

/** 审批列表 */
export function listApproval(params)         { return request.get('/requisition/approval/list', { params }) }

/** 审批通过 */
export function approveRequisition(data)     { return request.put('/requisition/approval/pass', data) }

/** 审批驳回 */
export function rejectRequisition(data)      { return request.put('/requisition/approval/reject', data) }

// ==================== 归还登记（PRD 6.3.3） ====================

/** 归还列表 */
export function listReturn(params)           { return request.get('/requisition/return/list', { params }) }

/** 确认归还 */
export function returnAsset(data)            { return request.put('/requisition/return', data) }

// ==================== 领用记录（PRD 6.3.4） ====================

/** 领用记录列表 */
export function listRecords(params)          { return request.get('/requisition/record/list', { params }) }

/** 领用记录详情 */
export function getRecordDetail(id)          { return request.get(`/requisition/record/detail/${id}`) }

/** 导出领用记录 */
export function exportRecords(params) {
  return request.get('/requisition/record/export', { params, responseType: 'blob', showLoading: true })
}
