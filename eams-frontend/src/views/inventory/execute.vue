<template>
  <!--
    执行盘点页（PRD 6.4.2）
    路径: /inventory/execute/:taskId
    权限: 超级管理员/资产管理员
    功能: 盘点明细核对、逐项/批量确认、盘盈登记
  -->
  <div>
    <div class="page-header">
      <h2>执行盘点</h2>
      <div class="page-actions">
        <el-button @click="$router.back()">返回任务列表</el-button>
      </div>
    </div>

    <!-- 任务摘要卡片 -->
    <el-card v-if="task" class="mb-16" shadow="never">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="任务编号">{{ task.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ task.taskName }}</el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ task.inventoryDate }}</el-descriptions-item>
        <el-descriptions-item label="应盘总数">{{ task.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="已确认数量">{{ task.checkedCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :value="task.status" type="inventory-task" />
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:16px">
        <span style="color:#606266;margin-right:8px">完成进度：</span>
        <el-progress :percentage="task.completionRate ?? 0" :stroke-width="18"
          :status="task.completionRate >= 100 ? 'success' : ''"
          style="flex:1" />
      </div>
    </el-card>

    <!-- 操作栏 -->
    <div class="table-toolbar">
      <div>
        <el-button type="primary" @click="openSurplusDialog">登记盘盈资产</el-button>
        <el-button :disabled="selectedRows.length === 0" @click="batchConfirm('normal')">批量确认(正常)</el-button>
        <el-button :disabled="selectedRows.length === 0" type="danger" plain @click="batchConfirm('shortage')">批量确认(盘亏)</el-button>
      </div>
      <div>
        <span style="color:#909399;font-size:13px">
          已选 {{ selectedRows.length }} 项 &nbsp; | &nbsp;
          未确认 {{ uncheckedCount }} 项
        </span>
      </div>
    </div>

    <!-- 盘点明细表格 -->
    <div class="table-container">
      <el-table :data="details" v-loading="loading" stripe @selection-change="onSelectionChange" ref="tableRef">
        <el-table-column type="selection" width="50" :selectable="rowNotConfirmed" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="assetCode" label="资产编码" width="160" />
        <el-table-column prop="assetName" label="资产名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="location" label="存放地点" width="130" show-overflow-tooltip />
        <el-table-column prop="bookUserName" label="账面使用人" width="100">
          <template #default="{ row }">{{ row.bookUserName || '—' }}</template>
        </el-table-column>
        <el-table-column label="账面状态" width="90">
          <template #default="{ row }">
            <StatusTag :value="row.bookStatus" type="asset" />
          </template>
        </el-table-column>
        <el-table-column label="实盘结果" width="180">
          <template #default="{ row }">
            <template v-if="row.isConfirmed">
              <StatusTag :value="row.inventoryResult" type="inventory-result" />
            </template>
            <template v-else>
              <el-radio-group v-model="row._result" size="small" @change="(v) => onResultChange(row, v)">
                <el-radio :value="2">正常</el-radio>
                <el-radio :value="0">盘盈</el-radio>
                <el-radio :value="1">盘亏</el-radio>
              </el-radio-group>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="实盘备注" width="160">
          <template #default="{ row }">
            <template v-if="row.isConfirmed">{{ row.remark || '—' }}</template>
            <template v-else>
              <el-input v-model="row._remark" size="small" placeholder="盘亏时必填" maxlength="200" />
            </template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isConfirmed" type="success" size="small">已确认</el-tag>
            <el-tag v-else type="warning" size="small">未确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="确认时间" width="170">
          <template #default="{ row }">{{ row.confirmTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isConfirmed" type="primary" size="small" link @click="confirmSingle(row)">确认</el-button>
            <span v-else style="color:#67C23A">已确认</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 盘盈登记弹窗 -->
    <el-dialog v-model="surplusVisible" title="盘盈资产登记" width="650px" :close-on-click-modal="false">
      <el-form ref="surplusFormRef" :model="surplusForm" :rules="surplusRules" label-width="120px" style="max-width:550px">
        <el-form-item label="资产名称" prop="assetName">
          <el-input v-model="surplusForm.assetName" placeholder="请输入资产名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="资产分类" prop="category">
          <DictSelect dictCode="asset_category" v-model="surplusForm.category" style="width:100%" />
        </el-form-item>
        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="surplusForm.specification" placeholder="选填" maxlength="50" />
        </el-form-item>
        <el-form-item label="SN序列号" prop="snNumber">
          <el-input v-model="surplusForm.snNumber" placeholder="选填" maxlength="50" />
        </el-form-item>
        <el-form-item label="原值(元)" prop="originalValue">
          <el-input-number v-model="surplusForm.originalValue" :precision="2" :min="0.01" :max="99999999.99" style="width:100%" />
        </el-form-item>
        <el-form-item label="采购日期" prop="purchaseDate">
          <el-date-picker v-model="surplusForm.purchaseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="使用年限(年)" prop="usefulLife">
          <el-input-number v-model="surplusForm.usefulLife" :min="1" :max="50" style="width:100%" />
        </el-form-item>
        <el-form-item label="净残值率(%)" prop="residualRate">
          <el-input-number v-model="surplusForm.residualRate" :precision="2" :min="0" :max="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="存放地点" prop="location">
          <el-input v-model="surplusForm.location" placeholder="请输入存放地点" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <DeptTreeSelect v-model="surplusForm.deptId" style="width:100%" />
        </el-form-item>
        <el-form-item label="实盘备注" prop="remark">
          <el-input v-model="surplusForm.remark" type="textarea" :rows="2" placeholder="选填，最大200字符" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="surplusVisible = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="submitSurplus">确认登记入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskDetail, getTaskDetails, confirmDetails, registerSurplusAsset } from '@/api/inventory'
import StatusTag from '@/components/common/StatusTag.vue'
import DictSelect from '@/components/common/DictSelect.vue'
import DeptTreeSelect from '@/components/common/DeptTreeSelect.vue'

const route = useRoute()
const taskId = computed(() => Number(route.params.taskId))

const loading = ref(false)
const task = ref(null)
const details = ref([])
const tableRef = ref(null)
const selectedRows = ref([])

const uncheckedCount = computed(() => details.value.filter(d => !d.isConfirmed).length)

function rowNotConfirmed(row) { return !row.isConfirmed }

function onSelectionChange(selection) { selectedRows.value = selection }

// ---------- 加载数据 ----------
async function load() {
  loading.value = true
  try {
    const [taskRes, detailsRes] = await Promise.all([
      getTaskDetail(taskId.value),
      getTaskDetails(taskId.value),
    ])
    task.value = taskRes.data
    details.value = (detailsRes.data || []).map(d => ({
      ...d,
      _result: d.inventoryResult ?? 2,
      _remark: d.remark || '',
    }))
  } catch { /* handled */ }
  finally { loading.value = false }
}

// ---------- 单条确认 ----------
async function confirmSingle(row) {
  if (row._result === 1 && !row._remark) {
    ElMessage.warning('请填写盘亏原因')
    return
  }
  try {
    await confirmDetails({
      taskId: taskId.value,
      detailIds: [row.id],
      inventoryResult: row._result,
      remark: row._remark,
    })
    ElMessage.success('确认成功')
    load()
  } catch { /* handled */ }
}

// ---------- 批量确认 ----------
async function batchConfirm(type) {
  const rows = selectedRows.value.filter(r => !r.isConfirmed)
  if (rows.length === 0) { ElMessage.warning('请选择未确认的资产'); return }

  const result = type === 'normal' ? 2 : 1

  if (result === 1) {
    // 盘亏时检查备注
    const emptyRemarks = rows.filter(r => !r._remark)
    if (emptyRemarks.length > 0) {
      ElMessage.warning(`还有${emptyRemarks.length}项盘亏资产未填写原因`)
      return
    }
  }

  const confirmText = result === 2 ? '确认批量标记为"正常"？' : '确认批量标记为"盘亏"？'
  try {
    await ElMessageBox.confirm(confirmText, '确认', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning',
    })
  } catch { return }

  loading.value = true
  try {
    await confirmDetails({
      taskId: taskId.value,
      detailIds: rows.map(r => r.id),
      inventoryResult: result,
      remark: rows.map(r => r._remark).filter(Boolean).join('; '),
    })
    ElMessage.success(`已批量确认${rows.length}项`)
    load()
  } catch { /* handled */ }
  finally { loading.value = false }
}

function onResultChange(row, val) {
  row._result = val
}

// ---------- 盘盈登记 ----------
const surplusVisible = ref(false)
const registering = ref(false)
const surplusFormRef = ref(null)

const surplusForm = reactive({
  assetName: '', category: '', specification: '', snNumber: '',
  originalValue: null, purchaseDate: '', usefulLife: 3, residualRate: 5.00,
  location: '', deptId: null, remark: '',
})

const surplusRules = {
  assetName: [{ required: true, message: '资产名称为2-50个字符', trigger: 'blur', min: 2, max: 50 }],
  category: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  originalValue: [{ required: true, message: '原值须大于0', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
  usefulLife: [{ required: true, message: '使用年限为1-50的整数', trigger: 'blur' }],
  location: [{ required: true, message: '存放地点为2-50个字符', trigger: 'blur', min: 2, max: 50 }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
}

function openSurplusDialog() {
  surplusForm.assetName = ''; surplusForm.category = ''; surplusForm.specification = ''
  surplusForm.snNumber = ''; surplusForm.originalValue = null
  surplusForm.purchaseDate = ''; surplusForm.usefulLife = 3; surplusForm.residualRate = 5.00
  surplusForm.location = ''; surplusForm.deptId = null; surplusForm.remark = ''
  surplusVisible.value = true
}

async function submitSurplus() {
  const valid = await surplusFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('确认登记盘盈资产并入库存？系统将自动生成资产编码', '确认登记', {
      confirmButtonText: '确认登记', cancelButtonText: '取消', type: 'warning',
    })
  } catch { return }

  registering.value = true
  try {
    const res = await registerSurplusAsset({ ...surplusForm, taskId: taskId.value })
    ElMessage.success(res.message)
    surplusVisible.value = false
    load()
  } catch { /* handled */ }
  finally { registering.value = false }
}

onMounted(() => { load() })
</script>
