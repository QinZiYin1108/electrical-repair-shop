<template>
  <div class="ops-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">客服工单</div>
          <div class="page-subtitle">
            关联订单/支付/退款/售后；支持分派、优先级、备注与双人审批（金额超阈值）。
          </div>
        </div>
        <div>
          <el-button type="primary" @click="openCreate">新建工单</el-button>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </div>

      <div class="filter-panel">
        <el-select
          v-model="filter.status"
          clearable
          placeholder="状态"
          class="w160"
          @change="loadList"
        >
          <el-option
            v-for="(label, code) in STATUS_TEXT"
            :key="code"
            :value="Number(code)"
            :label="label"
          />
        </el-select>
        <el-select
          v-model="filter.priority"
          clearable
          placeholder="优先级"
          class="w160"
          @change="loadList"
        >
          <el-option
            v-for="(label, code) in PRIORITY_TEXT"
            :key="code"
            :value="Number(code)"
            :label="label"
          />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="ticketNo" label="工单号" width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" show-overflow-tooltip />
        <el-table-column label="关联" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bizType || '-' }} / {{ row.bizId || '-' }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="priorityTagType(row.priority)">{{
              PRIORITY_TEXT[row.priority] || row.priority
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{
              STATUS_TEXT[row.status] || row.status
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeAdminId" label="处理人" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建工单" width="560px">
      <el-form label-width="90px">
        <el-form-item label="标题"><el-input v-model="createForm.title" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="createForm.category" /></el-form-item>
        <el-form-item label="内容"
          ><el-input v-model="createForm.content" type="textarea" :rows="3"
        /></el-form-item>
        <el-form-item label="关联类型"
          ><el-input
            v-model="createForm.bizType"
            placeholder="REPAIR_ORDER / PRODUCT_ORDER / PAYMENT / REFUND / AFTER_SALES / ACCOUNT"
        /></el-form-item>
        <el-form-item label="关联ID"><el-input v-model="createForm.bizId" /></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" class="w-full">
            <el-option
              v-for="(label, code) in PRIORITY_TEXT"
              :key="code"
              :value="Number(code)"
              :label="label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联金额"
          ><el-input-number v-model="createForm.amount" :min="0" :precision="2" class="w-full"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="工单详情" size="620px">
      <template v-if="detail.ticket">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="工单号">{{ detail.ticket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.ticket.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusTagType(detail.ticket.status)">{{
              STATUS_TEXT[detail.ticket.status]
            }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">{{
            PRIORITY_TEXT[detail.ticket.priority]
          }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{
            detail.ticket.assigneeAdminId || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="关联"
            >{{ detail.ticket.bizType }} / {{ detail.ticket.bizId }}</el-descriptions-item
          >
          <el-descriptions-item label="需审批">{{
            detail.ticket.requiresApproval === 1 ? '是' : '否'
          }}</el-descriptions-item>
          <el-descriptions-item label="内容">{{
            detail.ticket.content || '-'
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-bar">
          <el-button size="small" @click="doAssign">分派</el-button>
          <el-button size="small" @click="doPriority">优先级</el-button>
          <el-button size="small" @click="doStatus">变更状态</el-button>
          <el-button size="small" @click="doComment">备注</el-button>
          <el-button
            v-if="detail.ticket.status === 3"
            size="small"
            type="success"
            @click="doApprove"
            >审批通过</el-button
          >
          <el-button v-if="detail.ticket.status === 3" size="small" type="danger" @click="doReject"
            >审批驳回</el-button
          >
        </div>

        <el-divider>操作记录</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="log in detail.logs"
            :key="log.id"
            :timestamp="formatTime(log.createdTime)"
          >
            <div>
              <b>{{ log.action }}</b> · {{ log.operatorAdminId || '系统' }}
              {{ log.content ? '：' + log.content : '' }}
            </div>
            <div v-if="log.attachments" class="muted">附件：{{ log.attachments }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchSupportTickets,
  fetchSupportTicketDetail,
  createSupportTicket,
  assignSupportTicket,
  changeSupportTicketPriority,
  changeSupportTicketStatus,
  commentSupportTicket,
  approveSupportTicket,
  rejectSupportTicket
} from '../../api/adminSupportTickets';

const STATUS_TEXT = { 1: '待处理', 2: '处理中', 3: '待审批', 4: '已解决', 5: '已关闭' };
const PRIORITY_TEXT = { 1: '高', 2: '中', 3: '低' };

const loading = ref(false);
const list = ref([]);
const filter = reactive({ status: null, priority: null });

const createVisible = ref(false);
const creating = ref(false);
const createForm = reactive({
  title: '',
  category: '',
  content: '',
  bizType: '',
  bizId: '',
  priority: 2,
  amount: 0
});

const detailVisible = ref(false);
const detail = reactive({ ticket: null, logs: [] });

function statusTagType(status) {
  return { 1: 'warning', 2: 'primary', 3: 'danger', 4: 'success', 5: 'info' }[status] || '';
}
function priorityTagType(priority) {
  return { 1: 'danger', 2: 'warning', 3: 'info' }[priority] || '';
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
    const res = await fetchSupportTickets({
      status: filter.status ?? undefined,
      priority: filter.priority ?? undefined,
      limit: 100
    });
    if (res.code === 200) list.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(createForm, {
    title: '',
    category: '',
    content: '',
    bizType: '',
    bizId: '',
    priority: 2,
    amount: 0
  });
  createVisible.value = true;
}

async function doCreate() {
  if (!createForm.title) {
    ElMessage.warning('请输入标题');
    return;
  }
  creating.value = true;
  try {
    await createSupportTicket({ ...createForm });
    ElMessage.success('已创建');
    createVisible.value = false;
    loadList();
  } catch (e) {
    ElMessage.error(e.message || '创建失败');
  } finally {
    creating.value = false;
  }
}

async function openDetail(row) {
  detailVisible.value = true;
  detail.ticket = null;
  detail.logs = [];
  try {
    const res = await fetchSupportTicketDetail(row.id);
    if (res.code === 200 && res.data) {
      detail.ticket = res.data.ticket;
      detail.logs = res.data.logs || [];
    }
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败');
  }
}

async function refreshDetail() {
  if (detail.ticket) await openDetail(detail.ticket);
}

async function doAssign() {
  try {
    const { value } = await ElMessageBox.prompt('请输入处理人账号ID', '分派', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入处理人')
    });
    await assignSupportTicket(detail.ticket.id, { assigneeAdminId: value, remark: '' });
    ElMessage.success('已分派');
    refreshDetail();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function doPriority() {
  try {
    const { value } = await ElMessageBox.prompt('优先级：1-高 / 2-中 / 3-低', '调整优先级', {
      inputValue: String(detail.ticket.priority || 2)
    });
    await changeSupportTicketPriority(detail.ticket.id, { priority: Number(value), remark: '' });
    ElMessage.success('已调整');
    refreshDetail();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function doStatus() {
  try {
    const { value } = await ElMessageBox.prompt(
      '目标状态：2-处理中 / 3-待审批 / 4-已解决 / 5-已关闭',
      '变更状态',
      {
        inputValue: ''
      }
    );
    await changeSupportTicketStatus(detail.ticket.id, {
      status: Number(value),
      remark: ''
    });
    ElMessage.success('已变更');
    refreshDetail();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function doComment() {
  try {
    const { value } = await ElMessageBox.prompt('请输入备注内容', '追加备注', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入内容')
    });
    await commentSupportTicket(detail.ticket.id, { content: value, attachments: '' });
    ElMessage.success('已备注');
    refreshDetail();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function doApprove() {
  try {
    await ElMessageBox.confirm('确认审批通过？（审批人不能为创建人）', '审批通过', {
      type: 'warning'
    });
    await approveSupportTicket(detail.ticket.id, { remark: '' });
    ElMessage.success('已通过');
    refreshDetail();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

async function doReject() {
  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '审批驳回', {
      inputValidator: (v) => (v && v.trim() ? true : '请输入原因')
    });
    await rejectSupportTicket(detail.ticket.id, { remark: value });
    ElMessage.success('已驳回');
    refreshDetail();
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
.w160 {
  width: 160px;
}
.w-full {
  width: 100%;
}
.action-bar {
  margin: 14px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.muted {
  color: #909399;
  font-size: 12px;
}
</style>
