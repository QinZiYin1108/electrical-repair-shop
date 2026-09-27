<template>
  <div class="ops-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">师傅提现审核</div>
          <div class="page-subtitle">
            审核、打款与风控：高金额需复核确认，支持人工拦截。仅超级管理员可操作。
          </div>
        </div>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="filter-panel">
        <el-select
          v-model="filter.status"
          clearable
          placeholder="状态"
          class="w180"
          @change="loadList"
        >
          <el-option
            v-for="(label, code) in STATUS_TEXT"
            :key="code"
            :value="Number(code)"
            :label="label"
          />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="withdrawalNo" label="提现单号" width="200" show-overflow-tooltip />
        <el-table-column
          prop="technicianAccountId"
          label="师傅账号"
          width="180"
          show-overflow-tooltip
        />
        <el-table-column prop="amount" label="金额(元)" width="110" align="right" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{
              STATUS_TEXT[row.status] || row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风控" width="180">
          <template #default="{ row }">
            <el-tag
              v-if="row.requiresReview === 1"
              size="small"
              :type="row.reviewConfirmed === 1 ? 'success' : 'warning'"
            >
              {{ row.reviewConfirmed === 1 ? '已复核' : '待复核' }}
            </el-tag>
            <el-tag v-if="row.intercepted === 1" size="small" type="danger" style="margin-left: 4px"
              >已拦截</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column
          prop="payoutAccount"
          label="收款账户"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button size="small" type="success" @click="review(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="review(row, false)">驳回</el-button>
            </template>
            <template v-else-if="row.status === 2">
              <el-button
                v-if="row.requiresReview === 1 && row.reviewConfirmed !== 1"
                size="small"
                type="warning"
                @click="confirmReview(row)"
                >复核确认</el-button
              >
              <el-button
                v-else
                size="small"
                type="success"
                :disabled="row.intercepted === 1"
                @click="markPaid(row)"
                >打款成功</el-button
              >
              <el-button size="small" @click="markFailed(row)">打款失败</el-button>
              <el-button
                v-if="row.intercepted !== 1"
                size="small"
                type="danger"
                plain
                @click="intercept(row)"
                >拦截</el-button
              >
              <el-button v-else size="small" plain @click="unintercept(row)">解除拦截</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchWithdrawals,
  reviewWithdrawal,
  markWithdrawalPaid,
  markWithdrawalFailed,
  confirmWithdrawalReview,
  interceptWithdrawal,
  uninterceptWithdrawal
} from '../../api/adminWorkerWithdrawals';

const STATUS_TEXT = { 1: '待审核', 2: '已通过', 3: '打款成功', 4: '打款失败', 5: '已驳回' };

const loading = ref(false);
const list = ref([]);
const filter = reactive({ status: null });

function statusTagType(status) {
  return { 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger', 5: 'info' }[status] || '';
}

function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(Number(ts));
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function loadList() {
  loading.value = true;
  try {
    const res = await fetchWithdrawals({ status: filter.status ?? undefined, limit: 100 });
    if (res.code === 200) list.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function review(row, approve) {
  try {
    const { value } = await ElMessageBox.prompt(
      approve ? '确认通过该提现申请？' : '请填写驳回原因',
      approve ? '通过' : '驳回',
      {
        inputValue: '',
        inputValidator: approve ? undefined : (v) => (v && v.trim() ? true : '请输入原因')
      }
    );
    await reviewWithdrawal(row.id, { approve, remark: value || '' });
    ElMessage.success('操作成功');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function confirmReview(row) {
  try {
    await ElMessageBox.confirm('确认已人工复核该高金额提现？', '复核确认', { type: 'warning' });
    await confirmWithdrawalReview(row.id);
    ElMessage.success('已复核');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function markPaid(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写渠道/转账单号（可空）', '打款成功', {
      inputValue: ''
    });
    await markWithdrawalPaid(row.id, { providerNo: value || '' });
    ElMessage.success('已标记打款成功');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function markFailed(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写打款失败原因', '打款失败', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入原因')
    });
    await markWithdrawalFailed(row.id, { reason: value });
    ElMessage.success('已标记失败并退回');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function intercept(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写拦截原因', '人工拦截', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入原因')
    });
    await interceptWithdrawal(row.id, { reason: value });
    ElMessage.success('已拦截');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function unintercept(row) {
  try {
    await ElMessageBox.confirm('确认解除对该提现单的人工拦截？', '解除拦截', { type: 'warning' });
    await uninterceptWithdrawal(row.id);
    ElMessage.success('已解除拦截');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

onMounted(loadList);
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
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
.filter-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.w180 {
  width: 180px;
}
</style>
