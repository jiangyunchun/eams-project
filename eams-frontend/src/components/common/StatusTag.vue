<template>
  <!--
    状态标签组件
    色彩规范：绿色=正常/启用/在用/成功，橙色=待审批/盘点中/进行中，红色=异常/禁用/盘亏/驳回
    用法: <StatusTag :value="user.status" type="user" />
  -->
  <el-tag :type="tagType" size="small">
    {{ label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: [String, Number],
  type: { type: String, default: 'asset' }, // asset | requisition | user | boolean
})

const ASSET_MAP = {
  0: { label: '闲置',  type: 'info' },
  1: { label: '在用',  type: 'success' },
  2: { label: '借用',  type: 'warning' },
  3: { label: '维修',  type: 'danger' },
  4: { label: '报废',  type: 'info' },
  5: { label: '盘点中',type: 'warning' },
}
const REQ_MAP = {
  0: { label: '待部门审批', type: 'warning' },
  1: { label: '待资产管理员审批', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
  4: { label: '已归还', type: 'info' },
}
const USER_MAP = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '启用', type: 'success' },
}
const TRANSFER_MAP = {
  0: { label: '待调入确认', type: 'warning' },
  1: { label: '待资产管理员审批', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
}
const BOOL_MAP = {
  true:  { label: '是', type: 'success' },
  false: { label: '否', type: 'info' },
}

const tag = computed(() => {
  if (props.type === 'asset') return ASSET_MAP[props.value]
  if (props.type === 'requisition') return REQ_MAP[props.value]
  if (props.type === 'transfer') return TRANSFER_MAP[props.value]
  if (props.type === 'user') return USER_MAP[props.value]
  if (props.type === 'boolean') return BOOL_MAP[String(!!props.value)]
  return { label: String(props.value ?? ''), type: 'info' }
})

const label = computed(() => tag.value?.label ?? String(props.value ?? ''))
const tagType = computed(() => tag.value?.type ?? 'info')
</script>
