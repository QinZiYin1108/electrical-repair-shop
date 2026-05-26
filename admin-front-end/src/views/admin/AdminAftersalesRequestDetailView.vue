<template>
  <div class="detail-page" v-loading="loading">
    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回列表</el-button>
            <span class="card-title">售后申请详情</span>
          </div>
          <el-tag v-if="detail.id" :type="getStatusTagType(detail.status)" size="large">
            {{ detail.statusText || '-' }}
          </el-tag>
        </div>
      </template>

      <template v-if="detail.id">
        <el-row :gutter="16" class="summary-row">
          <el-col :span="8">
            <div class="summary-panel">
              <div class="summary-label">订单号</div>
              <div class="summary-value">{{ detail.orderNo || '-' }}</div>
              <div class="summary-meta">服务项目：{{ detail.serviceTypeName || '-' }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-panel">
              <div class="summary-label">申请人</div>
              <div class="summary-value">{{ detail.userName || '-' }}</div>
              <div class="summary-meta">联系电话：{{ detail.userPhone || '-' }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-panel">
              <div class="summary-label">维修师傅</div>
              <div class="summary-value">{{ detail.technicianName || '-' }}</div>
              <div class="summary-meta">服务方式：{{ detail.serviceModeText || '-' }}</div>
            </div>
          </el-col>
        </el-row>

        <el-descriptions :column="2" border class="detail-block">
          <el-descriptions-item label="申请类型">{{ detail.applicationTypeText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ detail.orderStatusText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ detail.paymentStatusText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(detail.createdTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ formatTime(detail.processedTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(detail.completedTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务分类">{{ detail.serviceCategoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务地址">{{ detail.serviceAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系手机">{{ detail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系地址">{{ detail.contactAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单总额">￥{{ detail.totalAmount || '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="已支付">￥{{ detail.paidAmount || '0.00' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-block text-block">
          <div class="block-title">申请原因</div>
          <div class="block-content">{{ detail.reason || '-' }}</div>
        </div>

        <div class="detail-block text-block">
          <div class="block-title">问题说明</div>
          <div class="block-content">{{ detail.description || '用户未补充说明' }}</div>
        </div>

        <div v-if="detail.adminRemark" class="detail-block text-block">
          <div class="block-title">处理备注</div>
          <div class="block-content">{{ detail.adminRemark }}</div>
        </div>

        <div class="detail-block">
          <div class="block-title">图片证据</div>
          <div v-if="detail.evidenceImages.length" class="image-grid">
            <el-image
              v-for="item in detail.evidenceImages"
              :key="item.id || item.url"
              class="evidence-image"
              :src="item.url"
              :preview-src-list="imagePreviewList"
              preview-teleported
              fit="cover"
            />
          </div>
          <el-empty v-else description="暂无图片证据" :image-size="72" />
        </div>

        <div class="detail-block">
          <div class="block-title">视频证据</div>
          <div v-if="detail.evidenceVideos.length" class="video-grid">
            <div v-for="item in detail.evidenceVideos" :key="item.id || item.url" class="video-card">
              <video class="evidence-video" :src="item.url" :poster="item.thumbnailUrl" controls />
              <div class="video-name">{{ item.name || '售后视频' }}</div>
              <div class="video-meta">{{ item.duration ? `${item.duration} 秒` : '时长未知' }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无视频证据" :image-size="72" />
        </div>

        <div v-if="detail.canProcess" class="detail-block">
          <div class="block-title">处理操作</div>
          <el-form label-width="88px" class="process-form">
            <el-form-item label="处理结果">
              <el-radio-group v-model="processForm.action">
                <el-radio label="refund">退款</el-radio>
                <el-radio label="reject">驳回申请</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="备注说明">
              <el-input
                v-model="processForm.adminRemark"
                type="textarea"
                :rows="4"
                maxlength="200"
                show-word-limit
                placeholder="请填写处理说明，用户将在售后详情中看到该备注。"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="processing" @click="submitProcess">提交处理</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchAdminAfterSalesDetail, processAdminAfterSales } from '../../api/adminAftersales';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const processing = ref(false);

const detail = reactive({
  id: '',
  status: 0,
  statusText: '',
  orderNo: '',
  applicationTypeText: '',
  orderStatusText: '',
  paymentStatusText: '',
  serviceTypeName: '',
  serviceCategoryName: '',
  serviceModeText: '',
  userName: '',
  userPhone: '',
  technicianName: '',
  reason: '',
  description: '',
  adminRemark: '',
  contactPhone: '',
  contactAddress: '',
  serviceAddress: '',
  totalAmount: '0.00',
  paidAmount: '0.00',
  createdTime: 0,
  processedTime: 0,
  completedTime: 0,
  canProcess: false,
  evidenceImages: [],
  evidenceVideos: []
});

const processForm = reactive({
  action: 'refund',
  adminRemark: ''
});

const imagePreviewList = computed(() => detail.evidenceImages.map((item) => item.url).filter(Boolean));

function getStatusTagType(value) {
  if (value === 1) return 'warning';
  if (value === 4) return 'primary';
  if (value === 5) return 'success';
  if (value === 3 || value === 6) return 'info';
  return 'danger';
}

function formatTime(value) {
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) return '';
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function applyDetail(data) {
  detail.id = data.id || '';
  detail.status = data.status || 0;
  detail.statusText = data.statusText || '';
  detail.orderNo = data.orderNo || '';
  detail.applicationTypeText = data.applicationTypeText || '';
  detail.orderStatusText = data.orderStatusText || '';
  detail.paymentStatusText = data.paymentStatusText || '';
  detail.serviceTypeName = data.serviceTypeName || '';
  detail.serviceCategoryName = data.serviceCategoryName || '';
  detail.serviceModeText = data.serviceModeText || '';
  detail.userName = data.userName || '';
  detail.userPhone = data.userPhone || '';
  detail.technicianName = data.technicianName || '';
  detail.reason = data.reason || '';
  detail.description = data.description || '';
  detail.adminRemark = data.adminRemark || '';
  detail.contactPhone = data.contactPhone || '';
  detail.contactAddress = data.contactAddress || '';
  detail.serviceAddress = data.serviceAddress || '';
  detail.totalAmount = data.totalAmount || '0.00';
  detail.paidAmount = data.paidAmount || '0.00';
  detail.createdTime = data.createdTime || 0;
  detail.processedTime = data.processedTime || 0;
  detail.completedTime = data.completedTime || 0;
  detail.canProcess = !!data.canProcess;
  detail.evidenceImages = Array.isArray(data.evidenceImages) ? data.evidenceImages : [];
  detail.evidenceVideos = Array.isArray(data.evidenceVideos) ? data.evidenceVideos : [];

  if (detail.canProcess) {
    processForm.action = 'refund';
    processForm.adminRemark = '';
  }
}

async function loadDetail() {
  const id = route.params.id;
  if (!id) return;
  loading.value = true;
  try {
    const res = await fetchAdminAfterSalesDetail(id);
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      return;
    }
    throw new Error((res && res.message) || '获取售后详情失败');
  } catch (error) {
    ElMessage.error(error.message || '获取售后详情失败');
  } finally {
    loading.value = false;
  }
}

async function submitProcess() {
  if (!detail.id || !detail.canProcess || processing.value) return;
  processing.value = true;
  try {
    const res = await processAdminAfterSales(detail.id, {
      action: processForm.action,
      adminRemark: processForm.adminRemark.trim()
    });
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      ElMessage.success('售后申请已处理');
      return;
    }
    throw new Error((res && res.message) || '处理售后申请失败');
  } catch (error) {
    ElMessage.error(error.message || '处理售后申请失败');
  } finally {
    processing.value = false;
  }
}

function goBack() {
  router.push('/admin/aftersales/requests');
}

onMounted(() => {
  loadDetail();
});

useAdminPageRefresh(async () => {
  await loadDetail();
});
</script>

<style scoped>
.detail-page {
  padding: 16px;
  box-sizing: border-box;
}

.detail-card {
  width: 100%;
}

.card-header,
.header-left {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-panel {
  height: 100%;
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #f5f7fa 100%);
  border: 1px solid #e5edf7;
  box-sizing: border-box;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
  word-break: break-all;
}

.summary-meta {
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.detail-block {
  margin-top: 18px;
}

.block-title {
  margin-bottom: 12px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.text-block {
  padding: 18px 20px;
  border-radius: 16px;
  background-color: #fafcff;
  border: 1px solid #e8eef6;
}

.block-content {
  color: #475467;
  line-height: 1.8;
  word-break: break-word;
}

.image-grid,
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.evidence-image {
  width: 100%;
  height: 160px;
  border-radius: 14px;
  overflow: hidden;
}

.video-card {
  padding: 12px;
  border: 1px solid #e5edf7;
  border-radius: 16px;
  background-color: #fafcff;
}

.evidence-video {
  width: 100%;
  height: 220px;
  border-radius: 12px;
  background-color: #111827;
}

.video-name {
  margin-top: 10px;
  color: #303133;
  font-weight: 600;
}

.video-meta {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.process-form {
  max-width: 760px;
  padding: 20px;
  border-radius: 16px;
  background-color: #fafcff;
  border: 1px solid #e8eef6;
}
</style>
