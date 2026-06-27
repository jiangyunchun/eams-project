<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">EAMS</div>
        <h2>企业资产管理系统</h2>
        <p class="subtitle">Enterprise Asset Management System</p>
      </div>

      <!-- 登录表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>

      <!-- PRD 登录提示 -->
      <div class="login-tips">
        <p>演示账号：admin / Eams@123456</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { useDictStore } from '@/store/dict'
import { useConfigStore } from '@/store/config'
import { loginApi } from '@/api/system'

const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()
const config = useConfigStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '', rememberMe: false })

const rules = {
  username: [{ required: true, message: '请输入用户名和密码', trigger: 'blur' }],
  password: [{ required: true, message: '请输入用户名和密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await loginApi(form)
    // 存入 Pinia + localStorage
    auth.token = res.data.token
    auth.userInfo = res.data.userInfo
    auth.permissions = res.data.userInfo?.permissions || []
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
    // 加载字典与系统参数
    dict.loadDict()
    config.loadConfig()
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // request.js 拦截器已处理错误提示，这里仅兜底
    if (e.message) ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #EBF4FF 0%, #F7FAFC 100%);
}
.login-card {
  width: 420px; background: #fff; border-radius: 12px; padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  .login-header { text-align: center; margin-bottom: 32px;
    .logo { font-size: 36px; font-weight: 700; color: #2B6CB0; margin-bottom: 8px; }
    h2 { margin: 0 0 4px; font-size: 22px; color: #1A202C; }
    .subtitle { font-size: 13px; color: #A0AEC0; }
  }
  .login-form { .login-btn { width: 100%; font-size: 16px; } }
  .login-tips { text-align: center; margin-top: 16px; font-size: 12px; color: #A0AEC0; }
}
</style>
