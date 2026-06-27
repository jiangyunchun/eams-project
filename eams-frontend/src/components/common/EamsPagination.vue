<template>
  <!--
    封装分页组件
    统一分页样式和pageSizes选项。
    用法: <EamsPagination :total="total" v-model:page="pageNum" v-model:size="pageSize" @change="fetchData" />
  -->
  <el-pagination
    v-if="total > 0"
    v-bind="$attrs"
    :current-page="page"
    :page-size="size"
    :page-sizes="[10, 20, 50, 100]"
    :total="total"
    layout="total, sizes, prev, pager, next, jumper"
    background
    small
    @current-change="handleCurrentChange"
    @size-change="handleSizeChange"
  />
</template>

<script setup>
const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
})
const emit = defineEmits(['update:page', 'update:size', 'change'])

function handleCurrentChange(val) {
  emit('update:page', val)
  emit('change')
}
function handleSizeChange(val) {
  emit('update:size', val)
  emit('update:page', 1)
  emit('change')
}
</script>
