<template>
  <!--
    盘点任务管理页（PRD 6.4.1）
    权限: 超级管理员/资产管理员 → 全部操作
         部门管理员 → 仅查看本部门任务
  -->
  <div>
    <div class="page-header">
      <h2>盘点任务</h2>
      <div class="page-actions">
        <el-button v-if="canCreate" type="primary" @click="openCreateDialog">创建任务</el-button>
        <el-button v-if="canCreate" type="success" @click="handleExport" :loading="exporting">导出Excel</el-button>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="q" inline>
        <el-form-item label="任务编号">
          <el-input v-model="q.taskNo" placeholder="任务编号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="任务名称">
          <el-input v-model="q.taskName" placeholder="任务名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="盘点状态">
          <el-select v-model="q.status" clearable placeholder="全部" style="width:140px">
            <el-option label="进行中" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:240px" />
        </el-form-item>
        <el-form-item label="创建人">
          <el-input v-model="q.creatorName" placeholder="创建人" clearable style="width:160px" />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="taskNo" label="任务编号" width="180" />
        <el-table-column prop="taskName" label="任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="盘点范围" width="120">
          <template #default="{ row }">{{ row.scopeTypeLabel || '—' }}</template>
        </el-table-column>
        <el-table-column prop="inventoryDate" label="盘点日期" width="120" />
        <el-table-column label="应盘/已盘" width="130">
          <template #default="{ row }">
            <span>{{ row.totalCount ?? 0 }} / {{ row.checkedCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="完成进度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.completionRate ?? 0" :stroke-width="16"
              :status="row.completionRate >= 100 ? 'success' : ''"
              :color="row.status === 2 ? '#909399' : '#2B6CB0'" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :value="row.status" type="inventory-task" />
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row)">查看</el-button>
            <el-button v-if="row.status === 0" type="success" size="small" link
              @click="$router.push(`/inventory/execute/${row.id}`)">执行盘点</el-button>
            <el-button v-if="row.status === 0" type="warning" size="small" link
              @click="handleComplete(row)">完成盘点</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" link
              @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EamsPagination v-model:page="q.pageNum" v-model:size="q.pageSize" :total="total" @change="load" />
    </div>

    <!-- 创建任务弹窗 -->
    <el-dialog v-model="createVisible" title="创建盘点任务" width="600px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px" style="max-width:500px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createForm.taskName" placeholder="请输入盘点任务名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="范围类型" prop="scopeType">
          <el-radio-group v-model="createForm.scopeType">
            <el-radio value="ALL">全公司</el-radio>
            <el-radio value="DEPT">按部门</el-radio>
            <el-radio value="CATEGORY">按分类</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.scopeType === 'DEPT'" label="盘点部门" prop="deptIds">
          <DeptTreeSelect v-model="createForm.deptIds" multiple style="width:100%" />
        </el-form-item>
        <el-form-item v-if="createForm.scopeType === 'CATEGORY'" label="盘点分类" prop="categoryCodes">
          <DictSelect dictCode="asset_category" v-model="createForm.categoryCodes" multiple style="width:100%" />
        </el-form-item>
        <el-form-item label="盘点日期" prop="inventoryDate">
          <el-date-picker v-model="createForm.inventoryDate" type="date" placeholder="请选择盘点日期"
            value-format="YYYY-MM-DD" style="width:100%" :disabled-date="dateBeforeToday" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="选填，最大500字符" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情弹窗 -->
    <el-dialog v-model="detailVisible" title="盘点任务详情" width="700px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ detail.taskName }}</el-descriptions-item>
        <el-descriptions-item label="盘点范围">{{ detail.scopeTypeLabel }}</el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ detail.inventoryDate }}</el-descriptions-item>
        <el-descriptions-item label="应盘数量">{{ detail.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="已盘数量">{{ detail.checkedCount }}</el-descriptions-item>
        <el-descriptions-item label="正常数量">{{ detail.normalCount }}</el-descriptions-item>
        <el-descriptions-item label="盘盈数量">
          <span style="color:#67C23A">{{ detail.surplusCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="盘亏数量">
          <span style="color:#F56C6C">{{ detail.shortageCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :value="detail.status" type="inventory-task" />
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTasks, createTask, cancelTask, completeTask, exportTasks, getTaskDetail } from '@/api/inventory'
import { useAuthStore } from '@/store/auth'
import StatusTag from '@/components/common/StatusTag.vue'
import EamsPagination from '@/components/common/EamsPagination.vue'
import DictSelect from '@/components/common/DictSelect.vue'
import DeptTreeSelect from '@/components/common/DeptTreeSelect.vue'

const auth = useAuthStore()
function hasPerm(p) { return auth.permissions?.includes(p) }
const canCreate = computed(() => auth.hasRole('ROLE_SUPER_ADMIN') || auth.hasRole('ROLE_ASSET_ADMIN'))

// ---------- 列表 ----------
const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])

const q = reactive({
  pageNum: 1, pageSize: 10,
  taskNo: '', taskName: '', status: null, creatorName: '',
  beginDate: '', endDate: '',
})

async function load() {
  loading.value = true
  try {
    const params = { ...q }
    if (dateRange.value?.length === 2) {
      params.beginDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await listTasks(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function search() { q.pageNum = 1; load() }
function reset() {
  q.taskNo = ''; q.taskName = ''; q.status = null; q.creatorName = ''
  dateRange.value = []
  search()
}

// ---------- 创建任务 ----------
const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref(null)

const createForm = reactive({
  taskName: '', scopeType: 'ALL', deptIds: [], categoryCodes: [],
  inventoryDate: '', remark: '',
})

const createRules = {
  taskName: [{ required: true, message: '任务名称为2-50个字符', trigger: 'blur', min: 2, max: 50 }],
  scopeType: [{ required: true, message: '请选择盘点范围类型', trigger: 'change' }],
  deptIds: [{ required: true, message: '请选择盘点部门', trigger: 'change' }],
  categoryCodes: [{ required: true, message: '请选择盘点分类', trigger: 'change' }],
  inventoryDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
}

function dateBeforeToday(date) {
  return date.getTime() < Date.now() - 86400000
}

function openCreateDialog() { createVisible.value = true }

async function submitCreate() {
  // 条件必填校验
  if (createForm.scopeType === 'DEPT' && (!createForm.deptIds || createForm.deptIds.length === 0)) {
    ElMessage.warning('请选择盘点部门'); return
  }
  if (createForm.scopeType === 'CATEGORY' && (!createForm.categoryCodes || createForm.categoryCodes.length === 0)) {
    ElMessage.warning('请选择盘点分类'); return
  }

  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const data = {
      taskName: createForm.taskName,
      scopeType: createForm.scopeType,
      inventoryDate: createForm.inventoryDate,
      remark: createForm.remark,
    }
    if (createForm.scopeType === 'DEPT') data.deptIds = createForm.deptIds
    if (createForm.scopeType === 'CATEGORY') data.categoryCodes = createForm.categoryCodes
    const res = await createTask(data)
    ElMessage.success(res.message)
    createVisible.value = false
    load()
  } catch { /* handled */ }
  finally { creating.value = false }
}

// ---------- 取消任务 ----------
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认取消盘点任务【${row.taskName}】？取消后资产状态将恢复`, '确认取消',
      { confirmButtonText: '确认取消', cancelButtonText: '返回', type: 'warning' }
    )
  } catch { return }

  try {
    const res = await cancelTask(row.id)
    ElMessage.success(res.message)
    load()
  } catch { /* handled */ }
}

// ---------- 完成盘点 ----------
async function handleComplete(row) {
  try {
    await ElMessageBox.confirm(
      `确认完成盘点【${row.taskName}】？完成后将生成盘点报告，不可再修改`, '确认完成',
      { confirmButtonText: '确认完成', cancelButtonText: '返回', type: 'warning' }
    )
  } catch { return }

  try {
    const res = await completeTask(row.id)
    ElMessage.success(res.message)
    load()
  } catch { /* handled */ }
}

// ---------- 详情 ----------
const detailVisible = ref(false)
const detail = ref(null)

async function showDetail(row) {
  try {
    const res = await getTaskDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch { /* handled */ }
}

// ---------- 导出 ----------
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try {
    const params = { ...q, pageNum: 1, pageSize: 10000 }
    if (dateRange.value?.length === 2) {
      params.beginDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await exportTasks(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `盘点任务_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { /* handled */ }
  finally { exporting.value = false }
}

onMounted(() => { load() })
</script>
