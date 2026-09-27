import request from './request';

/** 门店列表 */
export function fetchAdminStores(params) {
  return request({
    url: '/admin/stores',
    method: 'get',
    params
  });
}

/** 门店详情 */
export function fetchAdminStoreDetail(id) {
  return request({
    url: `/admin/stores/${id}`,
    method: 'get'
  });
}

/** 创建门店 */
export function createAdminStore(data) {
  return request({
    url: '/admin/stores/create',
    method: 'post',
    data
  });
}

/** 编辑门店 */
export function updateAdminStore(id, data) {
  return request({
    url: `/admin/stores/${id}/update`,
    method: 'post',
    data
  });
}

/** 审核门店 */
export function auditAdminStore(id, auditStatus, remark) {
  return request({
    url: `/admin/stores/${id}/audit`,
    method: 'post',
    data: { auditStatus, remark }
  });
}

/** 切换营业状态 */
export function toggleStoreBusinessStatus(id, businessStatus) {
  return request({
    url: `/admin/stores/${id}/status`,
    method: 'post',
    data: { businessStatus }
  });
}

/** 查询营业时间 */
export function fetchStoreBusinessHours(id) {
  return request({
    url: `/admin/stores/${id}/business-hours`,
    method: 'get'
  });
}

/** 保存营业时间 */
export function saveStoreBusinessHours(id, hours) {
  return request({
    url: `/admin/stores/${id}/business-hours`,
    method: 'post',
    data: { hours }
  });
}

/** 查询绑定列表 */
export function fetchStoreBindings(id, params) {
  return request({
    url: `/admin/stores/${id}/bindings`,
    method: 'get',
    params
  });
}

/** 邀请师傅 */
export function inviteTechnician(storeId, technicianId) {
  return request({
    url: `/admin/stores/${storeId}/invite/${technicianId}`,
    method: 'post'
  });
}

/** 直接解绑 */
export function unbindTechnician(storeId, technicianId) {
  return request({
    url: `/admin/stores/${storeId}/unbind/${technicianId}`,
    method: 'post'
  });
}

/** 同意解绑 */
export function approveTechnicianUnbind(storeId, technicianId) {
  return request({
    url: `/admin/stores/${storeId}/approve-unbind/${technicianId}`,
    method: 'post'
  });
}

/** 上传Logo */
export function uploadStoreLogo(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `/admin/stores/${id}/logo`,
    method: 'post',
    data: formData
  });
}

/** 重新计算评分 */
export function recalculateStoreRating(id) {
  return request({
    url: `/admin/stores/${id}/recalculate-rating`,
    method: 'post'
  });
}

/** 逆地理编码 */
export function reverseGeocode(latitude, longitude) {
  return request({
    url: '/admin/stores/reverse-geocode',
    method: 'get',
    params: { latitude, longitude }
  });
}
