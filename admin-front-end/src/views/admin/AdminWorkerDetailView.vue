<template>
  <div class="worker-detail-page" v-loading="loading">
    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left-head">
            <el-button text @click="goBack">返回列表</el-button>
            <span class="title">师傅详情</span>
          </div>
          <el-button type="primary" :loading="savingBase" @click="saveBaseInfo">保存基础信息</el-button>
        </div>
      </template>

      <div class="summary">
        <div class="avatar-wrap" @click="triggerAvatarUpload">
          <el-avatar :size="84" :src="detail.avatarUrl">{{ avatarInitial }}</el-avatar>
          <div v-if="avatarUploading" class="avatar-mask">上传中</div>
        </div>
        <div class="summary-main">
          <div class="name-row">
            <span class="name">{{ detail.username || '-' }}</span>
            <el-tag :type="getAccountStatusTagType(detail.accountStatus)">账号：{{ getAccountStatusText(detail.accountStatus) }}</el-tag>
            <el-tag :type="getWorkStatusTagType(detail.workStatus)">工作：{{ getWorkStatusText(detail.workStatus) }}</el-tag>
            <el-button
              v-if="detail.accountStatus === 1 || detail.accountStatus === 3"
              size="small"
              :type="detail.accountStatus === 1 ? 'danger' : 'success'"
              :loading="statusSaving"
              @click="toggleAccountStatus"
            >
              {{ detail.accountStatus === 1 ? '冻结账号' : '解除冻结' }}
            </el-button>
          </div>
          <div class="meta-row">
            <span>手机号：{{ detail.phone || '-' }}</span>
            <span>邮箱：{{ detail.email || '-' }}</span>
            <span>注册时间：{{ formatTime(detail.createdTime) || '-' }}</span>
          </div>
        </div>
      </div>

      <el-form label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="师傅ID"><el-input :model-value="detail.id" disabled /></el-form-item>
            <el-form-item label="账号名"><el-input v-model="baseForm.username" maxlength="50" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="baseForm.email" maxlength="100" /></el-form-item>
            <el-form-item label="手机号"><el-input :model-value="detail.phone" disabled /></el-form-item>
            <el-form-item label="真实姓名"><el-input :model-value="detail.realName || '-'" disabled /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证"><el-input :model-value="detail.idCard || '-'" disabled /></el-form-item>
            <el-form-item label="性别"><el-input :model-value="getGenderText(detail.gender)" disabled /></el-form-item>
            <el-form-item label="生日"><el-input :model-value="detail.birthday || '-'" disabled /></el-form-item>
            <el-form-item label="工龄"><el-input-number v-model="baseForm.workYears" :min="0" :max="60" /></el-form-item>
            <el-form-item label="学历"><el-input v-model="baseForm.education" maxlength="30" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="个人介绍">
          <el-input v-model="baseForm.introduction" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <input ref="avatarInputRef" class="hidden-file" type="file" accept="image/*" @change="handleAvatarFileChange" />
    </el-card>

    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="section-title">从表数据</span>
          <div class="slider">
            <div class="slider-track" :style="sliderStyle"></div>
            <button
              v-for="item in panelOptions"
              :key="item.key"
              type="button"
              class="slider-btn"
              :class="{ active: activePanel === item.key }"
              @click="selectPanel(item.key)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>
      </template>

      <div v-if="activePanel === 'skills'" class="panel">
        <div class="toolbar">
          <el-input
            v-model="skillQuery.keyword"
            class="input-keyword"
            clearable
            placeholder="搜索服务类型名称/描述"
            @keyup.enter="handleSkillSearch"
            @clear="handleSkillSearch"
          />
          <el-select v-model="skillQuery.serviceMode" class="input-mode" clearable placeholder="服务方式">
            <el-option :value="1" label="上门维修" />
            <el-option :value="2" label="上门安装" />
            <el-option :value="3" label="到店维修" />
          </el-select>
          <el-button type="primary" :loading="skillSourceLoading" @click="handleSkillSearch">查询</el-button>
        </div>

        <div class="skill-source">
          <div class="tree-box" v-loading="skillSourceLoading">
            <div class="sub-title">分类树</div>
            <el-tree
              :data="skillCategoryTree"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              highlight-current
              @node-click="handleCategorySelect"
            />
          </div>
          <div class="option-box" v-loading="skillSourceLoading">
            <div class="sub-title">可添加服务类型</div>
            <el-table :data="availableSkillList" border height="260" @selection-change="onAvailableSkillSelectionChange">
              <el-table-column type="selection" width="48" />
              <el-table-column prop="name" label="服务类型" min-width="150" />
              <el-table-column prop="typeText" label="服务方式" width="100" />
              <el-table-column prop="categoryPath" label="分类路径" min-width="180" show-overflow-tooltip />
            </el-table>
            <div class="add-row">
              <el-button type="primary" :loading="skillSaving" @click="handleBatchAddSkills">
                添加选中（{{ selectedAvailableSkillIds.length }}）
              </el-button>
            </div>
          </div>
        </div>

        <div class="sub-title current-title">当前技能</div>
        <el-table v-loading="skillLoading" :data="skillList" border>
          <el-table-column prop="serviceTypeName" label="服务类型" min-width="150" />
          <el-table-column prop="serviceModeText" label="服务方式" width="100" />
          <el-table-column prop="categoryPath" label="分类路径" min-width="180" show-overflow-tooltip />
          <el-table-column prop="skillLevelText" label="技能等级" width="100" />
          <el-table-column label="更新时间" min-width="160">
            <template #default="{ row }">{{ formatTime(row.updatedTime) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button type="danger" link @click="handleRemoveSkill(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="activePanel === 'policies'" class="panel">
        <div class="toolbar toolbar-right">
          <el-button type="primary" :loading="savingPolicies" @click="savePolicies">保存计费策略</el-button>
        </div>
        <el-table :data="policyForm" border>
          <el-table-column prop="serviceKind" label="服务类型" width="120">
            <template #default="{ row }">{{ getServiceKindText(row.serviceKind) }}</template>
          </el-table-column>
          <el-table-column label="最低上门费(元)" min-width="140">
            <template #default="{ row }"><el-input-number v-model="row.minVisitFee" :min="0" :precision="2" /></template>
          </el-table-column>
          <el-table-column label="基础半径(km)" min-width="140">
            <template #default="{ row }"><el-input-number v-model="row.baseRadiusKm" :min="0" :precision="3" /></template>
          </el-table-column>
          <el-table-column label="超区每公里(元)" min-width="140">
            <template #default="{ row }"><el-input-number v-model="row.extraFeePerKm" :min="0" :precision="2" /></template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-switch v-model="row.isActive" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="activePanel === 'workTimes'" class="panel">
        <div class="toolbar toolbar-right">
          <el-button type="primary" :loading="savingWorkTimes" @click="saveWorkTimes">保存工作时间</el-button>
        </div>
        <el-table :data="workTimeForm" border>
          <el-table-column prop="dayOfWeek" label="星期" width="90">
            <template #default="{ row }">{{ getDayOfWeekText(row.dayOfWeek) }}</template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="170">
            <template #default="{ row }">
              <el-time-picker v-model="row.startTime" format="HH:mm" value-format="HH:mm" :disabled="row.isAvailable === 0" />
            </template>
          </el-table-column>
          <el-table-column label="结束时间" min-width="170">
            <template #default="{ row }">
              <el-time-picker v-model="row.endTime" format="HH:mm" value-format="HH:mm" :disabled="row.isAvailable === 0" />
            </template>
          </el-table-column>
          <el-table-column label="可接单" width="120">
            <template #default="{ row }">
              <el-switch v-model="row.isAvailable" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="activePanel === 'serviceArea'" class="panel">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="区域名称">{{ detail.serviceAreaCenter?.areaName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.serviceAreaCenter?.isActive === 1 ? '启用' : '禁用' }}</el-descriptions-item>
          <el-descriptions-item label="中心纬度">{{ detail.serviceAreaCenter?.centerLatitude ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="中心经度">{{ detail.serviceAreaCenter?.centerLongitude ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">{{ formatTime(detail.serviceAreaCenter?.updatedTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="中心地址" :span="2">
            <span class="long-text">{{ detail.serviceAreaCenter?.centerAddress || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div v-else class="panel">
        <div class="stats-grid">
          <div class="stats-item"><div class="stats-label">总订单</div><div class="stats-value">{{ detail.orderStats?.totalCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">待接单</div><div class="stats-value">{{ detail.orderStats?.waitingCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">进行中</div><div class="stats-value">{{ detail.orderStats?.ongoingCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">待支付</div><div class="stats-value">{{ detail.orderStats?.waitingPayCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">已完成</div><div class="stats-value">{{ detail.orderStats?.completedCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">已取消</div><div class="stats-value">{{ detail.orderStats?.canceledCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">已退款</div><div class="stats-value">{{ detail.orderStats?.refundedCount ?? 0 }}</div></div>
          <div class="stats-item"><div class="stats-label">最近订单</div><div class="stats-value small">{{ formatTime(detail.orderStats?.latestOrderTime) || '-' }}</div></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import {
  batchAddAdminWorkerSkills,
  fetchAdminWorkerDetail,
  fetchAdminWorkerSkillCategoryTree,
  fetchAdminWorkerSkillServiceTypes,
  fetchAdminWorkerSkills,
  removeAdminWorkerSkill,
  updateAdminWorkerInfo,
  updateAdminWorkerStatus,
  updateAdminWorkerVisitFeePolicies,
  updateAdminWorkerWorkTimes,
  uploadAdminWorkerAvatar
} from '../../api/adminWorkers';
import { showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';

const route = useRoute();
const router = useRouter();
const workerId = computed(() => route.params.id);
const dayOfWeekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

const panelOptions = [
  { key: 'skills', label: '技能管理' },
  { key: 'policies', label: '计费策略' },
  { key: 'workTimes', label: '工作时间' },
  { key: 'serviceArea', label: '服务区域' },
  { key: 'orderStats', label: '订单数据' }
];

const loading = ref(false);
const savingBase = ref(false);
const savingPolicies = ref(false);
const savingWorkTimes = ref(false);
const statusSaving = ref(false);
const avatarUploading = ref(false);
const avatarInputRef = ref();

const activePanel = ref('skills');
const skillLoading = ref(false);
const skillSourceLoading = ref(false);
const skillSaving = ref(false);
const skillSourceLoaded = ref(false);
const skillList = ref([]);
const skillCategoryTree = ref([]);
const availableSkillList = ref([]);
const selectedAvailableSkillIds = ref([]);

const skillQuery = reactive({
  keyword: '',
  serviceMode: null,
  categoryId: ''
});

const detail = reactive({
  id: '',
  username: '',
  phone: '',
  email: '',
  accountStatus: null,
  workStatus: null,
  rating: null,
  createdTime: null,
  realName: '',
  idCard: '',
  gender: null,
  birthday: '',
  workYears: null,
  education: '',
  introduction: '',
  avatarUrl: '',
  orderStats: null,
  serviceAreaCenter: null,
  visitFeePolicies: [],
  workTimes: []
});

const baseForm = reactive({
  username: '',
  email: '',
  workYears: null,
  education: '',
  introduction: ''
});

const policyForm = ref([defaultPolicy(1), defaultPolicy(2)]);
const workTimeForm = ref(Array.from({ length: 7 }, (_, index) => defaultWorkTime(index + 1)));

const avatarInitial = computed(() => {
  if (detail.username) return String(detail.username).charAt(0).toUpperCase();
  if (detail.email) return String(detail.email).charAt(0).toUpperCase();
  return 'W';
});

const sliderStyle = computed(() => {
  const index = Math.max(0, panelOptions.findIndex(item => item.key === activePanel.value));
  return {
    width: `${100 / panelOptions.length}%`,
    transform: `translateX(${index * 100}%)`
  };
});

function defaultPolicy(serviceKind) {
  return {
    id: '',
    serviceKind,
    minVisitFee: 0,
    baseRadiusKm: 0,
    extraFeePerKm: 0,
    distanceCalcType: 1,
    roundingRule: 1,
    maxVisitFee: null,
    isActive: 1
  };
}

function defaultWorkTime(dayOfWeek) {
  return {
    id: '',
    dayOfWeek,
    startTime: '09:00',
    endTime: '18:00',
    isAvailable: 1
  };
}

function normalizeTimeValue(value, fallback) {
  if (value == null) return fallback;
  const text = String(value).trim();
  if (/^\d{2}:\d{2}$/.test(text)) return text;
  if (/^\d{2}:\d{2}:\d{2}$/.test(text)) return text.slice(0, 5);
  return fallback;
}

function formatTime(value) {
  if (!value) return '';
  const ts = Number(value);
  if (!Number.isFinite(ts)) return '';
  const date = new Date(ts);
  if (Number.isNaN(date.getTime())) return '';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mm}`;
}

function getAccountStatusText(status) {
  if (status === 1) return '正常';
  if (status === 2) return '未认证';
  if (status === 3) return '冻结';
  if (status === 4) return '离职';
  return '未知';
}

function getAccountStatusTagType(status) {
  if (status === 1) return 'success';
  if (status === 2) return 'warning';
  if (status === 3) return 'danger';
  return 'info';
}

function getWorkStatusText(status) {
  if (status === 1) return '在线';
  if (status === 2) return '忙碌';
  if (status === 3) return '休息';
  return '离线';
}

function getWorkStatusTagType(status) {
  if (status === 1) return 'success';
  if (status === 2) return 'warning';
  if (status === 3) return 'info';
  return '';
}

function getGenderText(gender) {
  if (gender === 1) return '男';
  if (gender === 2) return '女';
  return '未知';
}

function getServiceKindText(serviceKind) {
  return serviceKind === 2 ? '上门安装' : '上门维修';
}

function getDayOfWeekText(dayOfWeek) {
  if (!Number.isInteger(dayOfWeek)) return '-';
  return dayOfWeekLabels[dayOfWeek - 1] || '-';
}

function applyDetail(data) {
  detail.id = data.id || '';
  detail.username = data.username || '';
  detail.phone = data.phone || '';
  detail.email = data.email || '';
  detail.accountStatus = data.accountStatus ?? null;
  detail.workStatus = data.workStatus ?? null;
  detail.rating = data.rating ?? null;
  detail.createdTime = data.createdTime ?? null;
  detail.realName = data.realName || '';
  detail.idCard = data.idCard || '';
  detail.gender = data.gender ?? null;
  detail.birthday = data.birthday || '';
  detail.workYears = data.workYears ?? null;
  detail.education = data.education || '';
  detail.introduction = data.introduction || '';
  detail.avatarUrl = data.avatarUrl || '';
  detail.orderStats = data.orderStats || null;
  detail.serviceAreaCenter = data.serviceAreaCenter || null;
  detail.visitFeePolicies = Array.isArray(data.visitFeePolicies) ? data.visitFeePolicies : [];
  detail.workTimes = Array.isArray(data.workTimes) ? data.workTimes : [];

  baseForm.username = detail.username;
  baseForm.email = detail.email;
  baseForm.workYears = detail.workYears;
  baseForm.education = detail.education;
  baseForm.introduction = detail.introduction;

  const policyMap = new Map();
  detail.visitFeePolicies.forEach(item => {
    if (item && (item.serviceKind === 1 || item.serviceKind === 2)) policyMap.set(item.serviceKind, item);
  });
  policyForm.value = [1, 2].map(kind => {
    const source = policyMap.get(kind);
    if (!source) return defaultPolicy(kind);
    return {
      id: source.id || '',
      serviceKind: kind,
      minVisitFee: Number(source.minVisitFee ?? 0),
      baseRadiusKm: Number(source.baseRadiusKm ?? 0),
      extraFeePerKm: Number(source.extraFeePerKm ?? 0),
      distanceCalcType: source.distanceCalcType === 2 ? 2 : 1,
      roundingRule: source.roundingRule === 2 ? 2 : 1,
      maxVisitFee: source.maxVisitFee == null ? null : Number(source.maxVisitFee),
      isActive: source.isActive === 0 ? 0 : 1
    };
  });

  const workTimeMap = new Map();
  detail.workTimes.forEach(item => {
    if (item && Number.isInteger(item.dayOfWeek) && item.dayOfWeek >= 1 && item.dayOfWeek <= 7 && !workTimeMap.has(item.dayOfWeek)) {
      workTimeMap.set(item.dayOfWeek, item);
    }
  });
  workTimeForm.value = Array.from({ length: 7 }, (_, index) => {
    const dayOfWeek = index + 1;
    const source = workTimeMap.get(dayOfWeek);
    if (!source) return defaultWorkTime(dayOfWeek);
    return {
      id: source.id || '',
      dayOfWeek,
      startTime: normalizeTimeValue(source.startTime, '09:00'),
      endTime: normalizeTimeValue(source.endTime, '18:00'),
      isAvailable: source.isAvailable === 0 ? 0 : 1
    };
  });
}

async function loadDetail() {
  if (!workerId.value) return;
  loading.value = true;
  try {
    const res = await fetchAdminWorkerDetail(workerId.value);
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      return;
    }
    ElMessage.error((res && res.message) || '获取师傅详情失败');
  } catch {
    ElMessage.error('获取师傅详情失败');
  } finally {
    loading.value = false;
  }
}

async function saveBaseInfo() {
  if (!workerId.value) return;
  if (!baseForm.username || !baseForm.username.trim()) {
    ElMessage.warning('请输入账号名');
    return;
  }
  if (!baseForm.email || !baseForm.email.trim()) {
    ElMessage.warning('请输入邮箱');
    return;
  }
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(baseForm.email.trim())) {
    ElMessage.warning('邮箱格式不正确');
    return;
  }
  savingBase.value = true;
  try {
    const res = await updateAdminWorkerInfo(workerId.value, {
      username: baseForm.username.trim(),
      email: baseForm.email.trim(),
      workYears: baseForm.workYears == null ? null : Number(baseForm.workYears),
      education: baseForm.education || '',
      introduction: baseForm.introduction || ''
    });
    if (res && res.code === 200) {
      ElMessage.success('基础信息已保存');
      await loadDetail();
      return;
    }
    ElMessage.error((res && res.message) || '保存失败');
  } catch {
    ElMessage.error('保存失败');
  } finally {
    savingBase.value = false;
  }
}

async function toggleAccountStatus() {
  if (!workerId.value) return;
  const targetStatus = detail.accountStatus === 1 ? 3 : 1;
  const confirmText = targetStatus === 3 ? '确认冻结该师傅账号吗？' : '确认解除该师傅账号冻结吗？';
  try {
    await ElMessageBox.confirm(confirmText, '提示', { type: 'warning' });
  } catch {
    return;
  }
  statusSaving.value = true;
  try {
    const res = await updateAdminWorkerStatus(workerId.value, targetStatus);
    if (res && res.code === 200) {
      ElMessage.success('账号状态已更新');
      await loadDetail();
      return;
    }
    ElMessage.error((res && res.message) || '状态更新失败');
  } catch {
    ElMessage.error('状态更新失败');
  } finally {
    statusSaving.value = false;
  }
}

function validatePolicies() {
  for (const item of policyForm.value) {
    if (item.minVisitFee == null || item.minVisitFee < 0) return '最低上门费不能小于0';
    if (item.baseRadiusKm == null || item.baseRadiusKm < 0) return '基础半径不能小于0';
    if (item.extraFeePerKm == null || item.extraFeePerKm < 0) return '超区每公里费用不能小于0';
  }
  return '';
}

async function savePolicies() {
  if (!workerId.value) return;
  const errorMessage = validatePolicies();
  if (errorMessage) {
    ElMessage.warning(errorMessage);
    return;
  }
  savingPolicies.value = true;
  try {
    const payload = policyForm.value.map(item => ({
      id: item.id || '',
      serviceKind: item.serviceKind,
      minVisitFee: Number(item.minVisitFee ?? 0),
      baseRadiusKm: Number(item.baseRadiusKm ?? 0),
      extraFeePerKm: Number(item.extraFeePerKm ?? 0),
      distanceCalcType: item.distanceCalcType,
      roundingRule: item.roundingRule,
      maxVisitFee: item.maxVisitFee == null ? null : Number(item.maxVisitFee),
      isActive: item.isActive
    }));
    const res = await updateAdminWorkerVisitFeePolicies(workerId.value, payload);
    if (res && res.code === 200) {
      ElMessage.success('计费策略已保存');
      await loadDetail();
      return;
    }
    ElMessage.error((res && res.message) || '计费策略保存失败');
  } catch {
    ElMessage.error('计费策略保存失败');
  } finally {
    savingPolicies.value = false;
  }
}

function toMinutes(value) {
  const parts = String(value || '').split(':');
  if (parts.length < 2) return Number.NaN;
  return Number(parts[0]) * 60 + Number(parts[1]);
}

function validateWorkTimes() {
  for (const item of workTimeForm.value) {
    const startTime = normalizeTimeValue(item.startTime, '09:00');
    const endTime = normalizeTimeValue(item.endTime, '18:00');
    if (toMinutes(startTime) >= toMinutes(endTime)) return `${getDayOfWeekText(item.dayOfWeek)}时间区间不合法`;
  }
  return '';
}

async function saveWorkTimes() {
  if (!workerId.value) return;
  const errorMessage = validateWorkTimes();
  if (errorMessage) {
    ElMessage.warning(errorMessage);
    return;
  }
  savingWorkTimes.value = true;
  try {
    const payload = workTimeForm.value.map(item => ({
      id: item.id || '',
      dayOfWeek: item.dayOfWeek,
      startTime: `${normalizeTimeValue(item.startTime, '09:00')}:00`,
      endTime: `${normalizeTimeValue(item.endTime, '18:00')}:00`,
      isAvailable: item.isAvailable === 0 ? 0 : 1
    }));
    const res = await updateAdminWorkerWorkTimes(workerId.value, payload);
    if (res && res.code === 200) {
      ElMessage.success('工作时间已保存');
      await loadDetail();
      return;
    }
    ElMessage.error((res && res.message) || '工作时间保存失败');
  } catch {
    ElMessage.error('工作时间保存失败');
  } finally {
    savingWorkTimes.value = false;
  }
}

async function loadSkillList() {
  if (!workerId.value) return;
  skillLoading.value = true;
  try {
    const res = await fetchAdminWorkerSkills(workerId.value);
    if (res && res.code === 200 && Array.isArray(res.data)) {
      skillList.value = res.data;
      return;
    }
    skillList.value = [];
  } catch {
    skillList.value = [];
    ElMessage.error('加载技能列表失败');
  } finally {
    skillLoading.value = false;
  }
}

function buildSkillParams(withCategory) {
  const params = {};
  if (skillQuery.keyword && skillQuery.keyword.trim()) params.keyword = skillQuery.keyword.trim();
  if (skillQuery.serviceMode != null) params.serviceMode = skillQuery.serviceMode;
  if (withCategory && skillQuery.categoryId) params.categoryId = skillQuery.categoryId;
  return params;
}

async function loadAvailableSkillSources() {
  if (!workerId.value) return;
  skillSourceLoading.value = true;
  try {
    const [categoryRes, serviceTypeRes] = await Promise.all([
      fetchAdminWorkerSkillCategoryTree(workerId.value, buildSkillParams(false)),
      fetchAdminWorkerSkillServiceTypes(workerId.value, buildSkillParams(true))
    ]);
    skillCategoryTree.value = categoryRes && categoryRes.code === 200 && Array.isArray(categoryRes.data) ? categoryRes.data : [];
    availableSkillList.value = serviceTypeRes && serviceTypeRes.code === 200 && Array.isArray(serviceTypeRes.data) ? serviceTypeRes.data : [];
    selectedAvailableSkillIds.value = [];
    skillSourceLoaded.value = true;
  } catch {
    skillCategoryTree.value = [];
    availableSkillList.value = [];
    selectedAvailableSkillIds.value = [];
    ElMessage.error('加载可选技能失败');
  } finally {
    skillSourceLoading.value = false;
  }
}

async function handleSkillSearch() {
  skillQuery.categoryId = '';
  await loadAvailableSkillSources();
}

async function handleCategorySelect(node) {
  skillQuery.categoryId = node && node.id ? node.id : '';
  await loadAvailableSkillSources();
}

function onAvailableSkillSelectionChange(rows) {
  selectedAvailableSkillIds.value = (rows || []).map(item => item.id).filter(Boolean);
}

async function handleBatchAddSkills() {
  if (!workerId.value) return;
  if (!selectedAvailableSkillIds.value.length) {
    ElMessage.warning('请先选择要添加的技能');
    return;
  }
  skillSaving.value = true;
  try {
    const res = await batchAddAdminWorkerSkills(workerId.value, selectedAvailableSkillIds.value);
    if (res && res.code === 200) {
      ElMessage.success('技能添加成功');
      await Promise.all([loadSkillList(), loadAvailableSkillSources()]);
      return;
    }
    ElMessage.error((res && res.message) || '技能添加失败');
  } catch {
    ElMessage.error('技能添加失败');
  } finally {
    skillSaving.value = false;
  }
}

async function handleRemoveSkill(row) {
  if (!row || !row.serviceTypeId || !workerId.value) return;
  try {
    await ElMessageBox.confirm(`确认移除技能“${row.serviceTypeName || ''}”吗？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  skillSaving.value = true;
  try {
    const res = await removeAdminWorkerSkill(workerId.value, row.serviceTypeId);
    if (res && res.code === 200) {
      ElMessage.success('技能已移除');
      await Promise.all([loadSkillList(), loadAvailableSkillSources()]);
      return;
    }
    ElMessage.error((res && res.message) || '移除技能失败');
  } catch {
    ElMessage.error('移除技能失败');
  } finally {
    skillSaving.value = false;
  }
}

async function selectPanel(key) {
  activePanel.value = key;
  if (key !== 'skills') return;
  if (!skillSourceLoaded.value) await loadAvailableSkillSources();
  await loadSkillList();
}

function triggerAvatarUpload() {
  if (avatarUploading.value || !avatarInputRef.value) return;
  avatarInputRef.value.value = '';
  avatarInputRef.value.click();
}

async function handleAvatarFileChange(event) {
  const files = event.target && event.target.files;
  if (!files || !files.length || !workerId.value) return;
  const file = files[0];
  if (!file.type || !file.type.startsWith('image/')) {
    showUploadLimitDialog('请上传图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    showUploadLimitDialog('头像大小不能超过 5MB');
    return;
  }
  avatarUploading.value = true;
  try {
    const res = await uploadAdminWorkerAvatar(workerId.value, file);
    if (res && res.code === 200) {
      ElMessage.success('头像已更新');
      await loadDetail();
      return;
    }
    showUploadErrorDialog((res && res.message) || '头像上传失败', '头像上传失败', '头像上传失败');
  } catch (error) {
    showUploadErrorDialog(error, '头像上传失败', '头像上传失败');
  } finally {
    avatarUploading.value = false;
  }
}

function goBack() {
  router.push('/admin/workers/info');
}

onMounted(async () => {
  await loadDetail();
  await Promise.all([loadSkillList(), loadAvailableSkillSources()]);
});

useAdminPageRefresh(async () => {
  await Promise.all([loadDetail(), loadSkillList(), loadAvailableSkillSources()]);
});
</script>

<style scoped>
.worker-detail-page { padding: 16px; box-sizing: border-box; }
.detail-card { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.left-head { display: flex; align-items: center; gap: 8px; }
.title { font-size: 18px; font-weight: 600; }
.section-title { font-size: 15px; font-weight: 600; }
.summary { display: flex; gap: 16px; margin-bottom: 14px; }
.avatar-wrap { width: 84px; height: 84px; position: relative; cursor: pointer; border-radius: 50%; }
.avatar-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.summary-main { flex: 1; }
.name-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.name { font-size: 20px; font-weight: 600; }
.meta-row { margin-top: 10px; display: flex; gap: 14px; flex-wrap: wrap; color: #606266; }
.hidden-file { display: none; }

.slider {
  position: relative;
  display: inline-grid;
  grid-template-columns: repeat(5, minmax(80px, 1fr));
  border: 1px solid #dcdfe6;
  border-radius: 999px;
  overflow: hidden;
  background: #f5f7fa;
}
.slider-track { position: absolute; left: 0; top: 0; bottom: 0; background: #409eff; border-radius: 999px; transition: transform 0.25s; }
.slider-btn { position: relative; z-index: 1; border: none; background: transparent; height: 34px; padding: 0 12px; color: #606266; cursor: pointer; }
.slider-btn.active { color: #fff; font-weight: 600; }

.panel { min-height: 220px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.toolbar-right { justify-content: flex-end; }
.input-keyword { width: 280px; }
.input-mode { width: 140px; }

.skill-source { display: grid; grid-template-columns: 240px 1fr; gap: 12px; }
.tree-box, .option-box { border: 1px solid #ebeef5; border-radius: 10px; padding: 12px; }
.sub-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.add-row { display: flex; justify-content: flex-end; margin-top: 10px; }
.current-title { margin-top: 12px; }
.long-text { word-break: break-all; }

.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(130px, 1fr)); gap: 12px; }
.stats-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; background: #fafafa; }
.stats-label { color: #909399; font-size: 12px; }
.stats-value { margin-top: 6px; font-size: 18px; font-weight: 600; }
.stats-value.small { font-size: 13px; }

@media (max-width: 1280px) {
  .skill-source { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, minmax(120px, 1fr)); }
}
</style>
