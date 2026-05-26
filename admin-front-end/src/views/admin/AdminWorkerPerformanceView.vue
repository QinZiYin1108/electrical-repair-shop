<template>
  <div class="performance-page">
    <el-card class="performance-card" shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">绩效统计</div>
          <div class="page-subtitle">从订单、营收、工时和评分四个维度查看师傅综合表现</div>
        </div>
        <div class="page-toolbar">
          <el-input
            v-model="searchKeyword"
            clearable
            class="search-input"
            placeholder="搜索师傅姓名、手机号或邮箱"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <div class="summary-grid">
        <article v-for="card in summaryCards" :key="card.label" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-extra">{{ card.extra }}</div>
        </article>
      </div>

      <div v-if="topThree.length" class="top-grid">
        <div v-for="(worker, index) in topThree" :key="worker.id" class="top-card">
          <div class="top-rank">TOP {{ index + 1 }}</div>
          <div class="top-main">
            <el-avatar :size="52" :src="worker.avatarUrl">{{ getAvatarInitial(worker) }}</el-avatar>
            <div class="top-info">
              <div class="top-name">{{ worker.realName || worker.username || '未命名师傅' }}</div>
              <div class="top-meta">{{ worker.phone || worker.email || '暂无联系方式' }}</div>
            </div>
          </div>
          <div class="top-stats">
            <div>完工 {{ worker.completedOrders || 0 }} 单</div>
            <div>净收入 ¥{{ formatMoney(worker.netIncome) }}</div>
            <div>评分 {{ formatScore(worker.rating) }}</div>
          </div>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        class="performance-table"
        header-cell-class-name="performance-table-header"
      >
        <el-table-column type="index" label="排名" width="70" align="center">
          <template #default="{ $index }">
            {{ (page - 1) * pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="师傅信息" min-width="240">
          <template #default="{ row }">
            <div class="worker-cell">
              <el-avatar :size="40" :src="row.avatarUrl">{{ getAvatarInitial(row) }}</el-avatar>
              <div class="worker-cell-main">
                <div class="worker-cell-name">{{ row.realName || row.username || '-' }}</div>
                <div class="worker-cell-meta">{{ row.phone || '-' }}</div>
                <div class="worker-cell-meta">{{ row.email || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <div class="status-tags">
              <el-tag :type="getAccountStatusTagType(row.accountStatus)" size="small">
                {{ getAccountStatusText(row.accountStatus) }}
              </el-tag>
              <el-tag :type="getWorkStatusTagType(row.workStatus)" size="small">
                {{ getWorkStatusText(row.workStatus) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单表现" min-width="210">
          <template #default="{ row }">
            <div class="metric-stack">
              <div>总订单：{{ row.totalOrders || 0 }}</div>
              <div>已完成：{{ row.completedOrders || 0 }}</div>
              <div>处理中：{{ row.pendingOrders || 0 }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="完成率" width="150">
          <template #default="{ row }">
            <el-progress :percentage="normalizePercent(row.completionRate)" :stroke-width="10" :show-text="false" />
            <div class="progress-text">{{ formatPercent(row.completionRate) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="营收统计" min-width="210">
          <template #default="{ row }">
            <div class="metric-stack">
              <div>流水：¥{{ formatMoney(row.grossIncome) }}</div>
              <div>退款：¥{{ formatMoney(row.refundAmount) }}</div>
              <div>净收入：¥{{ formatMoney(row.netIncome) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工时/客单" width="150">
          <template #default="{ row }">
            <div class="metric-stack">
              <div>{{ formatHours(row.serviceHours) }} 小时</div>
              <div>客单 ¥{{ formatMoney(row.averageOrderAmount) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="110" align="center">
          <template #default="{ row }">
            <div class="rating-value">{{ formatScore(row.rating) }}</div>
            <div class="rating-meta">{{ row.reviewCount || 0 }} 条评价</div>
          </template>
        </el-table-column>
        <el-table-column label="最近完工" width="160" align="center">
          <template #default="{ row }">
            {{ formatTime(row.latestCompletedTime) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :current-page="page"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchAdminWorkerPerformance } from '../../api/adminWorkers';

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const searchKeyword = ref('');

const summary = reactive({
  totalWorkers: 0,
  activeWorkers: 0,
  totalOrders: 0,
  pendingOrders: 0,
  completedOrders: 0,
  grossIncome: 0,
  refundAmount: 0,
  netIncome: 0,
  averageRating: 0
});

const topThree = computed(() => list.value.slice(0, 3));

const summaryCards = computed(() => [
  {
    label: '师傅总数',
    value: summary.totalWorkers || 0,
    extra: `在岗 ${summary.activeWorkers || 0} 人`
  },
  {
    label: '累计订单',
    value: summary.totalOrders || 0,
    extra: `处理中 ${summary.pendingOrders || 0} 单`
  },
  {
    label: '累计完工单',
    value: summary.completedOrders || 0,
    extra: '按当前筛选条件实时汇总'
  },
  {
    label: '累计流水',
    value: `¥${formatMoney(summary.grossIncome)}`,
    extra: `退款 ¥${formatMoney(summary.refundAmount)}`
  },
  {
    label: '累计净收入',
    value: `¥${formatMoney(summary.netIncome)}`,
    extra: '已扣除退款金额'
  },
  {
    label: '平均评分',
    value: formatScore(summary.averageRating),
    extra: '基于当前筛选结果聚合'
  }
]);

function formatMoney(value) {
  return Number(value || 0).toFixed(2);
}

function formatScore(value) {
  return Number(value || 0).toFixed(2);
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(2)}%`;
}

function normalizePercent(value) {
  const percent = Number(value || 0);
  if (!Number.isFinite(percent)) return 0;
  return Math.max(0, Math.min(100, Number(percent.toFixed(2))));
}

function formatHours(value) {
  return Number(value || 0).toFixed(1);
}

function formatTime(value) {
  if (!value) return '';
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp)) return '';
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return '';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function getAvatarInitial(worker) {
  if (worker && worker.realName) return String(worker.realName).charAt(0);
  if (worker && worker.username) return String(worker.username).charAt(0).toUpperCase();
  return 'W';
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

function applySummary(data) {
  summary.totalWorkers = Number(data.totalWorkers || 0);
  summary.activeWorkers = Number(data.activeWorkers || 0);
  summary.totalOrders = Number(data.totalOrders || 0);
  summary.pendingOrders = Number(data.pendingOrders || 0);
  summary.completedOrders = Number(data.completedOrders || 0);
  summary.grossIncome = Number(data.grossIncome || 0);
  summary.refundAmount = Number(data.refundAmount || 0);
  summary.netIncome = Number(data.netIncome || 0);
  summary.averageRating = Number(data.averageRating || 0);
}

async function loadList() {
  if (loading.value) return;
  loading.value = true;
  try {
    const params = {
      pageNum: page.value,
      pageSize: pageSize.value
    };
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim();
    }
    const res = await fetchAdminWorkerPerformance(params);
    if (res && res.code === 200 && res.data) {
      list.value = Array.isArray(res.data.list) ? res.data.list : [];
      total.value = Number(res.data.total || 0);
      applySummary(res.data.summary || {});
      return;
    }
    throw new Error((res && res.message) || '获取绩效统计失败');
  } catch (error) {
    list.value = [];
    total.value = 0;
    applySummary({});
    ElMessage.error(error.message || '获取绩效统计失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadList();
}

function handlePageChange(nextPage) {
  page.value = nextPage;
  loadList();
}

function handleSizeChange(size) {
  pageSize.value = size;
  page.value = 1;
  loadList();
}

function goDetail(id) {
  if (!id) return;
  router.push(`/admin/workers/info/${id}`);
}

async function handleExternalRefresh(event) {
  if (!event || !event.detail || !String(event.detail.path || '').startsWith('/admin/workers/performance')) {
    return;
  }
  event.detail.handled = true;
  await loadList();
  ElMessage.success('刷新成功');
}

onMounted(() => {
  loadList();
  window.addEventListener('admin-page-refresh', handleExternalRefresh);
});

onBeforeUnmount(() => {
  window.removeEventListener('admin-page-refresh', handleExternalRefresh);
});
</script>

<style scoped>
.performance-page {
  padding: 16px;
}

.performance-card {
  border-radius: 18px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #172033;
}

.page-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #7f8a9a;
}

.page-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 300px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f5fbff 100%);
  border: 1px solid #e5edf6;
}

.summary-label {
  font-size: 13px;
  color: #7f8a9a;
}

.summary-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #14253d;
}

.summary-extra {
  margin-top: 8px;
  font-size: 12px;
  color: #8d98a8;
}

.top-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.top-card {
  padding: 18px;
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.36), transparent 30%),
    linear-gradient(135deg, #23486a 0%, #2d6c98 55%, #6fbc8c 100%);
  color: #fff;
}

.top-rank {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 700;
}

.top-main {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.top-info {
  min-width: 0;
}

.top-name {
  font-size: 18px;
  font-weight: 700;
}

.top-meta {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
}

.top-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}

.performance-table {
  width: 100%;
}

.performance-table-header {
  background: #f6f9fc;
}

.worker-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.worker-cell-main {
  min-width: 0;
}

.worker-cell-name {
  font-size: 15px;
  font-weight: 600;
  color: #172033;
}

.worker-cell-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #7f8a9a;
  word-break: break-all;
}

.status-tags {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #475467;
  font-size: 13px;
  line-height: 1.4;
}

.progress-text {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

.rating-value {
  font-size: 20px;
  font-weight: 700;
  color: #1d4d7c;
}

.rating-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #7f8a9a;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 1440px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-toolbar {
    justify-content: flex-start;
  }

  .top-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .page-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>
