<template>
  <div>
    <div class="page-header">
      <h2>归还登记</h2>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="资产编码"><el-input v-model="query.assetCode" placeholder="精确匹配" clearable style="width:160px" /></el-form-item>
        <el-form-item label="资产名称"><el-input v-model="query.assetName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
        <el-form-item label="使用人"><el-input v-model="query.userName" placeholder="模糊搜索" clearable style="width:120px" /></el-form-item>
        <el-form-item label="领用日期">
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
        <el-table-column prop="assetCode" label="资产编码" width="150" />
        <el-table-column prop="assetName" label="资产名称" min-width="140" />
        <el-table-column prop="applicantName" label="使用人" width="100" />
        <el-table-column prop="createTime" label="领用日期" width="160" />
        <el-table-column prop="expectReturnDate" label="预计归还" width="110" />
        <el-table-column label="已领用天数" width="110" align="center">
          <template #default="{ row }">
            <span :class="{ 'overdue-text': isOverdue(row) }">{{ computeDays(row.createTime) }} 天</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openReturnDialog(row)">归还登记</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 归还弹窗 -->
    <el-dialog v-model="returnVisible" title="归还登记" width="520px" :close-on-click-modal="false">
      <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="110px">
        <el-form-item label="资产编码">
          <el-input :model-value="currentRow?.assetCode" readonly />
        </el-form-item>
        <el-form-item label="归还日期" prop="returnDate">
          <el-date-picker v-model="returnForm.returnDate" type="date" value-format="YYYY-MM-DD" placeholder="选择归还日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="资产完好状态" prop="returnAssetStatus">
          <el-radio-group v-model="returnForm.returnAssetStatus">
            <el-radio :value="0">完好</el-radio>
            <el-radio :value="1">有损坏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="returnForm.returnAssetStatus === 1" label="损坏说明" prop="returnDamageDesc">
          <el-input v-model="returnForm.returnDamageDesc" type="textarea" :rows="3" placeholder="请描述资产损坏情况（10-500个字符）" />
        </el-form-item>
        <el-form-item label="备注" prop="returnRemark">
          <el-input v-model="returnForm.returnRemark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" :loading="returning" @click="handleReturn">确认归还</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { listReturn, returnAsset } from '@/api/requisition'

// ---- 表格数据 ----
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref([])
const query = reactive({ assetCode: '', assetName: '', userName: '', beginDate: '', endDate: '', pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) { query.beginDate = dateRange.value[0]; query.endDate = dateRange.value[1] }
    else { query.beginDate = ''; query.endDate = '' }
    const res = await listReturn(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '查询失败'); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

function resetQuery() {
  query.assetCode = ''; query.assetName = ''; query.userName = ''; dateRange.value = []; query.pageNum = 1
  fetchData()
}

function computeDays(createTime) {
  if (!createTime) return 0
  return dayjs().diff(dayjs(createTime), 'day')
}

function isOverdue(row) {
  if (!row.expectReturnDate) return false
  return dayjs().isAfter(dayjs(row.expectReturnDate))
}

// ---- 归还弹窗 ----
const returnVisible = ref(false)
const returning = ref(false)
const returnFormRef = ref(null)
const currentRow = ref(null)
const returnForm = reactive({ requisitionId: null, returnDate: '', returnAssetStatus: 0, returnDamageDesc: '', returnRemark: '' })

const returnRules = {
  returnDate: [{ required: true, message: '请选择归还日期', trigger: 'change' }],
  returnAssetStatus: [{ required: true, message: '请选择资产完好状态', trigger: 'change' }],
  returnDamageDesc: [
    { required: true, message: '请描述资产损坏情况', trigger: 'blur' },
    { min: 10, max: 500, message: '请描述资产损坏情况', trigger: 'blur' },
  ],
}

function openReturnDialog(row) {
  currentRow.value = row
  Object.assign(returnForm, { requisitionId: row.id, returnDate: dayjs().format('YYYY-MM-DD'), returnAssetStatus: 0, returnDamageDesc: '', returnRemark: '' })
  returnVisible.value = true
}

async function handleReturn() {
  const valid = await returnFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm(`确认归还资产【${currentRow.value.assetName}】？`, '确认归还', { type: 'warning' })
  } catch (e) { return }

  returning.value = true
  try {
    await returnAsset(returnForm)
    ElMessage.success('资产已归还')
    returnVisible.value = false
    fetchData()
  } catch (e) { /* request.js handles */ }
  finally { returning.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.overdue-text { color: #E53E3E; font-weight: 600; }
</style>
