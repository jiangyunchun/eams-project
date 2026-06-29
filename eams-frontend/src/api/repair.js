import request from '@/utils/request'

export function applyRepair(data)         { return request.post('/repair/apply', data) }
export function listMyApply(params)       { return request.get('/repair/apply/list', { params }) }
export function listHandle(params)        { return request.get('/repair/handle/list', { params }) }
export function acceptRepair(data)        { return request.put('/repair/handle/accept', data) }
export function completeRepair(data)      { return request.put('/repair/handle/complete', data) }
export function unfixableRepair(data)     { return request.put('/repair/handle/unfixable', data) }
export function listRecords(params)       { return request.get('/repair/record/list', { params }) }
export function getRecordDetail(id)       { return request.get(`/repair/record/detail/${id}`) }
export function exportRecords(params) {
  return request.get('/repair/record/export', { params, responseType: 'blob', showLoading: true })
}
