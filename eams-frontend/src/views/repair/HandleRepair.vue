<template>
  <div>
    <div class="page-header"><h2>维修处理</h2></div>
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
        <el-table-column label="故障类型" width="100"><template #default="{row}"><el-tag size="small">{{row.faultType}}</el-tag></template></el-table-column>
        <el-table-column label="紧急程度" width="80" align="center"><template #default="{row}"><span :class="{'red':row.urgency===1}">{{row.urgency===1?'紧急':'普通'}}</span></template></el-table-column>
        <el-table-column prop="faultDesc" label="故障描述" min-width="160" show-overflow-tooltip/>
        <el-table-column label="状态" width="100" align="center"><template #default="{row}"><StatusTag :value="row.repairStatus" type="repair"/></template></el-table-column>
        <el-table-column prop="createTime" label="报修时间" width="160"/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <div class="ab">
              <el-button v-if="row.repairStatus===0" link type="primary" size="small" @click="openDialog(row,'accept')">接单</el-button>
              <el-button v-if="row.repairStatus===1" link type="success" size="small" @click="openDialog(row,'complete')">已修复</el-button>
              <el-button v-if="row.repairStatus===1" link type="danger" size="small" @click="openDialog(row,'unfixable')">无法修复</el-button>
              <span v-if="row.repairStatus>=2" style="color:#A0AEC0;font-size:12px">已处理</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right"><EamsPagination :total="t" v-model:page="q.pageNum" v-model:size="q.pageSize" @change="fetch"/></div>
    </div>

    <!-- 维修处理弹窗 -->
    <el-dialog v-model="vis" :title="title" width="700px" :close-on-click-modal="false">
      <!-- 故障信息区（PRD 6.8.2：接单前查看故障详情） -->
      <el-descriptions v-if="cur" :column="3" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="报修编号">{{ cur.repairNo }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ cur.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ cur.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="故障类型">
          <el-tag size="small">{{ cur.faultType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <span :class="{'red':cur.urgency===1}">{{ cur.urgency===1?'紧急':'普通' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="资产">{{ cur.assetName }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="3">{{ cur.faultDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="cur.faultImages" label="故障图片" :span="3">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-image v-for="(s,i) in (cur.faultImages||'').split(',').filter(Boolean)" :key="i"
              :src="imgUrl(s)" style="width:80px;height:80px;border-radius:4px" fit="cover" :preview-src-list="[imgUrl(s)]"/>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <el-form ref="fr" :model="fd" :rules="hdr" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="维修方式" prop="repairMethod"><el-select v-model="fd.repairMethod" style="width:100%"><el-option v-for="m in methods" :key="m" :label="m" :value="m"/></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="维修人员" prop="repairPerson"><el-input v-model="fd.repairPerson"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="维修费用(元)" prop="repairFee"><el-input-number v-model="fd.repairFee" :min="0" :precision="2" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="开始日期" prop="startDate"><el-date-picker v-model="fd.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item></el-col>
        </el-row>
        <el-form-item v-if="act==='complete'" label="修复日期" prop="finishDate"><el-date-picker v-model="fd.finishDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="故障原因" prop="faultReason"><el-input v-model="fd.faultReason" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="处理措施" prop="solution"><el-input v-model="fd.solution" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="附件">
          <div>
            <el-upload drag :action="uploadUrl" :headers="{Authorization:'Bearer '+token}" :limit="5"
              :on-success="onFileSuccess" :on-remove="onFileRemove"
              :before-upload="beforeFile" accept=".jpg,.jpeg,.png,.gif,.bmp,.pdf,.doc,.docx,.xls,.xlsx">
              <el-icon class="el-icon--upload"><UploadFilled/></el-icon>
              <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
              <template #tip><div style="font-size:12px;color:#A0AEC0">支持 jpg/png/pdf/doc/xls，单文件≤10MB</div></template>
            </el-upload>
            <!-- 已上传文件列表 -->
            <div v-if="uploaded.length" style="margin-top:8px;display:flex;flex-direction:column;gap:4px">
              <div v-for="(f,i) in uploaded" :key="i" style="display:flex;align-items:center;justify-content:space-between;padding:8px 12px;background:#F7FAFC;border:1px solid #E2E8F0;border-radius:6px">
                <div style="display:flex;align-items:center;gap:8px;min-width:0">
                  <el-image v-if="isImage(f.url)" :src="f.url" style="width:36px;height:36px;border-radius:4px" fit="cover" :preview-src-list="[f.url]"/>
                  <el-icon v-else :size="20" color="#718096"><Document/></el-icon>
                  <span v-if="isImage(f.url)" style="font-size:13px;color:#4A5568;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;cursor:pointer" @click="previewFile(f)">{{ f.name }}</span>
                  <a v-else :href="f.url" target="_blank" style="font-size:13px;color:#2B6CB0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ f.name }}</a>
                </div>
                <el-button link type="danger" size="small" @click="rmFile(i)">删除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="fd.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="vis=false">取消</el-button><el-button type="primary" :loading="sb" @click="handle">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted,computed} from 'vue';import {ElMessage,ElMessageBox} from 'element-plus';import {UploadFilled,Document} from '@element-plus/icons-vue';import {listHandle,acceptRepair,completeRepair,unfixableRepair} from '@/api/repair';import {useAuthStore} from '@/store/auth'
const auth=useAuthStore();const token=computed(()=>auth.token);const uploadUrl='/api/file/upload';const uploaded=ref([])
function imgUrl(s){const i=s.indexOf('|');return i>=0?s.substring(0,i):s}
function isImage(f){return /\.(jpg|jpeg|png|gif|bmp)$/i.test(f)}
function beforeFile(file){const lt10=file.size/1024/1024<10;if(!lt10){ElMessage.error('文件大小不能超过10MB');return false}return true}
// 存储格式: url|原始文件名 逗号分隔
function encodeFile(url,name){return url+'|'+name}
function decodeFile(s){if(!s)return{url:'',name:''};const i=s.indexOf('|');return{url:s.substring(0,i>=0?i:s.length),name:i>=0?s.substring(i+1):s.split('/').pop()}}
function onFileSuccess(res,file){if(res.code===200){if(!fd.repairFiles)fd.repairFiles='';fd.repairFiles+=(fd.repairFiles?',':'')+encodeFile(res.data,file.name);uploaded.value.push({url:res.data,name:file.name})}}
function rmFile(i){fd.repairFiles=fd.repairFiles.split(',').filter(s=>!s.startsWith(uploaded.value[i].url)).join(',');uploaded.value.splice(i,1)}
function previewFile(f){const w=window.open('','_blank');w.document.write('<img src="'+f.url+'" style="max-width:100%"/>')}
const d=ref([]);const t=ref(0);const l=ref(false);const dr=ref([]);const q=reactive({repairNo:'',assetName:'',faultType:'',repairStatus:null,beginDate:'',endDate:'',pageNum:1,pageSize:10})
const types=['硬件故障','软件故障','网络故障','配件更换','其他'];const methods=['现场维修','送修','上门维修','远程支持']
async function fetch(){l.value=true;try{if(dr.value?.length===2){q.beginDate=dr.value[0];q.endDate=dr.value[1]}else{q.beginDate='';q.endDate=''}const r=await listHandle(q);d.value=r.data.list||[];t.value=r.data.total}catch(e){}finally{l.value=false}}
function resetQ(){q.repairNo='';q.assetName='';q.faultType='';q.repairStatus=null;dr.value=[];q.pageNum=1;fetch()}
const vis=ref(false);const sb=ref(false);const fr=ref(null);const act=ref('');const cur=ref(null)
const title=computed(()=>act.value==='accept'?'接单':act.value==='complete'?'标记已修复':'标记无法修复')
const fd=reactive({repairOrderId:null,action:'',repairMethod:'现场维修',repairPerson:'',repairFee:0,startDate:'',finishDate:'',faultReason:'',solution:'',repairFiles:'',remark:''})
const hdr={repairMethod:[{required:true,message:'请选择维修方式',trigger:'change'}],repairPerson:[{required:true,message:'请填写维修人员姓名',trigger:'blur'},{min:2,max:20,message:'请填写维修人员姓名',trigger:'blur'}],repairFee:[{required:true,type:'number',min:0,message:'维修费用不能为负数',trigger:'blur'}],startDate:[{required:true,message:'请选择开始维修日期',trigger:'change'}],faultReason:[{required:true,message:'故障原因为10-500个字符',trigger:'blur'},{min:10,max:500,message:'故障原因为10-500个字符',trigger:'blur'}],solution:[{required:true,message:'处理措施为10-500个字符',trigger:'blur'},{min:10,max:500,message:'处理措施为10-500个字符',trigger:'blur'}]}
function openDialog(row,a){act.value=a;cur.value=row;fd.repairOrderId=row.id;fd.action=a;fd.repairMethod=row.repairMethod||'现场维修';fd.repairPerson=row.repairPerson||'';fd.repairFee=row.repairFee||0;fd.startDate=row.startDate||'';fd.finishDate=row.finishDate||'';fd.faultReason=row.faultReason||'';fd.solution=row.solution||'';fd.repairFiles=row.repairFiles||'';fd.remark='';uploaded.value=(row.repairFiles||'').split(',').filter(Boolean).map(s=>{const f=decodeFile(s);return{url:f.url,name:f.name}});vis.value=true}
async function handle(){const v=await fr.value.validate().catch(()=>false);if(!v)return
  const msgs={accept:'',complete:'确认标记资产【'+cur.value.assetName+'】已修复？资产状态将恢复',unfixable:'确认标记资产【'+cur.value.assetName+'】无法修复？建议走报废流程'}
  if(msgs[act.value]){try{await ElMessageBox.confirm(msgs[act.value],'确认操作',{type:'warning'})}catch(e){return}}
  sb.value=true;try{
    if(act.value==='accept'){await acceptRepair(fd);ElMessage.success('已接单，开始维修')}
    else if(act.value==='complete'){await completeRepair(fd);ElMessage.success('维修完成，资产已恢复可用')}
    else{await unfixableRepair(fd);ElMessage.success('已标记为无法修复，请评估是否报废')}
    vis.value=false;fetch()
  }catch(e){}finally{sb.value=false}
}
onMounted(()=>{fetch()})
</script>
<style scoped>.ab{display:flex;flex-wrap:nowrap;gap:2px}.ab .el-button{padding:0 6px;white-space:nowrap}.red{color:#E53E3E;font-weight:600}</style>
