<template>
  <div>
    <div class="page-header">
      <h2>报废记录</h2>
      <div class="page-actions">
        <el-button type="success" @click="handleExport" :loading="exporting">
          导出Excel
        </el-button>
      </div>
    </div>

    <!-- 查询区（PRD 6.9 报废记录查询区） -->
    <el-card style="margin-bottom:16px">
      <el-form :model="q" inline>
        <el-form-item label="报废编号">
          <el-input v-model="q.scrapNo" clearable style="width:180px" placeholder="请输入报废编号" />
        </el-form-item>
        <el-form-item label="资产名称">
          <el-input v-model="q.assetName" clearable style="width:160px" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="资产编码">
          <el-input v-model="q.assetCode" clearable style="width:160px" placeholder="请输入资产编码" />
        </el-form-item>
        <el-form-item label="报废原因">
          <el-select v-model="q.scrapReason" clearable style="width:140px" placeholder="全部">
            <el-option label="老化损坏" value="老化损坏" />
            <el-option label="技术淘汰" value="技术淘汰" />
            <el-option label="维修成本过高" value="维修成本过高" />
            <el-option label="盘亏确认" value="盘亏确认" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="报废状态">
          <el-select v-model="q.status" clearable style="width:150px" placeholder="全部">
            <el-option label="待初审" :value="0" />
            <el-option label="待终审" :value="1" />
            <el-option label="已通过(待处置)" :value="2" />
            <el-option label="已驳回" :value="3" />
            <el-option label="已处置" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="q.applicantName" clearable style="width:140px" placeholder="请输入申请人" />
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区（PRD 6.9.2 报废审批表格列 + 状态流转时间线） -->
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="scrapNo" label="报废编号" width="180" />
        <el-table-column label="资产编码/名称" min-width="170">
          <template #default="{ row }">
            <div>{{ row.assetCode }}</div>
            <div style="color:#718096;font-size:12px">{{ row.assetName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="原值/净值" width="150">
          <template #default="{ row }">
            <div>¥{{ formatNumber(row.originalValue) }}</div>
            <div style="color:#718096;font-size:12px">
              净值: ¥{{ formatNumber(row.netValue) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="报废原因" width="110">
          <template #default="{ row }">{{ row.scrapReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column label="申请时间" width="155">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.substring(0, 16) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <StatusTag :value="row.status" type="scrap" />
          </template>
        </el-table-column>
        <!-- 处置信息（仅已处置显示） -->
        <el-table-column label="处置方式" width="90">
          <template #default="{ row }">
            {{ row.status === 4 ? (row.disposalMethod || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="处置日期" width="110">
          <template #default="{ row }">
            {{ row.status === 4 ? (row.disposalDate || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EamsPagination
        v-model:pageNum="q.pageNum"
        v-model:pageSize="q.pageSize"
        :total="total"
        @change="load"
      />
    </el-card>

    <!-- 详情弹窗（PRD 6.9：完整审批、处置流转时间线） -->
    <el-dialog v-model="detailVisible" title="报废单详情" width="750px">
      <template v-if="detail">
        <!-- 基本信息 -->
        <el-descriptions title="基本信息" :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="报废编号">{{ detail.scrapNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :value="detail.status" type="scrap" />
          </el-descriptions-item>
          <el-descriptions-item label="资产编码">{{ detail.assetCode }}</el-descriptions-item>
          <el-descriptions-item label="资产名称">{{ detail.assetName }}</el-descriptions-item>
          <el-descriptions-item label="资产分类">{{ detail.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="规格型号">{{ detail.specification || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原值">¥{{ formatNumber(detail.originalValue) }}</el-descriptions-item>
          <el-descriptions-item label="净值">¥{{ formatNumber(detail.netValue) }}</el-descriptions-item>
          <el-descriptions-item label="存放地点">{{ detail.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ detail.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="报废原因">{{ detail.scrapReason }}</el-descriptions-item>
          <el-descriptions-item label="处置建议">{{ detail.disposalAdvice }}</el-descriptions-item>
          <el-descriptions-item label="原因说明" :span="2">{{ detail.reasonDesc }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 状态流转概览（详细审批记录见操作日志） -->
        <el-descriptions title="状态流转" :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="当前状态">
            <StatusTag :value="detail.status" type="scrap" />
          </el-descriptions-item>
          <el-descriptions-item label="最后更新时间">
            {{ detail.updateTime ? detail.updateTime.substring(0, 16) : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 驳回信息：驳回原因存储在 remark 字段（PRD 6.9.2） -->
        <template v-if="detail.status === 3">
          <el-descriptions title="驳回信息" :column="2" border style="margin-bottom:16px">
            <el-descriptions-item label="驳回原因" :span="2">
              <span style="color:#E53E3E">{{ detail.remark || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </template>

        <!-- 处置信息 -->
        <template v-if="detail.status === 4">
          <el-descriptions title="处置信息" :column="2" border style="margin-bottom:16px">
            <el-descriptions-item label="处置方式">{{ detail.disposalMethod }}</el-descriptions-item>
            <el-descriptions-item label="处置日期">{{ detail.disposalDate }}</el-descriptions-item>
            <el-descriptions-item label="处置收入">¥{{ formatNumber(detail.disposalIncome) }}</el-descriptions-item>
            <el-descriptions-item label="处置费用">¥{{ formatNumber(detail.disposalCost) }}</el-descriptions-item>
            <el-descriptions-item label="经办人">{{ detail.disposalHandler }}</el-descriptions-item>
            <el-descriptions-item label="处置说明" :span="2">{{ detail.disposalDesc || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <!-- 附件下载 -->
        <div v-if="detail.attachmentUrls" style="margin-top:8px">
          <el-divider content-position="left">附件</el-divider>
          <div v-for="(url, idx) in detail.attachmentUrls.split(',')" :key="idx" style="margin-bottom:4px">
            <el-link type="primary" :href="url" target="_blank">
              {{ url.split('/').pop() || '附件' + (idx + 1) }}
            </el-link>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listRecords, getDetail, exportRecords } from '@/api/scrap'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

const loading = ref(false)
const exporting = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const q = reactive({
  pageNum: 1,
  pageSize: 10,
  scrapNo: '',
  assetName: '',
  assetCode: '',
  scrapReason: '',
  status: null,
  applicantName: '',
})

async function load() {
  loading.value = true
  try {
    const params = { ...q }
    if (dateRange.value?.length === 2) {
      params.beginDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await listRecords(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function search() {
  q.pageNum = 1
  load()
}

function reset() {
  q.scrapNo = ''
  q.assetName = ''
  q.assetCode = ''
  q.scrapReason = ''
  q.status = null
  q.applicantName = ''
  dateRange.value = []
  search()
}

async function showDetail(row) {
  try {
    const res = await getDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    // handled by interceptor
  }
}

async function handleExport() {
  // 二次确认
  exporting.value = true
  try {
    const params = { ...q, pageNum: 1, pageSize: 10000 }
    if (dateRange.value?.length === 2) {
      params.beginDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await exportRecords(params)
    // 处理导出（blob下载）
    if (res.data) {
      const list = res.data
      // 简单导出为CSV
      const headers = ['报废编号', '资产编码', '资产名称', '报废原因', '状态', '申请人', '申请时间', '处置方式', '处置日期']
      const rows = list.map(r => [
        r.scrapNo || '',
        r.assetCode || '',
        r.assetName || '',
        r.scrapReason || '',
        r.statusLabel || '',
        r.applicantName || '',
        r.createTime || '',
        r.disposalMethod || '',
        r.disposalDate || '',
      ])
      const csv = [headers.join(','), ...rows.map(r => r.map(c => `"${c}"`).join(','))].join('\n')
      const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `报废记录_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    exporting.value = false
  }
}

function formatNumber(val) {
  if (val == null) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  load()
})
</script>
