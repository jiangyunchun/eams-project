<template>
  <div>
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>供应商管理</h2>
      <div class="page-actions">
        <el-button v-if="hasPerm('procurement:add')" type="primary" @click="openAddDialog">新增供应商</el-button>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="供应商名称">
          <el-input v-model="query.supplierName" placeholder="模糊搜索" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="query.contactPerson" placeholder="模糊搜索" clearable style="width:150px" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="query.contactPhone" placeholder="模糊搜索" clearable style="width:150px" />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="supplierCode" label="供应商编码" width="140" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="160" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusLabel || (row.status === 1 ? '启用' : '禁用') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="hasPerm('procurement:add')" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="hasPerm('procurement:add')" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑供应商' : '新增供应商'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="供应商编码" prop="supplierCode">
          <el-input v-model="form.supplierCode" placeholder="SUP-XXXX" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input v-model="form.supplierName" placeholder="2-50个字符" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="最多20个字符" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入正确的手机号" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="最多200个字符" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="最多200个字符" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSupplier, addSupplier, editSupplier, deleteSupplier } from '@/api/procurement'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
function hasPerm(perm) { return auth.permissions?.includes(perm) }

// ---- 表格数据 ----
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ supplierName: '', contactPerson: '', contactPhone: '', pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await listSupplier(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败，请重试')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.supplierName = ''
  query.contactPerson = ''
  query.contactPhone = ''
  query.pageNum = 1
  fetchData()
}

// ---- 表单弹窗 ----
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ supplierName: '', supplierCode: 'SUP-', contactPerson: '', contactPhone: '', address: '', status: 1, remark: '' })

const rules = {
  supplierName: [
    { required: true, message: '供应商名称为2-50个字符', trigger: 'blur' },
    { min: 2, max: 50, message: '供应商名称为2-50个字符', trigger: 'blur' },
  ],
  supplierCode: [
    { required: true, message: '请输入供应商编码', trigger: 'blur' },
    { pattern: /^SUP-\d{4}$/, message: '编码格式为 SUP-XXXX', trigger: 'blur' },
  ],
  contactPerson: [
    { max: 20, message: '联系人不能超过20个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  address: [
    { max: 200, message: '地址不能超过200个字符', trigger: 'blur' },
  ],
  remark: [
    { max: 200, message: '备注不能超过200个字符', trigger: 'blur' },
  ],
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(form, { supplierName: '', supplierCode: 'SUP-', contactPerson: '', contactPhone: '', address: '', status: 1, remark: '' })
  formVisible.value = true
}

async function openEditDialog(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    supplierName: row.supplierName || '',
    supplierCode: row.supplierCode || '',
    contactPerson: row.contactPerson || '',
    contactPhone: row.contactPhone || '',
    address: row.address || '',
    status: row.status ?? 1,
    remark: row.remark || '',
  })
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await editSupplier(form)
      ElMessage.success('供应商信息修改成功')
    } else {
      await addSupplier(form)
      ElMessage.success('供应商创建成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e) {
    // request.js 已处理错误提示
  } finally {
    submitting.value = false
  }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除供应商【' + row.supplierName + '】？', '确认删除', { type: 'warning' })
    await deleteSupplier(row.id)
    ElMessage.success('供应商已删除')
    fetchData()
  } catch (e) { /* 取消或错误 */
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
