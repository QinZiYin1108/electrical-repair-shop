<template>
  <div class="product-detail-page" v-loading="loading">
    <div v-if="detail.id" class="detail-shell">
      <section class="hero-card">
        <div class="hero-main">
          <div class="hero-topbar">
            <el-button text @click="goBack">返回列表</el-button>
            <div class="hero-tags">
              <el-tag :type="getOrderTagType(detail.orderStatus)" size="large">{{ detail.orderStatusText || '-' }}</el-tag>
              <el-tag :type="getPaymentTagType(detail.paymentStatus)" size="large" effect="plain">{{ detail.paymentStatusText || '-' }}</el-tag>
              <el-tag :type="getDeliveryTagType(detail.deliveryStatus)" size="large" effect="plain">{{ detail.deliveryStatusText || '-' }}</el-tag>
            </div>
          </div>
          <div class="hero-title-row">
            <div class="hero-heading">
              <h1 class="hero-title">商品订单详情</h1>
              <p class="hero-subtitle">集中查看商品明细、支付记录和发货信息。</p>
            </div>
            <div v-if="canShip" class="hero-actions">
              <el-button type="primary" round @click="openShipDialog">立即发货</el-button>
            </div>
          </div>
        </div>
        <div class="hero-stats">
          <div class="stat-card accent-blue">
            <span class="stat-label">订单编号</span>
            <strong class="stat-value order-no">{{ detail.orderNo || '-' }}</strong>
            <span class="stat-meta">共 {{ detail.itemCount || 0 }} 件商品</span>
          </div>
          <div class="stat-card accent-green">
            <span class="stat-label">收货人</span>
            <strong class="stat-value">{{ detail.deliveryName || '-' }}</strong>
            <span class="stat-meta">{{ detail.deliveryPhone || '-' }}</span>
          </div>
          <div class="stat-card accent-amber">
            <span class="stat-label">实付金额</span>
            <strong class="stat-value">{{ formatMoney(detail.actualAmount) }}</strong>
            <span class="stat-meta">{{ detail.paymentMethodText || '未支付' }}</span>
          </div>
          <div class="stat-card accent-slate">
            <span class="stat-label">下单时间</span>
            <strong class="stat-value time-value">{{ formatTime(detail.createdTime) || '-' }}</strong>
            <span class="stat-meta">支付于 {{ formatTime(detail.paymentTime) || '-' }}</span>
          </div>
        </div>
      </section>

      <div class="content-grid">
        <div class="main-column">
          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">收货与物流</h2>
                <p class="section-desc">查看收货地址和当前物流信息。</p>
              </div>
            </div>
            <div class="info-grid two-column">
              <div v-for="item in shippingInfoItems" :key="item.label" class="info-item" :class="{ wide: item.wide }">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>

          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">商品清单</h2>
                <p class="section-desc">展示订单内所有商品与数量信息。</p>
              </div>
            </div>
            <div class="goods-list">
              <article v-for="item in detail.items" :key="item.id" class="goods-item">
                <el-image class="goods-image" :src="item.productImage" fit="cover">
                  <template #error>
                    <div class="goods-image-empty">暂无图片</div>
                  </template>
                </el-image>
                <div class="goods-main">
                  <div class="goods-name">{{ item.productName || '-' }}</div>
                  <div class="goods-meta">单价 {{ formatMoney(item.productPrice) }} / 数量 x{{ item.quantity || 0 }}</div>
                </div>
                <div class="goods-total">{{ formatMoney(item.totalPrice) }}</div>
              </article>
            </div>
          </section>

          <section class="section-card">
            <div class="section-head">
              <div>
                <h2 class="section-title">支付信息</h2>
                <p class="section-desc">查看支付方式、支付单号和金额拆分。</p>
              </div>
            </div>
            <div class="money-summary">
              <div class="money-card emphasis-card">
                <span class="money-label">订单合计</span>
                <strong class="money-value">{{ formatMoney(detail.totalAmount) }}</strong>
              </div>
              <div class="money-card paid-card">
                <span class="money-label">实付金额</span>
                <strong class="money-value">{{ formatMoney(detail.actualAmount) }}</strong>
              </div>
              <div class="money-card muted-card" v-for="item in amountInfoItems" :key="item.label">
                <span class="money-label">{{ item.label }}</span>
                <strong class="money-subvalue">{{ item.value }}</strong>
              </div>
            </div>
            <div class="info-grid two-column top-gap">
              <div v-for="item in paymentInfoItems" :key="item.label" class="info-item" :class="{ wide: item.wide }">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>
        </div>

        <aside class="side-column">
          <section class="section-card side-card">
            <div class="section-head compact-head">
              <div>
                <h2 class="section-title">状态时间</h2>
                <p class="section-desc">查看订单处理关键节点。</p>
              </div>
            </div>
            <div class="info-grid single-column compact-grid">
              <div v-for="item in statusInfoItems" :key="item.label" class="info-item">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>

          <section class="section-card side-card">
            <div class="section-head compact-head">
              <div>
                <h2 class="section-title">备注与售后</h2>
                <p class="section-desc">订单备注、取消和退款说明。</p>
              </div>
            </div>
            <div class="info-grid single-column compact-grid">
              <div v-for="item in afterSaleInfoItems" :key="item.label" class="info-item">
                <span class="info-label">{{ item.label }}</span>
                <span class="info-value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </div>

    <el-dialog v-model="shipDialogVisible" title="商品订单发货" width="460px" destroy-on-close>
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="88px">
        <el-form-item label="快递公司" prop="deliveryCompany">
          <el-input v-model="shipForm.deliveryCompany" maxlength="30" placeholder="请输入快递公司" />
        </el-form-item>
        <el-form-item label="快递单号" prop="deliveryNo">
          <el-input v-model="shipForm.deliveryNo" maxlength="50" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span>
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import {
  fetchAdminProductOrderDetail,
  shipAdminProductOrder
} from '../../api/adminProductOrders';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const shipDialogVisible = ref(false);
const shipSubmitting = ref(false);
const shipFormRef = ref();
const shipForm = reactive({
  deliveryCompany: '',
  deliveryNo: ''
});
const shipRules = {
  deliveryCompany: [{ required: true, message: '请输入快递公司', trigger: 'blur' }],
  deliveryNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }]
};

const detail = reactive({
  id: '',
  orderNo: '',
  orderStatus: 0,
  orderStatusText: '',
  paymentStatus: 0,
  paymentStatusText: '',
  deliveryStatus: 0,
  deliveryStatusText: '',
  userName: '',
  userPhone: '',
  deliveryName: '',
  deliveryPhone: '',
  deliveryAddress: '',
  deliveryCompany: '',
  deliveryNo: '',
  itemCount: 0,
  totalAmount: '0.00',
  productAmount: '0.00',
  shippingFee: '0.00',
  discountAmount: '0.00',
  actualAmount: '0.00',
  paymentMethodText: '',
  paymentNo: '',
  thirdPartyNo: '',
  paymentAmount: '0.00',
  paymentRemark: '',
  remark: '',
  cancelReason: '',
  cancelTime: 0,
  refundReason: '',
  refundAmount: '0.00',
  refundTime: 0,
  createdTime: 0,
  paymentTime: 0,
  deliveryTime: 0,
  receiveTime: 0,
  completionTime: 0,
  items: []
});

const canShip = computed(() => Number(detail.orderStatus) === 2 && Number(detail.deliveryStatus) === 1 && Number(detail.paymentStatus) === 2);

const shippingInfoItems = computed(() => [
  { label: '收货人', value: detail.deliveryName },
  { label: '联系电话', value: detail.deliveryPhone },
  { label: '快递公司', value: detail.deliveryCompany || '待填写' },
  { label: '快递单号', value: detail.deliveryNo || '待填写' },
  { label: '收货地址', value: detail.deliveryAddress, wide: true }
]);

const amountInfoItems = computed(() => [
  { label: '商品金额', value: formatMoney(detail.productAmount) },
  { label: '运费', value: formatMoney(detail.shippingFee) },
  { label: '优惠金额', value: formatMoney(detail.discountAmount) }
]);

const paymentInfoItems = computed(() => [
  { label: '支付方式', value: detail.paymentMethodText },
  { label: '支付单号', value: detail.paymentNo },
  { label: '第三方流水', value: normalizeThirdPartyNo(detail.thirdPartyNo) || '-', wide: true },
  { label: '支付备注', value: detail.paymentRemark || '暂无', wide: true }
]);

const statusInfoItems = computed(() => [
  { label: '下单时间', value: formatTime(detail.createdTime) },
  { label: '支付时间', value: formatTime(detail.paymentTime) },
  { label: '发货时间', value: formatTime(detail.deliveryTime) },
  { label: '收货时间', value: formatTime(detail.receiveTime) },
  { label: '完成时间', value: formatTime(detail.completionTime) }
]);

const afterSaleInfoItems = computed(() => {
  const items = [{ label: '订单备注', value: detail.remark || '暂无备注' }];
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

function getOrderTagType(value) {
  if (value === 2 || value === 3 || value === 4) return 'warning';
  if (value === 5) return 'success';
  return 'info';
}

function getPaymentTagType(value) {
  if (value === 2) return 'success';
  if (value === 3) return 'info';
  return 'warning';
}

function getDeliveryTagType(value) {
  if (value === 1) return 'warning';
  if (value === 2 || value === 3) return 'primary';
  if (value === 4) return 'success';
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

function normalizeThirdPartyNo(value) {
  const text = String(value || '').trim();
  if (!text) return '';
  if (!text.startsWith('MOCK-')) return text;
  return text.replace(/^MOCK-/, '').replace(/-/g, '');
}

function formatMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '￥0.00';
  return `￥${amount.toFixed(2)}`;
}

function applyDetail(data) {
  Object.assign(detail, {
    id: data.id || '',
    orderNo: data.orderNo || '',
    orderStatus: data.orderStatus || 0,
    orderStatusText: data.orderStatusText || '',
    paymentStatus: data.paymentStatus || 0,
    paymentStatusText: data.paymentStatusText || '',
    deliveryStatus: data.deliveryStatus || 0,
    deliveryStatusText: data.deliveryStatusText || '',
    userName: data.userName || '',
    userPhone: data.userPhone || '',
    deliveryName: data.deliveryName || '',
    deliveryPhone: data.deliveryPhone || '',
    deliveryAddress: data.deliveryAddress || '',
    deliveryCompany: data.deliveryCompany || '',
    deliveryNo: data.deliveryNo || '',
    itemCount: data.itemCount || 0,
    totalAmount: data.totalAmount || '0.00',
    productAmount: data.productAmount || '0.00',
    shippingFee: data.shippingFee || '0.00',
    discountAmount: data.discountAmount || '0.00',
    actualAmount: data.actualAmount || '0.00',
    paymentMethodText: data.paymentMethodText || '',
    paymentNo: data.paymentNo || '',
    thirdPartyNo: normalizeThirdPartyNo(data.thirdPartyNo),
    paymentAmount: data.paymentAmount || '0.00',
    paymentRemark: data.paymentRemark || '',
    remark: data.remark || '',
    cancelReason: data.cancelReason || '',
    cancelTime: data.cancelTime || 0,
    refundReason: data.refundReason || '',
    refundAmount: data.refundAmount || '0.00',
    refundTime: data.refundTime || 0,
    createdTime: data.createdTime || 0,
    paymentTime: data.paymentTime || 0,
    deliveryTime: data.deliveryTime || 0,
    receiveTime: data.receiveTime || 0,
    completionTime: data.completionTime || 0,
    items: Array.isArray(data.items) ? data.items : []
  });
}

async function loadDetail() {
  const { id } = route.params;
  if (!id) return;
  loading.value = true;
  try {
    const res = await fetchAdminProductOrderDetail(id);
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      return;
    }
    throw new Error((res && res.message) || '获取商品订单详情失败');
  } catch (error) {
    ElMessage.error(error.message || '获取商品订单详情失败');
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/admin/orders/product');
}

function openShipDialog() {
  shipForm.deliveryCompany = detail.deliveryCompany || '';
  shipForm.deliveryNo = detail.deliveryNo || '';
  shipDialogVisible.value = true;
}

async function submitShip() {
  if (!shipFormRef.value || !detail.id) return;
  const valid = await shipFormRef.value.validate().catch(() => false);
  if (!valid) return;
  shipSubmitting.value = true;
  try {
    const res = await shipAdminProductOrder(detail.id, {
      deliveryCompany: shipForm.deliveryCompany,
      deliveryNo: shipForm.deliveryNo
    });
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      shipDialogVisible.value = false;
      ElMessage.success('发货成功');
      return;
    }
    throw new Error((res && res.message) || '发货失败');
  } catch (error) {
    ElMessage.error(error.message || '发货失败');
  } finally {
    shipSubmitting.value = false;
  }
}

onMounted(() => {
  loadDetail();
});

useAdminPageRefresh(async () => {
  await loadDetail();
});
</script>

<style scoped>
.product-detail-page {
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

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.hero-topbar,
.hero-title-row,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hero-topbar,
.hero-title-row {
  flex-wrap: wrap;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-title-row {
  align-items: flex-end;
  gap: 16px;
}

.hero-heading {
  flex: 1 1 420px;
  min-width: 0;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
}

.hero-actions :deep(.el-button) {
  min-width: 108px;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  color: #10233f;
}

.hero-subtitle,
.section-desc,
.stat-meta {
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

.accent-blue::after { background: #3b82f6; }
.accent-green::after { background: #10b981; }
.accent-amber::after { background: #f59e0b; }
.accent-slate::after { background: #64748b; }

.stat-label,
.money-label,
.info-label {
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

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
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

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid #e6edf5;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbfe 100%);
}

.goods-image,
.goods-image-empty {
  width: 84px;
  height: 84px;
  flex-shrink: 0;
  border-radius: 18px;
  overflow: hidden;
}

.goods-image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 12px;
}

.goods-main {
  flex: 1;
  min-width: 0;
}

.goods-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.6;
}

.goods-meta {
  margin-top: 8px;
  font-size: 13px;
  color: #64748b;
}

.goods-total {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.money-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

.top-gap {
  margin-top: 14px;
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

  .hero-title-row {
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .product-detail-page {
    padding: 12px;
  }

  .hero-card,
  .section-card {
    padding: 18px;
    border-radius: 20px;
  }

  .hero-stats,
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

  .hero-topbar,
  .hero-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>





