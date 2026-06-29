<template>
  <div>
    <div class="page-header">
      <h2>调拨记录</h2>
      <div class="page-actions">
        <el-button v-if="hasPerm('transfer:apply')" type="primary" @click="$router.push('/transfer/apply')">调拨申请</el-button>
        <el-button v-if="hasPerm('transfer:record')" type="success" @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="调拨编号"><el-input v-model="query.transferNo" placeholder="模糊搜索" clearable style="width:180px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待调入确认" :value="0" />
            <el-option label="待资产管理员审批" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
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

    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="transferNo" label="调拨编号" width="180" />
        <el-table-column label="调出资产" min-width="160">
          <template #default="{row}">{{ row.assetCode }} {{ row.assetName }}</template>
        </el-table-column>
        <el-table-column prop="fromDeptName" label="调出部门" width="120" />
        <el-table-column prop="toDeptName" label="调入部门" width="120" />
        <el-table-column prop="toLocation" label="调入地点" width="100" />
        <el-table-column prop="toUserName" label="调入使用人" width="100">
          <template #default="{row}">{{ row.toUserName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="150" align="center">
          <template #default="{row}"><StatusTag :value="row.status" type="transfer" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="调拨详情" width="640px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="调拨编号" :span="2">{{ detail.transferNo }}</el-descriptions-item>
        <el-descriptions-item label="资产编码">{{ detail.assetCode }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
        <el-descriptions-item label="调出部门">{{ detail.fromDeptName }}</el-descriptions-item>
        <el-descriptions-item label="调入部门">{{ detail.toDeptName }}</el-descriptions-item>
        <el-descriptions-item label="调入地点">{{ detail.toLocation }}</el-descriptions-item>
        <el-descriptions-item label="调入使用人">{{ detail.toUserName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="调拨原因" :span="2">{{ detail.transferReason }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :value="detail.status" type="transfer" /></el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listRecords, getRecordDetail, exportRecords } from '@/api/transfer'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
function hasPerm(p) { return auth.permissions?.includes(p) }

const tableData = ref([]); const total = ref(0); const loading = ref(false)
const dateRange = ref([])
const query = reactive({ transferNo: '', status: null, beginDate: '', endDate: '', pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value?.length === 2) { query.beginDate = dateRange.value[0]; query.endDate = dateRange.value[1] }
    else { query.beginDate = ''; query.endDate = '' }
    const res = await listRecords(query)
    tableData.value = res.data.list || []; total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '查询失败'); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}
function resetQuery() { query.transferNo = ''; query.status = null; dateRange.value = []; query.pageNum = 1; fetchData() }

const detailVisible = ref(false); const detail = ref(null)
async function openDetail(row) {
  try { const r = await getRecordDetail(row.id); detail.value = r.data; detailVisible.value = true }
  catch (e) { /* handled */ }
}
async function handleExport() {
  try {
    const res = await exportRecords(query)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a'); link.href = url
    link.download = `调拨记录_${new Date().toISOString().slice(0,10)}.xlsx`; link.click()
    window.URL.revokeObjectURL(url); ElMessage.success('导出成功')
  } catch (e) { /* handled */ }
}
onMounted(() => { fetchData() })
</script>
