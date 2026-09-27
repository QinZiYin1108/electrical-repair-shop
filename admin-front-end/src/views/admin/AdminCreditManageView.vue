<template>
  <div class="credit-page">
    <el-card class="credit-card" shadow="never">
      <div class="credit-header">
        <div class="credit-title-group">
          <div class="credit-title">信用积分查询</div>
          <div class="credit-subtitle">查看和管理用户、师傅的信用积分及变动记录</div>
        </div>
        <div class="credit-toolbar">
          <el-input
            v-model="searchAccountId"
            placeholder="输入账号ID"
            clearable
            style="width: 200px"
            @keyup.enter="doSearch"
          />
          <el-select v-model="searchAccountType" style="width: 120px; margin-left: 8px">
            <el-option label="用户" :value="1" />
            <el-option label="师傅" :value="2" />
          </el-select>
          <el-button type="primary" style="margin-left: 8px" @click="doSearch">查询</el-button>
        </div>
      </div>

      <!-- 积分卡片 -->
      <div v-if="creditInfo" class="credit-score-area">
        <div class="score-card" :class="scoreStatusClass">
          <div class="score-value">{{ creditInfo.creditScore }}</div>
          <div class="score-label">当前信用积分</div>
        </div>
        <div class="score-meta">
          <el-tag :type="scoreStatusTag" size="large">{{ scoreStatusText }}</el-tag>
          <span style="margin-left: 12px; color: #909399">
            账号ID: {{ creditInfo.accountId }} | 类型: {{ accountTypeText(searchAccountType) }}
          </span>
        </div>
      </div>

      <!-- 积分变动历史 -->
      <div v-if="creditInfo" style="margin-top: 20px">
        <div style="font-size: 15px; font-weight: 600; margin-bottom: 10px">积分变动记录</div>
        <el-table
          :data="historyList"
          v-loading="historyLoading"
          border
          class="credit-table"
          header-cell-class-name="credit-table-header"
        >
          <el-table-column type="index" label="#" width="60" align="center" />
          <el-table-column label="变动类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="changeTypeTag(row.changeType)" size="small">{{
                changeTypeText(row.changeType)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="变动值" width="90" align="center">
            <template #default="{ row }">
              <span
                :style="{ color: row.scoreChange >= 0 ? '#67c23a' : '#f56c6c', fontWeight: 600 }"
              >
                {{ row.scoreChange >= 0 ? '+' : '' }}{{ row.scoreChange }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="变动前" width="70" align="center">
            <template #default="{ row }">{{ row.scoreBefore }}</template>
          </el-table-column>
          <el-table-column label="变动后" width="70" align="center">
            <template #default="{ row }">
              <span
                :style="{
                  fontWeight: 600,
                  color:
                    row.scoreAfter >= 60 ? '#67c23a' : row.scoreAfter >= 40 ? '#e6a23c' : '#f56c6c'
                }"
              >
                {{ row.scoreAfter }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
          </el-table-column>
        </el-table>

        <div class="credit-pagination">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="historyTotal"
            :page-size="historyPageSize"
            :current-page="historyPageNum"
            @current-change="
              (p) => {
                historyPageNum = p;
                fetchHistory();
              }
            "
          />
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!creditInfo && searched" description="请输入账号ID查询" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchCreditScore, fetchCreditHistory } from '../../api/adminCredit';

const searchAccountId = ref('');
const searchAccountType = ref(1);
const searched = ref(false);

const creditInfo = ref(null);
const historyList = ref([]);
const historyLoading = ref(false);
const historyPageNum = ref(1);
const historyPageSize = ref(10);
const historyTotal = ref(0);

async function doSearch() {
  if (!searchAccountId.value) {
    ElMessage.warning('请输入账号ID');
    return;
  }
  searched.value = true;

  const res = await fetchCreditScore(searchAccountId.value, searchAccountType.value);
  if (res.code === 200) {
    creditInfo.value = res.data;
    historyPageNum.value = 1;
    fetchHistory();
  }
}

async function fetchHistory() {
  historyLoading.value = true;
  try {
    const res = await fetchCreditHistory(searchAccountId.value, searchAccountType.value, {
      page: historyPageNum.value,
      size: historyPageSize.value
    });
    if (res.code === 200 && res.data) {
      historyList.value = res.data.records || [];
      historyTotal.value = res.data.total || 0;
    }
  } finally {
    historyLoading.value = false;
  }
}

function formatTime(ts) {
  return ts ? new Date(ts).toLocaleString('zh-CN') : '—';
}

const scoreStatusClass = computed(() => {
  if (!creditInfo.value) return '';
  const s = creditInfo.value.creditScore;
  return s >= 60 ? 'score-normal' : s >= 40 ? 'score-warning' : 'score-danger';
});

const scoreStatusTag = computed(() => {
  if (!creditInfo.value) return 'info';
  const s = creditInfo.value.creditScore;
  return s >= 60 ? 'success' : s >= 40 ? 'warning' : 'danger';
});

const scoreStatusText = computed(() => {
  if (!creditInfo.value) return '';
  const s = creditInfo.value.creditScore;
  return s >= 60 ? '正常' : s >= 40 ? '风险观察' : '已限制';
});

function changeTypeTag(t) {
  return t === 1
    ? 'danger'
    : t === 2
      ? 'success'
      : t === 3
        ? 'warning'
        : t === 4
          ? 'success'
          : t === 5
            ? ''
            : 'info';
}
function changeTypeText(t) {
  return t === 1
    ? '违规扣分'
    : t === 2
      ? '自动恢复'
      : t === 3
        ? '举报奖励'
        : t === 4
          ? '完单恢复'
          : t === 5
            ? '申诉恢复'
            : '未知';
}
function accountTypeText(t) {
  return t === 1 ? '用户' : t === 2 ? '师傅' : '未知';
}
</script>

<style scoped>
.credit-page {
  padding: 16px;
  box-sizing: border-box;
}
.credit-card {
  width: 100%;
}
.credit-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 12px;
}
.credit-title-group {
  display: flex;
  flex-direction: column;
}
.credit-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.credit-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}
.credit-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.credit-score-area {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 12px;
}
.score-card {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.score-normal {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}
.score-warning {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
}
.score-danger {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}
.score-value {
  font-size: 36px;
  font-weight: 700;
}
.score-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 4px;
}
.score-meta {
  display: flex;
  align-items: center;
}
.credit-table {
  width: 100%;
}
.credit-table-header {
  background-color: #f5f7fa;
}
.credit-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
