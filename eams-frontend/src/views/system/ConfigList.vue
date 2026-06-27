<template>
  <div>
    <div class="page-header">
      <h2>系统参数配置</h2>
      <div class="page-actions">
        <el-button type="primary" @click="openAddDialog">新增参数</el-button>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="参数键名"><el-input v-model="query.paramKey" placeholder="模糊搜索" clearable style="width:180px" /></el-form-item>
        <el-form-item label="参数名称"><el-input v-model="query.paramName" placeholder="模糊搜索" clearable style="width:180px" /></el-form-item>
        <el-form-item label="参数分组">
          <el-select v-model="query.paramGroup" placeholder="全部" clearable style="width:160px">
            <el-option label="用户管理" value="用户管理" />
            <el-option label="资产管理" value="资产管理" />
            <el-option label="领用管理" value="领用管理" />
            <el-option label="AI查询" value="AI查询" />
            <el-option label="系统安全" value="系统安全" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="query.paramKey='';query.paramName='';query.paramGroup='';fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 分组卡片展示 -->
    <div v-for="group in groupedData" :key="group.groupName" style="margin-bottom:16px">
      <el-card shadow="never">
        <template #header><span>{{ group.groupName }}（{{ group.items.length }}项）</span></template>
        <el-table :data="group.items" stripe border size="small">
          <el-table-column prop="paramKey" label="参数键名" width="220" />
          <el-table-column prop="paramName" label="参数名称" width="180" />
          <el-table-column prop="paramValue" label="当前值" width="150">
            <template #default="{ row }">
              <el-tag v-if="row.paramType==='SWITCH'" :type="row.paramValue==='true'?'success':'info'" size="small">{{ row.paramValue==='true'?'启用':'关闭' }}</el-tag>
              <span v-else>{{ row.paramValue }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="paramType" label="类型" width="80">
            <template #default="{ row }"><el-tag size="small">{{ row.paramType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="defaultValue" label="默认值" width="120" />
          <el-table-column prop="status" label="状态" width="70">
            <template #default="{ row }"><StatusTag :value="row.status" type="user" /></template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="修改时间" width="170">
            <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="action-btns">
                <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="primary" size="small" @click="handleReset(row)">重置</el-button>
                <el-button v-if="!row.isSystem" link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <div style="text-align:right; margin-top:16px">
      <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit?'编辑参数':'新增参数'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="参数键名" prop="paramKey">
          <el-input v-model="form.paramKey" :disabled="isEdit" placeholder="小写字母+点号+下划线" />
        </el-form-item>
        <el-form-item label="参数名称" prop="paramName">
          <el-input v-model="form.paramName" placeholder="2-50字符" />
        </el-form-item>
        <el-form-item label="参数值" prop="paramValue">
          <el-input v-model="form.paramValue" v-if="form.paramType==='TEXT'" placeholder="文本值" />
          <el-input-number v-model="form.paramValue" v-else-if="form.paramType==='NUMBER'" :min="0" :max="999999" style="width:100%" />
          <el-switch v-model="switchVal" v-else-if="form.paramType==='SWITCH'" />
          <el-input v-model="form.paramValue" v-else type="textarea" :rows="3" placeholder="JSON格式" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="参数类型" prop="paramType">
              <el-select v-model="form.paramType" placeholder="请选择" style="width:100%">
                <el-option label="文本" value="TEXT" /><el-option label="数字" value="NUMBER" />
                <el-option label="开关" value="SWITCH" /><el-option label="JSON" value="JSON" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数分组" prop="paramGroup">
              <el-select v-model="form.paramGroup" placeholder="请选择" style="width:100%">
                <el-option label="用户管理" value="用户管理" /><el-option label="资产管理" value="资产管理" />
                <el-option label="领用管理" value="领用管理" /><el-option label="AI查询" value="AI查询" />
                <el-option label="系统安全" value="系统安全" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="默认值" prop="defaultValue"><el-input v-model="form.defaultValue" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { listConfigs, addConfig, editConfig, resetConfig, deleteConfig } from '@/api/system'
import { useConfigStore } from '@/store/config'

function formatTime(time) { return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-' }

const configStore = useConfigStore()
const query = reactive({ paramKey: '', paramName: '', paramGroup: '', pageNum: 1, pageSize: 50 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 按分组展示
const groupedData = computed(() => {
  const groups = {}
  const groupOrder = ['用户管理', '资产管理', '领用管理', 'AI查询', '系统安全']
  tableData.value.forEach(item => {
    const g = item.paramGroup || '其他'
    if (!groups[g]) groups[g] = []
    groups[g].push(item)
  })
  return groupOrder.filter(g => groups[g]).map(g => ({ groupName: g, items: groups[g] }))
})

// ---- 表单 ----
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const form = reactive({
  id: null, paramKey: '', paramName: '', paramValue: '', paramType: 'TEXT',
  paramGroup: '资产管理', defaultValue: '', sortOrder: 0, status: 1, remark: '',
})
const switchVal = ref(false)
watch(() => form.paramType, (t) => {
  if (t === 'SWITCH') form.paramValue = switchVal.value ? 'true' : 'false'
})
watch(switchVal, (v) => { if (form.paramType === 'SWITCH') form.paramValue = v ? 'true' : 'false' })

const rules = {
  paramKey: [{ required: true, message: '参数键名不能为空', trigger: 'blur' }],
  paramName: [{ required: true, message: '参数名称为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '参数名称为2-50个字符', trigger: 'blur' }],
  paramValue: [{ required: true, message: '参数值不能为空', trigger: 'blur' }],
  paramType: [{ required: true, message: '请选择参数类型', trigger: 'change' }],
  paramGroup: [{ required: true, message: '请选择参数分组', trigger: 'change' }],
  defaultValue: [{ required: true, message: '默认值不能为空', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '排序号为0-9999的整数', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listConfigs(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败，请重试')
    tableData.value = []; total.value = 0
  } finally { loading.value = false }
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(form, { id: null, paramKey: '', paramName: '', paramValue: '', paramType: 'TEXT', paramGroup: '资产管理', defaultValue: '', sortOrder: 0, status: 1, remark: '' })
  formVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, paramKey: row.paramKey, paramName: row.paramName,
    paramValue: row.paramValue, paramType: row.paramType, paramGroup: row.paramGroup,
    defaultValue: row.defaultValue, sortOrder: row.sortOrder, status: row.status, remark: row.remark,
  })
  if (row.paramType === 'SWITCH') switchVal.value = row.paramValue === 'true'
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await editConfig(form)
      ElMessage.success(`参数【${form.paramName}】已更新为 ${form.paramValue}`)
    } else {
      await addConfig(form)
      ElMessage.success('参数配置创建成功')
    }
    formVisible.value = false
    await fetchData()
    configStore.refresh() // 全局刷新缓存
  } finally { submitting.value = false }
}

async function handleReset(row) {
  try {
    await ElMessageBox.confirm(`确认重置参数【${row.paramName}】为默认值【${row.defaultValue}】？`, '确认重置', { type: 'warning' })
    await resetConfig(row.id)
    ElMessage.success('参数已重置为默认值')
    await fetchData()
    configStore.refresh()
  } catch (e) { /* 取消 */ }
}

async function handleDelete(id) {
  await deleteConfig(id)
  ElMessage.success('参数已删除')
  await fetchData()
  configStore.refresh()
}

onMounted(fetchData)
</script>
