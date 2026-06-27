<template>
  <div>
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>资产台账</h2>
      <div class="page-actions">
        <el-button v-if="hasPerm('asset:add')" type="primary" @click="openAddDialog">新增资产</el-button>
        <el-button v-if="hasPerm('asset:import')" @click="importVisible=true">批量导入</el-button>
        <el-button v-if="hasPerm('asset:export')" @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="资产编码"><el-input v-model="query.assetCode" placeholder="精确匹配" clearable style="width:150px" /></el-form-item>
        <el-form-item label="资产名称"><el-input v-model="query.assetName" placeholder="模糊搜索" clearable style="width:150px" /></el-form-item>
        <el-form-item label="资产分类">
          <DictSelect dictCode="asset_category" v-model="query.category" style="width:150px" />
        </el-form-item>
        <el-form-item label="资产状态">
          <DictSelect dictCode="asset_status" v-model="query.status" style="width:120px" />
        </el-form-item>
        <el-form-item label="所属部门"><DeptTreeSelect v-model="query.deptId" style="width:160px" /></el-form-item>
        <el-form-item label="使用人"><el-input v-model="query.userName" placeholder="模糊搜索" clearable style="width:150px" /></el-form-item>
        <el-form-item label="存放地点"><el-input v-model="query.location" placeholder="模糊搜索" clearable style="width:150px" /></el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="purchaseDateRange" type="daterange" range-separator="~" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:220px" />
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
        <el-table-column label="图片" width="55" align="center">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:40px;height:40px" fit="cover" />
            <span v-else style="color:#CBD5E0;font-size:12px">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="assetCode" label="资产编码" width="160">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">{{ row.assetCode }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="assetName" label="资产名称" min-width="140" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ dictLabel('asset_category', row.category) }}</template>
        </el-table-column>
        <el-table-column prop="specification" label="规格型号" min-width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :value="row.status" type="asset" /></template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属部门" min-width="120" />
        <el-table-column prop="userName" label="使用人" width="100" />
        <el-table-column prop="location" label="存放地点" min-width="120" />
        <el-table-column label="采购日期" width="100">
          <template #default="{ row }">{{ row.purchaseDate }}</template>
        </el-table-column>
        <el-table-column label="原值(元)" width="110" align="right">
          <template #default="{ row }">{{ formatMoney(row.originalValue) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-if="hasPerm('asset:edit')" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="hasPerm('asset:delete')" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              <el-button v-if="hasPerm('asset:depreciation')" link type="primary" size="small" @click="openDepreciation(row)">折旧</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>

    <!-- ==================== 新增/编辑弹窗 ==================== -->
    <el-dialog v-model="formVisible" :title="isEdit?'编辑资产':'新增资产'" width="640px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName">
              <el-input v-model="form.assetName" placeholder="2-50个字符" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产分类" prop="category">
              <DictSelect dictCode="asset_category" v-model="form.category" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" placeholder="最大50字符" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SN序列号" prop="snNumber">
              <el-input v-model="form.snNumber" placeholder="最大50字符" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="原值(元)" prop="originalValue">
              <el-input-number v-model="form.originalValue" :min="0.01" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="使用年限(年)" prop="usefulLife">
              <el-input-number v-model="form.usefulLife" :min="1" :max="50" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="净残值率(%)" prop="residualRate">
              <el-input-number v-model="form.residualRate" :min="0" :max="100" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="存放地点" prop="location">
              <el-input v-model="form.location" placeholder="2-50个字符" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <DeptTreeSelect v-model="form.deptId" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="使用人" prop="userId">
              <el-select v-model="form.userId" placeholder="选择使用人" clearable filterable style="width:100%">
                <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择" style="width:100%">
                <el-option label="闲置" :value="0" />
                <el-option label="在用" :value="1" />
                <el-option label="借用" :value="2" />
                <el-option label="维修" :value="3" />
                <el-option label="报废" :value="4" />
                <el-option label="盘点中" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片">
          <el-upload :action="uploadUrl" :headers="{Authorization:'Bearer '+token}" :limit="1" list-type="picture-card"
            :file-list="uploadFileList" @success="handleUploadSuccess" :on-remove="()=>{form.imageUrl='';uploadFileList=[]}" accept=".jpg,.jpeg,.png"
            @preview="handlePreview">
            <el-icon><Plus /></el-icon>
            <template #tip><div style="font-size:12px;color:#A0AEC0;line-height:1.4">仅支持 JPG/PNG 格式，大小不超过2MB<br>只能上传一张图片</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 详情弹窗 ==================== -->
    <el-dialog v-model="detailVisible" title="资产详情" width="700px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="资产编码" :span="2">{{ detailData.assetCode }}</el-descriptions-item>
          <el-descriptions-item label="资产名称">{{ detailData.assetName }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ dictLabel('asset_category', detailData.category) }}</el-descriptions-item>
          <el-descriptions-item label="规格型号">{{ detailData.specification||'-' }}</el-descriptions-item>
          <el-descriptions-item label="SN序列号">{{ detailData.snNumber||'-' }}</el-descriptions-item>
          <el-descriptions-item label="存放地点">{{ detailData.location }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ detailData.deptName }}</el-descriptions-item>
          <el-descriptions-item label="使用人">{{ detailData.userName||'-' }}</el-descriptions-item>
          <el-descriptions-item label="资产状态"><StatusTag :value="detailData.status" type="asset" /></el-descriptions-item>
          <el-descriptions-item label="原值(元)">{{ formatMoney(detailData.originalValue) }}</el-descriptions-item>
          <el-descriptions-item label="采购日期">{{ detailData.purchaseDate }}</el-descriptions-item>
          <el-descriptions-item label="使用年限">{{ detailData.usefulLife }}年</el-descriptions-item>
          <el-descriptions-item label="净残值率">{{ detailData.residualRate }}%</el-descriptions-item>
          <el-descriptions-item label="预计报废日期">{{ detailData.scrapDate||'-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark||'-' }}</el-descriptions-item>
          <el-descriptions-item label="图片" :span="2">
            <template v-if="detailData.imageUrl">
              <el-image :src="detailData.imageUrl" style="max-width:200px;max-height:150px" fit="contain" :preview-src-list="[detailData.imageUrl]" preview-teleported />
              <el-button link type="primary" size="small" @click="viewImage(detailData.imageUrl)">查看原图</el-button>
            </template>
            <span v-else style="color:#A0AEC0">无</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />
        <h4 style="margin-bottom:8px">折旧信息</h4>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="已计提月数">{{ detailData.depreciatedMonths||0 }}个月</el-descriptions-item>
          <el-descriptions-item label="月折旧额">{{ formatMoney(detailData.monthlyAmount) }}</el-descriptions-item>
          <el-descriptions-item label="累计折旧">{{ formatMoney(detailData.accumulated) }}</el-descriptions-item>
          <el-descriptions-item label="当前净值"><span style="color:#E53E3E;font-weight:600">{{ formatMoney(detailData.netValue) }}</span></el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- ==================== 折旧明细弹窗 ==================== -->
    <el-dialog v-model="deprVisible" title="折旧明细" width="600px">
      <el-table :data="deprList" stripe border size="small" v-loading="deprLoading">
        <el-table-column prop="depreciationMonth" label="计提月份" width="120" />
        <el-table-column label="月折旧额" width="130" align="right">
          <template #default="{row}">{{ formatMoney(row.monthlyAmount) }}</template>
        </el-table-column>
        <el-table-column label="累计折旧" width="130" align="right">
          <template #default="{row}">{{ formatMoney(row.accumulated) }}</template>
        </el-table-column>
        <el-table-column label="净值" width="130" align="right">
          <template #default="{row}">{{ formatMoney(row.netValue) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{row}"><StatusTag :value="row.status" type="user" /></template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!deprLoading && deprList.length===0" description="暂无折旧记录" />
    </el-dialog>

    <!-- ==================== 批量导入弹窗 ==================== -->
    <el-dialog v-model="importVisible" title="批量导入资产" width="560px">
      <div style="margin-bottom:16px">
        <el-button @click="downloadTemplate">下载导入模板</el-button>
      </div>
      <el-upload drag :action="uploadImportUrl" :headers="{Authorization:'Bearer '+token}" :on-success="handleImportSuccess" :on-error="handleImportError" :show-file-list="true" accept=".xlsx,.xls">
        <el-icon style="font-size:40px;color:#2B6CB0"><UploadFilled /></el-icon>
        <div style="margin-top:8px">将Excel文件拖到此处，或<em>点击上传</em></div>
        <template #tip><div style="font-size:12px;color:#A0AEC0;margin-top:4px">仅支持 .xlsx/.xls 格式，单次最多1000条</div></template>
      </el-upload>
    </el-dialog>

    <!-- ==================== 导入结果弹窗 ==================== -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="480px">
      <div style="text-align:center;padding:20px 0">
        <div style="font-size:48px;margin-bottom:16px">📊</div>
        <div style="font-size:16px;margin-bottom:8px">
          成功导入 <span style="color:#38A169;font-weight:700">{{ importResult.success }}</span> 条
        </div>
        <div v-if="importResult.fail > 0" style="font-size:16px;margin-bottom:16px">
          失败 <span style="color:#E53E3E;font-weight:700">{{ importResult.fail }}</span> 条
        </div>
        <div v-else style="font-size:16px;margin-bottom:16px;color:#38A169">
          全部导入成功，共计 {{ importResult.success }} 条
        </div>
        <el-button v-if="importResult.fail > 0" type="danger" @click="downloadErrorReport">下载错误报告</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import request from '@/utils/request'
import { listAssets, getAssetDetail, addAsset, editAsset, deleteAsset, getDepreciation, exportAsset } from '@/api/asset'
import { useDictStore } from '@/store/dict'
import { useAuthStore } from '@/store/auth'
import { listUsers } from '@/api/system'

// ---- stores ----
const dict = useDictStore()
const auth = useAuthStore()

/** 权限校验 */
function hasPerm(perm) { return auth.permissions?.includes(perm) }

/** 字典标签 */
function dictLabel(code, val) { return dict.getLabel(code, val) }

/** 金额格式化 */
function formatMoney(v) { return v ? Number(v).toLocaleString('zh-CN', {minimumFractionDigits:2}) : '-' }

/** 时间格式化 */
function formatTime(t) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-' }

// ---- 查询 ----
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const purchaseDateRange = ref(null)
const query = reactive({
  assetCode: '', assetName: '', category: '', status: null, deptId: null,
  userName: '', location: '', beginPurchaseDate: '', endPurchaseDate: '',
  pageNum: 1, pageSize: 10,
})

async function fetchData() {
  if (purchaseDateRange.value) {
    query.beginPurchaseDate = purchaseDateRange.value[0]
    query.endPurchaseDate = purchaseDateRange.value[1]
  } else { query.beginPurchaseDate = ''; query.endPurchaseDate = '' }
  loading.value = true
  try {
    const res = await listAssets(query)
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message||'查询失败'); tableData.value=[]; total.value=0 }
  finally { loading.value = false }
}

function resetQuery() {
  Object.assign(query, {assetCode:'',assetName:'',category:'',status:null,deptId:null,userName:'',location:'',pageNum:1})
  purchaseDateRange.value = null
  fetchData()
}

// ---- 权限按钮显隐 ----
const token = computed(() => auth.token)
const uploadUrl = '/api/file/upload'
const uploadImportUrl = '/api/asset/import'
const uploadFileList = ref([])

function handleUploadSuccess(response, file, fileList) {
  console.log('上传响应:', response)
  if (response && response.code === 200) {
    form.imageUrl = response.data
    uploadFileList.value = [{ name: file.name, url: response.data }]
  } else {
    ElMessage.error('图片上传失败：' + (response?.message || '未知错误'))
  }
}
function handlePreview(file) {
  window.open(file.url, '_blank')
}
function viewImage(url) {
  window.open(url, '_blank')
}

// ---- 新增/编辑 ----
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, assetName: '', category: '', specification: '', snNumber: '',
  procurementNo: '', originalValue: '', purchaseDate: null, usefulLife: '',
  residualRate: 5, location: '', deptId: null, userId: null, status: 0,
  imageUrl: '', remark: '', version: null,
})
// 用户列表（使用人下拉用）
const userOptions = ref([])

// PRD 6.2.2 校验规则
const rules = {
  assetName: [{ required: true, message: '资产名称为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '资产名称为2-50个字符', trigger: 'blur' }],
  category: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  originalValue: [{ required: true, type: 'number', min: 0.01, message: '原值须大于0', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
  usefulLife: [{ required: true, type: 'number', min: 1, max: 50, message: '使用年限为1-50的整数', trigger: 'blur' }],
  location: [{ required: true, message: '存放地点为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '存放地点为2-50个字符', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  userId: [],
  specification: [{ max: 50, message: '规格型号不能超过50个字符', trigger: 'blur' }],
  snNumber: [{ max: 50, message: 'SN序列号不能超过50个字符', trigger: 'blur' }],
  procurementNo: [{ max: 50, message: '采购编号不能超过50个字符', trigger: 'blur' }],
  residualRate: [{ type: 'number', min: 0, max: 100, message: '净残值率须在0-100之间', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }],
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(form, {
    id:null, assetName:'', category:'', specification:'', snNumber:'',
    procurementNo:'', originalValue:null, purchaseDate:null, usefulLife:null,
    residualRate:5, location:'', deptId:null, userId:null, status:0,
    imageUrl:'', remark:'', version:null,
  })
  uploadFileList.value = []
  formVisible.value = true
}

async function openEditDialog(row) {
  isEdit.value = true
  const res = await getAssetDetail(row.id)
  const d = res.data
  Object.assign(form, {
    id: d.id, assetName: d.assetName, category: d.category, specification: d.specification||'',
    snNumber: d.snNumber||'', procurementNo: d.procurementNo||'', originalValue: d.originalValue,
    purchaseDate: d.purchaseDate, usefulLife: d.usefulLife, residualRate: d.residualRate,
    location: d.location, deptId: d.deptId, userId: d.userId, status: d.status,
    imageUrl: d.imageUrl||'', remark: d.remark||'', version: row.version != null ? row.version : 0,
  })
  uploadFileList.value = d.imageUrl ? [{ name: '资产图片', url: d.imageUrl }] : []
  formVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    // e 是验证失败的字段列表
    const msg = e?.[0]?.message || e?.message || '请检查表单中的红色错误提示'
    ElMessage.warning(msg)
    return
  }
  // 在用/借用状态时使用人必填
  if ((form.status == 1 || form.status == 2) && !form.userId) {
    ElMessage.warning('资产状态为【在用/借用】时请指定使用人')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await editAsset(form)
      ElMessage.success(`资产【${form.assetName}】信息修改成功`)
    } else {
      const res = await addAsset(form)
      ElMessage.success(res.message || `资产【${form.assetName}】创建成功`)
    }
    formVisible.value = false
    fetchData()
  } catch (e) { /* 已在 request.js 处理 */ }
  finally { submitting.value = false }
}

// ---- 删除 ----
async function handleDelete(row) {
  // 拦截在用/借用删除（后端也会拦截，前端提前提示）
  if (row.status === 1) { ElMessage.warning('该资产当前为【在用】状态，请先归还后再删除'); return }
  if (row.status === 2) { ElMessage.warning('该资产当前为【借用】状态，请先归还后再删除'); return }
  if (row.status === 5) { ElMessage.warning('该资产正在盘点中，请等待盘点结束后再操作'); return }
  try {
    await ElMessageBox.confirm('确认删除资产【' + row.assetName + '】？', '确认删除', { type: 'warning' })
    await deleteAsset(row.id)
    ElMessage.success('资产已删除')
    fetchData()
  } catch (e) { /* 取消 */ }
}

// ---- 详情 ----
const detailVisible = ref(false)
const detailData = ref(null)

async function openDetail(row) {
  const res = await getAssetDetail(row.id)
  detailData.value = res.data
  detailVisible.value = true
}

// ---- 折旧明细 ----
const deprVisible = ref(false)
const deprList = ref([])
const deprLoading = ref(false)

async function openDepreciation(row) {
  deprVisible.value = true
  deprLoading.value = true
  try {
    const res = await getDepreciation(row.id)
    deprList.value = res.data || []
  } catch (e) { deprList.value = [] }
  finally { deprLoading.value = false }
}

// ---- 导出 ----
async function handleExport() {
  try {
    const response = await exportAsset(query)
    const blob = response.data instanceof Blob ? response.data : new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `资产台账_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功，文件正在下载')
  } catch (e) {
    if (e.message) ElMessage.error(e.message)
  }
}

// ---- 批量导入 ----
const importVisible = ref(false)
const importResultVisible = ref(false)
const importResult = reactive({ success: 0, fail: 0, errors: [] })

async function downloadTemplate() {
  try {
    const response = await request.get('/asset/template', { responseType: 'blob' })
    const blob = response.data instanceof Blob ? response.data : new Blob([response])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    // 从响应头提取后端返回的文件名，含时间戳
    const disposition = response.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename=(.+?)(?:\.xlsx|$)/)
    a.download = (match ? decodeURIComponent(match[1]) : '资产导入模板') + '.xlsx'
    a.href = url; a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

function handleImportSuccess(res) {
  importVisible.value = false
  if (res.code === 200 && res.data) {
    importResult.success = res.data.success || 0
    importResult.fail = res.data.fail || 0
    importResult.errors = res.data.errors || []
    importResultVisible.value = true
    fetchData()
  } else {
    ElMessage.error(res.message || '导入失败')
  }
}

function handleImportError(err) {
  ElMessage.error('导入失败：' + (err.message || '请检查文件格式'))
}

async function downloadErrorReport() {
  try {
    const response = await request.post('/asset/import-error-report', importResult.errors, { responseType: 'blob' })
    const blob = response.data instanceof Blob ? response.data : new Blob([response])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    const disposition = response.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename=(.+?)(?:\.xlsx|$)/)
    a.download = (match ? decodeURIComponent(match[1]) : '导入错误报告') + '.xlsx'
    a.href = url; a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('错误报告下载失败')
  }
}

// ---- 初始化 ----
onMounted(async () => {
  await fetchData()
  // 加载用户列表供使用人选择
  try {
    const res = await listUsers({ pageNum: 1, pageSize: 200 })
    userOptions.value = res.data.list || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
