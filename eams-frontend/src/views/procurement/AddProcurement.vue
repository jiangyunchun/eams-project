<template>
  <div>
    <div class="page-header">
      <h2>采购登记</h2>
      <div class="page-actions">
        <el-button @click="$router.push('/procurement/record')">返回采购记录</el-button>
      </div>
    </div>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width:900px">
        <el-row :gutter="24">
          <!-- 左列 -->
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
              <el-input v-model="form.specification" placeholder="最多50个字符" clearable />
            </el-form-item>
            <el-form-item label="SN序列号" prop="snNumber">
              <el-input v-model="form.snNumber" placeholder="最多50个字符" clearable />
            </el-form-item>
            <el-form-item label="采购数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :max="100" :step="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="单价（元）" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0.01" :precision="2" :step="100" style="width:100%" />
            </el-form-item>
            <el-form-item label="总价（元）">
              <el-input :model-value="computedTotal" readonly style="width:100%" />
            </el-form-item>
          </el-col>
          <!-- 右列 -->
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" style="width:100%" filterable>
                <el-option v-for="s in supplierList" :key="s.id" :label="s.supplierName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="使用年限（年）" prop="usefulLife">
              <el-input-number v-model="form.usefulLife" :min="1" :max="50" :step="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="净残值率（%）" prop="residualRate">
              <el-input-number v-model="form.residualRate" :min="0" :max="100" :precision="2" :step="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="所属部门" prop="deptId">
              <DeptTreeSelect v-model="form.deptId" style="width:100%" />
            </el-form-item>
            <el-form-item label="存放地点" prop="location">
              <el-input v-model="form.location" placeholder="2-50个字符" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 全宽行 -->
        <el-divider />
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="验收状态" prop="acceptStatus">
              <el-radio-group v-model="form.acceptStatus">
                <el-radio :value="0">待验收</el-radio>
                <el-radio :value="1">已验收</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.acceptStatus === 1" label="验收日期" prop="acceptDate">
              <el-date-picker v-model="form.acceptDate" type="date" value-format="YYYY-MM-DD" placeholder="选择验收日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="最多500个字符" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 操作按钮 -->
        <el-row>
          <el-col :span="24" style="text-align:center; padding-top:16px">
            <el-button @click="$router.push('/procurement/record')">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addProcurement, getAllEnabledSupplier } from '@/api/procurement'

const router = useRouter()

// ---- 供应商下拉 ----
const supplierList = ref([])
async function loadSuppliers() {
  try {
    const res = await getAllEnabledSupplier()
    supplierList.value = res.data || []
  } catch (e) { /* ignore */ }
}

// ---- 表单 ----
const formRef = ref(null)
const submitting = ref(false)
const form = reactive({
  procurementNo: '',
  assetName: '',
  category: '',
  specification: '',
  snNumber: '',
  quantity: 1,
  unitPrice: null,
  purchaseDate: '',
  supplierId: null,
  usefulLife: 5,
  residualRate: 5.00,
  deptId: null,
  location: '',
  acceptStatus: 0,
  acceptDate: '',
  remark: '',
})

// 总价自动计算
const computedTotal = computed(() => {
  if (form.quantity && form.unitPrice) {
    return (form.quantity * form.unitPrice).toFixed(2)
  }
  return '0.00'
})

// 验收状态切换确认
watch(() => form.acceptStatus, (val) => {
  if (val === 1 && form.quantity > 0) {
    ElMessageBox.confirm(
      `确认验收并入库？系统将自动生成${form.quantity}项资产记录`,
      '确认验收',
      { type: 'warning', confirmButtonText: '确认验收', cancelButtonText: '取消' }
    ).catch(() => { form.acceptStatus = 0 })
  }
})

const rules = {
  assetName: [
    { required: true, message: '资产名称为2-50个字符', trigger: 'blur' },
    { min: 2, max: 50, message: '资产名称为2-50个字符', trigger: 'blur' },
  ],
  category: [
    { required: true, message: '请选择资产分类', trigger: 'change' },
  ],
  specification: [
    { max: 50, message: '规格型号不能超过50个字符', trigger: 'blur' },
  ],
  snNumber: [
    { max: 50, message: 'SN序列号不能超过50个字符', trigger: 'blur' },
  ],
  quantity: [
    { required: true, type: 'number', min: 1, max: 100, message: '采购数量为1-100的整数', trigger: 'blur' },
  ],
  unitPrice: [
    { required: true, type: 'number', min: 0.01, message: '单价须大于0', trigger: 'blur' },
  ],
  purchaseDate: [
    { required: true, message: '请选择采购日期', trigger: 'change' },
  ],
  supplierId: [
    { required: true, message: '请选择供应商', trigger: 'change' },
  ],
  usefulLife: [
    { required: true, type: 'number', min: 1, max: 50, message: '使用年限为1-50的整数', trigger: 'blur' },
  ],
  residualRate: [
    { type: 'number', min: 0, max: 100, message: '净残值率须在0-100之间', trigger: 'blur' },
  ],
  deptId: [
    { required: true, message: '请选择所属部门', trigger: 'change' },
  ],
  location: [
    { required: true, message: '存放地点为2-50个字符', trigger: 'blur' },
    { min: 2, max: 50, message: '存放地点为2-50个字符', trigger: 'blur' },
  ],
  acceptStatus: [
    { required: true, message: '请选择验收状态', trigger: 'change' },
  ],
  acceptDate: [
    {
      validator: (rule, value, callback) => {
        if (form.acceptStatus === 1 && !value) {
          callback(new Error('请选择验收日期'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 验收状态=已验收时再确认一次
  if (form.acceptStatus === 1) {
    try {
      await ElMessageBox.confirm(
        `确认验收并入库？系统将自动生成${form.quantity}项资产记录`,
        '确认验收',
        { type: 'warning', confirmButtonText: '确认验收', cancelButtonText: '取消' }
      )
    } catch (e) {
      return
    }
  }

  submitting.value = true
  try {
    await addProcurement(form)
    if (form.acceptStatus === 1) {
      ElMessage.success(`验收完成，已生成${form.quantity}项资产`)
    } else {
      ElMessage.success('采购记录保存成功，验收后资产将自动入库')
    }
    router.push('/procurement/record')
  } catch (e) {
    // request.js 已处理错误提示
  } finally {
    submitting.value = false
  }
}

onMounted(() => { loadSuppliers() })
</script>
