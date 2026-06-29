<template>
  <div>
    <div class="page-header"><h2>报修登记</h2><div class="page-actions"><el-button @click="$router.push('/repair/record')">维保记录</el-button></div></div>

    <el-steps :active="step" align-center style="margin-bottom:24px">
      <el-step title="选择资产" /><el-step title="填写报修信息" /><el-step title="提交完成" />
    </el-steps>

    <!-- 步骤1：选择资产 -->
    <el-card v-if="step===0">
      <div class="search-card">
        <el-form :model="q" inline>
          <el-form-item label="资产名称"><el-input v-model="q.assetName" clearable style="width:160px"/></el-form-item>
          <el-form-item label="资产编码"><el-input v-model="q.assetCode" clearable style="width:160px"/></el-form-item>
          <el-form-item label="资产分类"><DictSelect dictCode="asset_category" v-model="q.category" style="width:160px"/></el-form-item>
          <el-form-item class="search-buttons"><el-button type="primary" @click="load">查询</el-button><el-button @click="resetQ">重置</el-button></el-form-item>
        </el-form>
      </div>
      <el-empty v-if="!assets.length" description="当前没有可报修的资产"/>
      <el-row v-else :gutter="16">
        <el-col v-for="a in assets" :key="a.id" :span="8" style="margin-bottom:16px">
          <el-card shadow="hover" :class="['asset-card',{'is-selected':sel?.id===a.id}]" @click="sel=sel?.id===a.id?null:a">
            <div class="asset-card-body">
              <el-image v-if="a.imageUrl" :src="a.imageUrl" style="width:60px;height:60px;border-radius:6px" fit="cover"/>
              <div v-else class="ph"><el-icon :size="28"><Box/></el-icon></div>
              <div class="info"><div class="n">{{a.assetName}}</div><div class="c">{{a.assetCode}}</div><div class="m"><StatusTag :value="a.status" type="asset"/> {{a.specification||'-'}} · {{a.deptName||'-'}}</div></div>
              <el-icon v-if="sel?.id===a.id" class="chk" color="#2B6CB0" :size="24"><CircleCheckFilled/></el-icon>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <div style="text-align:center;margin-top:24px"><el-button type="primary" :disabled="!sel" @click="step=1">下一步</el-button></div>
    </el-card>

    <!-- 步骤2：填写报修信息 -->
    <el-card v-if="step===1">
      <el-form ref="fr" :model="f" :rules="r" label-width="110px" style="max-width:700px">
        <el-form-item label="报修资产"><div style="display:flex;align-items:center;gap:8px"><span>{{sel?.assetCode}} {{sel?.assetName}}</span><StatusTag :value="sel?.status" type="asset"/></div></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="故障类型" prop="faultType"><el-select v-model="f.faultType" style="width:100%"><el-option v-for="t in types" :key="t" :label="t" :value="t"/></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="紧急程度" prop="urgency"><el-radio-group v-model="f.urgency"><el-radio :value="0">普通</el-radio><el-radio :value="1">紧急</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-form-item label="故障描述" prop="faultDesc"><el-input v-model="f.faultDesc" type="textarea" :rows="3" placeholder="10-500字符"/></el-form-item>
        <el-form-item label="故障图片">
          <el-upload :action="uploadUrl" :headers="{Authorization:'Bearer '+token}" :limit="3" list-type="picture-card"
            :file-list="imgList" :on-success="onImgSuccess" :on-remove="onImgRemove" accept=".jpg,.jpeg,.png" :before-upload="beforeImg">
            <el-icon><Plus/></el-icon>
            <template #tip><div style="font-size:12px;color:#A0AEC0">仅支持 JPG/PNG，大小不超过2MB，最多3张</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone"><el-input v-model="f.contactPhone" placeholder="11位手机号"/></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="f.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <div style="text-align:center;margin-top:24px"><el-button @click="step=0">上一步</el-button><el-button type="primary" :loading="sub" @click="submit">确认提交</el-button></div>
    </el-card>

    <!-- 步骤3：完成 -->
    <el-card v-if="step===2"><el-result icon="success" title="报修申请已提交" sub-title="维修人员将尽快处理"><template #extra><el-button type="primary" @click="$router.push('/repair/record')">查看维保记录</el-button><el-button @click="reset">继续报修</el-button></template></el-result></el-card>
  </div>
</template>

<script setup>
import {ref,reactive,onMounted,computed} from 'vue';import {ElMessage} from 'element-plus';import {CircleCheckFilled,Plus} from '@element-plus/icons-vue';import {listAssets} from '@/api/asset';import {applyRepair} from '@/api/repair';import {useAuthStore} from '@/store/auth'
const auth=useAuthStore();const token=computed(()=>auth.token);const uploadUrl='/api/file/upload'
const step=ref(0);const assets=ref([]);const sel=ref(null);const q=reactive({assetName:'',assetCode:'',category:''});const types=['硬件故障','软件故障','网络故障','配件更换','其他']
async function load(){try{const p={...q,pageNum:1,pageSize:200};const res=await listAssets(p);assets.value=(res.data.list||[]).filter(a=>a.status!==3&&a.status!==4)}catch(e){}}
function resetQ(){q.assetName='';q.assetCode='';q.category='';load()}
const fr=ref(null);const sub=ref(false);const imgList=ref([])
const f=reactive({faultType:'硬件故障',urgency:0,faultDesc:'',contactPhone:'',remark:''})
const r={faultType:[{required:true,message:'请选择故障类型',trigger:'change'}],faultDesc:[{required:true,message:'故障描述为10-500个字符',trigger:'blur'},{min:10,max:500,message:'故障描述为10-500个字符',trigger:'blur'}],contactPhone:[{required:true,pattern:/^1[3-9]\d{9}$/,message:'请输入正确的联系电话',trigger:'blur'}]}
function beforeImg(file){const isJPG=file.type==='image/jpeg'||file.type==='image/png';const isLt2M=file.size/1024/1024<2;if(!isJPG){ElMessage.error('仅支持JPG/PNG格式');return false}if(!isLt2M){ElMessage.error('大小不超过2MB');return false}return true}
function onImgSuccess(res,file){if(res.code===200){if(!f.faultImages)f.faultImages='';f.faultImages+=(f.faultImages?',':'')+res.data+'|'+file.name;imgList.value.push({name:file.name,url:res.data})}}
function onImgRemove(file){f.faultImages=f.faultImages.split(',').filter(u=>u!==file.url).join(',');imgList.value=imgList.value.filter(i=>i.url!==file.url)}
async function submit(){const v=await fr.value.validate().catch(()=>false);if(!v)return;sub.value=true;try{await applyRepair({assetId:sel.value.id,...f});ElMessage.success('报修申请已提交，维修人员将尽快处理');step.value=2}catch(e){}finally{sub.value=false}}
function reset(){step.value=0;sel.value=null;imgList.value=[];Object.assign(f,{faultType:'硬件故障',urgency:0,faultDesc:'',faultImages:'',contactPhone:'',remark:''});load()}
onMounted(()=>{load()})
</script>

<style scoped>
.asset-card{cursor:pointer;border:2px solid transparent;transition:all .2s}.asset-card.is-selected{border-color:#2B6CB0;background:#EBF4FF}.asset-card-body{display:flex;align-items:center;gap:12px;position:relative}.ph{width:60px;height:60px;border-radius:6px;background:#F7FAFC;display:flex;align-items:center;justify-content:center;color:#CBD5E0}.info{flex:1;min-width:0}.n{font-size:15px;font-weight:600;color:#1A202C}.c{font-size:12px;color:#718096;margin-top:2px}.m{font-size:12px;color:#A0AEC0;margin-top:2px}.chk{position:absolute;top:0;right:0}
</style>
