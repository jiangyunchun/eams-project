<template>
  <div>
    <div class="page-header">
      <h2>工作台</h2>
    </div>

    <el-row :gutter="16">
      <el-col :span="6" v-for="card in stats" :key="card.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px;">
      <template #header>
        <span>快捷操作</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="4" v-for="item in quickActions" :key="item.label">
          <div class="quick-action" @click="item.action">
            <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { User, Setting, Document, Box, Present, Tools } from '@element-plus/icons-vue'

const router = useRouter()

const stats = [
  { label: '资产总数', value: '—' },
  { label: '在用资产', value: '—' },
  { label: '待审批领用', value: '—' },
  { label: '本次盘点任务', value: '—' },
]

const quickActions = [
  { label: '用户管理', icon: User, color: '#2B6CB0', action: () => router.push('/system/user') },
  { label: '资产台账', icon: Box, color: '#38A169', action: () => {} },
  { label: '领用申请', icon: Present, color: '#DD6B20', action: () => {} },
  { label: '报修登记', icon: Tools, color: '#E53E3E', action: () => {} },
  { label: '系统参数', icon: Setting, color: '#718096', action: () => router.push('/system/config') },
  { label: '操作日志', icon: Document, color: '#2B6CB0', action: () => router.push('/system/log') },
]
</script>

<style scoped lang="scss">
.stat-card {
  text-align: center;
  cursor: default;
  .stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #2B6CB0;
  }
  .stat-label {
    font-size: 14px;
    color: #718096;
    margin-top: 4px;
  }
}
.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  span { font-size: 13px; color: #4A5568; }
  &:hover { background: #EBF4FF; }
}
</style>
