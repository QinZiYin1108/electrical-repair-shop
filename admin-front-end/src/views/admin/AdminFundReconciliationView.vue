<template>
  <div class="ops-page">
    <el-card shadow="never" class="mb16">
      <div class="page-header">
        <div>
          <div class="page-title">资金对账</div>
          <div class="page-subtitle">对账批次与异常明细；异常仅记录与告警，不自动改账务。</div>
        </div>
        <div>
          <el-select v-model="runBatchType" class="w160" size="default">
            <el-option :value="1" label="增量对账" />
            <el-option :value="2" label="全量对账" />
          </el-select>
          <el-button type="primary" :loading="running" @click="runNow">手动触发</el-button>
          <el-button :icon="Refresh" @click="loadBatches">刷新</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="batches"
        border
        highlight-current-row
        @current-change="onSelectBatch"
      >
        <el-table-column prop="batchNo" label="批次号" width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ row.batchType === 2 ? '全量' : '增量' }}</template>
        </el-table-column>
        <el-table-column prop="scannedCount" label="扫描数" width="100" align="center" />
        <el-table-column prop="issueCount" label="异常数" width="100" align="center" />
        <el-table-column
          prop="errorSummary"
          label="错误摘要"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="currentBatch" shadow="never">
      <div class="page-title">批次异常明细（{{ currentBatch.batchNo }}）</div>
      <el-table v-loading="issueLoading" :data="issues" border style="margin-top: 12px">
        <el-table-column prop="category" label="分类" width="220" show-overflow-tooltip />
        <el-table-column prop="bizType" label="业务类型" width="120" />
        <el-table-column prop="bizId" label="业务ID" width="180" show-overflow-tooltip />
        <el-table-column prop="message" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="issueTagType(row.status)">{{
              ISSUE_STATUS_TEXT[row.status] || row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" @click="handle(row)">处理</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchReconciliationBatches,
  fetchReconciliationIssues,
  handleReconciliationIssue,
  runReconciliation
} from '../../api/adminFundReconciliation';

const ISSUE_STATUS_TEXT = { 1: '待处理', 2: '已处理', 3: '已忽略' };

const loading = ref(false);
const issueLoading = ref(false);
const batches = ref([]);
const issues = ref([]);
const currentBatch = ref(null);
const runBatchType = ref(1);
const running = ref(false);

function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(Number(ts));
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function issueTagType(status) {
  return { 1: 'warning', 2: 'success', 3: 'info' }[status] || '';
}

async function loadBatches() {
  loading.value = true;
  try {
    const res = await fetchReconciliationBatches({ limit: 30 });
    if (res.code === 200) batches.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function onSelectBatch(row) {
  if (!row) return;
  currentBatch.value = row;
  issueLoading.value = true;
  try {
    const res = await fetchReconciliationIssues(row.id);
    if (res.code === 200) issues.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载异常明细失败');
  } finally {
    issueLoading.value = false;
  }
}

async function runNow() {
  running.value = true;
  try {
    await runReconciliation({ batchType: runBatchType.value });
    ElMessage.success('已触发对账');
    loadBatches();
  } catch (e) {
    ElMessage.error(e.message || '触发失败');
  } finally {
    running.value = false;
  }
}

async function handle(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      '处理备注（可空）；确认后将标记为已处理',
      '处理异常',
      {
        inputValue: ''
      }
    );
    await handleReconciliationIssue(row.id, { status: 2, remark: value || '' });
    ElMessage.success('已处理');
    if (currentBatch.value) onSelectBatch(currentBatch.value);
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

onMounted(loadBatches);
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.page-subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.w160 {
  width: 160px;
}
</style>
