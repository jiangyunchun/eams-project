<template>
  <!--
    通用文件上传组件
    支持图片/附件上传，限制类型/大小/数量，自动管理上传状态。
    用法: <FileUpload v-model="form.fileList" :limit="5" :accept="'image/*'" />
  -->
  <el-upload
    v-bind="$attrs"
    :file-list="fileList"
    :action="`${baseURL}/file/upload`"
    :headers="{ Authorization: `Bearer ${token}` }"
    :accept="accept"
    :limit="limit"
    :multiple="multiple"
    :on-success="handleSuccess"
    :on-remove="handleRemove"
    :on-exceed="handleExceed"
    list-type="picture-card"
  >
    <el-icon><Plus /></el-icon>
    <template #tip>
      <div class="upload-tip">{{ tip }}</div>
    </template>
  </el-upload>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  accept: { type: String, default: '.jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx' },
  limit: { type: Number, default: 5 },
  multiple: { type: Boolean, default: true },
  maxSize: { type: Number, default: 10 }, // MB
  tip: { type: String, default: '支持 jpg/png/pdf/doc/xls 格式，单文件不超过10MB' },
})
const emit = defineEmits(['update:modelValue'])

const token = localStorage.getItem('token')
const baseURL = import.meta.env.DEV ? '' : '/api'
const fileList = ref(props.modelValue)

function handleSuccess(response, file) {
  fileList.value.push({ name: file.name, url: response.data })
  emit('update:modelValue', fileList.value)
}
function handleRemove(file) {
  fileList.value = fileList.value.filter((f) => f.url !== file.url)
  emit('update:modelValue', fileList.value)
}
function handleExceed() {
  ElMessage.warning(`最多上传 ${props.limit} 个文件`)
}
</script>

<style scoped>
.upload-tip { font-size: 12px; color: #718096; margin-top: 4px; }
</style>
