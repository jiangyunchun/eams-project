<template>
  <div>
    <div class="page-header">
      <h2>审批管理</h2>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="申请编号"><el-input v-model="query.applyNo" placeholder="精确匹配" clearable style="width:180px" /></el-form-item>
        <el-form-item label="资产名称"><el-input v-model="query.assetName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待部门审批" :value="0" />
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

    <!-- 表格 -->
    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="assetCode" label="资产编码" width="150" />
        <el-table-column prop="assetName" label="资产名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column prop="expectReturnDate" label="预计归还" width="110" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.status" type="requisition" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button
                v-if="canApprove(row)"
                link type="success" size="small" @click="handlePass(row)"
              >通过</el-button>
              <el-button
                v-if="canApprove(row)"
                link type="danger" size="small" @click="openRejectDialog(row)"
              >驳回</el-button>
              <el-button
                v-if="!canApprove(row)"
                link type="info" size="small" disabled
              >{{ row.status >= 2 ? '已处理' : '仅可审批本部门申请' }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回申请" width="480px" :close-on-click-modal="false">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因（10-200个字符）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApproval, approveRequisition, rejectRequisition } from '@/api/requisition'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
function hasPerm(perm) { return auth.permissions?.includes(perm) }

// ---- 表格数据 ----
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref([])
const query = reactive({ applyNo: '', assetName: '', status: null, beginDate: '', endDate: '', pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) { query.beginDate = dateRange.value[0]; query.endDate = dateRange.value[1] }
    else { query.beginDate = ''; query.endDate = '' }
    const res = await listApproval(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '查询失败'); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

function resetQuery() {
  query.applyNo = ''; query.assetName = ''; query.status = null; dateRange.value = []; query.pageNum = 1
  fetchData()
}

// ---- 判断是否可以审批 ----
function canApprove(row) {
  // 超级管理员和资产管理员可以审批所有
  if (auth.permissions?.includes('requisition:approve')) {
    // 部门管理员只能审批本部门且 status=0
    if (!auth.permissions?.includes('requisition:approve:pass') && row.status !== 0) return false
    return row.status === 0 || row.status === 1
  }
  return false
}

// ---- 通过 ----
async function handlePass(row) {
  try {
    const msg = row.status === 1
      ? `确认通过该领用申请？通过后资产【${row.assetName}】状态将变更为【在用】，使用人绑定为【${row.applicantName}】`
      : `确认通过该领用申请？（部门初审，通过后将进入资产管理员终审）`
    await ElMessageBox.confirm(msg, '确认通过', { type: 'warning', confirmButtonText: '确认通过' })
  } catch (e) { return }

  try {
    await approveRequisition({ requisitionId: row.id })
    ElMessage.success('审批通过')
    fetchData()
  } catch (e) { /* request.js handles */ }
}

// ---- 驳回 ----
const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({ requisitionId: null, rejectReason: '' })
const rejectRules = {
  rejectReason: [
    { required: true, message: '驳回原因为10-200个字符', trigger: 'blur' },
    { min: 10, max: 200, message: '驳回原因为10-200个字符', trigger: 'blur' },
  ],
}

function openRejectDialog(row) {
  rejectForm.requisitionId = row.id
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

async function handleReject() {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return
  rejecting.value = true
  try {
    await rejectRequisition(rejectForm)
    ElMessage.success('已驳回该申请')
    rejectVisible.value = false
    fetchData()
  } catch (e) { /* request.js handles */ }
  finally { rejecting.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
