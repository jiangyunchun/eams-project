<template>
  <div>
    <div class="page-header"><h2>登录日志</h2></div>

    <div class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="用户名"><el-input v-model="query.username" placeholder="模糊搜索" clearable style="width:160px" /></el-form-item>
        <el-form-item label="登录结果">
          <el-select v-model="query.loginStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="query.username='';query.loginStatus=null;fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="登录结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.loginStatus===1?'success':'danger'" size="small">
              {{ row.loginStatus===1?'成功':'失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="180">
          <template #default="{ row }">{{ row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column label="登录时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px; text-align:right">
        <EamsPagination :total="total" v-model:page="query.pageNum" v-model:size="query.pageSize" @change="fetchData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import request from '@/utils/request'

function formatTime(time) { return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-' }

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ username: '', loginStatus: null, pageNum: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/system/login-log/list', { params: query })
    tableData.value = res.data.list || []
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || '查询失败')
    tableData.value = []; total.value = 0
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>
