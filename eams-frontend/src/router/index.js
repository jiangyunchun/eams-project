import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Monitor' },
      },
      // ---- 领用管理 ----
      {
        path: 'requisition/apply',
        name: 'RequisitionApply',
        component: () => import('@/views/requisition/ApplyRequisition.vue'),
        meta: { title: '领用申请', icon: 'Present', perm: 'requisition:apply' },
      },
      {
        path: 'requisition/approval',
        name: 'RequisitionApproval',
        component: () => import('@/views/requisition/ApprovalManagement.vue'),
        meta: { title: '审批管理', icon: 'Present', perm: 'requisition:approve' },
      },
      {
        path: 'requisition/return',
        name: 'RequisitionReturn',
        component: () => import('@/views/requisition/ReturnRegistration.vue'),
        meta: { title: '归还登记', icon: 'Present', perm: 'requisition:return' },
      },
      {
        path: 'requisition/record',
        name: 'RequisitionRecord',
        component: () => import('@/views/requisition/RequisitionRecord.vue'),
        meta: { title: '领用记录', icon: 'Present', perm: 'requisition:record' },
      },
      // ---- 采购入库 ----
      {
        path: 'procurement/add',
        name: 'ProcurementAdd',
        component: () => import('@/views/procurement/AddProcurement.vue'),
        meta: { title: '采购登记', icon: 'ShoppingCart', perm: 'procurement:add' },
      },
      {
        path: 'procurement/supplier',
        name: 'ProcurementSupplier',
        component: () => import('@/views/procurement/SupplierList.vue'),
        meta: { title: '供应商管理', icon: 'ShoppingCart', perm: 'procurement:add' },
      },
      {
        path: 'procurement/record',
        name: 'ProcurementRecord',
        component: () => import('@/views/procurement/ProcurementRecord.vue'),
        meta: { title: '采购记录', icon: 'ShoppingCart', perm: 'procurement:add' },
      },
      // ---- 资产台账 ----
      {
        path: 'asset/list',
        name: 'AssetList',
        component: () => import('@/views/asset/list.vue'),
        meta: { title: '资产列表', icon: 'Box', perm: 'asset:list' },
      },
      // ---- 系统管理 ----
      {
        path: 'system/user',
        name: 'User',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'User', perm: 'system:user:list' },
      },
      {
        path: 'system/role',
        name: 'Role',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Avatar', perm: 'system:role:list' },
      },
      {
        path: 'system/dept',
        name: 'Dept',
        component: () => import('@/views/system/DeptList.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', perm: 'system:dept:list' },
      },
      {
        path: 'system/dict',
        name: 'Dict',
        component: () => import('@/views/system/DictList.vue'),
        meta: { title: '数据字典', icon: 'Collection', perm: 'system:dict:list' },
      },
      {
        path: 'system/config',
        name: 'Config',
        component: () => import('@/views/system/ConfigList.vue'),
        meta: { title: '系统参数', icon: 'SetUp', perm: 'system:config:list' },
      },
      {
        path: 'system/log',
        name: 'OperationLog',
        component: () => import('@/views/system/OperationLog.vue'),
        meta: { title: '操作日志', icon: 'Document', perm: 'system:log:list' },
      },
      {
        path: 'system/login-log',
        name: 'LoginLog',
        component: () => import('@/views/system/LoginLog.vue'),
        meta: { title: '登录日志', icon: 'Key', perm: 'system:log:list' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  if (to.meta.noAuth) return next()
  const auth = useAuthStore()
  if (!auth.isLoggedIn) {
    return next('/login')
  }
  next()
})

export default router
