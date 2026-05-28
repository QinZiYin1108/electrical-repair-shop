<template>
  <div class="store-page">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <el-button v-if="isSuperAdmin" type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 创建门店
      </el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="searchKeyword" placeholder="搜索门店名称" clearable style="width: 240px" @keyup.enter="fetchList" />
      <el-select v-model="filterAuditStatus" placeholder="审核状态" clearable style="width: 140px; margin-left: 12px">
        <el-option label="待审核" :value="1" />
        <el-option label="审核通过" :value="2" />
        <el-option label="审核拒绝" :value="3" />
      </el-select>
      <el-select v-model="filterBusinessStatus" placeholder="营业状态" clearable style="width: 140px; margin-left: 12px">
        <el-option label="营业中" :value="1" />
        <el-option label="休息中" :value="2" />
        <el-option label="已关闭" :value="3" />
      </el-select>
      <el-button type="default" style="margin-left: 12px" @click="fetchList">查询</el-button>
    </div>

    <el-table :data="storeList" v-loading="loading" border stripe style="margin-top: 16px">
      <el-table-column prop="name" label="门店名称" min-width="160" />
      <el-table-column label="Logo" width="80">
        <template #default="{ row }">
          <el-avatar v-if="row.logoImageUrl" :src="row.logoImageUrl" size="small" />
          <el-avatar v-else size="small">{{ row.name?.charAt(0) || '门' }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="address" label="门店地址" min-width="200" show-overflow-tooltip />
      <el-table-column label="师傅数" width="80" align="center">
        <template #default="{ row }">{{ row.technicianCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="营业状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="businessStatusTag(row.businessStatus)" size="small">{{ businessStatusText(row.businessStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="auditStatusTag(row.auditStatus)" size="small">{{ auditStatusText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="80" align="center">
        <template #default="{ row }">{{ row.rating != null ? row.rating.toFixed(1) : '暂无' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="isSuperAdmin && row.auditStatus === 1" size="small" type="success" @click="auditStore(row, 2)">通过</el-button>
          <el-button v-if="isSuperAdmin && row.auditStatus === 1" size="small" type="danger" @click="auditStore(row, 3)">拒绝</el-button>
          <el-button v-if="canToggleStatus()" size="small" @click="toggleStatus(row)">
            {{ row.businessStatus === 1 ? '休息' : row.businessStatus === 2 ? '营业' : '开启' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="fetchList" @size-change="fetchList"
    />

    <!-- ==================== 创建门店对话框（含百度地图选址） ==================== -->
    <el-dialog v-model="showCreateDialog" title="创建门店" width="500px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-divider content-position="left">门店信息</el-divider>
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="createForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>

        <!-- 门店地址（选填：创建后可让门店管理员在师傅端设置） -->
        <el-form-item label="门店地址" prop="address">
          <div style="display:flex; gap:8px; width:100%">
            <el-button @click="locateCurrentPosition" :loading="locating">
              <el-icon><Aim /></el-icon> 获取当前位置
            </el-button>
            <span v-if="createForm.address" style="font-size:13px; color:#67c23a; line-height:32px">
              {{ createForm.address }}
            </span>
            <span v-else style="font-size:12px; color:#909399; line-height:32px">
              点击按钮获取当前浏览器位置
            </span>
          </div>
        </el-form-item>
        <el-form-item label="门店介绍">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="请输入门店介绍（选填）" />
        </el-form-item>

        <el-divider content-position="left">门店管理员</el-divider>
        <el-form-item label="管理员姓名" prop="adminName">
          <el-input v-model="createForm.adminName" placeholder="请输入管理员姓名" />
        </el-form-item>
        <el-form-item label="管理员手机" prop="adminPhone">
          <el-input v-model="createForm.adminPhone" placeholder="请输入管理员手机号" />
        </el-form-item>
        <el-form-item label="管理员邮箱" prop="adminEmail">
          <el-input v-model="createForm.adminEmail" placeholder="请输入管理员登录邮箱" />
        </el-form-item>
        <el-form-item label="登录密码" prop="adminPassword">
          <el-input v-model="createForm.adminPassword" type="password" placeholder="请输入管理员登录密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 门店详情/营业时间 -->
    <el-dialog v-model="showDetailDialog" title="门店详情" width="700px" destroy-on-close>
      <template v-if="detailStore">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="门店名称">{{ detailStore.name }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailStore.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="门店地址" :span="2">{{ detailStore.address }}</el-descriptions-item>
          <el-descriptions-item label="经纬度" :span="2">
            {{ detailStore.latitude != null ? detailStore.latitude.toFixed(6) + ', ' + detailStore.longitude.toFixed(6) : '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="师傅数量">{{ detailStore.technicianCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ detailStore.rating != null ? detailStore.rating.toFixed(1) : '暂无评分' }}</el-descriptions-item>
          <el-descriptions-item label="营业状态">
            <el-tag :type="businessStatusTag(detailStore.businessStatus)" size="small">{{ businessStatusText(detailStore.businessStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditStatusTag(detailStore.auditStatus)" size="small">{{ auditStatusText(detailStore.auditStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailStore.description" label="门店介绍" :span="2">{{ detailStore.description }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">营业时间</el-divider>
        <el-table :data="businessHours" border size="small">
          <el-table-column label="星期" width="120">
            <template #default="{ row }">{{ weekDayText(row.dayOfWeek) }}</template>
          </el-table-column>
          <el-table-column label="开始时间" width="140">
            <template #default="{ row }">
              <el-time-picker v-if="editingHours" v-model="row._startTime" format="HH:mm" value-format="HH:mm:ss" size="small" style="width:120px" />
              <span v-else>{{ row.startTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="140">
            <template #default="{ row }">
              <el-time-picker v-if="editingHours" v-model="row._endTime" format="HH:mm" value-format="HH:mm:ss" size="small" style="width:120px" />
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
          <el-button v-if="!editingHours && canToggleStatus()" size="small" @click="startEditHours">编辑营业时间</el-button>
          <template v-if="editingHours">
            <el-button size="small" type="primary" :loading="savingHours" @click="saveBusinessHours">保存</el-button>
            <el-button size="small" @click="cancelEditHours">取消</el-button>
          </template>
        </div>
      </template>
    </el-dialog>

    <!-- 审核拒绝理由 -->
    <el-dialog v-model="showAuditDialog" title="审核拒绝" width="400px">
      <el-input v-model="auditRemark" type="textarea" :rows="3" placeholder="请输入拒绝理由" />
      <template #footer>
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmAuditReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Aim } from '@element-plus/icons-vue';
import { useAdminStore } from '../../stores/admin';
import request from '../../api/request';

const adminStore = useAdminStore();
const isSuperAdmin = computed(() => adminStore.adminRole === 1);
const isStoreAdmin = computed(() => adminStore.adminRole === 2);
const pageTitle = computed(() => isSuperAdmin.value ? '门店管理' : '我的门店');

// 列表
const loading = ref(false);
const storeList = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKeyword = ref('');
const filterAuditStatus = ref(null);
const filterBusinessStatus = ref(null);

// 创建
const showCreateDialog = ref(false);
const creating = ref(false);
const createFormRef = ref();
const createForm = reactive({
  name: '', contactPhone: '', address: '', description: '',
  latitude: null, longitude: null,
  adminName: '', adminPhone: '', adminEmail: '', adminPassword: ''
});
const createRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [],
  adminName: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  adminPhone: [{ required: true, message: '请输入管理员手机号', trigger: 'blur' }],
  adminEmail: [{ required: true, message: '请输入管理员邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  adminPassword: [{ required: true, message: '请输入管理员登录密码', trigger: 'blur' }]
};

// 定位
const locating = ref(false);

// 详情
const showDetailDialog = ref(false);
const detailStore = ref(null);
const businessHours = ref([]);
const editingHours = ref(false);
const savingHours = ref(false);
const hoursBackup = ref([]);

// 审核
const showAuditDialog = ref(false);
const auditRemark = ref('');
const pendingAuditRow = ref(null);

onMounted(() => { fetchList(); });

// ===== 当前位置定位 =====

function locateCurrentPosition() {
  if (!navigator.geolocation) {
    ElMessage.warning('浏览器不支持定位功能');
    return;
  }
  locating.value = true;
  ElMessage.info('正在获取当前位置...');
  navigator.geolocation.getCurrentPosition(
    function (pos) {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      createForm.latitude = lat;
      createForm.longitude = lng;
      createForm.address = lat.toFixed(6) + ', ' + lng.toFixed(6);
      locating.value = false;
      ElMessage.success('已定位到当前位置');
    },
    function () {
      locating.value = false;
      ElMessage.warning('定位失败，请检查浏览器定位权限');
    },
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

// ==================== 业务逻辑 ====================

async function fetchList() {
  loading.value = true;
  try {
    const res = await request({
      url: '/admin/stores', method: 'get',
      params: {
        page: pageNum.value, size: pageSize.value,
        keyword: searchKeyword.value || undefined,
        auditStatus: filterAuditStatus.value,
        businessStatus: filterBusinessStatus.value
      }
    });
    if (res.code === 200 && res.data) {
      storeList.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } finally { loading.value = false; }
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false);
  if (!valid) return;
  creating.value = true;
  try {
    const res = await request({ url: '/admin/stores/create', method: 'post', data: createForm });
    if (res.code === 200) {
      ElMessage.success('门店创建成功');
      showCreateDialog.value = false;
      fetchList();
    }
  } finally { creating.value = false; }
}

async function openDetail(row) {
  try {
    const res = await request({ url: '/admin/stores/' + row.id, method: 'get' });
    if (res.code === 200 && res.data) {
      detailStore.value = res.data;
      businessHours.value = (res.data.businessHours || []).map(h => ({
        ...h,
        _startTime: h.startTime ? new Date('2000-01-01 ' + h.startTime) : null,
        _endTime: h.endTime ? new Date('2000-01-01 ' + h.endTime) : null
      }));
      editingHours.value = false;
      showDetailDialog.value = true;
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
    await request({ url: '/admin/stores/' + detailStore.value.id + '/business-hours', method: 'post', data: { hours } });
    ElMessage.success('营业时间保存成功');
    editingHours.value = false;
  } finally { savingHours.value = false; }
}
function formatTime(date) {
  if (!date) return '00:00:00';
  if (typeof date === 'string') return date;
  return [date.getHours(), date.getMinutes(), 0].map(v => String(v).padStart(2, '0')).join(':');
}

function auditStore(row, status) {
  if (status === 3) {
    pendingAuditRow.value = row;
    auditRemark.value = '';
    showAuditDialog.value = true;
    return;
  }
  doAudit(row.id, status, '');
}
async function confirmAuditReject() {
  await doAudit(pendingAuditRow.value.id, 3, auditRemark.value);
  showAuditDialog.value = false;
}
async function doAudit(storeId, status, remark) {
  try {
    await request({ url: '/admin/stores/' + storeId + '/audit', method: 'post', data: { auditStatus: status, remark } });
    ElMessage.success(status === 2 ? '审核通过' : '已拒绝');
    fetchList();
  } catch (e) { /* ignore */ }
}

async function toggleStatus(row) {
  const newStatus = row.businessStatus === 1 ? 2 : 1;
  const text = newStatus === 1 ? '营业中' : '休息中';
  try {
    await ElMessageBox.confirm('确认切换为"' + text + '"吗？', '提示', { type: 'warning' });
    await request({ url: '/admin/stores/' + row.id + '/status', method: 'post', data: { businessStatus: newStatus } });
    ElMessage.success('已切换为' + text);
    fetchList();
  } catch (e) { /* ignore */ }
}

function canToggleStatus() {
  return isSuperAdmin.value || isStoreAdmin.value;
}

function businessStatusTag(s) { return s === 1 ? 'success' : s === 2 ? 'warning' : 'info'; }
function businessStatusText(s) { return s === 1 ? '营业中' : s === 2 ? '休息中' : '已关闭'; }
function auditStatusTag(s) { return s === 1 ? 'warning' : s === 2 ? 'success' : 'danger'; }
function auditStatusText(s) { return s === 1 ? '待审核' : s === 2 ? '通过' : '拒绝'; }
function weekDayText(d) { return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][d] || ''; }
</script>

<style scoped>
.store-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; }
.search-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 0; }
</style>
