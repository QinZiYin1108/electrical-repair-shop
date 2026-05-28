<template>
  <div class="store-detail-page">
    <el-card class="store-detail-card" shadow="never" v-loading="loading">
      <template v-if="store">
        <div class="back-row">
          <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回门店列表</el-button>
          <el-button v-if="canEdit()" type="primary" size="small" style="float:right" @click="editingInfo ? saveInfo() : startEditInfo()">
            {{ editingInfo ? '保存' : '编辑门店信息' }}
          </el-button>
        </div>

        <div class="store-avatar-row">
          <el-avatar :size="64" :src="store.logoImageUrl" shape="square" style="border:1px solid #e4e7ed">
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
          <span>营业时间</span>
          <el-button v-if="!editingHours && canEdit()" size="small" style="margin-left:12px" @click="startEditHours">编辑</el-button>
          <template v-if="editingHours">
            <el-button size="small" type="primary" :loading="savingHours" style="margin-left:12px" @click="saveBusinessHours">保存</el-button>
            <el-button size="small" style="margin-left:4px" @click="cancelEditHours">取消</el-button>
          </template>
        </el-divider>
        <div class="hours-grid">
          <div v-for="(h, idx) in businessHours" :key="idx" class="hours-row" :class="{ 'hours-row-off': !h.isAvailable && !editingHours }">
            <span class="hours-day">{{ weekDayText(h.dayOfWeek) }}</span>
            <template v-if="editingHours">
              <el-time-picker v-model="h._startTime" format="HH:mm" value-format="HH:mm:ss" size="small" style="width:120px" />
              <span class="hours-sep">—</span>
              <el-time-picker v-model="h._endTime" format="HH:mm" value-format="HH:mm:ss" size="small" style="width:120px" />
              <el-switch v-model="h.isAvailable" :active-value="1" :inactive-value="0" size="small" style="margin-left:12px" />
            </template>
            <template v-else>
              <span class="hours-time">{{ h.startTime ? h.startTime.substring(0,5) : '--:--' }} — {{ h.endTime ? h.endTime.substring(0,5) : '--:--' }}</span>
              <el-tag :type="h.isAvailable ? 'success' : 'info'" size="small" class="hours-tag">{{ h.isAvailable ? '营业' : '休息' }}</el-tag>
            </template>
          </div>
        </div>
      </template>
      <el-empty v-else description="门店不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
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

const isSuperAdmin = computed(() => adminStore.adminRole === 1);
const isStoreAdmin = computed(() => adminStore.adminRole === 2);

onMounted(() => { fetchDetail(); });

function goBack() { router.push('/admin/stores/list'); }

function canEdit() { return isSuperAdmin.value || (isStoreAdmin.value && store.value && store.value.id === adminStore.storeId); }

async function fetchDetail() {
  const id = route.params.id;
  if (!id) return;
  loading.value = true;
  try {
    const res = await request({ url: `/admin/stores/${id}`, method: 'get' });
    if (res.code === 200 && res.data) {
      store.value = res.data;
      businessHours.value = (res.data.businessHours || []).map(h => ({
        ...h,
        _startTime: h.startTime ? new Date('2000-01-01 ' + h.startTime) : null,
        _endTime: h.endTime ? new Date('2000-01-01 ' + h.endTime) : null
      }));
    }
  } finally { loading.value = false; }
}

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
      startTime: formatTime(h._startTime),
      endTime: formatTime(h._endTime),
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
.store-avatar-info { display: flex; flex-direction: column; }
.store-avatar-name { font-size: 16px; font-weight: 600; color: #303133; }
.store-avatar-id { font-size: 12px; color: #909399; margin-top: 2px; }

.hours-grid { max-width: 520px; }
.hours-row { display: flex; align-items: center; padding: 10px 12px; border-bottom: 1px solid #f0f0f0; gap: 10px; }
.hours-row:last-child { border-bottom: none; }
.hours-row-off { opacity: 0.5; }
.hours-day { width: 48px; font-size: 14px; font-weight: 500; color: #303133; flex-shrink: 0; }
.hours-time { font-size: 14px; color: #606266; }
.hours-sep { color: #c0c4cc; }
.hours-tag { margin-left: auto; }
</style>
