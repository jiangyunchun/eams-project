<template>
  <div>
    <div class="page-header"><h2>操作日志</h2></div>

    <!-- 搜索区（PRD 6.1.6） -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="操作人"><el-input v-model="query.operator" placeholder="模糊搜索" clearable style="width:150px" /></el-form-item>
        <el-form-item label="操作模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width:160px">
            <el-option label="系统管理" value="系统管理" />
            <el-option label="资产台账" value="资产台账" />
            <el-option label="领用管理" value="领用管理" />
            <el-option label="盘点管理" value="盘点管理" />
            <el-option label="采购入库" value="采购入库" />
            <el-option label="维保报修" value="维保报修" />
            <el-option label="报废处置" value="报废处置" />
            <el-option label="AI查询" value="AI查询" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.actionType" placeholder="全部" clearable style="width:140px">
            <el-option label="新增" value="新增" />
            <el-option label="编辑" value="编辑" />
            <el-option label="删除" value="删除" />
            <el-option label="导入" value="导入" />
            <el-option label="导出" value="导出" />
            <el-option label="登录" value="登录" />
            <el-option label="审批" value="审批" />
            <el-option label="查询" value="查询" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:240px" />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="actionType" label="操作类型" width="100">
          <template #default="{ row }"><el-tag size="small">{{ row.actionType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <el-form v-if="detailData" label-width="100px">
        <el-form-item label="操作人">{{ detailData.operator }}</el-form-item>
        <el-form-item label="操作模块">{{ detailData.module }}</el-form-item>
        <el-form-item label="操作类型">{{ detailData.actionType }}</el-form-item>
        <el-form-item label="操作描述">{{ detailData.description }}</el-form-item>
        <el-form-item label="请求参数">
          <pre class="json-block">{{ formatJson(detailData.requestParams) }}</pre>
        </el-form-item>
        <el-form-item label="变更前数据">
          <pre class="json-block">{{ formatJson(detailData.beforeData) }}</pre>
        </el-form-item>
        <el-form-item label="变更后数据">
          <pre class="json-block">{{ formatJson(detailData.afterData) }}</pre>
        </el-form-item>
        <el-form-item label="IP地址">{{ detailData.ipAddress }}</el-form-item>
        <el-form-item label="操作时间">{{ formatTime(detailData.createTime) }}</el-form-item>
        <el-form-item label="耗时(ms)">{{ detailData.costTime }}</el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { listLogs, getLogDetail } from '@/api/system'

function formatTime(time) { return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-' }

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ operator: '', module: '', actionType: '', beginTime: '', endTime: '', pageNum: 1, pageSize: 10 })
const dateRange = ref(null)

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref(null)

async function fetchData() {
  if (dateRange.value) {
    query.beginTime = dateRange.value[0]
    query.endTime = dateRange.value[1]
  } else { query.beginTime = ''; query.endTime = '' }
  loading.value = true
  try {
    const res = await listLogs(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败，请重试')
    tableData.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetQuery() {
  query.operator = ''; query.module = ''; query.actionType = ''
  dateRange.value = null; query.pageNum = 1; fetchData()
}

async function openDetail(row) {
  const res = await getLogDetail(row.id)
  detailData.value = res.data
  detailVisible.value = true
}

function formatJson(str) {
  if (!str) return '-'
  try { return JSON.stringify(JSON.parse(str), null, 2) }
  catch (e) { return str }
}

onMounted(fetchData)
</script>

<style scoped>
.json-block {
  background: #F7FAFC; border: 1px solid #E2E8F0; border-radius: 4px;
  padding: 12px; max-height: 300px; overflow: auto;
  font-size: 13px; line-height: 1.6; white-space: pre-wrap; margin: 0;
}
</style>
