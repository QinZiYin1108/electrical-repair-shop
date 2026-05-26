<template>
  <div class="reserve-detail-page" v-loading="loading">
    <div v-if="detail.id" class="detail-shell">
      <section class="hero-card">
        <div class="hero-main">
          <div class="hero-topbar">
            <el-button text @click="goBack">返回列表</el-button>
            <div class="hero-tags">
              <el-tag :type="getStatusTagType(detail.status)" size="large">{{ detail.statusText || '-' }}</el-tag>
              <el-tag :type="getPaymentTagType(detail.paymentStatus)" size="large" effect="plain">
                {{ detail.paymentStatusText || '-' }}
              </el-tag>
            </div>
          </div>
          <div class="hero-title-row">
            <div>
              <h1 class="hero-title">预约订单详情</h1>
              <p class="hero-subtitle">集中查看预约订单的服务信息、费用构成与处理进度。</p>
            </div>
          </div>
        </div>
        <div class="hero-stats">
          <div class="stat-card accent-blue">
            <span class="stat-label">订单编号</span>
            <strong class="stat-value order-no">{{ detail.orderNo || '-' }}</strong>
            <span class="stat-meta">{{ detail.serviceTypeName || '-' }}</span>
          </div>
          <div class="stat-card accent-green">
            <span class="stat-label">服务用户</span>
            <strong class="stat-value">{{ detail.userName || detail.contactName || '-' }}</strong>
            <span class="stat-meta">{{ detail.userPhone || detail.contactPhone || '-' }}</span>
          </div>
          <div class="stat-card accent-amber">
            <span class="stat-label">维修师傅</span>
            <strong class="stat-value">{{ detail.technicianName || '暂未分配' }}</strong>
            <span class="stat-meta">{{ detail.technicianPhone || '暂无联系电话' }}</span>
          </div>
          <div class="stat-card accent-slate">
            <span class="stat-label">预约时间</span>
            <strong class="stat-value time-value">{{ formatTime(detail.appointmentTime) || '-' }}</strong>
            <span class="stat-meta">创建于 {{ formatTime(detail.createdTime) || '-' }}</span>
          </div>
        </div>
      </section>

      <div class="content-grid">
        <div class="main-column">
          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">服务信息概览</h2>
                <p class="section-desc">服务类型、设备型号和预约地址等基础信息。</p>
              </div>
            </div>
            <div class="info-grid two-column">
              <div v-for="item in serviceInfoItems" :key="item.label" class="info-item" :class="{ wide: item.wide }">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>

          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">客户与师傅信息</h2>
                <p class="section-desc">快速确认客户联系方式及当前指派师傅信息。</p>
              </div>
            </div>
            <div class="split-panels">
              <div class="mini-panel">
                <div class="mini-panel-title">客户信息</div>
                <div class="info-grid single-column compact-grid">
                  <div v-for="item in userInfoItems" :key="item.label" class="info-item">
                    <span class="info-label">{{ item.label }}</span>
                    <span class="info-value">{{ item.value || '-' }}</span>
                  </div>
                </div>
              </div>
              <div class="mini-panel">
                <div class="mini-panel-title">师傅信息</div>
                <div class="info-grid single-column compact-grid">
                  <div v-for="item in technicianInfoItems" :key="item.label" class="info-item">
                    <span class="info-label">{{ item.label }}</span>
                    <span class="info-value">{{ item.value || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">费用信息</h2>
                <p class="section-desc">查看订单金额、已支付金额与费用拆分。</p>
              </div>
            </div>
            <div class="money-summary">
              <div class="money-card emphasis-card">
                <span class="money-label">订单总额</span>
                <strong class="money-value">{{ formatMoney(detail.totalAmount) }}</strong>
              </div>
              <div class="money-card paid-card">
                <span class="money-label">已支付</span>
                <strong class="money-value">{{ formatMoney(detail.paidAmount) }}</strong>
              </div>
              <div class="money-card muted-card" v-for="item in feeInfoItems" :key="item.label">
                <span class="money-label">{{ item.label }}</span>
                <strong class="money-subvalue">{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">故障信息</h2>
                <p class="section-desc">用户提交的故障现象、补充说明与现场凭证。</p>
              </div>
            </div>
            <div v-if="detail.faultSummary" class="summary-strip">
              <span class="summary-strip-label">故障摘要</span>
              <span class="summary-strip-value">{{ detail.faultSummary }}</span>
            </div>
            <div v-if="detail.faultList.length" class="fault-list">
              <article v-for="item in detail.faultList" :key="item.id" class="fault-card">
                <div class="fault-header">
                  <div>
                    <div class="fault-title">{{ item.faultPhenomenonName || '未知故障' }}</div>
                    <div class="fault-desc">{{ item.faultDescription || item.faultPhenomenonDescription || '暂无补充说明' }}</div>
                  </div>
                </div>
                <div v-if="item.images && item.images.length" class="media-section">
                  <div class="media-label">图片凭证</div>
                  <div class="image-grid">
                    <el-image
                      v-for="media in item.images"
                      :key="media.id || media.url"
                      class="evidence-image"
                      :src="media.url"
                      :preview-src-list="buildPreviewList(item.images)"
                      preview-teleported
                      fit="cover"
                    />
                  </div>
                </div>
                <div v-if="item.videos && item.videos.length" class="media-section">
                  <div class="media-label">视频凭证</div>
                  <div class="video-grid">
                    <div v-for="media in item.videos" :key="media.id || media.url" class="video-card">
                      <video class="evidence-video" :src="media.url" :poster="media.thumbnailUrl" controls />
                    </div>
                  </div>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无故障信息" :image-size="72" />
          </section>

          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">检查结果</h2>
                <p class="section-desc">师傅上传的检查结论、维修方案与检查凭证。</p>
              </div>
            </div>
            <div class="inspection-grid">
              <div class="mini-panel soft-panel">
                <div class="mini-panel-title">检查结论</div>
                <div class="inspect-text-block">
                  <div class="inspect-label">检查时间</div>
                  <div class="inspect-value">{{ formatTime(detail.inspectionTime) || '-' }}</div>
                </div>
                <div class="inspect-text-block">
                  <div class="inspect-label">问题说明</div>
                  <div class="inspect-value multiline">{{ detail.inspectionDiagnosis || '-' }}</div>
                </div>
                <div class="inspect-text-block">
                  <div class="inspect-label">维修方案</div>
                  <div class="inspect-value multiline">{{ detail.repairPlan || '-' }}</div>
                </div>
              </div>
              <div class="mini-panel soft-panel">
                <div class="mini-panel-title">检查图片</div>
                <div v-if="detail.inspectionImages.length" class="image-grid compact-media-grid">
                  <el-image
                    v-for="media in detail.inspectionImages"
                    :key="media.id || media.url"
                    class="evidence-image"
                    :src="media.url"
                    :preview-src-list="buildPreviewList(detail.inspectionImages)"
                    preview-teleported
                    fit="cover"
                  />
                </div>
                <el-empty v-else description="暂无检查图片" :image-size="64" />
              </div>
            </div>
            <div class="media-section top-gap">
              <div class="media-label">检查视频</div>
              <div v-if="detail.inspectionVideos.length" class="video-grid">
                <div v-for="media in detail.inspectionVideos" :key="media.id || media.url" class="video-card">
                  <video class="evidence-video" :src="media.url" :poster="media.thumbnailUrl" controls />
                </div>
              </div>
              <el-empty v-else description="暂无检查视频" :image-size="64" />
            </div>
          </section>
        </div>

        <aside class="side-column">
          <section class="section-card side-card">
            <div class="section-head compact-head">
              <div>
                <h2 class="section-title">状态信息</h2>
                <p class="section-desc">关键状态与时间节点。</p>
              </div>
            </div>
            <div class="info-grid single-column compact-grid">
              <div v-for="item in statusInfoItems" :key="item.label" class="info-item">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>

          <section v-if="afterSaleInfoItems.length" class="section-card side-card">
            <div class="section-head compact-head">
              <div>
                <h2 class="section-title">取消 / 退款信息</h2>
                <p class="section-desc">订单取消或退款时的补充说明。</p>
              </div>
            </div>
            <div class="info-grid single-column compact-grid">
              <div v-for="item in afterSaleInfoItems" :key="item.label" class="info-item">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>

          <section class="section-card side-card timeline-card">
            <div class="section-head compact-head">
              <div>
                <h2 class="section-title">处理进度</h2>
                <p class="section-desc">按时间查看订单流转记录。</p>
              </div>
            </div>
            <el-timeline v-if="detail.progressList.length">
              <el-timeline-item
                v-for="item in detail.progressList"
                :key="item.id"
                :timestamp="formatTime(item.createdTime) || '-'"
                placement="top"
                :type="getTimelineType(item.status)"
              >
                <div class="timeline-title">{{ item.statusText || '-' }}</div>
                <div class="timeline-meta">{{ item.operatorName || '-' }} / {{ formatOperatorType(item.operatorType) }}</div>
                <div class="timeline-desc">{{ item.description || '-' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无进度记录" :image-size="64" />
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchAdminReserveOrderDetail } from '../../api/adminOrders';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';

const route = useRoute();
const router = useRouter();
const loading = ref(false);

const detail = reactive({
  id: '',
  orderNo: '',
  status: 0,
  statusText: '',
  paymentStatus: 0,
  paymentStatusText: '',
  serviceTypeName: '',
  serviceCategoryName: '',
  serviceCategoryPath: '',
  serviceModeText: '',
  userName: '',
  userPhone: '',
  userEmail: '',
  technicianName: '',
  technicianPhone: '',
  technicianEmail: '',
  contactName: '',
  contactPhone: '',
  serviceAddress: '',
  serviceAddressShort: '',
  applianceBrand: '',
  applianceModel: '',
  purchaseDate: 0,
  appointmentTime: 0,
  createdTime: 0,
  startTime: 0,
  endTime: 0,
  completionTime: 0,
  faultSummary: '',
  totalAmount: '0.00',
  paidAmount: '0.00',
  doorFee: '0.00',
  distanceFee: '0.00',
  serviceFee: '0.00',
  materialFee: '0.00',
  overtimeFee: '0.00',
  remark: '',
  inspectionDiagnosis: '',
  repairPlan: '',
  inspectionTime: 0,
  cancelReason: '',
  cancelTime: 0,
  refundReason: '',
  refundAmount: '0.00',
  refundTime: 0,
  faultList: [],
  inspectionImages: [],
  inspectionVideos: [],
  progressList: []
});

const serviceInfoItems = computed(() => [
  { label: '服务类型', value: detail.serviceTypeName },
  { label: '服务分类', value: detail.serviceCategoryPath || detail.serviceCategoryName },
  { label: '服务方式', value: detail.serviceModeText },
  { label: '预约时间', value: formatTime(detail.appointmentTime) },
  { label: '完整地址', value: detail.serviceAddress, wide: true },
  { label: '详细地址', value: detail.serviceAddressShort },
  { label: '设备品牌', value: detail.applianceBrand },
  { label: '设备型号', value: detail.applianceModel },
  { label: '购买日期', value: formatDate(detail.purchaseDate) },
  { label: '订单备注', value: detail.remark, wide: true }
]);

const userInfoItems = computed(() => [
  { label: '用户姓名', value: detail.userName || detail.contactName },
  { label: '联系电话', value: detail.userPhone || detail.contactPhone },
  { label: '邮箱地址', value: detail.userEmail },
  { label: '联系人', value: detail.contactName },
  { label: '联系手机', value: detail.contactPhone }
]);

const technicianInfoItems = computed(() => [
  { label: '师傅姓名', value: detail.technicianName || '暂未分配' },
  { label: '师傅电话', value: detail.technicianPhone },
  { label: '师傅邮箱', value: detail.technicianEmail }
]);

const feeInfoItems = computed(() => [
  { label: '上门费', value: formatMoney(detail.doorFee) },
  { label: '距离费', value: formatMoney(detail.distanceFee) },
  { label: '服务费', value: formatMoney(detail.serviceFee) },
  { label: '材料费', value: formatMoney(detail.materialFee) },
  { label: '加班费', value: formatMoney(detail.overtimeFee) }
]);

const statusInfoItems = computed(() => [
  { label: '订单状态', value: detail.statusText },
  { label: '支付状态', value: detail.paymentStatusText },
  { label: '创建时间', value: formatTime(detail.createdTime) },
  { label: '开始服务', value: formatTime(detail.startTime) },
  { label: '结束服务', value: formatTime(detail.endTime) },
  { label: '完成时间', value: formatTime(detail.completionTime) }
]);

const afterSaleInfoItems = computed(() => {
  const items = [];
  if (detail.cancelReason) {
    items.push({ label: '取消原因', value: detail.cancelReason });
  }
  if (detail.cancelTime) {
    items.push({ label: '取消时间', value: formatTime(detail.cancelTime) });
  }
  if (detail.refundReason) {
    items.push({ label: '退款原因', value: detail.refundReason });
  }
  if (detail.refundTime) {
    items.push({ label: '退款时间', value: formatTime(detail.refundTime) });
    items.push({ label: '退款金额', value: formatMoney(detail.refundAmount) });
  }
  return items;
});

function getStatusTagType(value) {
  if (value === 1 || value === 2 || value === 3 || value === 4) return 'warning';
  if (value === 5) return 'primary';
  if (value === 6) return 'success';
  return 'info';
}

function getPaymentTagType(value) {
  if (value === 2) return 'success';
  if (value === 3) return 'info';
  return 'warning';
}

function getTimelineType(value) {
  if (value === 6) return 'success';
  if (value === 7 || value === 8) return 'info';
  if (value === 5) return 'primary';
  return 'warning';
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

function formatDate(value) {
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) return '';
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '￥0.00';
  return `￥${amount.toFixed(2)}`;
}

function formatOperatorType(value) {
  if (value === 1) return '用户';
  if (value === 2) return '师傅';
  if (value === 3) return '管理员';
  if (value === 4) return '系统';
  return '未知';
}

function buildPreviewList(list) {
  return Array.isArray(list) ? list.map((item) => item.url).filter(Boolean) : [];
}

function applyDetail(data) {
  Object.assign(detail, {
    id: data.id || '',
    orderNo: data.orderNo || '',
    status: data.status || 0,
    statusText: data.statusText || '',
    paymentStatus: data.paymentStatus || 0,
    paymentStatusText: data.paymentStatusText || '',
    serviceTypeName: data.serviceTypeName || '',
    serviceCategoryName: data.serviceCategoryName || '',
    serviceCategoryPath: data.serviceCategoryPath || '',
    serviceModeText: data.serviceModeText || '',
    userName: data.userName || '',
    userPhone: data.userPhone || '',
    userEmail: data.userEmail || '',
    technicianName: data.technicianName || '',
    technicianPhone: data.technicianPhone || '',
    technicianEmail: data.technicianEmail || '',
    contactName: data.contactName || '',
    contactPhone: data.contactPhone || '',
    serviceAddress: data.serviceAddress || '',
    serviceAddressShort: data.serviceAddressShort || '',
    applianceBrand: data.applianceBrand || '',
    applianceModel: data.applianceModel || '',
    purchaseDate: data.purchaseDate || 0,
    appointmentTime: data.appointmentTime || 0,
    createdTime: data.createdTime || 0,
    startTime: data.startTime || 0,
    endTime: data.endTime || 0,
    completionTime: data.completionTime || 0,
    faultSummary: data.faultSummary || '',
    totalAmount: data.totalAmount || '0.00',
    paidAmount: data.paidAmount || '0.00',
    doorFee: data.doorFee || '0.00',
    distanceFee: data.distanceFee || '0.00',
    serviceFee: data.serviceFee || '0.00',
    materialFee: data.materialFee || '0.00',
    overtimeFee: data.overtimeFee || '0.00',
    remark: data.remark || '',
    inspectionDiagnosis: data.inspectionDiagnosis || '',
    repairPlan: data.repairPlan || '',
    inspectionTime: data.inspectionTime || 0,
    cancelReason: data.cancelReason || '',
    cancelTime: data.cancelTime || 0,
    refundReason: data.refundReason || '',
    refundAmount: data.refundAmount || '0.00',
    refundTime: data.refundTime || 0,
    faultList: Array.isArray(data.faultList) ? data.faultList : [],
    inspectionImages: Array.isArray(data.inspectionImages) ? data.inspectionImages : [],
    inspectionVideos: Array.isArray(data.inspectionVideos) ? data.inspectionVideos : [],
    progressList: Array.isArray(data.progressList) ? data.progressList : []
  });
}

async function loadDetail() {
  const { id } = route.params;
  if (!id) return;
  loading.value = true;
  try {
    const res = await fetchAdminReserveOrderDetail(id);
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      return;
    }
    throw new Error((res && res.message) || '获取预约订单详情失败');
  } catch (error) {
    ElMessage.error(error.message || '获取预约订单详情失败');
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/admin/orders/reserve');
}

onMounted(() => {
  loadDetail();
});

useAdminPageRefresh(async () => {
  await loadDetail();
});
</script>

<style scoped>
.reserve-detail-page {
  padding: 16px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at top right, rgba(191, 219, 254, 0.35), transparent 24%),
    linear-gradient(180deg, #f6f8fc 0%, #eef3f9 100%);
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card,
.section-card {
  border: 1px solid rgba(208, 216, 230, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
}

.hero-card {
  padding: 24px;
}

.hero-topbar,
.hero-title-row,
.section-head,
.fault-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  color: #10233f;
}

.hero-subtitle,
.section-desc,
.stat-meta,
.timeline-meta,
.fault-desc,
.inspect-value.multiline {
  color: #6b7280;
}

.hero-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.stat-card {
  position: relative;
  padding: 18px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
}

.stat-card::after {
  content: '';
  position: absolute;
  inset: auto -30px -40px auto;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0.12;
}

.accent-blue::after {
  background: #3b82f6;
}

.accent-green::after {
  background: #10b981;
}

.accent-amber::after {
  background: #f59e0b;
}

.accent-slate::after {
  background: #64748b;
}

.stat-label,
.money-label,
.info-label,
.inspect-label,
.summary-strip-label {
  display: block;
  color: #6b7280;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.stat-value,
.money-value {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.3;
}

.order-no {
  font-size: 18px;
  word-break: break-all;
}

.time-value {
  font-size: 18px;
}

.stat-meta {
  display: block;
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.6;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.85fr);
  gap: 18px;
  align-items: start;
}

.main-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-card {
  padding: 22px;
}

.compact-head {
  margin-bottom: 14px;
}

.section-title,
.mini-panel-title,
.block-title,
.fault-title,
.timeline-title {
  margin: 0;
  color: #0f172a;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
}

.section-desc {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.info-grid {
  display: grid;
  gap: 12px;
}

.two-column {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.single-column {
  grid-template-columns: minmax(0, 1fr);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 80px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e6edf5;
}

.info-item.wide {
  grid-column: span 2;
}

.info-value {
  color: #1f2937;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}

.split-panels,
.inspection-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.mini-panel {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e5edf7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.soft-panel {
  background: linear-gradient(180deg, #fbfdff 0%, #f4f8fd 100%);
}

.mini-panel-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 14px;
}

.compact-grid .info-item {
  min-height: auto;
  padding: 14px 16px;
}

.money-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.money-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e5edf7;
}

.emphasis-card {
  background: linear-gradient(135deg, #0f4c81 0%, #2969a3 100%);
}

.paid-card {
  background: linear-gradient(135deg, #147a63 0%, #1f9d7a 100%);
}

.muted-card {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.emphasis-card .money-label,
.emphasis-card .money-value,
.paid-card .money-label,
.paid-card .money-value {
  color: #fff;
}

.money-subvalue {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 18px;
}

.summary-strip {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(90deg, rgba(14, 116, 144, 0.08), rgba(59, 130, 246, 0.04));
  border: 1px solid rgba(103, 176, 233, 0.22);
}

.summary-strip-value {
  color: #0f172a;
  line-height: 1.8;
}

.fault-list {
  display: grid;
  gap: 16px;
}

.fault-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #e6edf5;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbfe 100%);
}

.fault-title {
  font-size: 16px;
  font-weight: 700;
}

.fault-desc,
.timeline-desc,
.inspect-value {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}

.media-section {
  margin-top: 16px;
}

.top-gap {
  margin-top: 14px;
}

.media-label {
  margin-bottom: 10px;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
}

.image-grid,
.video-grid,
.compact-media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}

.evidence-image {
  width: 100%;
  height: 160px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e5edf7;
}

.video-card {
  padding: 10px;
  border: 1px solid #e5edf7;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.evidence-video {
  width: 100%;
  height: 220px;
  display: block;
  border-radius: 14px;
  background-color: #0f172a;
}

.inspect-text-block + .inspect-text-block {
  margin-top: 14px;
}

.inspect-value {
  color: #1f2937;
}

.side-card {
  position: relative;
}

.timeline-card :deep(.el-timeline-item__timestamp) {
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1360px) {
  .hero-stats,
  .money-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .side-column {
    order: -1;
  }
}

@media (max-width: 768px) {
  .reserve-detail-page {
    padding: 12px;
  }

  .hero-card,
  .section-card {
    padding: 18px;
    border-radius: 20px;
  }

  .hero-stats,
  .split-panels,
  .inspection-grid,
  .money-summary,
  .two-column {
    grid-template-columns: minmax(0, 1fr);
  }

  .info-item.wide {
    grid-column: span 1;
  }

  .hero-title {
    font-size: 24px;
  }
}
</style>
