import { defineStore } from 'pinia'
import { getAllDict } from '@/api/system/dict'

/**
 * 字典缓存 Store
 * 系统启动时从后端获取全量字典，缓存至 Pinia，供所有下拉框复用
 */
export const useDictStore = defineStore('dict', {
  state: () => ({
    /** 全量字典数据: { dictCode: [{ label, value, cssClass }] } */
    dictMap: {},
    /** 是否已加载 */
    loaded: false,
  }),

  getters: {
    /** 获取某个字典的选项列表 */
    getDict: (state) => (dictCode) => state.dictMap[dictCode] || [],
    /** 根据字典编码和值获取显示文本 */
    getLabel: (state) => (dictCode, value) => {
      const items = state.dictMap[dictCode] || []
      const found = items.find((i) => i.value === String(value))
      return found ? found.label : value
    },
  },

  actions: {
    /** 加载全量字典 */
    async loadDict() {
      if (this.loaded) return
      try {
        const res = await getAllDict()
        this.dictMap = res.data || {}
        this.loaded = true
      } catch (e) {
        console.warn('字典加载失败，将在需要时重试')
      }
    },
    /** 强制刷新（字典编辑后调用） */
    async refresh() {
      this.loaded = false
      await this.loadDict()
    },
  },
})
