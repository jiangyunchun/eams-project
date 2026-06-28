<template>
  <div>
    <div class="page-header"><h2>调拨审批</h2></div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="调拨编号"><el-input v-model="query.transferNo" placeholder="精确匹配" clearable style="width:180px" /></el-form-item>
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
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="150" align="center">
          <template #default="{row}"><StatusTag :value="row.status" type="transfer" /></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <div class="action-btns">
              <el-button v-if="canApprove(row)" link type="success" size="small" @click="handlePass(row)">通过</el-button>
              <el-button v-if="canApprove(row)" link type="danger" size="small" @click="openReject(row)">驳回</el-button>
              <span v-if="!canApprove(row)" style="color:#A0AEC0;font-size:12px">已处理</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回调拨申请" width="480px" :close-on-click-modal="false">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因（10-200个字符）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible=false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApproval, approveTransfer, rejectTransfer } from '@/api/transfer'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const tableData = ref([]); const total = ref(0); const loading = ref(false)
const dateRange = ref([])
const query = reactive({ transferNo: '', status: null, beginDate: '', endDate: '', pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value?.length === 2) { query.beginDate = dateRange.value[0]; query.endDate = dateRange.value[1] }
    else { query.beginDate = ''; query.endDate = '' }
    const res = await listApproval(query)
    tableData.value = res.data.list || []; total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '查询失败'); tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}
function resetQuery() { query.transferNo = ''; query.status = null; dateRange.value = []; query.pageNum = 1; fetchData() }

function canApprove(row) {
  if (!auth.permissions?.includes('transfer:approve')) return false
  // 部门管理员仅有 confirm 权限，只能处理 status=0（确认调入）
  if (auth.permissions?.includes('transfer:approve:confirm') && !auth.permissions?.includes('transfer:approve:pass')) {
    return row.status === 0
  }
  // 资产管理员有 pass 权限，可处理 status=0 或 status=1
  return row.status === 0 || row.status === 1
}

async function handlePass(row) {
  const msg = row.status === 0
    ? `确认接收资产【${row.assetName}】？确认后将提交资产管理员审批`
    : `确认通过该调拨申请？资产【${row.assetName}】将从【${row.fromDeptName}】划转至【${row.toDeptName}】`
  try { await ElMessageBox.confirm(msg, '确认通过', { type: 'warning', confirmButtonText: '确认通过' }) }
  catch (e) { return }
  try {
    await approveTransfer({ transferId: row.id })
    ElMessage.success(row.status === 0 ? '已确认接收，待资产管理员审批' : '调拨审批通过，资产已划转')
    fetchData()
  } catch (e) { /* handled */ }
}

const rejectVisible = ref(false); const rejecting = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({ transferId: null, rejectReason: '' })
const rejectRules = { rejectReason: [{ required: true, message: '驳回原因为10-200个字符', trigger: 'blur' }, { min: 10, max: 200, message: '驳回原因为10-200个字符', trigger: 'blur' }] }

function openReject(row) { rejectForm.transferId = row.id; rejectForm.rejectReason = ''; rejectVisible.value = true }
async function handleReject() {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return
  rejecting.value = true
  try { await rejectTransfer(rejectForm); ElMessage.success('已驳回该调拨申请'); rejectVisible.value = false; fetchData() }
  catch (e) { /* handled */ }
  finally { rejecting.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
