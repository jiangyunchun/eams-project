<template>
  <div>
    <div class="page-header">
      <h2>角色管理</h2>
      <div class="page-actions"><el-button type="primary" @click="openAddDialog">新增角色</el-button></div>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="角色名称"><el-input v-model="query.roleName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="query.roleCode" placeholder="精确匹配" clearable style="width:200px" /></el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="query.roleName='';query.roleCode='';fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleCode" label="角色编码" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="250" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss') }}</template>
        </el-table-column>
        <el-table-column label="修改时间" width="170">
          <template #default="{ row }">{{ dayjs(row.updateTime).format('YYYY-MM-DD HH:mm:ss') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="openPermDialog(row)">权限配置</el-button>
              <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="row.roleCode!=='ROLE_SUPER_ADMIN' && row.roleCode!=='ROLE_ASSET_ADMIN' && row.roleCode!=='ROLE_DEPT_ADMIN' && row.roleCode!=='ROLE_EMPLOYEE'"
                link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit?'编辑角色':'新增角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="2-20个字符" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isEdit" placeholder="大写字母+下划线，如 ROLE_XXX" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog v-model="permVisible" title="权限配置" width="520px" :close-on-click-modal="false">
      <div style="max-height:460px; overflow-y:auto">
        <el-tree
          ref="treeRef"
          :data="menuTree"
          :props="{ label: 'menuName', children: 'children' }"
          show-checkbox
          node-key="id"
          default-expand-all
          highlight-current
        />
      </div>
      <template #footer>
        <el-button @click="permVisible=false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="handleSavePerm">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  listRoles, addRole, editRole, deleteRole, assignPermission, getMenuIds, getMenuTree,
} from '@/api/system'

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const permSubmitting = ref(false)
const query = reactive({ roleName: '', roleCode: '', pageNum: 1, pageSize: 10 })

// ---- 角色表单 ----
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, roleName: '', roleCode: '', description: '' })
const rules = {
  roleName: [{ required: true, message: '角色名称为2-20个字符', trigger: 'blur' }, { min: 2, max: 20, message: '角色名称为2-20个字符', trigger: 'blur' }],
  roleCode: [{ required: true, message: '角色编码须为大写字母+下划线格式', trigger: 'blur' }, { pattern: /^[A-Z_]{1,50}$/, message: '角色编码须为大写字母+下划线格式', trigger: 'blur' }],
  description: [{ max: 200, message: '描述不能超过200个字符', trigger: 'blur' }],
}

// ---- 权限配置 ----
const permVisible = ref(false)
const treeRef = ref(null)
const menuTree = ref([])
const currentRoleId = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const res = await listRoles(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败，请重试')
    tableData.value = []; total.value = 0
  } finally { loading.value = false }
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(form, { id: null, roleName: '', roleCode: '', description: '' })
  formVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, description: row.description })
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await editRole(form); ElMessage.success('角色信息修改成功') }
    else { await addRole(form); ElMessage.success('角色创建成功') }
    formVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确认删除角色？', '确认删除', { type: 'warning' })
    await deleteRole(id)
    ElMessage.success('角色已删除'); fetchData()
  } catch (e) { /* 取消 */ }
}

// ---- 权限配置 ----
async function openPermDialog(row) {
  currentRoleId.value = row.id
  permVisible.value = true
  // 加载菜单树
  const treeRes = await getMenuTree()
  menuTree.value = buildTree(treeRes.data || [])
  // 加载已有权限
  const idsRes = await getMenuIds(row.id)
  const checkedIds = idsRes.data || []
  // 延迟等待树渲染
  setTimeout(() => { treeRef.value?.setCheckedKeys(checkedIds) }, 200)
}

function buildTree(list, parentId = 0) {
  return list.filter(i => i.parentId === parentId).map(i => ({ ...i, children: buildTree(list, i.id) }))
}

async function handleSavePerm() {
  const checkedIds = treeRef.value?.getCheckedKeys() || []
  const halfIds = treeRef.value?.getHalfCheckedKeys() || []
  permSubmitting.value = true
  try {
    await assignPermission({ roleId: currentRoleId.value, menuIds: [...checkedIds, ...halfIds] })
    ElMessage.success('权限配置保存成功')
    permVisible.value = false
  } finally { permSubmitting.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
