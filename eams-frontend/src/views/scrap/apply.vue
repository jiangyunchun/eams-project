<template>
  <div>
    <div class="page-header">
      <h2>报废申请</h2>
      <div class="page-actions">
        <el-button @click="$router.push('/scrap/record')">报废记录</el-button>
      </div>
    </div>

    <el-steps :active="step" align-center style="margin-bottom:24px">
      <el-step title="选择资产" />
      <el-step title="填写报废信息" />
      <el-step title="提交完成" />
    </el-steps>

    <!-- 步骤1：选择报废资产（PRD 6.9.1 步骤1） -->
    <el-card v-if="step === 0">
      <div class="search-card">
        <el-form :model="q" inline>
          <el-form-item label="资产名称">
            <el-input v-model="q.assetName" clearable style="width:160px" placeholder="请输入资产名称" />
          </el-form-item>
          <el-form-item label="资产编码">
            <el-input v-model="q.assetCode" clearable style="width:160px" placeholder="请输入资产编码" />
          </el-form-item>
          <el-form-item label="资产分类">
            <DictSelect dictCode="asset_category" v-model="q.category" style="width:160px" />
          </el-form-item>
          <el-form-item label="资产状态">
            <el-select v-model="q.status" clearable style="width:140px">
              <el-option label="闲置" :value="0" />
              <el-option label="在用" :value="1" />
              <el-option label="维修" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item class="search-buttons">
            <el-button type="primary" @click="loadAssets">查询</el-button>
            <el-button @click="resetQ">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-empty v-if="!filteredAssets.length" description="当前没有可报废的资产" />

      <el-row v-else :gutter="16">
        <el-col
          v-for="a in filteredAssets"
          :key="a.id"
          :span="8"
          style="margin-bottom:16px"
        >
          <el-card
            shadow="hover"
            :class="['asset-card', { 'is-selected': sel?.id === a.id, 'is-disabled': a.status === 4 || a.status === 5 }]"
            @click="selectAsset(a)"
          >
            <div class="asset-card-body">
              <el-image
                v-if="a.imageUrl"
                :src="a.imageUrl"
                style="width:60px;height:60px;border-radius:6px"
                fit="cover"
              />
              <div v-else class="ph"><el-icon :size="28"><Box /></el-icon></div>
              <div class="info">
                <div class="n">
                  {{ a.assetName }}
                  <el-tag v-if="a.isDepreciated" size="small" type="warning">已提满折旧</el-tag>
                </div>
                <div class="c">{{ a.assetCode }}</div>
                <div class="m">
                  <StatusTag :value="a.status" type="asset" />
                  <el-tag v-if="a.hasUnfixableRepair" size="small" type="danger" effect="dark">无法维修</el-tag>
                  原值: ¥{{ formatNumber(a.originalValue) }} · {{ a.deptName || '-' }}
                </div>
              </div>
              <el-icon v-if="sel?.id === a.id" class="chk" color="#2B6CB0" :size="24">
                <CircleCheckFilled />
              </el-icon>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div style="text-align:center;margin-top:24px">
        <el-button type="primary" :disabled="!sel" @click="step = 1">下一步</el-button>
      </div>
    </el-card>

    <!-- 步骤2：填写报废信息（PRD 6.9.1 步骤2） -->
    <el-card v-if="step === 1">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width:700px">
        <el-form-item label="报废资产">
          <div style="display:flex;align-items:center;gap:8px;">
            <span>{{ sel?.assetCode }} {{ sel?.assetName }}</span>
            <StatusTag :value="sel?.status" type="asset" />
            <el-tag v-if="sel?.hasUnfixableRepair" size="small" type="danger" effect="dark">无法维修</el-tag>
            <span style="color:#718096;font-size:13px">
              原值: ¥{{ formatNumber(sel?.originalValue) }}
              <template v-if="sel?.netValue !== undefined"> · 净值: ¥{{ formatNumber(sel?.netValue) }}</template>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="报废原因" prop="scrapReason">
          <el-select v-model="form.scrapReason" style="width:100%" placeholder="请选择报废原因">
            <el-option label="老化损坏" value="老化损坏" />
            <el-option label="技术淘汰" value="技术淘汰" />
            <el-option label="维修成本过高" value="维修成本过高" />
            <el-option label="盘亏确认" value="盘亏确认" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="原因说明" prop="reasonDesc">
          <el-input
            v-model="form.reasonDesc"
            type="textarea"
            :rows="3"
            placeholder="请详细描述报废原因（10-500个字符）"
            show-word-limit
            maxlength="500"
          />
        </el-form-item>

        <el-form-item label="处置建议" prop="disposalAdvice">
          <el-select v-model="form.disposalAdvice" style="width:100%" placeholder="请选择处置建议">
            <el-option label="变卖" value="变卖" />
            <el-option label="回收" value="回收" />
            <el-option label="销毁" value="销毁" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
            :action="uploadUrl"
            :headers="{ Authorization: 'Bearer ' + token }"
            :limit="5"
            :file-list="fileList"
            :on-success="onUploadSuccess"
            :on-remove="onUploadRemove"
            :before-upload="beforeUpload"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div style="font-size:12px;color:#A0AEC0">支持 jpg/png/pdf/doc/xls，单文件≤10MB，最多5个</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="选填，最大200字符"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <div style="text-align:center;margin-top:24px">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确认提交</el-button>
      </div>
    </el-card>

    <!-- 步骤3：提交完成 -->
    <el-card v-if="step === 2">
      <el-result icon="success" title="报废申请已提交" sub-title="请等待审批">
        <template #extra>
          <el-button type="primary" @click="$router.push('/scrap/record')">查看报废记录</el-button>
          <el-button @click="resetAll">继续申请</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Box } from '@element-plus/icons-vue'
import { listAssets } from '@/api/asset'
import { applyScrap } from '@/api/scrap'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const token = computed(() => auth.token)
const uploadUrl = '/api/file/upload'

const step = ref(0)
const assets = ref([])
const sel = ref(null)
const formRef = ref(null)
const submitting = ref(false)
const fileList = ref([])

// 查询条件
const q = reactive({ assetName: '', assetCode: '', category: '', status: 3 })

// 表单
const form = reactive({
  scrapReason: '',
  reasonDesc: '',
  disposalAdvice: '',
  attachmentUrls: '',
  remark: '',
})

// 校验规则（PRD 6.9.1）
const rules = {
  scrapReason: [{ required: true, message: '请选择报废原因', trigger: 'change' }],
  reasonDesc: [
    { required: true, message: '原因说明为10-500个字符', trigger: 'blur' },
    { min: 10, max: 500, message: '原因说明为10-500个字符', trigger: 'blur' },
  ],
  disposalAdvice: [{ required: true, message: '请选择处置建议', trigger: 'change' }],
}

// 已提满折旧判断
function isDepreciated(asset) {
  if (!asset.purchaseDate || !asset.usefulLife) return false
  const purchaseDate = new Date(asset.purchaseDate)
  const scrapDate = new Date(purchaseDate)
  scrapDate.setFullYear(scrapDate.getFullYear() + asset.usefulLife)
  return new Date() >= scrapDate
}

// 筛选可报废的资产（PRD 6.9.1: 非报废、非盘点中，闲置/在用/维修可申请）
const filteredAssets = computed(() => {
  return assets.value
    .filter(a => a.status !== 4 && a.status !== 5) // 排除已报废和盘点中
    .map(a => ({
      ...a,
      isDepreciated: isDepreciated(a),
    }))
    .sort((a, b) => {
      // 有"无法维修"维保报修记录的资产靠前显示
      if (a.hasUnfixableRepair && !b.hasUnfixableRepair) return -1
      if (!a.hasUnfixableRepair && b.hasUnfixableRepair) return 1
      return 0
    })
})

function selectAsset(a) {
  if (a.status === 4 || a.status === 5) {
    if (a.status === 4) ElMessage.warning('该资产已报废，不可报废')
    if (a.status === 5) ElMessage.warning('该资产正在盘点中，不可报废')
    return
  }
  sel.value = sel.value?.id === a.id ? null : a
}

async function loadAssets() {
  try {
    // 过滤空值，避免后端 Integer 字段绑定空字符串报错
    const params = Object.fromEntries(
      Object.entries({ ...q, pageNum: 1, pageSize: 200 }).filter(([_, v]) => v !== '' && v !== null && v !== undefined)
    )
    const res = await listAssets(params)
    assets.value = res.data?.list || []
  } catch (e) {
    // handled by interceptor
  }
}

function resetQ() {
  q.assetName = ''
  q.assetCode = ''
  q.category = ''
  q.status = 3
  loadAssets()
}

function beforeUpload(file) {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

function onUploadSuccess(res, file) {
  if (res.code === 200) {
    const url = res.data
    form.attachmentUrls = form.attachmentUrls
      ? form.attachmentUrls + ',' + url
      : url
    fileList.value.push({ name: file.name, url })
  }
}

function onUploadRemove(file) {
  form.attachmentUrls = form.attachmentUrls
    .split(',')
    .filter(u => u !== file.url)
    .join(',')
  fileList.value = fileList.value.filter(i => i.url !== file.url)
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 二次确认（PRD 6.9.1: 提交确认弹窗）
  try {
    await ElMessageBox.confirm(
      '确认提交报废申请？审批通过后资产将标记为报废状态',
      '提交确认',
      { confirmButtonText: '确认提交', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await applyScrap({
      assetId: sel.value.id,
      ...form,
    })
    ElMessage.success('报废申请已提交，请等待审批')
    step.value = 2
  } catch (e) {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

function resetAll() {
  step.value = 0
  sel.value = null
  fileList.value = []
  Object.assign(form, {
    scrapReason: '',
    reasonDesc: '',
    disposalAdvice: '',
    attachmentUrls: '',
    remark: '',
  })
  loadAssets()
}

function formatNumber(val) {
  if (val == null) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

import { ElMessageBox } from 'element-plus'

onMounted(() => {
  loadAssets()
})
</script>

<style scoped>
.asset-card {
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}
.asset-card.is-selected {
  border-color: #2B6CB0;
  background: #EBF4FF;
}
.asset-card.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.asset-card-body {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}
.ph {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  background: #F7FAFC;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #CBD5E0;
}
.info {
  flex: 1;
  min-width: 0;
}
.n {
  font-size: 15px;
  font-weight: 600;
  color: #1A202C;
  display: flex;
  align-items: center;
  gap: 6px;
}
.c {
  font-size: 12px;
  color: #718096;
  margin-top: 2px;
}
.m {
  font-size: 12px;
  color: #A0AEC0;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.chk {
  position: absolute;
  top: 0;
  right: 0;
}
</style>
