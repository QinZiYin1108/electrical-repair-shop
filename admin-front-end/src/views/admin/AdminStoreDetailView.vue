<template>
  <div class="store-detail-page">
    <el-card class="store-detail-card" shadow="never" v-loading="loading">
      <template v-if="store">
        <div class="back-row">
          <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> {{ isSuperAdmin ? '返回门店列表' : '返回首页' }}</el-button>
          <el-button v-if="canEdit()" type="primary" size="small" style="float:right" @click="editingInfo ? saveInfo() : startEditInfo()">
            {{ editingInfo ? '保存' : '编辑门店信息' }}
          </el-button>
        </div>

        <div class="store-avatar-row">
          <el-upload
            v-if="canEdit()"
            :show-file-list="false"
            accept="image/*"
            :http-request="handleLogoUpload"
            class="logo-upload"
          >
            <el-avatar :size="64" :src="store.logoImageUrl" class="store-logo-avatar">
              {{ store.name?.charAt(0) || '门' }}
            </el-avatar>
            <div class="logo-upload-tip">更换</div>
          </el-upload>
          <el-avatar v-else :size="64" :src="store.logoImageUrl" class="store-logo-avatar">
            {{ store.name?.charAt(0) || '门' }}
          </el-avatar>
          <div class="store-avatar-info">
            <div class="store-avatar-name">{{ store.name }}</div>
            <div class="store-avatar-id">ID: {{ store.id }}</div>
          </div>
        </div>

        <el-divider content-position="left">门店信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="门店名称">
            <el-input v-if="editingInfo" v-model="editForm.name" size="small" />
            <template v-else>{{ store.name }}</template>
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            <el-input v-if="editingInfo" v-model="editForm.contactPhone" size="small" />
            <template v-else>{{ store.contactPhone }}</template>
          </el-descriptions-item>
          <el-descriptions-item label="门店地址" :span="2">
            <template v-if="editingInfo">
              <div style="display:flex;gap:8px;align-items:center">
                <el-input v-model="editForm.address" size="small" style="flex:1" readonly placeholder="点击右侧按钮获取位置" />
                <el-button size="small" :loading="locating" @click="locateAndGeocode">获取当前位置</el-button>
              </div>
            </template>
            <template v-else>{{ store.address || '未设置' }}</template>
          </el-descriptions-item>
          <el-descriptions-item label="营业状态">
            <el-tag :type="businessStatusTag(store.businessStatus)" size="small">{{ businessStatusText(store.businessStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditStatusTag(store.auditStatus)" size="small">{{ auditStatusText(store.auditStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分">{{ store.rating != null ? store.rating.toFixed(1) : '暂无评分' }}</el-descriptions-item>
          <el-descriptions-item label="师傅数量">{{ store.technicianCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item v-if="store.description" label="门店介绍" :span="2">
            <el-input v-if="editingInfo" v-model="editForm.description" type="textarea" :rows="2" size="small" />
            <template v-else>{{ store.description }}</template>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatTimestamp(store.createdTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">门店管理员</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="管理员ID">{{ store.storeAdminId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ store.storeAdminName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ store.storeAdminPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ store.storeAdminEmail || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          <span>师傅绑定</span>
          <el-button v-if="canEdit()" style="margin-left:12px" @click="showInviteDialog = true">邀请师傅</el-button>
        </el-divider>
        <el-table v-if="bindings.length" :data="bindings" border size="small" style="max-width:600px">
          <el-table-column label="师傅" min-width="120">
            <template #default="{ row }">{{ row.technicianName || row.technicianId }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="bindStatusTag(row.status)" size="small">{{ bindStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ formatTimestamp(row.confirmedTime || row.invitedTime) }}</template>
          </el-table-column>
          <el-table-column v-if="canEdit()" label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 2" size="small" type="danger" link @click="handleDirectUnbind(row)">解绑</el-button>
              <el-button v-if="row.status === 3" size="small" type="success" link @click="handleApproveUnbind(row)">同意解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无绑定师傅" :image-size="40" />

        <el-dialog v-model="showInviteDialog" title="邀请师傅" width="450px">
          <el-input v-model="inviteKeyword" placeholder="搜索师傅姓名/手机号" clearable @keyup.enter="searchTechnicians" />
          <div style="margin-top:12px;max-height:300px;overflow-y:auto">
            <div v-for="tech in inviteCandidates" :key="tech.id" style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f0f0f0">
              <div>
                <div>{{ tech.username || tech.realName || tech.phone }}</div>
                <div style="font-size:12px;color:#909399">{{ tech.phone }}</div>
              </div>
              <el-button size="small" type="primary" @click="handleInvite(tech)">邀请</el-button>
            </div>
            <el-empty v-if="inviteSearched && !inviteCandidates.length" description="未找到可邀请的师傅" :image-size="30" />
          </div>
        </el-dialog>

        <el-divider content-position="left">
          <span>营业时间</span>
          <el-button v-if="!editingHours && canEdit()" style="margin-left:12px" @click="startEditHours">编辑</el-button>
          <template v-if="editingHours">
            <el-button type="primary" :loading="savingHours" style="margin-left:12px" @click="saveBusinessHours">保存</el-button>
            <el-button style="margin-left:4px" @click="cancelEditHours">取消</el-button>
          </template>
        </el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item v-for="(h, idx) in businessHours" :key="idx" :label="weekDayText(h.dayOfWeek)" label-class-name="hours-label">
            <template v-if="editingHours">
              <div class="hours-edit-row">
                <el-time-picker v-model="h._startTime" format="HH:mm" value-format="HH:mm:ss" class="hours-time-picker" />
                <span class="hours-sep">—</span>
                <el-time-picker v-model="h._endTime" format="HH:mm" value-format="HH:mm:ss" class="hours-time-picker" />
                <el-switch v-model="h.isAvailable" :active-value="1" :inactive-value="0" size="small" style="margin-left:8px" />
              </div>
            </template>
            <template v-else>
              <span :class="{ 'hours-off': !h.isAvailable }">
                {{ h.startTime ? h.startTime.substring(0,5) : '--:--' }} — {{ h.endTime ? h.endTime.substring(0,5) : '--:--' }}
              </span>
              <el-tag :type="h.isAvailable ? 'success' : 'info'" size="small" style="margin-left:12px">{{ h.isAvailable ? '营业' : '休息' }}</el-tag>
            </template>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="门店不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useAdminStore } from '../../stores/admin';
import request from '../../api/request';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();

const store = ref(null);
const loading = ref(false);
const businessHours = ref([]);
const editingHours = ref(false);
const savingHours = ref(false);
const hoursBackup = ref([]);
const editingInfo = ref(false);
const locating = ref(false);
const editForm = reactive({ name: '', contactPhone: '', address: '', description: '', latitude: null, longitude: null });

// 绑定
const bindings = ref([]);
const showInviteDialog = ref(false);
const inviteKeyword = ref('');
const inviteCandidates = ref([]);
const inviteSearched = ref(false);

const isSuperAdmin = computed(() => adminStore.adminRole === 1);
const isStoreAdmin = computed(() => adminStore.adminRole === 2);

onMounted(() => { fetchDetail(); });

function goBack() { router.push(isSuperAdmin.value ? '/admin/stores/list' : '/admin/dashboard'); }

function canEdit() { return isSuperAdmin.value || (isStoreAdmin.value && store.value && store.value.id === adminStore.storeId); }

async function handleLogoUpload(options) {
  const formData = new FormData();
  formData.append('file', options.file);
  try {
    const res = await request({
      url: `/admin/stores/${store.value.id}/logo`,
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    if (res.code === 200) {
      store.value.logoImageUrl = res.data + '?t=' + Date.now();
      ElMessage.success('Logo已更新');
    } else {
      ElMessage.error(res.message || '上传失败');
    }
  } catch (e) {
    ElMessage.error('上传失败');
  }
}

async function fetchDetail() {
  const id = route.params.id;
  if (!id) return;
  loading.value = true;
  try {
    const res = await request({ url: `/admin/stores/${id}`, method: 'get' });
    if (res.code === 200 && res.data) {
      store.value = res.data;
      const hours = res.data.businessHours || [];
      if (hours.length === 0) {
        for (let d = 1; d <= 7; d++) {
          hours.push({
            dayOfWeek: d,
            startTime: '09:00:00',
            endTime: '18:00:00',
            isAvailable: 1
          });
        }
      }
      businessHours.value = hours.map(h => ({
        ...h,
        _startTime: h.startTime || '09:00:00',
        _endTime: h.endTime || '18:00:00'
      }));
    }
    await loadBindings();
  } finally { loading.value = false; }
}

async function loadBindings() {
  try {
    const res = await request({ url: `/admin/stores/${store.value.id}/bindings`, method: 'get' });
    if (res.code === 200) bindings.value = res.data || [];
  } catch (e) { /* ignore */ }
}

async function searchTechnicians() {
  inviteSearched.value = true;
  try {
    const res = await request({
      url: '/admin/workers/stats/performance',
      method: 'get',
      params: { keyword: inviteKeyword.value, pageSize: 20 }
    });
    if (res.code === 200 && res.data) {
      inviteCandidates.value = (res.data.list || []).filter(t => !bindings.value.some(b => b.technicianId === t.id));
    }
  } catch (e) { inviteCandidates.value = []; }
}

async function handleInvite(tech) {
  try {
    await request({ url: `/admin/stores/${store.value.id}/invite/${tech.id}`, method: 'post' });
    ElMessage.success('已发送邀请');
    showInviteDialog.value = false;
    await loadBindings();
  } catch (e) { /* ignore */ }
}

async function handleDirectUnbind(row) {
  try {
    await ElMessageBox.confirm('确认直接解绑该师傅？', '提示', { type: 'warning' });
    await request({ url: `/admin/stores/${store.value.id}/unbind/${row.technicianId}`, method: 'post' });
    ElMessage.success('已解绑');
    await loadBindings();
  } catch (e) { /* ignore */ }
}

async function handleApproveUnbind(row) {
  try {
    await ElMessageBox.confirm('确认同意该师傅的解绑申请？', '提示', { type: 'warning' });
    await request({ url: `/admin/stores/${store.value.id}/approve-unbind/${row.technicianId}`, method: 'post' });
    ElMessage.success('已同意解绑');
    await loadBindings();
  } catch (e) { /* ignore */ }
}

function bindStatusTag(s) { return s === 1 ? 'warning' : s === 2 ? 'success' : s === 3 ? 'danger' : 'info'; }
function bindStatusText(s) { return s === 1 ? '待确认' : s === 2 ? '已绑定' : s === 3 ? '申请解绑' : '已解绑'; }

function startEditInfo() {
  editForm.name = store.value.name || '';
  editForm.contactPhone = store.value.contactPhone || '';
  editForm.address = store.value.address || '';
  editForm.description = store.value.description || '';
  editForm.latitude = store.value.latitude;
  editForm.longitude = store.value.longitude;
  editingInfo.value = true;
}

async function locateAndGeocode() {
  if (!navigator.geolocation) {
    ElMessage.warning('浏览器不支持定位');
    return;
  }
  locating.value = true;
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      editForm.latitude = lat;
      editForm.longitude = lng;
      try {
        const geoRes = await request({
          url: '/admin/stores/reverse-geocode',
          method: 'get',
          params: { latitude: lat, longitude: lng }
        });
        if (geoRes.code === 200 && geoRes.data && geoRes.data.address) {
          editForm.address = geoRes.data.address;
        } else {
          editForm.address = lat.toFixed(6) + ', ' + lng.toFixed(6);
        }
      } catch (e) {
        editForm.address = lat.toFixed(6) + ', ' + lng.toFixed(6);
      }
      locating.value = false;
    },
    () => {
      locating.value = false;
      ElMessage.warning('定位失败');
    },
    { enableHighAccuracy: true, timeout: 8000 }
  );
}

async function saveInfo() {
  try {
    const res = await request({
      url: `/admin/stores/${store.value.id}/update`,
      method: 'post',
      data: {
        name: editForm.name,
        contactPhone: editForm.contactPhone,
        address: editForm.address,
        latitude: editForm.latitude,
        longitude: editForm.longitude,
        description: editForm.description
      }
    });
    if (res.code === 200) {
      store.value.name = editForm.name;
      store.value.contactPhone = editForm.contactPhone;
      store.value.address = editForm.address;
      store.value.latitude = editForm.latitude;
      store.value.longitude = editForm.longitude;
      store.value.description = editForm.description;
      editingInfo.value = false;
      ElMessage.success('门店信息已保存');
    }
  } catch (e) { /* ignore */ }
}

function startEditHours() {
  hoursBackup.value = JSON.parse(JSON.stringify(businessHours.value));
  editingHours.value = true;
}
function cancelEditHours() {
  businessHours.value = hoursBackup.value;
  editingHours.value = false;
}
async function saveBusinessHours() {
  savingHours.value = true;
  try {
    const hours = businessHours.value.map(h => ({
      dayOfWeek: h.dayOfWeek,
      startTime: typeof h._startTime === 'string' ? h._startTime : formatTime(h._startTime),
      endTime: typeof h._endTime === 'string' ? h._endTime : formatTime(h._endTime),
      isAvailable: h.isAvailable
    }));
    await request({ url: `/admin/stores/${store.value.id}/business-hours`, method: 'post', data: { hours } });
    ElMessage.success('营业时间保存成功');
    editingHours.value = false;
  } finally { savingHours.value = false; }
}

function formatTime(date) {
  if (!date) return '00:00:00';
  if (typeof date === 'string') return date;
  return [date.getHours(), date.getMinutes(), 0].map(v => String(v).padStart(2, '0')).join(':');
}

function businessStatusTag(s) { return s === 1 ? 'success' : s === 2 ? 'warning' : 'info'; }
function businessStatusText(s) { return s === 1 ? '营业中' : s === 2 ? '休息中' : '已关闭'; }
function auditStatusTag(s) { return s === 1 ? 'warning' : s === 2 ? 'success' : 'danger'; }
function auditStatusText(s) { return s === 1 ? '待审核' : s === 2 ? '通过' : '拒绝'; }
function weekDayText(d) { return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][d] || ''; }
function formatTimestamp(ts) {
  if (!ts) return '-';
  const d = new Date(ts);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style scoped>
.store-detail-page { padding: 16px; box-sizing: border-box; }
.store-detail-card { width: 100%; }
.back-row { margin-bottom: 8px; overflow: hidden; }
.store-avatar-row { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.store-logo-avatar { border: 2px solid #e4e7ed; cursor: default; }
.logo-upload { position: relative; cursor: pointer; }
.logo-upload :deep(.el-upload) { display: block; }
.logo-upload-tip { position: absolute; bottom: 0; left: 0; right: 0; text-align: center; font-size: 10px; color: #fff; background: rgba(0,0,0,.5); border-radius: 0 0 50% 50%; padding: 2px 0; opacity: 0; transition: opacity .2s; }
.logo-upload:hover .logo-upload-tip { opacity: 1; }
.logo-upload:hover .store-logo-avatar { border-color: #409eff; }
.store-avatar-info { display: flex; flex-direction: column; }
.store-avatar-name { font-size: 16px; font-weight: 600; color: #303133; }
.store-avatar-id { font-size: 12px; color: #909399; margin-top: 2px; }

.hours-edit-row { display: flex; align-items: center; gap: 8px; }
.hours-time-picker { width: 130px; }
.hours-sep { color: #c0c4cc; margin: 0 4px; }
.hours-off { color: #c0c4cc; }
:deep(.hours-label) { width: 80px; text-align: center; font-weight: 500; }
</style>
