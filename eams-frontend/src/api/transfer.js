import request from '@/utils/request'

/** 提交调拨申请 */
export function applyTransfer(data)         { return request.post('/transfer/apply', data) }

/** 我的调拨申请 */
export function listMyApply(params)         { return request.get('/transfer/apply/list', { params }) }

/** 审批列表 */
export function listApproval(params)        { return request.get('/transfer/approval/list', { params }) }

/** 审批通过 */
export function approveTransfer(data)       { return request.put('/transfer/approval/pass', data) }

/** 审批驳回 */
export function rejectTransfer(data)        { return request.put('/transfer/approval/reject', data) }

/** 调拨记录列表 */
export function listRecords(params)         { return request.get('/transfer/record/list', { params }) }

/** 调拨记录详情 */
export function getRecordDetail(id)         { return request.get(`/transfer/record/detail/${id}`) }

/** 导出调拨记录 */
export function exportRecords(params) {
  return request.get('/transfer/record/export', { params, responseType: 'blob', showLoading: true })
}
