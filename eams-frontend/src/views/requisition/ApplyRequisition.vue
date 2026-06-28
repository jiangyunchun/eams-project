<template>
  <div>
    <div class="page-header">
      <h2>领用申请</h2>
      <div class="page-actions">
        <el-button @click="$router.push('/requisition/record')">我的申请</el-button>
      </div>
    </div>

    <!-- 步骤条 -->
    <el-steps :active="activeStep" align-center style="margin-bottom:24px">
      <el-step title="选择资产" />
      <el-step title="填写信息" />
      <el-step title="提交完成" />
    </el-steps>

    <!-- ===== 步骤1: 选择闲置资产 ===== -->
    <el-card v-if="activeStep === 0">
      <div class="search-card">
        <el-form :model="assetQuery" inline>
          <el-form-item label="资产名称"><el-input v-model="assetQuery.assetName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
          <el-form-item label="资产编码"><el-input v-model="assetQuery.assetCode" placeholder="精确匹配" clearable style="width:160px" /></el-form-item>
          <el-form-item label="资产分类">
            <DictSelect dictCode="asset_category" v-model="assetQuery.category" style="width:160px" />
          </el-form-item>
          <el-form-item label="资产范围">
            <el-radio-group v-model="assetQuery.onlyMyDept" @change="loadIdleAssets">
              <el-radio-button :value="true">本部门</el-radio-button>
              <el-radio-button :value="false">全部</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item class="search-buttons">
            <el-button type="primary" @click="loadIdleAssets">查询</el-button>
            <el-button @click="resetAssetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="idleAssets.length === 0" style="text-align:center;padding:60px 0">
        <el-empty description="当前没有可领用的闲置资产" />
      </div>

      <el-row v-else :gutter="16">
        <el-col v-for="asset in idleAssets" :key="asset.id" :span="8" style="margin-bottom:16px">
          <el-card
            shadow="hover"
            :class="['asset-card', { 'is-selected': selectedAsset?.id === asset.id }]"
            @click="selectAsset(asset)"
          >
            <div class="asset-card-body">
              <el-image v-if="asset.imageUrl" :src="asset.imageUrl" style="width:60px;height:60px;border-radius:6px" fit="cover" />
              <div v-else class="asset-placeholder-img">
                <el-icon :size="28"><Box /></el-icon>
              </div>
              <div class="asset-card-info">
                <div class="asset-name">{{ asset.assetName }}</div>
                <div class="asset-code">{{ asset.assetCode }}</div>
                <div class="asset-meta">
                  <StatusTag :value="asset.status" type="asset" />
                  {{ asset.specification || '-' }} · {{ asset.deptName || '-' }}
                </div>
              </div>
              <el-icon v-if="selectedAsset?.id === asset.id" class="check-icon" color="#2B6CB0" :size="24"><CircleCheckFilled /></el-icon>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div style="text-align:center;margin-top:24px">
        <el-button type="primary" :disabled="!selectedAsset" @click="activeStep = 1">
          下一步：填写领用信息
        </el-button>
      </div>
    </el-card>

    <!-- ===== 步骤2: 填写领用信息 ===== -->
    <el-card v-if="activeStep === 1">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width:700px">
        <el-form-item label="领用资产">
          <div style="display:flex;align-items:center;gap:8px">
            <span>{{ selectedAsset?.assetCode }} {{ selectedAsset?.assetName }}</span>
            <StatusTag :value="selectedAsset?.status" type="asset" />
          </div>
        </el-form-item>
        <el-form-item label="领用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="请描述领用用途（10-500个字符）" />
        </el-form-item>
        <el-form-item label="预计领用时长" prop="expectDuration">
          <el-select v-model="form.expectDuration" placeholder="请选择" style="width:100%">
            <el-option label="1个月" value="1个月" />
            <el-option label="3个月" value="3个月" />
            <el-option label="6个月" value="6个月" />
            <el-option label="12个月" value="12个月" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计归还日期" prop="expectReturnDate" v-if="form.expectDuration === '自定义'">
          <el-date-picker v-model="form.expectReturnDate" type="date" value-format="YYYY-MM-DD" placeholder="选择归还日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填，最多200个字符" />
        </el-form-item>
      </el-form>

      <div style="text-align:center;margin-top:24px">
        <el-button @click="activeStep = 0">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认提交领用申请</el-button>
      </div>
    </el-card>

    <!-- ===== 步骤3: 提交完成 ===== -->
    <el-card v-if="activeStep === 2">
      <el-result icon="success" title="领用申请已提交" sub-title="请等待审批">
        <template #extra>
          <el-button type="primary" @click="$router.push('/requisition/record')">查看我的申请</el-button>
          <el-button @click="resetForm">继续领用</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { listAssets } from '@/api/asset'
import { applyRequisition } from '@/api/requisition'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

// ---- 步骤 ----
const activeStep = ref(0)

// ---- 闲置资产加载 ----
const idleAssets = ref([])
const selectedAsset = ref(null)
const assetQuery = reactive({ assetName: '', assetCode: '', category: '', onlyMyDept: true })

async function loadIdleAssets() {
  try {
    const params = { ...assetQuery, status: 0, pageNum: 1, pageSize: 100 }
    if (params.onlyMyDept) {
      params.deptId = auth.userInfo?.deptId
    }
    delete params.onlyMyDept
    const res = await listAssets(params)
    idleAssets.value = (res.data.list || []).filter(a => a.status === 0)
  } catch (e) { /* ignore */ }
}

function resetAssetQuery() {
  assetQuery.assetName = ''
  assetQuery.assetCode = ''
  assetQuery.category = ''
  assetQuery.onlyMyDept = true
  loadIdleAssets()
}

function selectAsset(asset) {
  selectedAsset.value = selectedAsset.value?.id === asset.id ? null : asset
}

// ---- 领用表单 ----
const formRef = ref(null)
const submitting = ref(false)
const form = reactive({ purpose: '', expectDuration: '1个月', expectReturnDate: '', remark: '' })

const rules = {
  purpose: [
    { required: true, message: '领用用途为10-500个字符', trigger: 'blur' },
    { min: 10, max: 500, message: '领用用途为10-500个字符', trigger: 'blur' },
  ],
  expectDuration: [{ required: true, message: '请选择预计领用时长', trigger: 'change' }],
  expectReturnDate: form.expectDuration === '自定义' ? [{ required: true, message: '请选择预计归还日期', trigger: 'change' }] : [],
  remark: [{ max: 200, message: '备注不能超过200个字符', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('确认提交领用申请？提交后将进入审批流程', '确认提交', {
      type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '取消',
    })
  } catch (e) { return }

  submitting.value = true
  try {
    await applyRequisition({
      assetId: selectedAsset.value.id,
      purpose: form.purpose,
      expectDuration: form.expectDuration,
      expectReturnDate: form.expectDuration === '自定义' ? form.expectReturnDate : computeReturnDate(form.expectDuration),
      remark: form.remark,
    })
    ElMessage.success('领用申请已提交，请等待审批')
    activeStep.value = 2
  } catch (e) { /* request.js handles errors */ }
  finally { submitting.value = false }
}

function computeReturnDate(duration) {
  const now = new Date()
  switch (duration) {
    case '1个月': now.setMonth(now.getMonth() + 1); break
    case '3个月': now.setMonth(now.getMonth() + 3); break
    case '6个月': now.setMonth(now.getMonth() + 6); break
    case '12个月': now.setMonth(now.getMonth() + 12); break
    default: return ''
  }
  return now.toISOString().slice(0, 10)
}

function resetForm() {
  activeStep.value = 0
  selectedAsset.value = null
  Object.assign(form, { purpose: '', expectDuration: '1个月', expectReturnDate: '', remark: '' })
  loadIdleAssets()
}

onMounted(() => { loadIdleAssets() })
</script>

<style scoped>
.asset-card { cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.asset-card.is-selected { border-color: #2B6CB0; background: #EBF4FF; }
.asset-card-body { display: flex; align-items: center; gap: 12px; position: relative; }
.asset-placeholder-img { width: 60px; height: 60px; border-radius: 6px; background: #F7FAFC; display: flex; align-items: center; justify-content: center; color: #CBD5E0; }
.asset-card-info { flex: 1; min-width: 0; }
.asset-name { font-size: 15px; font-weight: 600; color: #1A202C; }
.asset-code { font-size: 12px; color: #718096; margin-top: 2px; }
.asset-meta { font-size: 12px; color: #A0AEC0; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.check-icon { position: absolute; top: 0; right: 0; }
</style>
