<template>
  <!--
    空数据占位页
    列表无数据时显示，替代空白页面。
    用法: <EmptyState description="暂无资产数据" />
  -->
  <el-empty :description="description" :image-size="imageSize">
    <template v-if="$slots.default">
      <slot />
    </template>
  </el-empty>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
  imageSize: { type: Number, default: 200 },
})
</script>
