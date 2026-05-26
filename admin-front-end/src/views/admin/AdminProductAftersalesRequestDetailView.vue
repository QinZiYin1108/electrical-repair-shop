<template>
  <div class="detail-page" v-loading="loading">
    <el-card shadow="never" class="detail-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回列表</el-button>
            <span class="card-title">商品售后详情</span>
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
              <div class="summary-meta">商品：{{ detail.productSummary || '-' }}</div>
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
              <div class="summary-label">收货信息</div>
              <div class="summary-value">{{ detail.deliveryName || '-' }}</div>
              <div class="summary-meta">{{ detail.deliveryPhone || '-' }}</div>
            </div>
          </el-col>
        </el-row>

        <el-descriptions :column="2" border class="detail-block">
          <el-descriptions-item label="售后类型">{{ detail.applicationTypeText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ detail.orderStatusText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ detail.paymentStatusText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流状态">{{ detail.deliveryStatusText || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(detail.createdTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ formatTime(detail.processedTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(detail.completedTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系手机">{{ detail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系地址" :span="2">{{ detail.contactAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="快递公司">{{ detail.deliveryCompany || '待填写' }}</el-descriptions-item>
          <el-descriptions-item label="快递单号">{{ detail.deliveryNo || '待填写' }}</el-descriptions-item>
          <el-descriptions-item label="订单总额">￥{{ detail.totalAmount || '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">￥{{ detail.paidAmount || '0.00' }}</el-descriptions-item>
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
          <div class="block-title">商品明细</div>
          <el-table :data="detail.items" border>
            <el-table-column prop="productName" label="商品名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="productId" label="商品ID" min-width="140" show-overflow-tooltip />
            <el-table-column label="单价" width="110" align="center">
              <template #default="{ row }">￥{{ row.productPrice || '0.00' }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column label="小计" width="120" align="center">
              <template #default="{ row }">￥{{ row.totalPrice || '0.00' }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-block">
          <div class="block-title">图片凭证</div>
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
          <el-empty v-else description="暂无图片凭证" :image-size="72" />
        </div>

        <div class="detail-block">
          <div class="block-title">视频凭证</div>
          <div v-if="detail.evidenceVideos.length" class="video-grid">
            <div v-for="item in detail.evidenceVideos" :key="item.id || item.url" class="video-card">
              <video class="evidence-video" :src="item.url" :poster="item.thumbnailUrl" controls preload="metadata" />
              <div class="video-meta">{{ item.duration ? `${item.duration} 秒` : '时长未知' }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无视频凭证" :image-size="72" />
        </div>

        <div v-if="detail.canApprove || detail.canReject || detail.canRefund" class="detail-block">
          <div class="block-title">处理操作</div>
          <el-form label-width="88px" class="process-form">
            <el-form-item label="处理结果">
              <el-radio-group v-model="processForm.action">
                <el-radio v-if="detail.canApprove" label="approve">
                  {{ detail.applicationType === 1 ? '同意并退款' : '审核通过' }}
                </el-radio>
                <el-radio v-if="detail.canReject" label="reject">驳回申请</el-radio>
                <el-radio v-if="detail.canRefund" label="refund">确认退款</el-radio>
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
import { fetchAdminProductAfterSalesDetail, processAdminProductAfterSales } from '../../api/adminAftersales';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const processing = ref(false);

const detail = reactive({
  id: '',
  status: 0,
  statusText: '',
  applicationType: 0,
  applicationTypeText: '',
  orderNo: '',
  orderStatusText: '',
  paymentStatusText: '',
  deliveryStatusText: '',
  userName: '',
  userPhone: '',
  deliveryName: '',
  deliveryPhone: '',
  deliveryAddress: '',
  deliveryCompany: '',
  deliveryNo: '',
  productSummary: '',
  totalAmount: '0.00',
  paidAmount: '0.00',
  reason: '',
  description: '',
  adminRemark: '',
  contactPhone: '',
  contactAddress: '',
  createdTime: 0,
  processedTime: 0,
  completedTime: 0,
  canApprove: false,
  canReject: false,
  canRefund: false,
  items: [],
  evidenceImages: [],
  evidenceVideos: []
});

const processForm = reactive({
  action: 'approve',
  adminRemark: ''
});

const imagePreviewList = computed(() => detail.evidenceImages.map((item) => item.url).filter(Boolean));

function getStatusTagType(value) {
  if (value === 1) return 'warning';
  if (value === 2) return 'primary';
  if (value === 5) return 'success';
  return 'info';
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
  Object.assign(detail, {
    id: data.id || '',
    status: data.status || 0,
    statusText: data.statusText || '',
    applicationType: data.applicationType || 0,
    applicationTypeText: data.applicationTypeText || '',
    orderNo: data.orderNo || '',
    orderStatusText: data.orderStatusText || '',
    paymentStatusText: data.paymentStatusText || '',
    deliveryStatusText: data.deliveryStatusText || '',
    userName: data.userName || '',
    userPhone: data.userPhone || '',
    deliveryName: data.deliveryName || '',
    deliveryPhone: data.deliveryPhone || '',
    deliveryAddress: data.deliveryAddress || '',
    deliveryCompany: data.deliveryCompany || '',
    deliveryNo: data.deliveryNo || '',
    productSummary: data.productSummary || '',
    totalAmount: data.totalAmount || '0.00',
    paidAmount: data.paidAmount || '0.00',
    reason: data.reason || '',
    description: data.description || '',
    adminRemark: data.adminRemark || '',
    contactPhone: data.contactPhone || '',
    contactAddress: data.contactAddress || '',
    createdTime: data.createdTime || 0,
    processedTime: data.processedTime || 0,
    completedTime: data.completedTime || 0,
    canApprove: !!data.canApprove,
    canReject: !!data.canReject,
    canRefund: !!data.canRefund,
    items: Array.isArray(data.items) ? data.items : [],
    evidenceImages: Array.isArray(data.evidenceImages) ? data.evidenceImages : [],
    evidenceVideos: Array.isArray(data.evidenceVideos) ? data.evidenceVideos : []
  });

  if (detail.canApprove) {
    processForm.action = 'approve';
  } else if (detail.canRefund) {
    processForm.action = 'refund';
  } else {
    processForm.action = 'reject';
  }
  processForm.adminRemark = '';
}

async function loadDetail() {
  const id = route.params.id;
  if (!id) return;
  loading.value = true;
  try {
    const res = await fetchAdminProductAfterSalesDetail(id);
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      return;
    }
    throw new Error((res && res.message) || '获取商品售后详情失败');
  } catch (error) {
    ElMessage.error(error.message || '获取商品售后详情失败');
  } finally {
    loading.value = false;
  }
}

async function submitProcess() {
  if (!detail.id || processing.value) return;
  if (processForm.action === 'reject' && !processForm.adminRemark.trim()) {
    ElMessage.warning('请填写驳回原因');
    return;
  }
  processing.value = true;
  try {
    const res = await processAdminProductAfterSales(detail.id, {
      action: processForm.action,
      adminRemark: processForm.adminRemark.trim()
    });
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      ElMessage.success('商品售后已处理');
      return;
    }
    throw new Error((res && res.message) || '处理商品售后失败');
  } catch (error) {
    ElMessage.error(error.message || '处理商品售后失败');
  } finally {
    processing.value = false;
  }
}

function goBack() {
  router.push('/admin/aftersales/product-requests');
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

.text-block {
  padding: 18px 20px;
  border-radius: 16px;
  background: #fafcff;
  border: 1px solid #edf2f8;
}

.block-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.block-content {
  color: #606266;
  line-height: 1.8;
  word-break: break-word;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.video-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-start;
}

.evidence-image {
  width: 100%;
  height: 140px;
  border-radius: 14px;
}

.video-card {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 12px;
  border-radius: 14px;
  background: #fafcff;
  border: 1px solid #edf2f8;
  width: fit-content;
  max-width: min(100%, 460px);
}

.evidence-video {
  display: block;
  width: auto;
  max-width: min(100%, 436px);
  height: auto;
  max-height: 320px;
  border-radius: 10px;
  background: #000;
  object-fit: contain;
}

.video-meta {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
