<template>
  <div>
    <div class="page-header"><h2>维保记录</h2><div class="page-actions"><el-button v-if="has('repair:apply')" type="primary" @click="$router.push('/repair/apply')">报修登记</el-button><el-button v-if="has('repair:record')" type="success" @click="exp">导出Excel</el-button></div></div>
    <div class="search-card">
      <el-form :model="q" inline>
        <el-form-item label="报修编号"><el-input v-model="q.repairNo" clearable style="width:180px"/></el-form-item>
        <el-form-item label="资产名称"><el-input v-model="q.assetName" clearable style="width:160px"/></el-form-item>
        <el-form-item label="故障类型"><el-select v-model="q.faultType" clearable style="width:130px"><el-option v-for="t in types" :key="t" :label="t" :value="t"/></el-select></el-form-item>
        <el-form-item label="维修状态"><el-select v-model="q.repairStatus" clearable style="width:130px"><el-option label="待维修" :value="0"/><el-option label="维修中" :value="1"/><el-option label="已修复" :value="2"/><el-option label="无法修复" :value="3"/></el-select></el-form-item>
        <el-form-item label="报修时间"><el-date-picker v-model="dr" type="daterange" range-separator="~" value-format="YYYY-MM-DD" style="width:220px"/></el-form-item>
        <el-form-item class="search-buttons"><el-button type="primary" @click="fetch">查询</el-button><el-button @click="resetQ">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <el-table :data="d" v-loading="l" stripe border>
        <el-table-column type="index" label="序号" width="55" align="center"/>
        <el-table-column prop="repairNo" label="报修编号" width="180"/>
        <el-table-column label="资产" min-width="150"><template #default="{row}">{{row.assetCode}} {{row.assetName}}</template></el-table-column>
        <el-table-column prop="applicantName" label="报修人" width="100"/>
        <el-table-column prop="faultType" label="故障类型" width="100"/>
        <el-table-column prop="repairMethod" label="维修方式" width="100"/>
        <el-table-column prop="repairPerson" label="维修人员" width="100"/>
        <el-table-column label="维修费用" width="100" align="right"><template #default="{row}">{{row.repairFee}}</template></el-table-column>
        <el-table-column label="状态" width="100" align="center"><template #default="{row}"><StatusTag :value="row.repairStatus" type="repair"/></template></el-table-column>
        <el-table-column prop="createTime" label="报修时间" width="160"/>
        <el-table-column label="操作" width="80" fixed="right"><template #default="{row}"><el-button link type="primary" size="small" @click="open(row)">详情</el-button></template></el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right"><EamsPagination :total="t" v-model:page="q.pageNum" v-model:size="q.pageSize" @change="fetch"/></div>
    </div>

    <el-dialog v-model="vis" title="维保详情" width="640px">
      <el-descriptions v-if="dt" :column="2" border>
        <el-descriptions-item label="报修编号" :span="2">{{dt.repairNo}}</el-descriptions-item>
        <el-descriptions-item label="资产编码">{{dt.assetCode}}</el-descriptions-item><el-descriptions-item label="资产名称">{{dt.assetName}}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{dt.applicantName}}</el-descriptions-item><el-descriptions-item label="故障类型">{{dt.faultType}}</el-descriptions-item>
        <el-descriptions-item label="紧急程度"><span :class="{'red':dt.urgency===1}">{{dt.urgency===1?'紧急':'普通'}}</span></el-descriptions-item>
        <el-descriptions-item label="联系电话">{{dt.contactPhone}}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{dt.faultDesc}}</el-descriptions-item>
        <el-descriptions-item v-if="dt.faultImages" label="故障图片" :span="2">
          <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div v-for="(s,i) in (dt.faultImages||'').split(',').filter(Boolean)" :key="i" style="text-align:center">
              <el-image :src="parseUrl(s).url" style="width:80px;height:80px;border-radius:4px" fit="cover" :preview-src-list="[parseUrl(s).url]"/>
              <div v-if="parseUrl(s).name" style="font-size:11px;color:#718096;margin-top:2px;max-width:80px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ parseUrl(s).name }}</div>
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="维修状态"><StatusTag :value="dt.repairStatus" type="repair"/></el-descriptions-item>
        <el-descriptions-item label="报修时间">{{dt.createTime}}</el-descriptions-item>
        <template v-if="dt.repairPerson">
          <el-descriptions-item label="维修人员">{{dt.repairPerson}}</el-descriptions-item><el-descriptions-item label="维修方式">{{dt.repairMethod}}</el-descriptions-item>
          <el-descriptions-item label="维修费用">{{dt.repairFee}}元</el-descriptions-item><el-descriptions-item label="开始日期">{{dt.startDate}}</el-descriptions-item>
          <el-descriptions-item label="完成日期">{{dt.finishDate||'-'}}</el-descriptions-item>
          <el-descriptions-item label="故障原因" :span="2">{{dt.faultReason}}</el-descriptions-item>
          <el-descriptions-item label="处理措施" :span="2">{{dt.solution}}</el-descriptions-item>
          <el-descriptions-item v-if="dt.repairFiles" label="维修附件" :span="2">
            <div style="display:flex;flex-direction:column;gap:4px">
              <div v-for="(s,i) in (dt.repairFiles||'').split(',').filter(Boolean)" :key="i" style="display:flex;align-items:center;gap:8px;padding:6px 10px;background:#F7FAFC;border:1px solid #E2E8F0;border-radius:6px">
                <el-image v-if="isRepairImage(parseUrl(s).url)" :src="parseUrl(s).url" style="width:40px;height:40px;border-radius:4px" fit="cover" :preview-src-list="[parseUrl(s).url]"/>
                <el-icon v-else :size="20" color="#718096"><Document/></el-icon>
                <span style="flex:1;font-size:13px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ parseUrl(s).name || parseUrl(s).url.split('/').pop() }}</span>
                <a :href="parseUrl(s).url" target="_blank" style="font-size:12px;color:#2B6CB0;white-space:nowrap">{{ isRepairImage(parseUrl(s).url) ? '预览' : '下载' }}</a>
              </div>
            </div>
          </el-descriptions-item>
        </template>
        <el-descriptions-item label="备注" :span="2">{{dt.remark||'-'}}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="vis=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted} from 'vue';import {ElMessage} from 'element-plus';import {Document} from '@element-plus/icons-vue';import {listRecords,getRecordDetail,exportRecords} from '@/api/repair';import {useAuthStore} from '@/store/auth'
const auth=useAuthStore();function has(p){return auth.permissions?.includes(p)}
function parseUrl(s){const i=s.indexOf('|');return{url:i>=0?s.substring(0,i):s,name:i>=0?s.substring(i+1):''}}
function isRepairImage(url){return /\.(jpg|jpeg|png|gif|bmp)$/i.test(url)}
const d=ref([]);const t=ref(0);const l=ref(false);const dr=ref([]);const types=['硬件故障','软件故障','网络故障','配件更换','其他']
const q=reactive({repairNo:'',assetName:'',faultType:'',repairStatus:null,beginDate:'',endDate:'',pageNum:1,pageSize:10})
async function fetch(){l.value=true;try{if(dr.value?.length===2){q.beginDate=dr.value[0];q.endDate=dr.value[1]}else{q.beginDate='';q.endDate=''}const r=await listRecords(q);d.value=r.data.list||[];t.value=r.data.total}catch(e){}finally{l.value=false}}
function resetQ(){q.repairNo='';q.assetName='';q.faultType='';q.repairStatus=null;dr.value=[];q.pageNum=1;fetch()}
const vis=ref(false);const dt=ref(null)
async function open(row){try{const r=await getRecordDetail(row.id);dt.value={...r.data.order,...r.data.record};vis.value=true}catch(e){}}
async function exp(){try{const r=await exportRecords(q);const u=window.URL.createObjectURL(new Blob([r.data]));const a=document.createElement('a');a.href=u;a.download='维保记录_'+new Date().toISOString().slice(0,10)+'.xlsx';a.click();window.URL.revokeObjectURL(u);ElMessage.success('导出成功')}catch(e){}}
onMounted(()=>{fetch()})
</script>
<style scoped>.red{color:#E53E3E;font-weight:600}</style>
