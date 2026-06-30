<template>
  <!--
    盘点差异记录页（PRD 6.4.3）
    权限: 超级管理员/资产管理员 → 查看全部 + 处理
         部门管理员 → 仅查看本部门差异
  -->
  <div>
    <div class="page-header">
      <h2>盘点差异记录</h2>
      <div class="page-actions">
        <el-button v-if="canHandle" type="primary" :disabled="selectedRows.length === 0" @click="batchHandle">批量标记已处理</el-button>
        <el-button v-if="canHandle" type="success" @click="handleExport" :loading="exporting">导出Excel</el-button>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="q" inline>
        <el-form-item label="任务编号">
          <el-input v-model="q.taskNo" placeholder="任务编号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="资产编码">
          <el-input v-model="q.assetCode" placeholder="资产编码" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="差异类型">
          <el-select v-model="q.diffType" clearable placeholder="全部" style="width:130px">
            <el-option label="盘盈" :value="0" />
            <el-option label="盘亏" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="q.handleStatus" clearable placeholder="全部" style="width:130px">
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:240px" />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" :selectable="rowCanHandle" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="taskNo" label="任务编号" width="180" />
        <el-table-column prop="assetCode" label="资产编码" width="160" />
        <el-table-column prop="assetName" label="资产名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="差异类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.diffType === 0 ? 'success' : 'danger'" size="small">{{ row.diffTypeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账面/实盘数量" width="130">
          <template #default="{ row }">
            <span>{{ row.bookQty }} / {{ row.actualQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diffDesc" label="差异说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.handleStatus === 1 ? 'info' : 'warning'" size="small">{{ row.handleStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" width="100">
          <template #default="{ row }">{{ row.handlerName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="inventoryDate" label="盘点日期" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row)">查看详情</el-button>
            <el-button v-if="row.handleStatus === 0 && canHandle" type="success" size="small" link
              @click="handleSingle(row)">标记处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EamsPagination v-model:page="q.pageNum" v-model:size="q.pageSize" :total="total" @change="load" />
    </div>

    <!-- 差异详情弹窗 -->
    <el-dialog v-model="detailVisible" title="差异详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ detail.taskName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="资产编码">{{ detail.assetCode }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
        <el-descriptions-item label="差异类型">
          <el-tag :type="detail.diffType === 0 ? 'success' : 'danger'" size="small">{{ detail.diffTypeLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账面数量">{{ detail.bookQty }}</el-descriptions-item>
        <el-descriptions-item label="实盘数量">{{ detail.actualQty }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ detail.handleStatusLabel }}</el-descriptions-item>
        <el-descriptions-item label="差异说明">{{ detail.diffDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.handlerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ detail.handleTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ detail.inventoryDate }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDifferences, getDifferenceDetail, handleDifferences, exportDifferences } from '@/api/inventory'
import { useAuthStore } from '@/store/auth'
import EamsPagination from '@/components/common/EamsPagination.vue'

const auth = useAuthStore()
function hasPerm(p) { return auth.permissions?.includes(p) }
const canHandle = computed(() => auth.hasRole('ROLE_SUPER_ADMIN') || auth.hasRole('ROLE_ASSET_ADMIN'))

// ---------- 列表 ----------
const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])
const selectedRows = ref([])

const q = reactive({
  pageNum: 1, pageSize: 10,
  taskNo: '', assetCode: '', diffType: null, handleStatus: null,
  beginDate: '', endDate: '',
})

function rowCanHandle(row) { return row.handleStatus === 0 && canHandle.value }

function onSelectionChange(selection) { selectedRows.value = selection }

async function load() {
  loading.value = true
  try {
    const params = { ...q }
    if (dateRange.value?.length === 2) {
      params.beginDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await listDifferences(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* handled */ }
  finally { loading.value = false }
}

function search() { q.pageNum = 1; load() }
function reset() {
  q.taskNo = ''; q.assetCode = ''; q.diffType = null; q.handleStatus = null
  dateRange.value = []
  search()
}

// ---------- 单条处理 ----------
async function handleSingle(row) {
  try {
    await ElMessageBox.confirm('确认标记该差异为【已处理】？', '确认', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning',
    })
  } catch { return }

  try {
    await handleDifferences({ ids: [row.id] })
    ElMessage.success('差异已标记为已处理')
    load()
  } catch { /* handled */ }
}

// ---------- 批量处理 ----------
async function batchHandle() {
  const ids = selectedRows.value.filter(r => r.handleStatus === 0).map(r => r.id)
  if (ids.length === 0) { ElMessage.warning('请选择待处理的差异'); return }

  try {
    await ElMessageBox.confirm(
      `确认将选中的${ids.length}条差异标记为【已处理】？`, '确认批量处理',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  try {
    const res = await handleDifferences({ ids })
    ElMessage.success(res.message)
    load()
  } catch { /* handled */ }
}

// ---------- 详情 ----------
const detailVisible = ref(false)
const detail = ref(null)

async function showDetail(row) {
  try {
    const res = await getDifferenceDetail(row.id)
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
    const res = await exportDifferences(params)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `盘点差异_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { /* handled */ }
  finally { exporting.value = false }
}

onMounted(() => { load() })
</script>
