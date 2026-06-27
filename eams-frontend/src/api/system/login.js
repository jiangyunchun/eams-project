import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request.post('/login', data)
}

/** 登出 */
export function logout() {
  return request.post('/logout')
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request.get('/user/info')
}
