<template>
  <!--
    确认弹窗（二次确认操作）
    封装 ElMessageBox.confirm，统一确认文案风格。
    用法: <ConfirmDialog title="确认删除？" content="删除后不可恢复" @confirm="handleDelete" />
  -->
  <el-button v-bind="$attrs" @click="openConfirm">
    <slot />
  </el-button>
</template>

<script setup>
import { ElMessageBox, ElMessage } from 'element-plus'

const props = defineProps({
  title: { type: String, default: '确认操作' },
  content: { type: String, default: '确认执行此操作吗？' },
  confirmText: { type: String, default: '确认' },
  cancelText: { type: String, default: '取消' },
  type: { type: String, default: 'warning' },
  successMsg: { type: String, default: '' },
})
const emit = defineEmits(['confirm'])

async function openConfirm() {
  try {
    await ElMessageBox.confirm(props.content, props.title, {
      confirmButtonText: props.confirmText,
      cancelButtonText: props.cancelText,
      type: props.type,
    })
    emit('confirm')
    if (props.successMsg) ElMessage.success(props.successMsg)
  } catch (e) {
    // 用户取消
  }
}
</script>
