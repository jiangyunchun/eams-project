<template>
  <div>
    <div class="page-header">
      <h2>领用记录</h2>
      <div class="page-actions">
        <el-button v-if="hasPerm('requisition:record')" @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="申请编号"><el-input v-model="query.applyNo" placeholder="模糊搜索" clearable style="width:180px" /></el-form-item>
        <el-form-item label="资产编码"><el-input v-model="query.assetCode" placeholder="精确匹配" clearable style="width:160px" /></el-form-item>
        <el-form-item label="资产名称"><el-input v-model="query.assetName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待部门审批" :value="0" />
            <el-option label="待资产管理员审批" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
            <el-option label="已归还" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="~" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:220px" />
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
        <el-table-column prop="applyNo" label="申请编号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="assetCode" label="资产编码" width="150" />
        <el-table-column prop="assetName" label="资产名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="purpose" label="用途" min-width="160" show-overflow-tooltip />
        <el-table-column prop="expectReturnDate" label="预计归还" width="110" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.status" type="requisition" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="领用记录详情" width="680px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="申请编号" :span="2">{{ detail.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="资产编码">{{ detail.assetCode }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.specification || '-' }}</el-descriptions-item>
        <el-descriptions-item label="存放地点" :span="2">{{ detail.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="领用用途" :span="2">{{ detail.purpose }}</el-descriptions-item>
        <el-descriptions-item label="预计时长">{{ detail.expectDuration }}</el-descriptions-item>
        <el-descriptions-item label="预计归还日期">{{ detail.expectReturnDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :value="detail.status" type="requisition" />
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.approvalTime" label="审批时间">{{ detail.approvalTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.approverName" label="审批人">{{ detail.approverName }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.returnDate" label="归还日期">{{ detail.returnDate }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.returnAssetStatus != null" label="归还时完好状态">{{ detail.returnAssetStatus === 0 ? '完好' : '有损坏' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.returnDamageDesc" label="损坏说明" :span="2">{{ detail.returnDamageDesc }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listRecords, getRecordDetail, exportRecords } from '@/api/requisition'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
function hasPerm(perm) { return auth.permissions?.includes(perm) }

// ---- 表格数据 ----
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref([])
const query = reactive({ applyNo: '', assetCode: '', assetName: '', status: null, beginDate: '', endDate: '', pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) { query.beginDate = dateRange.value[0]; query.endDate = dateRange.value[1] }
    else { query.beginDate = ''; query.endDate = '' }
    const res = await listRecords(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '查询失败'); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

function resetQuery() {
  query.applyNo = ''; query.assetCode = ''; query.assetName = ''; query.status = null; dateRange.value = []; query.pageNum = 1
  fetchData()
}

// ---- 详情弹窗 ----
const detailVisible = ref(false)
const detail = ref(null)

async function openDetail(row) {
  try {
    const res = await getRecordDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) { /* request.js handles */ }
}

// ---- 导出 ----
async function handleExport() {
  try {
    const res = await exportRecords(query)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = `领用记录_${new Date().toISOString().slice(0,10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) { /* request.js handles */ }
}

onMounted(() => { fetchData() })
</script>
