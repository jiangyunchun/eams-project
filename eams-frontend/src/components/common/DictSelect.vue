<template>
  <!--
    字典下拉选择器
    从 Pinia 字典缓存中自动读取选项，无需重复请求后端。
    用法: <DictSelect dictCode="asset_category" v-model="form.category" />
  -->
  <el-select v-bind="$attrs" :model-value="modelValue" @update:modelValue="$emit('update:modelValue', $event)" :placeholder="placeholder" clearable filterable>
    <el-option
      v-for="item in options"
      :key="item.value"
      :label="item.label"
      :value="toValue(item.value)"
    />
  </el-select>
</template>

<script setup>
import { computed } from 'vue'
import { useDictStore } from '@/store/dict'

const props = defineProps({
  modelValue: [String, Number],
  dictCode: { type: String, required: true },
  placeholder: { type: String, default: '请选择' },
})
defineEmits(['update:modelValue'])

const dictStore = useDictStore()
const options = computed(() => dictStore.getDict(props.dictCode))

/** 将字典值转为合适类型：数字字符串转数字，其他保持原样 */
function toValue(val) {
  if (val === null || val === undefined) return val
  if (/^-?\d+(\.\d+)?$/.test(String(val))) return Number(val)
  return val
}
</script>
