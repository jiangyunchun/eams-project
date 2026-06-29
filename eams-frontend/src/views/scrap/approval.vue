<template>
  <div>
    <div class="page-header">
      <h2>报废审批</h2>
      <div class="page-actions">
        <el-button @click="$router.push('/scrap/record')">报废记录</el-button>
      </div>
    </div>

    <!-- 查询区（PRD 6.9.2） -->
    <el-card style="margin-bottom:16px">
      <el-form :model="q" inline>
        <el-form-item label="报废编号">
          <el-input v-model="q.scrapNo" clearable style="width:180px" placeholder="请输入报废编号" />
        </el-form-item>
        <el-form-item label="资产名称">
          <el-input v-model="q.assetName" clearable style="width:160px" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="报废原因">
          <el-select v-model="q.scrapReason" clearable style="width:140px" placeholder="全部">
            <el-option label="老化损坏" value="老化损坏" />
            <el-option label="技术淘汰" value="技术淘汰" />
            <el-option label="维修成本过高" value="维修成本过高" />
            <el-option label="盘亏确认" value="盘亏确认" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="报废状态">
          <el-select v-model="q.status" clearable style="width:140px" placeholder="全部">
            <el-option label="待初审" :value="0" />
            <el-option label="待终审" :value="1" />
            <el-option label="已通过(待处置)" :value="2" />
            <el-option label="已驳回" :value="3" />
            <el-option label="已处置" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区（PRD 6.9.2 审批表格列） -->
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="scrapNo" label="报废编号" width="180" />
        <el-table-column label="资产编码/名称" min-width="180">
          <template #default="{ row }">
            <div>{{ row.assetCode }}</div>
            <div style="color:#718096;font-size:12px">{{ row.assetName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="原值/净值" width="160">
          <template #default="{ row }">
            <div>¥{{ formatNumber(row.originalValue) }}</div>
            <div style="color:#718096;font-size:12px">
              净值: ¥{{ formatNumber(row.netValue) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="usedYears" label="已用年限" width="100" />
        <el-table-column label="报废原因" width="120">
          <template #default="{ row }">
            <span>{{ row.scrapReason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处置建议" width="100">
          <template #default="{ row }">
            <span>{{ row.disposalAdvice || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.substring(0, 16) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <StatusTag :value="row.status" type="scrap" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <!-- 待初审：资产管理员可操作（PRD 6.9.2） -->
            <template v-if="row.status === 0 && isAssetAdminOrSuper">
              <el-button type="success" size="small" @click="handleApprove(row, 1)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">
                驳回
              </el-button>
            </template>
            <!-- 待终审：超级管理员可操作（PRD 6.9.2） -->
            <template v-else-if="row.status === 1 && isSuperAdmin">
              <el-button type="success" size="small" @click="handleApprove(row, 1)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">
                驳回
              </el-button>
            </template>
            <!-- 其他状态：查看详情 -->
            <template v-else>
              <el-button type="primary" size="small" link @click="showDetail(row)">
                查看详情
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <EamsPagination
        v-model:pageNum="q.pageNum"
        v-model:pageSize="q.pageSize"
        :total="total"
        @change="load"
      />
    </el-card>

    <!-- 驳回原因弹窗（PRD 6.9.2：驳回必填驳回原因，10-200字符） -->
    <el-dialog v-model="rejectVisible" title="驳回原因" width="500px" :close-on-click-modal="false">
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回原因（10-200个字符）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="报废单详情" width="700px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="报废编号">{{ detail.scrapNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :value="detail.status" type="scrap" />
        </el-descriptions-item>
        <el-descriptions-item label="资产编码">{{ detail.assetCode }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
        <el-descriptions-item label="原值">¥{{ formatNumber(detail.originalValue) }}</el-descriptions-item>
        <el-descriptions-item label="净值">¥{{ formatNumber(detail.netValue) }}</el-descriptions-item>
        <el-descriptions-item label="报废原因">{{ detail.scrapReason }}</el-descriptions-item>
        <el-descriptions-item label="处置建议">{{ detail.disposalAdvice }}</el-descriptions-item>
        <el-descriptions-item label="原因说明" :span="2">{{ detail.reasonDesc }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
        <!-- 驳回状态：显示驳回原因（存储在 remark 字段，PRD 6.9.2） -->
        <template v-if="detail.status === 3">
          <el-descriptions-item label="驳回原因" :span="2">
            <span style="color:#E53E3E">{{ detail.remark || '-' }}</span>
          </el-descriptions-item>
        </template>
        <template v-if="detail.status === 4">
          <el-descriptions-item label="处置方式">{{ detail.disposalMethod }}</el-descriptions-item>
          <el-descriptions-item label="处置日期">{{ detail.disposalDate }}</el-descriptions-item>
          <el-descriptions-item label="处置收入">¥{{ formatNumber(detail.disposalIncome) }}</el-descriptions-item>
          <el-descriptions-item label="处置费用">¥{{ formatNumber(detail.disposalCost) }}</el-descriptions-item>
          <el-descriptions-item label="经办人">{{ detail.disposalHandler }}</el-descriptions-item>
          <el-descriptions-item label="处置说明" :span="2">{{ detail.disposalDesc || '-' }}</el-descriptions-item>
        </template>
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
import { listApproval, approveScrap, getDetail } from '@/api/scrap'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

// 角色判断
const isSuperAdmin = computed(() => auth.hasRole('ROLE_SUPER_ADMIN'))
const isAssetAdminOrSuper = computed(
  () => auth.hasRole('ROLE_ASSET_ADMIN') || auth.hasRole('ROLE_SUPER_ADMIN')
)

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const q = reactive({
  pageNum: 1,
  pageSize: 10,
  scrapNo: '',
  assetName: '',
  scrapReason: '',
  status: null,
})

// 驳回弹窗
const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectFormRef = ref(null)
const currentRow = ref(null)
const rejectForm = reactive({ rejectReason: '' })
const rejectRules = {
  rejectReason: [
    { required: true, message: '驳回原因为10-200个字符', trigger: 'blur' },
    { min: 10, max: 200, message: '驳回原因为10-200个字符', trigger: 'blur' },
  ],
}

async function load() {
  loading.value = true
  try {
    const params = { ...q }
    if (dateRange.value?.length === 2) {
      params.beginDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await listApproval(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function search() {
  q.pageNum = 1
  load()
}

function reset() {
  q.scrapNo = ''
  q.assetName = ''
  q.scrapReason = ''
  q.status = null
  dateRange.value = []
  search()
}

// 通过（PRD 6.9.2）
async function handleApprove(row, result) {
  let confirmText = ''
  if (row.status === 0) {
    confirmText = '确认通过报废初审？将提交超级管理员终审'
  } else if (row.status === 1) {
    confirmText = '确认通过报废终审？通过后资产状态将变更为【报废】，不可撤销'
  }

  try {
    await ElMessageBox.confirm(confirmText, '审批确认', {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await approveScrap({ scrapId: row.id, approvalResult: 1 })
    ElMessage.success(row.status === 0 ? '初审通过，待超级管理员终审' : '报废审批通过，资产已标记为报废，请执行处置')
    load()
  } catch (e) {
    // handled by interceptor
  }
}

// 驳回（PRD 6.9.2：弹出驳回原因弹窗）
function handleReject(row) {
  currentRow.value = row
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

async function confirmReject() {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return

  rejecting.value = true
  try {
    await approveScrap({
      scrapId: currentRow.value.id,
      approvalResult: 0,
      rejectReason: rejectForm.rejectReason,
    })
    ElMessage.success('已驳回该报废申请')
    rejectVisible.value = false
    load()
  } catch (e) {
    // handled by interceptor
  } finally {
    rejecting.value = false
  }
}

async function showDetail(row) {
  try {
    const res = await getDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    // handled by interceptor
  }
}

function formatNumber(val) {
  if (val == null) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  load()
})
</script>
