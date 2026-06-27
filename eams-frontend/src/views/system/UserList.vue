<template>
  <div>
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="openAddDialog">新增用户</el-button>
      </div>
    </div>

    <!-- 搜索区（PRD 6.1.2） -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="姓名"><el-input v-model="query.realName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
        <el-form-item label="所属部门"><DeptTreeSelect v-model="query.deptId" style="width:180px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="query.phone" placeholder="精确匹配" clearable style="width:160px" /></el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="deptName" label="所属部门" min-width="140" />
        <el-table-column prop="roleNames" label="角色" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleNames">
              <el-tag v-for="r in row.roleNames.split(',')" :key="r" size="small" style="margin-right:4px">{{ r }}</el-tag>
            </template>
            <span v-else style="color:#A0AEC0">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><StatusTag :value="row.status" type="user" /></template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="修改时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="primary" size="small" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button link :type="row.status===1?'warning':'success'" size="small" @click="handleToggleStatus(row)">
                {{ row.status===1?'禁用':'启用' }}
              </el-button>
              <el-button link type="success" size="small" :disabled="!row.locked" @click="handleUnlock(row)">解锁</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑用户':'新增用户'" width="580px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="isEdit" placeholder="4-20位，字母开头" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="2-20个字符" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" v-if="!isEdit">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="6-20位，含字母和数字" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <DeptTreeSelect v-model="form.deptId" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width:100%">
                <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="11位手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  listUsers, addUser, editUser, deleteUser, resetPassword as resetPwdApi,
  toggleUserStatus as toggleStatusApi, getUserDetail, allRoles, unlockUser
} from '@/api/system'

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const roleOptions = ref([])

const query = reactive({ realName: '', deptId: null, status: null, phone: '', pageNum: 1, pageSize: 10 })

// ---- 弹窗 ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, username: '', realName: '', password: '',
  deptId: null, roleIds: [], phone: '', email: '', status: 1,
})

// PRD 6.1.2 校验规则
const rules = {
  username: [
    { required: true, message: '用户名为4-20位，字母开头，仅支持字母、数字、下划线', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/, message: '用户名为4-20位，字母开头，仅支持字母、数字、下划线', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '姓名为2-20个字符', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名为2-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '密码为6-20位，须包含字母和数字', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/, message: '密码为6-20位，须包含字母和数字', trigger: 'blur' },
  ],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  roleIds: [{ type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}

/** 时间格式化 yyyy-MM-dd HH:mm:ss */
function formatTime(time) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listUsers(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败，请重试')
    tableData.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetQuery() {
  query.realName = ''; query.deptId = null; query.status = null; query.phone = ''
  query.pageNum = 1; fetchData()
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(form, { id: null, username: '', realName: '', password: '', deptId: null, roleIds: [], phone: '', email: '', status: 1 })
  dialogVisible.value = true
}

async function openEditDialog(row) {
  isEdit.value = true
  const res = await getUserDetail(row.id)
  const u = res.data.user
  Object.assign(form, {
    id: u.id, username: u.username, realName: u.realName,
    deptId: u.deptId, roleIds: res.data.roleIds || [],
    phone: u.phone || '', email: u.email || '', status: u.status, password: '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await editUser(form)
      ElMessage.success('用户信息修改成功')
    } else {
      await addUser(form)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确认删除用户？删除后该账号将无法登录', '确认删除', { type: 'warning' })
    await deleteUser(id)
    ElMessage.success('用户已删除')
    fetchData()
  } catch (e) { /* 取消 */ }
}

async function handleResetPwd(row) {
  try {
    await ElMessageBox.confirm(`确认重置用户【${row.realName}】的密码？密码将重置为默认密码`, '确认重置', { type: 'warning' })
    await resetPwdApi(row.id)
    ElMessage.success('密码已重置为默认密码')
  } catch (e) { /* 取消 */ }
}

async function handleUnlock(row) {
  try {
    await ElMessageBox.confirm(`确认解锁用户【${row.realName}】的账号？`, '确认解锁', { type: 'info' })
    await unlockUser(row.username)
    ElMessage.success(`账号【${row.username}】已解锁`)
  } catch (e) { /* 取消 */ }
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户【${row.realName}】？${action==='禁用'?'禁用后该账号将无法登录':''}`, '确认操作', { type: 'warning' })
    await toggleStatusApi({ id: row.id, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(`用户已${action}`)
    fetchData()
  } catch (e) { /* 取消 */ }
}

onMounted(async () => {
  await fetchData()
  try {
    const res = await allRoles()
    roleOptions.value = res.data || []
  } catch (e) { /* 角色列表加载失败，不影响主流程 */ }
})
</script>

<style scoped>
.action-btns {
  display: flex;
  flex-wrap: nowrap;
  gap: 2px;
}
.action-btns .el-button {
  padding: 0 6px;
  white-space: nowrap;
}
</style>
