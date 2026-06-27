<template>
  <div>
    <div class="page-header">
      <h2>部门管理</h2>
    </div>
    <el-row :gutter="16">
      <!-- 左侧树 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>组织架构</span>
            <el-button text type="primary" size="small" style="float:right" @click="openAdd(0)">新增根部门</el-button>
          </template>
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          />
        </el-card>
      </el-col>
      <!-- 右侧详情 -->
      <el-col :span="16">
        <el-card shadow="never" v-if="currentDept">
          <template #header>
            <span>{{ currentDept.deptName }}</span>
            <div style="float:right">
              <el-button text type="primary" size="small" @click="openAdd(currentDept.id)">新增子部门</el-button>
              <el-button text type="primary" size="small" @click="openEdit(currentDept)">编辑</el-button>
              <el-button text type="danger" size="small" @click="handleDel(currentDept.id)">删除</el-button>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="部门名称">{{ currentDept.deptName }}</el-descriptions-item>
            <el-descriptions-item label="部门编码">{{ currentDept.deptCode }}</el-descriptions-item>
            <el-descriptions-item label="排序号">{{ currentDept.sortOrder }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ currentDept.status===1?'启用':'禁用' }}</el-descriptions-item>
            <el-descriptions-item label="负责人ID">{{ currentDept.leaderId||'-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(currentDept.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card shadow="never" v-else>
          <p style="text-align:center;color:#A0AEC0;padding:60px 0">请从左侧选择一个部门</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit?'编辑部门':'新增部门'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级部门" v-if="!isEdit">
          <el-select v-model="form.parentId" placeholder="顶级部门（不选=一级部门）" clearable style="width:100%">
            <el-option v-for="d in flatDepts" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="2-20个字符" />
        </el-form-item>
        <el-form-item label="部门编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="DEPT_XXXX" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getDeptTree, addDept, editDept, deleteDept } from '@/api/system'

function formatTime(time) { return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-' }

const treeData = ref([])
const treeRef = ref(null)
const currentDept = ref(null)
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({ id: null, parentId: 0, deptName: '', deptCode: '', sortOrder: 0, status: 1 })

const rules = {
  deptName: [
    { required: true, message: '部门名称为2-20个字符', trigger: 'blur' },
    { min: 2, max: 20, message: '部门名称为2-20个字符', trigger: 'blur' },
  ],
  deptCode: [
    { required: true, message: '部门编码格式为 DEPT_XXXX', trigger: 'blur' },
    { pattern: /^DEPT_[A-Z0-9]+$/, message: '部门编码格式为 DEPT_XXXX', trigger: 'blur' },
  ],
  sortOrder: [{ required: true, message: '排序号为0-9999的整数', trigger: 'blur' }],
}

// 扁平化部门列表，供上级部门选择
const flatDepts = ref([])
function flatten(list, depth = 0) {
  const result = []
  list.forEach((i) => {
    result.push({ ...i, _depth: depth })
    if (i.children && i.children.length) result.push(...flatten(i.children, depth + 1))
  })
  return result
}

function buildTree(list, parentId = 0) {
  return list.filter((i) => i.parentId === parentId).map((i) => ({
    ...i,
    children: buildTree(list, i.id),
  }))
}

async function fetchTree() {
  try {
    const res = await getDeptTree()
    const list = res.data || []
    treeData.value = buildTree(list)
    flatDepts.value = flatten(treeData.value)
  } catch (e) { ElMessage.error(e.message || '部门数据加载失败'); }
}

function handleNodeClick(data) { currentDept.value = data }

function openAdd(parentId) {
  isEdit.value = false
  form.id = null; form.parentId = parentId; form.deptName = ''
  form.deptCode = ''; form.sortOrder = 0; form.status = 1
  formVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.id = row.id; form.parentId = row.parentId; form.deptName = row.deptName
  form.deptCode = row.deptCode; form.sortOrder = row.sortOrder; form.status = row.status
  formVisible.value = true
}

async function handleSubmit() {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch { return }
  submitting.value = true
  try {
    if (isEdit.value) { await editDept(form); ElMessage.success('部门信息修改成功') }
    else { await addDept(form); ElMessage.success('部门创建成功') }
    formVisible.value = false
    await fetchTree()
  } catch (e) { /* 已在 request.js 处理 */ }
  finally { submitting.value = false }
}

async function handleDel(id) {
  try {
    await ElMessageBox.confirm('确认删除部门？删除后部门及关联数据将清除', '确认删除', { type: 'warning' })
    await deleteDept(id)
    ElMessage.success('部门已删除')
    currentDept.value = null
    await fetchTree()
  } catch (e) { /* 取消或错误 */ }
}

// 页面加载时获取数据
onMounted(() => { fetchTree() })
</script>
