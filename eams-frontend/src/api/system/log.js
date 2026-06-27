import request from '@/utils/request'

export function listLogs(params) { return request.get('/system/log/list', { params }) }
export function getLogDetail(id) { return request.get(`/system/log/detail/${id}`) }
