<template>
  <div>
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>采购记录</h2>
      <div class="page-actions">
        <el-button v-if="hasPerm('procurement:add')" type="primary" @click="$router.push('/procurement/add')">采购登记</el-button>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="采购单号">
          <el-input v-model="query.procurementNo" placeholder="模糊搜索" clearable style="width:150px" />
        </el-form-item>
        <el-form-item label="资产名称">
          <el-input v-model="query.assetName" placeholder="模糊搜索" clearable style="width:150px" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="query.supplierId" placeholder="全部" clearable style="width:160px" filterable>
            <el-option v-for="s in supplierList" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="验收状态">
          <el-select v-model="query.acceptStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="待验收" :value="0" />
            <el-option label="已验收" :value="1" />
            <el-option label="已入库" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购日期">
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
        <el-table-column prop="procurementNo" label="采购单号" width="150">
          <template #default="{ row }">
            <span v-if="row.procurementNo">{{ row.procurementNo }}</span>
            <span v-else style="color:#CBD5E0">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="assetName" label="资产名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="categoryLabel" label="分类" width="80">
          <template #default="{ row }">{{ row.categoryLabel || row.category }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="60" align="center" />
        <el-table-column label="单价(元)" width="100" align="right">
          <template #default="{ row }">{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column label="总价(元)" width="110" align="right">
          <template #default="{ row }">{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" min-width="120" />
        <el-table-column prop="purchaseDate" label="采购日期" width="110" />
        <el-table-column label="验收状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="acceptStatusType(row.acceptStatus)" size="small">
              {{ row.acceptStatusLabel || acceptStatusLabel(row.acceptStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="hasPerm('procurement:add')" link type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-if="hasPerm('procurement:add') && row.acceptStatus < 2" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="hasPerm('procurement:add') && row.acceptStatus < 1" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="采购记录详情" width="640px" :close-on-click-modal="false">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="采购单号">{{ detail.procurementNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
        <el-descriptions-item label="资产分类">{{ detail.categoryLabel }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.specification || '-' }}</el-descriptions-item>
        <el-descriptions-item label="SN序列号">{{ detail.snNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采购数量">{{ detail.quantity }}</el-descriptions-item>
        <el-descriptions-item label="单价(元)">{{ detail.unitPrice }}</el-descriptions-item>
        <el-descriptions-item label="总价(元)">{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="采购日期">{{ detail.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="使用年限">{{ detail.usefulLife }}年</el-descriptions-item>
        <el-descriptions-item label="净残值率">{{ detail.residualRate }}%</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="存放地点">{{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="验收状态">
          <el-tag :type="acceptStatusType(detail.acceptStatus)" size="small">{{ detail.acceptStatusLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="验收日期">{{ detail.acceptDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记人">{{ detail.createBy }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="formVisible" title="编辑采购记录" width="700px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="editRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="采购单号" prop="procurementNo">
              <el-input v-model="form.procurementNo" placeholder="外部采购单号（可选）" clearable />
            </el-form-item>
            <el-form-item label="资产名称" prop="assetName">
              <el-input v-model="form.assetName" placeholder="2-50个字符" />
            </el-form-item>
            <el-form-item label="资产分类" prop="category">
              <DictSelect dictCode="asset_category" v-model="form.category" style="width:100%" />
            </el-form-item>
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" clearable />
            </el-form-item>
            <el-form-item label="SN序列号" prop="snNumber">
              <el-input v-model="form.snNumber" clearable />
            </el-form-item>
            <el-form-item label="采购数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :max="100" style="width:100%" />
            </el-form-item>
            <el-form-item label="单价（元）" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0.01" :precision="2" style="width:100%" />
            </el-form-item>
            <el-form-item label="总价（元）">
              <el-input :model-value="editTotal" readonly style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" style="width:100%" filterable>
                <el-option v-for="s in supplierList" :key="s.id" :label="s.supplierName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="使用年限" prop="usefulLife">
              <el-input-number v-model="form.usefulLife" :min="1" :max="50" style="width:100%" />
            </el-form-item>
            <el-form-item label="净残值率（%）" prop="residualRate">
              <el-input-number v-model="form.residualRate" :min="0" :max="100" :precision="2" style="width:100%" />
            </el-form-item>
            <el-form-item label="所属部门" prop="deptId">
              <DeptTreeSelect v-model="form.deptId" style="width:100%" />
            </el-form-item>
            <el-form-item label="存放地点" prop="location">
              <el-input v-model="form.location" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider />
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="验收状态" prop="acceptStatus">
              <el-radio-group v-model="form.acceptStatus" :disabled="form._originAcceptStatus >= 1">
                <el-radio :value="0">待验收</el-radio>
                <el-radio :value="1">已验收</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.acceptStatus === 1" label="验收日期" prop="acceptDate">
              <el-date-picker v-model="form.acceptDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleEditSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProcurement, getProcurementDetail, editProcurement, deleteProcurement, getAllEnabledSupplier } from '@/api/procurement'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
function hasPerm(perm) { return auth.permissions?.includes(perm) }

// ---- 供应商下拉 ----
const supplierList = ref([])
async function loadSuppliers() {
  try {
    const res = await getAllEnabledSupplier()
    supplierList.value = res.data || []
  } catch (e) { /* ignore */ }
}

// ---- 表格数据 ----
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref([])
const query = reactive({
  procurementNo: '', assetName: '', supplierId: null, acceptStatus: null,
  beginDate: '', endDate: '', pageNum: 1, pageSize: 10,
})

async function fetchData() {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      query.beginDate = dateRange.value[0]
      query.endDate = dateRange.value[1]
    } else {
      query.beginDate = ''
      query.endDate = ''
    }
    const res = await listProcurement(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败，请重试')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.procurementNo = ''
  query.assetName = ''
  query.supplierId = null
  query.acceptStatus = null
  dateRange.value = []
  query.pageNum = 1
  fetchData()
}

// ---- 验收状态标签 ----
function acceptStatusType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

function acceptStatusLabel(status) {
  const map = { 0: '待验收', 1: '已验收', 2: '已入库', 3: '已取消' }
  return map[status] || '未知'
}

// ---- 详情弹窗 ----
const detailVisible = ref(false)
const detail = ref(null)

async function openDetail(row) {
  try {
    const res = await getProcurementDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) { /* error handled by request.js */ }
}

// ---- 编辑弹窗 ----
const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, procurementNo: '', assetName: '', category: '', specification: '', snNumber: '',
  quantity: 1, unitPrice: null, purchaseDate: '', supplierId: null, usefulLife: 5,
  residualRate: 5.00, deptId: null, location: '', acceptStatus: 0, acceptDate: '', remark: '',
  _originAcceptStatus: 0,
})

const editTotal = computed(() => {
  if (form.quantity && form.unitPrice) {
    return (form.quantity * form.unitPrice).toFixed(2)
  }
  return '0.00'
})

const editRules = {
  assetName: [{ required: true, message: '资产名称为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '资产名称为2-50个字符', trigger: 'blur' }],
  category: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  quantity: [{ required: true, type: 'number', min: 1, max: 100, message: '采购数量为1-100的整数', trigger: 'blur' }],
  unitPrice: [{ required: true, type: 'number', min: 0.01, message: '单价须大于0', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  usefulLife: [{ required: true, type: 'number', min: 1, max: 50, message: '使用年限为1-50的整数', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  location: [{ required: true, message: '存放地点为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '存放地点为2-50个字符', trigger: 'blur' }],
}

// 验收状态切换（补充验收确认）
watch(() => form.acceptStatus, (val, oldVal) => {
  if (val === 1 && oldVal === 0 && form.quantity > 0) {
    ElMessageBox.confirm(
      `确认验收并入库？系统将自动生成${form.quantity}项资产记录`,
      '确认验收',
      { type: 'warning', confirmButtonText: '确认验收', cancelButtonText: '取消' }
    ).catch(() => { form.acceptStatus = 0 })
  }
})

async function openEditDialog(row) {
  try {
    const res = await getProcurementDetail(row.id)
    const d = res.data
    Object.assign(form, {
      id: d.id,
      procurementNo: d.procurementNo || '',
      assetName: d.assetName || '',
      category: d.category || '',
      specification: d.specification || '',
      snNumber: d.snNumber || '',
      quantity: d.quantity,
      unitPrice: d.unitPrice,
      purchaseDate: d.purchaseDate || '',
      supplierId: d.supplierId,
      usefulLife: d.usefulLife,
      residualRate: d.residualRate,
      deptId: d.deptId,
      location: d.location || '',
      acceptStatus: d.acceptStatus,
      acceptDate: d.acceptDate || '',
      remark: d.remark || '',
      _originAcceptStatus: d.acceptStatus,
    })
    formVisible.value = true
  } catch (e) { /* error handled by request.js */ }
}

async function handleEditSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 补充验收确认
  if (form._originAcceptStatus === 0 && form.acceptStatus === 1) {
    try {
      await ElMessageBox.confirm(
        `确认验收并入库？系统将自动生成${form.quantity}项资产记录`,
        '确认验收',
        { type: 'warning', confirmButtonText: '确认验收', cancelButtonText: '取消' }
      )
    } catch (e) { return }
  }

  submitting.value = true
  try {
    await editProcurement(form)
    if (form._originAcceptStatus === 0 && form.acceptStatus === 1) {
      ElMessage.success(`验收完成，已生成${form.quantity}项资产`)
    } else {
      ElMessage.success('采购记录修改成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e) { /* error handled by request.js */ }
  finally { submitting.value = false }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除采购记录【' + (row.procurementNo || row.assetName) + '】？', '确认删除', { type: 'warning' })
    await deleteProcurement(row.id)
    ElMessage.success('采购记录已删除')
    fetchData()
  } catch (e) { /* cancelled or error */ }
}

onMounted(() => { loadSuppliers(); fetchData() })
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
