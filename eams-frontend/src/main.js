import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/store/auth'
import { useDictStore } from '@/store/dict'
import { useConfigStore } from '@/store/config'
import './assets/styles/global.scss'
// 显式导入程序化 API 样式（不会被 auto-import 自动加载 CSS）
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/message/style/css'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 应用启动时：检查登录状态并加载字典 + 系统参数 + 刷新权限
const auth = useAuthStore()
if (auth.isLoggedIn) {
  const dict = useDictStore()
  const config = useConfigStore()
  dict.loadDict()
  config.loadConfig()
  // 页面刷新时从后端重新获取权限，覆盖 localStorage 旧数据
  auth.fetchUserInfo()
}

app.mount('#app')
