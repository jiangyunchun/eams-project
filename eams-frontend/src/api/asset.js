import request from '@/utils/request'

/** 资产分页列表 */
export function listAssets(params)    { return request.get('/asset/list', { params }) }

/** 资产详情 */
export function getAssetDetail(id)    { return request.get(`/asset/detail/${id}`) }

/** 新增资产 */
export function addAsset(data)        { return request.post('/asset/add', data) }

/** 编辑资产 */
export function editAsset(data)       { return request.put('/asset/edit', data) }

/** 删除资产 */
export function deleteAsset(id)       { return request.delete(`/asset/delete/${id}`) }

/** 折旧明细 */
export function getDepreciation(assetId) { return request.get(`/asset/depreciation/${assetId}`) }

/** 批量导入 */
export function importAsset(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/asset/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    showLoading: true,
  })
}

/** 导出资产 */
export function exportAsset(params) {
  return request.get('/asset/export', {
    params,
    responseType: 'blob',
    showLoading: true,
  })
}
