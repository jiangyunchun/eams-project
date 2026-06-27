<template>
  <!--
    部门树形选择器
    从后端获取部门树数据，展示为级联下拉或树形选择。
    用法: <DeptTreeSelect v-model="form.deptId" />
  -->
  <el-tree-select
    v-bind="$attrs"
    :model-value="modelValue"
    @update:modelValue="$emit('update:modelValue', $event)"
    :data="treeData"
    :props="{ label: 'deptName', value: 'id', children: 'children' }"
    :placeholder="placeholder"
    clearable
    filterable
    check-strictly
  />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDeptTree } from '@/api/system/dept'

const props = defineProps({
  modelValue: [String, Number],
  placeholder: { type: String, default: '请选择部门' },
})
defineEmits(['update:modelValue'])

const treeData = ref([])

/** 将扁平部门列表转为树形结构 */
function buildTree(list, parentId = 0) {
  return list
    .filter((item) => item.parentId === parentId)
    .map((item) => ({
      ...item,
      children: buildTree(list, item.id),
    }))
}

onMounted(async () => {
  const res = await getDeptTree()
  treeData.value = buildTree(res.data || [])
})
</script>
