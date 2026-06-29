<template>
  <div>
    <div class="page-header">
      <h2>报废处置登记</h2>
      <div class="page-actions">
        <el-button @click="$router.push('/scrap/record')">报废记录</el-button>
      </div>
    </div>

    <!-- 查询区 -->
    <el-card style="margin-bottom:16px">
      <el-form :model="q" inline>
        <el-form-item label="报废编号">
          <el-input v-model="q.scrapNo" clearable style="width:180px" placeholder="请输入报废编号" />
        </el-form-item>
        <el-form-item label="资产名称">
          <el-input v-model="q.assetName" clearable style="width:160px" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <div style="color:#E53E3E;font-size:13px;margin-top:8px">
        注意：以下仅展示已通过终审、待处置的报废资产，请及时完成处置登记
      </div>
    </el-card>

    <!-- 表格区：仅展示 status=2 已通过(待处置)（PRD 6.9.3） -->
    <el-card>
      <el-table :data="list" v-loading="loading" stripe
        :row-class-name="tableRowClassName">
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
        <el-table-column label="报废原因" width="120">
          <template #default="{ row }">{{ row.scrapReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="处置建议" width="100">
          <template #default="{ row }">{{ row.disposalAdvice || '-' }}</template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column label="终审通过时间" width="160">
          <template #default="{ row }">
            {{ row.updateTime ? row.updateTime.substring(0, 16) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <StatusTag :value="row.status" type="scrap" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openDisposal(row)">
              处置登记
            </el-button>
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

    <!-- 处置登记弹窗（PRD 6.9.3） -->
    <el-dialog
      v-model="disposalVisible"
      title="处置登记"
      width="600px"
      :close-on-click-modal="false"
      @closed="resetDisposalForm"
    >
      <el-form
        ref="disposalFormRef"
        :model="disposalForm"
        :rules="disposalRules"
        label-width="120px"
      >
        <el-form-item label="报废资产">
          <div style="display:flex;align-items:center;gap:8px">
            <span>{{ currentDisposal?.assetCode }} {{ currentDisposal?.assetName }}</span>
            <StatusTag :value="currentDisposal?.status" type="scrap" />
          </div>
        </el-form-item>

        <el-form-item label="处置方式" prop="disposalMethod">
          <el-select v-model="disposalForm.disposalMethod" style="width:100%" placeholder="请选择处置方式">
            <el-option label="变卖" value="变卖" />
            <el-option label="回收" value="回收" />
            <el-option label="销毁" value="销毁" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="处置日期" prop="disposalDate">
          <el-date-picker
            v-model="disposalForm.disposalDate"
            type="date"
            style="width:100%"
            placeholder="请选择处置日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="处置收入(元)" prop="disposalIncome">
              <el-input-number
                v-model="disposalForm.disposalIncome"
                :min="0"
                :precision="2"
                style="width:100%"
                placeholder="0.00"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处置费用(元)" prop="disposalCost">
              <el-input-number
                v-model="disposalForm.disposalCost"
                :min="0"
                :precision="2"
                style="width:100%"
                placeholder="0.00"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="经办人" prop="disposalHandler">
          <el-input v-model="disposalForm.disposalHandler" placeholder="请填写经办人姓名（2-20个字符）" />
        </el-form-item>

        <el-form-item label="处置说明" prop="disposalDesc">
          <el-input
            v-model="disposalForm.disposalDesc"
            type="textarea"
            :rows="2"
            placeholder="选填，最大500字符"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
            :action="uploadUrl"
            :headers="{ Authorization: 'Bearer ' + token }"
            :limit="5"
            :file-list="disposalFileList"
            :on-success="onDisposalUploadSuccess"
            :on-remove="onDisposalUploadRemove"
            :before-upload="beforeUpload"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div style="font-size:12px;color:#A0AEC0">处置凭证、回收证明等，单文件≤10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="disposalVisible = false">取消</el-button>
        <el-button type="primary" :loading="disposing" @click="confirmDisposal">
          确认处置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDisposal, disposalScrap } from '@/api/scrap'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const token = computed(() => auth.token)
const uploadUrl = '/api/file/upload'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const q = reactive({
  pageNum: 1,
  pageSize: 10,
  scrapNo: '',
  assetName: '',
})

// 处置弹窗
const disposalVisible = ref(false)
const disposing = ref(false)
const disposalFormRef = ref(null)
const currentDisposal = ref(null)
const disposalFileList = ref([])

const disposalForm = reactive({
  disposalMethod: '',
  disposalDate: '',
  disposalIncome: 0.00,
  disposalCost: 0.00,
  disposalHandler: '',
  disposalDesc: '',
  attachmentUrls: '',
})

// 处置表单校验（PRD 6.9.3）
const disposalRules = {
  disposalMethod: [{ required: true, message: '请选择处置方式', trigger: 'change' }],
  disposalDate: [{ required: true, message: '请选择处置日期', trigger: 'change' }],
  disposalIncome: [
    { required: true, message: '处置收入不能为负数', trigger: 'blur' },
    { type: 'number', min: 0, message: '处置收入不能为负数', trigger: 'blur' },
  ],
  disposalCost: [
    { required: true, message: '处置费用不能为负数', trigger: 'blur' },
    { type: 'number', min: 0, message: '处置费用不能为负数', trigger: 'blur' },
  ],
  disposalHandler: [
    { required: true, message: '请填写经办人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '经办人姓名为2-20个字符', trigger: 'blur' },
  ],
}

function disabledDate(time) {
  return time.getTime() > Date.now()
}

async function load() {
  loading.value = true
  try {
    const res = await listDisposal({ ...q })
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
  search()
}

// 超期标红（终审通过超过30天未处置，通过 updateTime 判断）
function tableRowClassName({ row }) {
  if (row.updateTime) {
    const approveDate = new Date(row.updateTime)
    const daysSince = (Date.now() - approveDate.getTime()) / (1000 * 60 * 60 * 24)
    if (daysSince > 30) return 'overdue-row'
  }
  return ''
}

function beforeUpload(file) {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

function onDisposalUploadSuccess(res, file) {
  if (res.code === 200) {
    const url = res.data
    disposalForm.attachmentUrls = disposalForm.attachmentUrls
      ? disposalForm.attachmentUrls + ',' + url
      : url
    disposalFileList.value.push({ name: file.name, url })
  }
}

function onDisposalUploadRemove(file) {
  disposalForm.attachmentUrls = disposalForm.attachmentUrls
    .split(',')
    .filter(u => u !== file.url)
    .join(',')
  disposalFileList.value = disposalFileList.value.filter(i => i.url !== file.url)
}

function openDisposal(row) {
  currentDisposal.value = row
  disposalForm.disposalMethod = row.disposalAdvice || '' // 默认复用处置建议
  disposalForm.disposalDate = ''
  disposalForm.disposalIncome = 0.00
  disposalForm.disposalCost = 0.00
  disposalForm.disposalHandler = ''
  disposalForm.disposalDesc = ''
  disposalForm.attachmentUrls = ''
  disposalFileList.value = []
  disposalVisible.value = true
}

function resetDisposalForm() {
  disposalFormRef.value?.resetFields()
  disposalFileList.value = []
}

async function confirmDisposal() {
  const valid = await disposalFormRef.value.validate().catch(() => false)
  if (!valid) return

  // 二次确认（PRD 6.9.3：确认处置弹窗）
  try {
    await ElMessageBox.confirm(
      `确认完成资产【${currentDisposal.value?.assetName}】的处置登记？登记后报废流程结束`,
      '处置确认',
      { confirmButtonText: '确认处置', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  disposing.value = true
  try {
    await disposalScrap({
      scrapId: currentDisposal.value.id,
      ...disposalForm,
    })
    ElMessage.success('报废处置登记完成，资产已归档')
    disposalVisible.value = false
    load()
  } catch (e) {
    // handled by interceptor
  } finally {
    disposing.value = false
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

<style scoped>
:deep(.overdue-row) {
  background-color: #FFF5F5 !important;
}
:deep(.overdue-row td) {
  color: #E53E3E;
}
</style>
