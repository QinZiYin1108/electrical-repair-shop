<template>
  <div class="ops-page">
    <el-card shadow="never" class="mb16">
      <div class="page-header">
        <div>
          <div class="page-title">预约停业 / 请假</div>
          <div class="page-subtitle">
            配置不可预约时段（全平台 / 师傅 / 门店），下单与改约据此校验。
          </div>
        </div>
        <div>
          <el-button type="primary" @click="openCreate">新增时段</el-button>
          <el-button :icon="Refresh" @click="loadClosures">刷新</el-button>
        </div>
      </div>

      <div class="filter-panel">
        <el-select
          v-model="closureFilter.ownerType"
          clearable
          placeholder="范围"
          class="w160"
          @change="loadClosures"
        >
          <el-option value="GLOBAL" label="全平台" />
          <el-option value="TECHNICIAN" label="师傅" />
          <el-option value="STORE" label="门店" />
        </el-select>
        <el-input
          v-model="closureFilter.ownerId"
          placeholder="对象ID"
          clearable
          class="w180"
          @keyup.enter="loadClosures"
        />
        <el-button type="primary" @click="loadClosures">查询</el-button>
      </div>

      <el-table v-loading="closureLoading" :data="closures" border>
        <el-table-column prop="ownerType" label="范围" width="110" align="center" />
        <el-table-column prop="ownerId" label="对象ID" width="180" show-overflow-tooltip />
        <el-table-column label="开始" width="170">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束" width="170">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeClosure(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <div class="page-header">
        <div class="page-title">师傅时段占用</div>
        <el-button :icon="Refresh" @click="loadReservations">刷新</el-button>
      </div>
      <div class="filter-panel">
        <el-input
          v-model="reservationFilter.technicianAccountId"
          placeholder="师傅账号ID"
          clearable
          class="w220"
          @keyup.enter="loadReservations"
        />
        <el-button type="primary" @click="loadReservations">查询</el-button>
      </div>
      <el-table v-loading="reservationLoading" :data="reservations" border>
        <el-table-column
          prop="technicianAccountId"
          label="师傅账号"
          width="180"
          show-overflow-tooltip
        />
        <el-table-column prop="orderId" label="占用订单" width="180" show-overflow-tooltip />
        <el-table-column label="预约时间" width="170">
          <template #default="{ row }">{{ formatTime(row.appointmentTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{
              row.status === 1 ? '占用中' : '已释放'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="releaseReason"
          label="释放原因"
          min-width="160"
          show-overflow-tooltip
        />
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="新增停业/请假时段" width="480px">
      <el-form label-width="90px">
        <el-form-item label="范围">
          <el-select v-model="createForm.ownerType" class="w-full">
            <el-option value="GLOBAL" label="全平台" />
            <el-option value="TECHNICIAN" label="师傅" />
            <el-option value="STORE" label="门店" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.ownerType !== 'GLOBAL'" label="对象ID">
          <el-input v-model="createForm.ownerId" />
        </el-form-item>
        <el-form-item label="时间段">
          <el-date-picker
            v-model="createForm.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="x"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="createForm.reason" placeholder="节假日 / 临时停业 / 请假" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import {
  fetchAppointmentClosures,
  createAppointmentClosure,
  deleteAppointmentClosure,
  fetchAppointmentReservations
} from '../../api/adminAppointmentCapacity';

const closureLoading = ref(false);
const closures = ref([]);
const closureFilter = reactive({ ownerType: null, ownerId: '' });

const reservationLoading = ref(false);
const reservations = ref([]);
const reservationFilter = reactive({ technicianAccountId: '' });

const createVisible = ref(false);
const creating = ref(false);
const createForm = reactive({ ownerType: 'GLOBAL', ownerId: '', range: null, reason: '' });

function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(Number(ts));
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function loadClosures() {
  closureLoading.value = true;
  try {
    const res = await fetchAppointmentClosures({
      ownerType: closureFilter.ownerType || undefined,
      ownerId: closureFilter.ownerId || undefined,
      limit: 100
    });
    if (res.code === 200) closures.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    closureLoading.value = false;
  }
}

async function loadReservations() {
  reservationLoading.value = true;
  try {
    const res = await fetchAppointmentReservations({
      technicianAccountId: reservationFilter.technicianAccountId || undefined,
      limit: 100
    });
    if (res.code === 200) reservations.value = res.data || [];
  } catch (e) {
    ElMessage.error(e.message || '加载失败');
  } finally {
    reservationLoading.value = false;
  }
}

function openCreate() {
  Object.assign(createForm, { ownerType: 'GLOBAL', ownerId: '', range: null, reason: '' });
  createVisible.value = true;
}

async function doCreate() {
  if (!createForm.range || createForm.range.length !== 2) {
    ElMessage.warning('请选择时间段');
    return;
  }
  if (createForm.ownerType !== 'GLOBAL' && !createForm.ownerId) {
    ElMessage.warning('请填写对象ID');
    return;
  }
  creating.value = true;
  try {
    await createAppointmentClosure({
      ownerType: createForm.ownerType,
      ownerId: createForm.ownerId || null,
      startTime: createForm.range[0],
      endTime: createForm.range[1],
      reason: createForm.reason
    });
    ElMessage.success('已保存');
    createVisible.value = false;
    loadClosures();
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  } finally {
    creating.value = false;
  }
}

async function removeClosure(row) {
  try {
    await ElMessageBox.confirm('确认删除该停业/请假时段？', '删除', { type: 'warning' });
    await deleteAppointmentClosure(row.id);
    ElMessage.success('已删除');
    loadClosures();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
}

onMounted(() => {
  loadClosures();
  loadReservations();
});
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
.filter-panel {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.w160 {
  width: 160px;
}
.w180 {
  width: 180px;
}
.w220 {
  width: 220px;
}
.w-full {
  width: 100%;
}
</style>
