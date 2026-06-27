<template>
  <div>
    <div class="page-header"><h2>数据字典</h2></div>

    <el-row :gutter="16">
      <!-- 左侧字典类型 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>字典类型</span>
            <el-button text type="primary" size="small" style="float:right" @click="openTypeDialog(false)">新增</el-button>
          </template>
          <el-table :data="typeList" highlight-current-row stripe size="small" @row-click="handleTypeClick">
            <el-table-column prop="dictName" label="名称" />
            <el-table-column prop="dictCode" label="编码" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button link type="primary" size="small" @click.stop="openTypeDialog(true, row)">编辑</el-button>
                  <el-button v-if="!row.isSystem" link type="danger" size="small" @click.stop="handleDeleteType(row.id)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px; text-align:right">
            <EamsPagination :total="typeTotal" v-model:page="typeQuery.pageNum" v-model:size="typeQuery.pageSize" @change="fetchTypes" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧字典项 -->
      <el-col :span="16">
        <el-card shadow="never" v-if="currentType">
          <template #header>
            <span>{{ currentType.dictName }} - 字典项</span>
            <el-button text type="primary" size="small" style="float:right" @click="openItemDialog(false)">新增项</el-button>
          </template>
          <el-table :data="itemList" stripe size="small" border>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
            <el-table-column prop="dictValue" label="字典值" min-width="120" />
            <el-table-column prop="cssClass" label="样式" width="100">
              <template #default="{ row }"><el-tag v-if="row.cssClass" :type="row.cssClass" size="small">{{ row.cssClass }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="60" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button link type="primary" size="small" @click="openItemDialog(true, row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteItem(row.id)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-card shadow="never" v-else><EmptyState description="请从左侧选择一个字典类型" /></el-card>
      </el-col>
    </el-row>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeFormVisible" :title="typeIsEdit?'编辑字典类型':'新增字典类型'" width="460px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName"><el-input v-model="typeForm.dictName" placeholder="2-20字符" /></el-form-item>
        <el-form-item label="字典编码" prop="dictCode"><el-input v-model="typeForm.dictCode" :disabled="typeIsEdit" placeholder="小写字母+下划线" /></el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="typeForm.description" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeFormVisible=false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="handleTypeSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 字典项弹窗 -->
    <el-dialog v-model="itemFormVisible" :title="itemIsEdit?'编辑字典项':'新增字典项'" width="460px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="100px">
        <el-form-item label="字典标签" prop="dictLabel"><el-input v-model="itemForm.dictLabel" placeholder="2-50字符" /></el-form-item>
        <el-form-item label="字典值" prop="dictValue"><el-input v-model="itemForm.dictValue" :disabled="itemIsEdit" /></el-form-item>
        <el-form-item label="样式类名" prop="cssClass">
          <el-select v-model="itemForm.cssClass" placeholder="请选择" clearable style="width:100%">
            <el-option label="primary" value="primary" /><el-option label="success" value="success" />
            <el-option label="warning" value="warning" /><el-option label="danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder"><el-input-number v-model="itemForm.sortOrder" :min="0" :max="9999" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="itemForm.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemFormVisible=false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="handleItemSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDictTypes, addDictType, editDictType, deleteDictType, listDictItems, addDictItem, editDictItem, deleteDictItem } from '@/api/system'
import { useDictStore } from '@/store/dict'

const dictStore = useDictStore()

// ---- 类型 ----
const typeList = ref([])
const typeTotal = ref(0)
const typeQuery = reactive({ dictName: '', dictCode: '', pageNum: 1, pageSize: 20 })
const typeFormVisible = ref(false)
const typeIsEdit = ref(false)
const typeSubmitting = ref(false)
const typeFormRef = ref(null)
const typeForm = reactive({ id: null, dictName: '', dictCode: '', description: '' })
const typeRules = {
  dictName: [{ required: true, message: '字典名称为2-20个字符', trigger: 'blur' }, { min: 2, max: 20, message: '字典名称为2-20个字符', trigger: 'blur' }],
  dictCode: [{ required: true, message: '字典编码须为小写字母+下划线格式', trigger: 'blur' }, { pattern: /^[a-z_]{2,50}$/, message: '字典编码须为小写字母+下划线格式', trigger: 'blur' }],
  description: [{ max: 200, message: '描述不能超过200个字符', trigger: 'blur' }],
}

// ---- 项 ----
const currentType = ref(null)
const itemList = ref([])
const itemFormVisible = ref(false)
const itemIsEdit = ref(false)
const itemSubmitting = ref(false)
const itemFormRef = ref(null)
const itemForm = reactive({ id: null, dictCode: '', dictLabel: '', dictValue: '', cssClass: '', sortOrder: 0, status: 1 })
const itemRules = {
  dictLabel: [{ required: true, message: '字典标签为2-50个字符', trigger: 'blur' }, { min: 2, max: 50, message: '字典标签为2-50个字符', trigger: 'blur' }],
  dictValue: [{ required: true, message: '字典值不能为空', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '排序号为0-9999的整数', trigger: 'blur' }],
}

// ---- 方法 ----
async function fetchTypes() {
  try {
    const res = await listDictTypes(typeQuery)
    typeList.value = res.data.list || []
    typeTotal.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '字典类型加载失败')
    typeList.value = []; typeTotal.value = 0
  }
}
function handleTypeClick(row) {
  currentType.value = row
  fetchItems(row.dictCode)
}
async function fetchItems(dictCode) {
  try {
    const res = await listDictItems(dictCode)
    itemList.value = res.data || []
  } catch (e) { ElMessage.error(e.message || '字典项加载失败') }
}

// 类型 CRUD
function openTypeDialog(edit, row) {
  typeIsEdit.value = edit
  if (edit) Object.assign(typeForm, { id: row.id, dictName: row.dictName, dictCode: row.dictCode, description: row.description })
  else Object.assign(typeForm, { id: null, dictName: '', dictCode: '', description: '' })
  typeFormVisible.value = true
}
async function handleTypeSubmit() {
  const valid = await typeFormRef.value.validate().catch(() => false)
  if (!valid) return
  typeSubmitting.value = true
  try {
    if (typeIsEdit.value) { await editDictType(typeForm); ElMessage.success('字典类型修改成功') }
    else { await addDictType(typeForm); ElMessage.success('字典类型创建成功') }
    typeFormVisible.value = false; await fetchTypes(); dictStore.refresh()
  } finally { typeSubmitting.value = false }
}
async function handleDeleteType(id) {
  try {
    await ElMessageBox.confirm('删除后其下所有字典项将同步删除', '确认删除', { type: 'warning' })
    await deleteDictType(id)
    ElMessage.success('字典类型已删除'); currentType.value = null; await fetchTypes(); dictStore.refresh()
  } catch (e) { /* 取消 */ }
}

// 项 CRUD
function openItemDialog(edit, row) {
  itemIsEdit.value = edit
  if (edit) Object.assign(itemForm, { id: row.id, dictCode: row.dictCode, dictLabel: row.dictLabel, dictValue: row.dictValue, cssClass: row.cssClass, sortOrder: row.sortOrder, status: row.status })
  else Object.assign(itemForm, { id: null, dictCode: currentType.value.dictCode, dictLabel: '', dictValue: '', cssClass: '', sortOrder: 0, status: 1 })
  itemFormVisible.value = true
}
async function handleItemSubmit() {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSubmitting.value = true
  try {
    if (itemIsEdit.value) { await editDictItem(itemForm); ElMessage.success('字典项修改成功') }
    else { await addDictItem(itemForm); ElMessage.success('字典项创建成功') }
    itemFormVisible.value = false; await fetchItems(currentType.value.dictCode); dictStore.refresh()
  } finally { itemSubmitting.value = false }
}
async function handleDeleteItem(id) {
  try {
    await ElMessageBox.confirm('确认删除此字典项？', '确认删除', { type: 'warning' })
    await deleteDictItem(id)
    ElMessage.success('字典项已删除'); await fetchItems(currentType.value.dictCode); dictStore.refresh()
  } catch (e) { /* 取消 */ }
}

onMounted(fetchTypes)
</script>

<style scoped>
.action-btns { display: flex; flex-wrap: nowrap; gap: 2px; }
.action-btns .el-button { padding: 0 6px; white-space: nowrap; }
</style>
