import { defineStore } from 'pinia'
import { listConfigs } from '@/api/system/config'

/**
 * 系统参数配置缓存 Store
 * 启动时加载全量参数，供全局组件读取（上传限制、分页大小等）
 */
export const useConfigStore = defineStore('config', {
  state: () => ({
    configMap: {},
    loaded: false,
  }),

  getters: {
    /** 获取某个参数的值 */
    getValue: (state) => (key) => state.configMap[key] || null,
    /** 获取数字参数 */
    getInt: (state) => (key, def) => {
      const v = state.configMap[key]
      return v !== undefined ? parseInt(v, 10) : def
    },
  },

  actions: {
    async loadConfig() {
      try {
        const res = await listConfigs({ pageNum: 1, pageSize: 200 })
        const list = res.data?.list || []
        const map = {}
        list.forEach((item) => { map[item.paramKey] = item.paramValue })
        this.configMap = map
        this.loaded = true
      } catch (e) { console.warn('系统参数加载失败', e) }
    },

    async refresh() {
      this.loaded = false
      await this.loadConfig()
    },
  },
})
