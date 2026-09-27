<template>
  <div class="penalty-page">
    <el-card class="penalty-card" shadow="never">
      <div class="penalty-header">
        <div class="penalty-title-group">
          <div class="penalty-title">处罚管理</div>
          <div class="penalty-subtitle">查看、执行和管理处罚记录及申诉处理</div>
        </div>
        <div class="penalty-toolbar">
          <el-select
            v-model="filterStatus"
            placeholder="状态"
            clearable
            style="width: 120px"
            @change="fetchList"
          >
            <el-option label="执行中" :value="1" />
            <el-option label="已解除" :value="2" />
            <el-option label="已过期" :value="3" />
          </el-select>
          <el-select
            v-model="filterViolationLevel"
            placeholder="违规等级"
            clearable
            style="width: 110px; margin-left: 8px"
            @change="fetchList"
          >
            <el-option label="一级" :value="1" />
            <el-option label="二级" :value="2" />
            <el-option label="三级" :value="3" />
            <el-option label="四级" :value="4" />
          </el-select>
          <el-select
            v-model="filterAppealStatus"
            placeholder="申诉状态"
            clearable
            style="width: 120px; margin-left: 8px"
            @change="fetchList"
          >
            <el-option label="未申诉" :value="0" />
            <el-option label="申诉中" :value="1" />
            <el-option label="申诉通过" :value="2" />
            <el-option label="申诉驳回" :value="3" />
          </el-select>
          <el-button type="primary" style="margin-left: 8px" @click="fetchList">查询</el-button>
          <el-button type="success" style="margin-left: 8px" @click="showExecuteDialog"
            >手动处罚</el-button
          >
        </div>
      </div>

      <el-table
        :data="penaltyList"
        v-loading="loading"
        border
        class="penalty-table"
        header-cell-class-name="penalty-table-header"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="accountId" label="被处罚人" width="140" show-overflow-tooltip>
          <template #default="{ row }"
            >{{ row.accountId }}<br /><el-tag size="small" type="info">{{
              accountTypeText(row.accountType)
            }}</el-tag></template
          >
        </el-table-column>
        <el-table-column label="违规等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.violationLevel)" size="small">{{
              levelText(row.violationLevel)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scoreDeducted" label="扣分" width="70" align="center">
          <template #default="{ row }"
            ><span style="color: #f56c6c; font-weight: 600"
              >-{{ row.scoreDeducted }}</span
            ></template
          >
        </el-table-column>
        <el-table-column label="处罚类型" width="90" align="center">
          <template #default="{ row }">{{ penaltyTypeText(row.penaltyType) }}</template>
        </el-table-column>
        <el-table-column label="封禁时长" width="100" align="center">
          <template #default="{ row }">{{
            row.banDurationHours ? row.banDurationHours + 'h' : '—'
          }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="penaltyStatusTag(row.status)" size="small">{{
              penaltyStatusText(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申诉" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="appealTag(row.appealStatus)" size="small">{{
              appealText(row.appealStatus)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.appealStatus === 1"
              size="small"
              type="warning"
              link
              @click="showAppealDialog(row, true)"
              >通过申诉</el-button
            >
            <el-button
              v-if="row.appealStatus === 1"
              size="small"
              type="danger"
              link
              @click="showAppealDialog(row, false)"
              >驳回申诉</el-button
            >
            <el-button
              v-if="row.status === 1"
              size="small"
              type="danger"
              link
              @click="handleLift(row)"
              >解除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div class="penalty-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          :current-page="pageNum"
          @size-change="
            (s) => {
              pageSize = s;
              fetchList();
            }
          "
          @current-change="
            (p) => {
              pageNum = p;
              fetchList();
            }
          "
        />
      </div>
    </el-card>

    <!-- 手动处罚弹窗 -->
    <el-dialog v-model="executeVisible" title="手动执行处罚" width="480px" destroy-on-close>
      <el-form :model="executeForm" label-width="100px">
        <el-form-item label="账号ID" prop="accountId">
          <el-input v-model="executeForm.accountId" placeholder="用户/师傅/管理员账号ID" />
        </el-form-item>
        <el-form-item label="账号类型" prop="accountType">
          <el-select
            v-model="executeForm.accountType"
            placeholder="选择账号类型"
            style="width: 100%"
          >
            <el-option label="用户" :value="1" />
            <el-option label="师傅" :value="2" />
            <el-option label="门店管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规等级" prop="violationLevel">
          <el-select
            v-model="executeForm.violationLevel"
            placeholder="选择违规等级"
            style="width: 100%"
            @change="onLevelChange"
          >
            <el-option label="一级（轻微）- 扣5分" :value="1" />
            <el-option label="二级（一般）- 扣10分" :value="2" />
            <el-option label="三级（严重）- 扣20分" :value="3" />
            <el-option label="四级（重大）- 扣40分" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="executeForm.violationLevel" label="预计扣分">
          <el-tag type="danger">{{ scoreMap[executeForm.violationLevel - 1] }}分</el-tag>
        </el-form-item>
        <el-form-item label="处罚类型" prop="penaltyType">
          <el-select
            v-model="executeForm.penaltyType"
            placeholder="选择处罚类型"
            style="width: 100%"
          >
            <el-option label="警告" :value="1" />
            <el-option label="功能限制" :value="2" />
            <el-option label="封禁" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="executeForm.penaltyType === 3" label="封禁时长(小时)">
          <el-input-number
            v-model="executeForm.banDurationHours"
            :min="1"
            :max="8760"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="executeForm.remark"
            type="textarea"
            :rows="2"
            placeholder="处罚原因说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" :loading="executing" @click="confirmExecute">确认执行</el-button>
      </template>
    </el-dialog>

    <!-- 申诉处理弹窗 -->
    <el-dialog
      v-model="appealVisible"
      :title="appealApproved ? '通过申诉' : '驳回申诉'"
      width="440px"
      destroy-on-close
    >
      <el-form :model="appealForm" label-width="80px">
        <el-form-item label="申诉原因">
          <div style="color: #606266; padding: 8px; background: #f5f7fa; border-radius: 4px">
            {{ currentPenalty?.appealReason || '—' }}
          </div>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input
            v-model="appealForm.result"
            type="textarea"
            :rows="2"
            :placeholder="appealApproved ? '申诉通过说明（选填）' : '驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button
          :type="appealApproved ? 'success' : 'danger'"
          :loading="appealing"
          @click="confirmAppeal"
          >确认</el-button
        >
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="处罚详情" width="500px" destroy-on-close>
      <el-descriptions v-if="currentPenalty" :column="2" border>
        <el-descriptions-item label="处罚ID">{{ currentPenalty.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="penaltyStatusTag(currentPenalty.status)" size="small">{{
            penaltyStatusText(currentPenalty.status)
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="被处罚人">{{ currentPenalty.accountId }}</el-descriptions-item>
        <el-descriptions-item label="账号类型">{{
          accountTypeText(currentPenalty.accountType)
        }}</el-descriptions-item>
        <el-descriptions-item label="违规等级">{{
          levelText(currentPenalty.violationLevel)
        }}</el-descriptions-item>
        <el-descriptions-item label="扣分"
          >-{{ currentPenalty.scoreDeducted }}</el-descriptions-item
        >
        <el-descriptions-item label="处罚类型">{{
          penaltyTypeText(currentPenalty.penaltyType)
        }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentPenalty.operatorId }}</el-descriptions-item>
        <el-descriptions-item v-if="currentPenalty.banDurationHours" label="封禁时长"
          >{{ currentPenalty.banDurationHours }}小时</el-descriptions-item
        >
        <el-descriptions-item v-if="currentPenalty.banEndTime" label="封禁结束">{{
          formatTime(currentPenalty.banEndTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{
          formatTime(currentPenalty.createdTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{
          currentPenalty.remark || '—'
        }}</el-descriptions-item>
        <el-descriptions-item label="申诉状态">
          <el-tag :type="appealTag(currentPenalty.appealStatus)" size="small">{{
            appealText(currentPenalty.appealStatus)
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentPenalty.appealReason" label="申诉原因">{{
          currentPenalty.appealReason
        }}</el-descriptions-item>
        <el-descriptions-item v-if="currentPenalty.appealResult" label="申诉结果" :span="2">{{
          currentPenalty.appealResult
        }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchPenalties, executePenalty, processAppeal, liftPenalty } from '../../api/adminPenalty';

const scoreMap = [5, 10, 20, 40];

const loading = ref(false);
const penaltyList = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterStatus = ref(null);
const filterViolationLevel = ref(null);
const filterAppealStatus = ref(null);

const detailVisible = ref(false);
const currentPenalty = ref(null);

const executeVisible = ref(false);
const executing = ref(false);
const executeForm = reactive({
  accountId: '',
  accountType: 1,
  violationLevel: null,
  penaltyType: 1,
  banDurationHours: 168,
  remark: ''
});

const appealVisible = ref(false);
const appealApproved = ref(true);
const appealing = ref(false);
const appealForm = reactive({ result: '' });

onMounted(() => {
  fetchList();
});

async function fetchList() {
  loading.value = true;
  try {
    const res = await fetchPenalties({
      page: pageNum.value,
      size: pageSize.value,
      status: filterStatus.value || undefined,
      violationLevel: filterViolationLevel.value || undefined,
      appealStatus: filterAppealStatus.value !== null ? filterAppealStatus.value : undefined
    });
    if (res.code === 200 && res.data) {
      penaltyList.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } finally {
    loading.value = false;
  }
}

function showDetail(row) {
  currentPenalty.value = row;
  detailVisible.value = true;
}

function showExecuteDialog() {
  Object.assign(executeForm, {
    accountId: '',
    accountType: 1,
    violationLevel: null,
    penaltyType: 1,
    banDurationHours: 168,
    remark: ''
  });
  executeVisible.value = true;
}

function onLevelChange() {}

async function confirmExecute() {
  if (!executeForm.accountId || !executeForm.violationLevel || !executeForm.penaltyType) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  executing.value = true;
  try {
    const res = await executePenalty({
      accountId: executeForm.accountId,
      accountType: executeForm.accountType,
      violationLevel: executeForm.violationLevel,
      penaltyType: executeForm.penaltyType,
      banDurationHours: executeForm.penaltyType === 3 ? executeForm.banDurationHours : null,
      remark: executeForm.remark
    });
    if (res.code === 200) {
      ElMessage.success('处罚已执行');
      executeVisible.value = false;
      fetchList();
    }
  } finally {
    executing.value = false;
  }
}

function showAppealDialog(row, approved) {
  currentPenalty.value = row;
  appealApproved.value = approved;
  appealForm.result = '';
  appealVisible.value = true;
}

async function confirmAppeal() {
  appealing.value = true;
  try {
    const res = await processAppeal(currentPenalty.value.id, {
      approved: appealApproved.value,
      result: appealForm.result
    });
    if (res.code === 200) {
      ElMessage.success(appealApproved.value ? '申诉已通过，积分已恢复' : '申诉已驳回');
      appealVisible.value = false;
      fetchList();
    }
  } finally {
    appealing.value = false;
  }
}

async function handleLift(row) {
  try {
    await ElMessageBox.confirm('确认解除该处罚吗？', '提示', { type: 'warning' });
    const res = await liftPenalty(row.id);
    if (res.code === 200) {
      ElMessage.success('处罚已解除');
      fetchList();
    }
  } catch (e) {
    /* cancel */
  }
}

function formatTime(ts) {
  return ts ? new Date(ts).toLocaleString('zh-CN') : '—';
}
function levelTag(l) {
  return l === 1 ? '' : l === 2 ? 'warning' : l === 3 ? 'danger' : 'danger';
}
function levelText(l) {
  return l === 1
    ? '一级·轻微'
    : l === 2
      ? '二级·一般'
      : l === 3
        ? '三级·严重'
        : l === 4
          ? '四级·重大'
          : '未知';
}
function penaltyTypeText(t) {
  return t === 1 ? '警告' : t === 2 ? '功能限制' : t === 3 ? '封禁' : '未知';
}
function penaltyStatusTag(s) {
  return s === 1 ? 'danger' : s === 2 ? 'success' : 'info';
}
function penaltyStatusText(s) {
  return s === 1 ? '执行中' : s === 2 ? '已解除' : s === 3 ? '已过期' : '未知';
}
function appealTag(s) {
  return s === 0 ? 'info' : s === 1 ? 'warning' : s === 2 ? 'success' : 'danger';
}
function appealText(s) {
  return s === 0 ? '未申诉' : s === 1 ? '申诉中' : s === 2 ? '已通过' : s === 3 ? '已驳回' : '未知';
}
function accountTypeText(t) {
  return t === 1 ? '用户' : t === 2 ? '师傅' : t === 3 ? '门店管理员' : '未知';
}
</script>

<style scoped>
.penalty-page {
  padding: 16px;
  box-sizing: border-box;
}
.penalty-card {
  width: 100%;
}
.penalty-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 12px;
}
.penalty-title-group {
  display: flex;
  flex-direction: column;
}
.penalty-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.penalty-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}
.penalty-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.penalty-table {
  width: 100%;
}
.penalty-table-header {
  background-color: #f5f7fa;
}
.penalty-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
