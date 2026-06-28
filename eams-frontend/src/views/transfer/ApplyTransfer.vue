<template>
  <div>
    <div class="page-header">
      <h2>调拨申请</h2>
      <div class="page-actions">
        <el-button @click="$router.push('/transfer/record')">调拨记录</el-button>
      </div>
    </div>

    <el-steps :active="activeStep" align-center style="margin-bottom:24px">
      <el-step title="选择资产" />
      <el-step title="填写调拨信息" />
      <el-step title="提交完成" />
    </el-steps>

    <!-- 步骤1: 选择本部门闲置/在用资产 -->
    <el-card v-if="activeStep === 0">
      <div class="search-card">
        <el-form :model="assetQuery" inline>
          <el-form-item label="资产名称"><el-input v-model="assetQuery.assetName" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
          <el-form-item label="资产编码"><el-input v-model="assetQuery.assetCode" placeholder="精确匹配" clearable style="width:160px" /></el-form-item>
          <el-form-item label="资产分类">
            <DictSelect dictCode="asset_category" v-model="assetQuery.category" style="width:160px" />
          </el-form-item>
          <el-form-item class="search-buttons">
            <el-button type="primary" @click="loadAssets">查询</el-button>
            <el-button @click="resetAssetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-empty v-if="assets.length === 0" description="当前没有可调拨的资产" />

      <el-row v-else :gutter="16">
        <el-col v-for="a in assets" :key="a.id" :span="8" style="margin-bottom:16px">
          <el-card shadow="hover" :class="['asset-card', { 'is-selected': selectedAsset?.id === a.id }]" @click="selectAsset(a)">
            <div class="asset-card-body">
              <el-image v-if="a.imageUrl" :src="a.imageUrl" style="width:60px;height:60px;border-radius:6px" fit="cover" />
              <div v-else class="asset-placeholder-img"><el-icon :size="28"><Box /></el-icon></div>
              <div class="asset-card-info">
                <div class="asset-name">{{ a.assetName }}</div>
                <div class="asset-code">{{ a.assetCode }}</div>
                <div class="asset-meta">
                  <StatusTag :value="a.status" type="asset" />
                  {{ a.specification || '-' }} · {{ a.deptName || '-' }}
                </div>
              </div>
              <el-icon v-if="selectedAsset?.id === a.id" class="check-icon" color="#2B6CB0" :size="24"><CircleCheckFilled /></el-icon>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div style="text-align:center;margin-top:24px">
        <el-button type="primary" :disabled="!selectedAsset" @click="activeStep = 1">下一步</el-button>
      </div>
    </el-card>

    <!-- 步骤2: 填写调拨信息 -->
    <el-card v-if="activeStep === 1">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width:700px">
        <el-form-item label="调出资产">
          <div style="display:flex;align-items:center;gap:8px">
            <span>{{ selectedAsset?.assetCode }} {{ selectedAsset?.assetName }}</span>
            <StatusTag :value="selectedAsset?.status" type="asset" />
          </div>
        </el-form-item>
        <el-form-item label="调入部门" prop="toDeptId">
          <DeptTreeSelect v-model="form.toDeptId" style="width:100%" />
        </el-form-item>
        <el-form-item label="调入使用人" prop="toUserId">
          <el-select v-model="form.toUserId" placeholder="请选择（可选）" clearable filterable style="width:100%">
            <el-option v-for="u in deptUsers" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调入地点" prop="toLocation">
          <el-input v-model="form.toLocation" placeholder="2-50个字符" />
        </el-form-item>
        <el-form-item label="调拨原因" prop="transferReason">
          <el-input v-model="form.transferReason" type="textarea" :rows="3" placeholder="10-500个字符" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <div style="text-align:center;margin-top:24px">
        <el-button @click="activeStep=0">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认提交</el-button>
      </div>
    </el-card>

    <!-- 步骤3: 完成 -->
    <el-card v-if="activeStep === 2">
      <el-result icon="success" title="调拨申请已提交" sub-title="请等待调入部门确认及资产管理员审批">
        <template #extra>
          <el-button type="primary" @click="$router.push('/transfer/record')">查看调拨记录</el-button>
          <el-button @click="resetForm">继续调拨</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { listAssets } from '@/api/asset'
import { applyTransfer } from '@/api/transfer'
import { listUsers } from '@/api/system'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const activeStep = ref(0)
const assets = ref([])
const selectedAsset = ref(null)
const assetQuery = reactive({ assetName: '', assetCode: '', category: '' })

async function loadAssets() {
  try {
    const res = await listAssets({ ...assetQuery, deptId: auth.userInfo?.deptId, pageNum: 1, pageSize: 200 })
    assets.value = (res.data.list || []).filter(a => a.status === 0 || a.status === 1)
  } catch (e) { /* ignore */ }
}
function resetAssetQuery() { assetQuery.assetName = ''; assetQuery.assetCode = ''; assetQuery.category = ''; loadAssets() }
function selectAsset(a) { selectedAsset.value = selectedAsset.value?.id === a.id ? null : a }

// 调入部门用户
const deptUsers = ref([])
async function loadDeptUsers(deptId) {
  if (!deptId) { deptUsers.value = []; return }
  try {
    const res = await listUsers({ deptId, pageNum: 1, pageSize: 200 })
    deptUsers.value = res.data?.list || []
  } catch (e) { deptUsers.value = [] }
}

const formRef = ref(null)
const submitting = ref(false)
const form = reactive({ toDeptId: null, toUserId: null, toLocation: '', transferReason: '', remark: '' })

watch(() => form.toDeptId, (val) => {
  form.toUserId = null
  loadDeptUsers(val)
})
const rules = {
  toDeptId: [{ required: true, message: '请选择调入部门', trigger: 'change' }],
  toLocation: [{ required: true, message: '调入地点为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '调入地点为2-50个字符', trigger: 'blur' }],
  transferReason: [{ required: true, message: '调拨原因为10-500个字符', trigger: 'blur' }, { min: 10, max: 500, message: '调拨原因为10-500个字符', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ElMessageBox.confirm('确认提交调拨申请？提交后需调入部门确认及资产管理员审批', '确认提交', { type: 'warning', confirmButtonText: '确认提交' })
  } catch (e) { return }
  submitting.value = true
  try {
    await applyTransfer({ assetId: selectedAsset.value.id, ...form })
    ElMessage.success('调拨申请已提交，请等待调入部门确认')
    activeStep.value = 2
  } catch (e) { /* handled */ }
  finally { submitting.value = false }
}

function resetForm() {
  activeStep.value = 0; selectedAsset.value = null
  Object.assign(form, { toDeptId: null, toUserId: null, toLocation: '', transferReason: '', remark: '' })
  loadAssets()
}

onMounted(() => { loadAssets() })
</script>

<style scoped>
.asset-card { cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.asset-card.is-selected { border-color: #2B6CB0; background: #EBF4FF; }
.asset-card-body { display: flex; align-items: center; gap: 12px; position: relative; }
.asset-placeholder-img { width: 60px; height: 60px; border-radius: 6px; background: #F7FAFC; display: flex; align-items: center; justify-content: center; color: #CBD5E0; }
.asset-card-info { flex: 1; min-width: 0; }
.asset-name { font-size: 15px; font-weight: 600; color: #1A202C; }
.asset-code { font-size: 12px; color: #718096; margin-top: 2px; }
.asset-meta { font-size: 12px; color: #A0AEC0; margin-top: 2px; }
.check-icon { position: absolute; top: 0; right: 0; }
</style>
