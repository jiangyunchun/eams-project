<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="admin-sidebar">
      <div class="sidebar-header">
        <span v-if="!collapsed" class="sidebar-title">EAMS</span>
        <span v-else class="sidebar-title-short">E</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        background-color="#1A202C"
        text-color="#CBD5E0"
        active-text-color="#63B3ED"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <!-- 系统管理 -->
        <el-sub-menu v-if="checkPerm('system:user:list')" index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
          <el-menu-item index="/system/dict">数据字典</el-menu-item>
          <el-menu-item index="/system/config">系统参数</el-menu-item>
          <el-menu-item index="/system/log">操作日志</el-menu-item>
          <el-menu-item index="/system/login-log">登录日志</el-menu-item>
        </el-sub-menu>

        <!-- 资产台账 -->
        <el-sub-menu v-if="checkPerm('asset:list')" index="/asset">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>资产台账</span>
          </template>
          <el-menu-item index="/asset/list">资产列表</el-menu-item>
        </el-sub-menu>

        <!-- 采购入库 -->
        <el-sub-menu v-if="checkPerm('procurement:add')" index="/procurement">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>采购入库</span>
          </template>
          <el-menu-item index="/procurement/add">采购登记</el-menu-item>
          <el-menu-item index="/procurement/supplier">供应商管理</el-menu-item>
          <el-menu-item index="/procurement/record">采购记录</el-menu-item>
        </el-sub-menu>

        <!-- 领用管理 -->
        <el-sub-menu v-if="checkPerm('requisition:apply')" index="/requisition">
          <template #title>
            <el-icon><Present /></el-icon>
            <span>领用管理</span>
          </template>
          <el-menu-item index="/requisition/apply">领用申请</el-menu-item>
          <el-menu-item index="/requisition/approval">审批管理</el-menu-item>
          <el-menu-item index="/requisition/return">归还登记</el-menu-item>
          <el-menu-item index="/requisition/record">领用记录</el-menu-item>
        </el-sub-menu>

        <!-- 资产调拨 -->
        <el-sub-menu v-if="checkPerm('transfer:apply')" index="/transfer">
          <template #title>
            <el-icon><Switch /></el-icon>
            <span>资产调拨</span>
          </template>
          <el-menu-item index="/transfer/apply">调拨申请</el-menu-item>
          <el-menu-item index="/transfer/approval">调拨审批</el-menu-item>
          <el-menu-item index="/transfer/record">调拨记录</el-menu-item>
        </el-sub-menu>

        <!-- 维保报修 -->
        <el-sub-menu v-if="checkPerm('repair:apply')" index="/repair">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>维保报修</span>
          </template>
          <el-menu-item index="/repair/apply">报修登记</el-menu-item>
          <el-menu-item index="/repair/handle">维修处理</el-menu-item>
          <el-menu-item index="/repair/record">维保记录</el-menu-item>
        </el-sub-menu>

        <!-- 报废处置 -->
        <el-sub-menu v-if="checkPerm('scrap:apply')" index="/scrap">
          <template #title>
            <el-icon><Delete /></el-icon>
            <span>报废处置</span>
          </template>
          <el-menu-item index="/scrap/apply">报废申请</el-menu-item>
          <el-menu-item index="/scrap/approval">报废审批</el-menu-item>
          <el-menu-item index="/scrap/disposal">处置登记</el-menu-item>
          <el-menu-item index="/scrap/record">报废记录</el-menu-item>
        </el-sub-menu>

        <!-- 盘点管理 -->
        <el-sub-menu v-if="checkPerm('inventory:task')" index="/inventory">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>盘点管理</span>
          </template>
          <el-menu-item index="/inventory/task">盘点任务</el-menu-item>
          <el-menu-item index="/inventory/difference">差异记录</el-menu-item>
        </el-sub-menu>

        <!-- AI查询 -->
        <el-sub-menu v-if="checkPerm('ai:query')" index="/ai">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>AI查询</span>
          </template>
          <el-menu-item index="/ai/query">智能查询</el-menu-item>
          <el-menu-item index="/ai/log">查询日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta?.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ auth.userInfo?.realName || auth.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="auth.logout()">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import {
  Fold, Expand, Monitor, Setting, Box, ShoppingCart, Present,
  Switch, Tools, Delete, DataAnalysis, Cpu,
} from '@element-plus/icons-vue'

const route = useRoute()
const auth = useAuthStore()
const collapsed = ref(false)

/** 权限校验：有对应权限才显示菜单，无权限时直接隐藏整个模块 */
function checkPerm(perm) {
  if (!auth.token) return false
  if (!auth.permissions || auth.permissions.length === 0) return false
  return auth.permissions.includes(perm)
}
</script>

<style scoped lang="scss">
.admin-layout { height: 100vh; }
.admin-sidebar {
  background: #1A202C;
  overflow-y: auto;
  transition: width 0.3s;
  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #63B3ED;
    font-size: 20px;
    font-weight: 700;
    border-bottom: 1px solid #2D3748;
  }
  .sidebar-title-short { font-size: 24px; }
}
.admin-header {
  background: #fff;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    .collapse-btn { font-size: 20px; cursor: pointer; color: #4A5568; }
  }
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      .username { font-size: 14px; color: #2D3748; }
    }
  }
}
.admin-main {
  background: #F7FAFC;
  padding: 20px 24px;
  overflow-y: auto;
}
</style>
