<template>
  <div class="store-detail-page">
    <el-card class="store-detail-card" shadow="never" v-loading="loading">
      <template v-if="store">
        <!-- 返回 -->
        <div class="back-row">
          <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回门店列表</el-button>
        </div>

        <el-divider content-position="left">门店信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="门店名称">{{ store.name }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ store.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="门店地址" :span="2">{{ store.address || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="经纬度" :span="2">
            {{ store.latitude != null ? store.latitude.toFixed(6) + ', ' + store.longitude.toFixed(6) : '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="营业状态">
            <el-tag :type="businessStatusTag(store.businessStatus)" size="small">{{ businessStatusText(store.businessStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditStatusTag(store.auditStatus)" size="small">{{ auditStatusText(store.auditStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分">{{ store.rating != null ? store.rating.toFixed(1) : '暂无评分' }}</el-descriptions-item>
          <el-descriptions-item label="师傅数量">{{ store.technicianCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item v-if="store.description" label="门店介绍" :span="2">{{ store.description }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatTimestamp(store.createdTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">门店管理员</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="管理员ID">{{ store.storeAdminId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ store.storeAdminName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ store.storeAdminPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ store.storeAdminEmail || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">营业时间</el-divider>
        <el-table :data="businessHours" border size="small" class="hours-table">
          <el-table-column label="星期" width="120">
            <template #default="{ row }">{{ weekDayText(row.dayOfWeek) }}</template>
          </el-table-column>
          <el-table-column label="开始时间" width="160">
            <template #default="{ row }">
              <el-time-picker v-if="editingHours" v-model="row._startTime" format="HH:mm" value-format="HH:mm:ss" size="small" style="width:130px" />
              <span v-else>{{ row.startTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template #default="{ row }">
              <el-time-picker v-if="editingHours" v-model="row._endTime" format="HH:mm" value-format="HH:mm:ss" size="small" style="width:130px" />
              <span v-else>{{ row.endTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="营业" width="80" align="center">
            <template #default="{ row }">
              <el-switch v-if="editingHours" v-model="row.isAvailable" :active-value="1" :inactive-value="0" size="small" />
              <el-tag v-else :type="row.isAvailable ? 'success' : 'info'" size="small">{{ row.isAvailable ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 12px">
          <el-button v-if="!editingHours && canEdit()" size="small" @click="startEditHours">编辑营业时间</el-button>
          <template v-if="editingHours">
            <el-button size="small" type="primary" :loading="savingHours" @click="saveBusinessHours">保存</el-button>
            <el-button size="small" @click="cancelEditHours">取消</el-button>
          </template>
        </div>
      </template>
      <el-empty v-else description="门店不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
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

const isSuperAdmin = computed(() => adminStore.adminRole === 1);
const isStoreAdmin = computed(() => adminStore.adminRole === 2);

onMounted(() => { fetchDetail(); });

function goBack() { router.push('/admin/stores/list'); }

function canEdit() {
  return isSuperAdmin.value || (isStoreAdmin.value && store.value && store.value.id === adminStore.storeId);
}

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
.back-row { margin-bottom: 8px; }
.hours-table { max-width: 600px; }
</style>
