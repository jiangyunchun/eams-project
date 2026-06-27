import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/system/login'
import router from '@/router'

/**
 * 用户认证 Store
 * 管理 Token、用户信息、角色、权限列表
 * 页面刷新时从 localStorage 恢复权限数据
 */
export const useAuthStore = defineStore('auth', {
  state: () => {
    const savedUserInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    return {
      token: localStorage.getItem('token') || '',
      userInfo: savedUserInfo,
      // 从 localStorage 恢复 permissions，解决刷新后权限丢失的问题
      permissions: savedUserInfo?.permissions || [],
    }
  },

  getters: {
    isLoggedIn: (state) => !!state.token,
    roles: (state) => state.userInfo?.roles || [],
    hasRole: (state) => (role) => state.userInfo?.roles?.includes(role),
    hasPerm: (state) => (perm) => state.permissions?.includes(perm),
  },

  actions: {
    /** 登录 */
    async login(loginDTO) {
      const res = await loginApi(loginDTO)
      this.token = res.data.token
      this.userInfo = res.data.userInfo
      this.permissions = res.data.userInfo?.permissions || []
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
      return res
    },

    /** 获取用户信息 */
    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res.data.userInfo
        this.permissions = res.data.userInfo?.permissions || []
      } catch (e) {
        this.logout()
      }
    },

    /** 登出 */
    async logout() {
      try { await logoutApi() } catch (e) { /* ignore */ }
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    },
  },
})
