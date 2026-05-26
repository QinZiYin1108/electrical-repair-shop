<template>
  <div class="dashboard-page" v-loading="loading">
    <button
      v-if="slideIndex > 0"
      class="edge-switch edge-switch-left"
      type="button"
      aria-label="上一屏"
      @click="goPrev"
    >
      <el-icon><ArrowLeftBold /></el-icon>
    </button>

    <div class="dashboard-shell">
      <div class="slider-viewport">
        <div class="slider-track" :style="trackStyle">
          <section
            class="dashboard-slide overview-slide"
            :class="{ 'is-active': slideIndex === 0 }"
            :style="slideStyle"
          >
            <div class="overview-layout">
              <article class="hero-panel slide-panel">
                <div class="hero-copy">
                  <div class="hero-chip">LIVE DATA STREAM</div>
                  <h2 class="hero-title">华丽运营驾驶舱</h2>
                  <p class="hero-desc">
                    将订单、履约、营收与售后压缩进一块主舞台，在固定尺寸下仍保持信息层次与视觉张力。
                  </p>
                  <div class="hero-tags">
                    <span>今日订单 {{ overview.todayOrders || 0 }}</span>
                    <span>待处理 {{ overview.pendingOrders || 0 }}</span>
                    <span>今日收款 ¥{{ formatMoney(overview.todayIncome) }}</span>
                  </div>

                  <div class="hero-metrics">
                    <div
                      v-for="card in heroMetricCards"
                      :key="card.label"
                      class="hero-metric"
                      :class="card.className"
                    >
                      <div class="metric-icon">{{ card.icon }}</div>
                      <div class="metric-main">
                        <div class="metric-label">{{ card.label }}</div>
                        <div class="metric-value">{{ card.value }}</div>
                        <div class="metric-extra">{{ card.extra }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="hero-visual">
                  <div class="orbit orbit-main">
                    <span class="orbit-label">累计净收入</span>
                    <strong class="orbit-value">¥{{ formatMoney(overview.totalNetIncome) }}</strong>
                  </div>
                  <div class="orbit orbit-top">
                    <span class="orbit-small-label">活跃师傅</span>
                    <strong class="orbit-small-value">{{ overview.activeWorkers || 0 }}</strong>
                  </div>
                  <div class="orbit orbit-left">
                    <span class="orbit-small-label">完工率</span>
                    <strong class="orbit-small-value">{{ formatPercent(todayCompletionRate) }}</strong>
                  </div>
                  <div class="orbit orbit-right">
                    <span class="orbit-small-label">售后压力</span>
                    <strong class="orbit-small-value">{{ overview.pendingAfterSales || 0 }}</strong>
                  </div>
                </div>
              </article>

              <article class="health-panel slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">SERVICE HEALTH</div>
                    <div class="panel-title">运营健康中枢</div>
                  </div>
                  <div class="score-pill">{{ serviceHealthScore }} 分</div>
                </div>

                <div class="health-progress">
                  <div class="health-progress-bar">
                    <span :style="{ width: `${serviceHealthScore}%` }"></span>
                  </div>
                  <div class="health-progress-text">当前综合健康分由履约率、在岗率、积压率和退款率联合计算。</div>
                </div>

                <div class="health-grid">
                  <div v-for="item in healthCards" :key="item.label" class="health-card">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                    <small>{{ item.desc }}</small>
                  </div>
                </div>
              </article>

              <article class="panel-card trend-card slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">TREND ENGINE</div>
                    <div class="panel-title">订单 / 完工 / 营收联动</div>
                  </div>
                  <div class="panel-tip">近 7 天趋势</div>
                </div>
                <div class="chart-wrap">
                  <GlowChart :option="trendChartOption" height="100%" />
                </div>
              </article>

              <div class="overview-side">
                <article class="panel-card status-card slide-panel">
                  <div class="panel-head">
                    <div>
                      <div class="panel-kicker">ORDER MATRIX</div>
                      <div class="panel-title">订单状态分布</div>
                    </div>
                    <div class="panel-tip">玫瑰图聚合</div>
                  </div>
                  <div class="mini-panel-body">
                    <div class="mini-chart-wrap">
                      <GlowChart :option="statusChartOption" height="100%" />
                    </div>
                    <div class="mini-data-list status-data-list">
                      <div v-for="item in statusPreviewList" :key="item.label" class="mini-data-item">
                        <span class="mini-data-dot" :style="{ background: item.color }"></span>
                        <div class="mini-data-main">
                          <strong>{{ item.count }}</strong>
                          <span>{{ item.label }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </article>

                <article class="panel-card gauge-card slide-panel">
                  <div class="panel-head">
                    <div>
                      <div class="panel-kicker">EFFICIENCY METER</div>
                      <div class="panel-title">履约效率仪表</div>
                    </div>
                    <div class="panel-tip">一眼判断状态</div>
                  </div>
                  <div class="mini-panel-body">
                    <div class="mini-chart-wrap">
                      <GlowChart :option="gaugeChartOption" height="100%" />
                    </div>
                    <div class="mini-data-list gauge-data-list">
                      <div v-for="item in gaugeInsightList" :key="item.label" class="mini-data-item gauge-data-item">
                        <strong>{{ item.value }}</strong>
                        <span>{{ item.label }}</span>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </section>

          <section
            class="dashboard-slide performance-slide"
            :class="{ 'is-active': slideIndex === 1 }"
            :style="slideStyle"
          >
            <div class="performance-layout">
              <article class="spotlight-panel slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">TEAM SPOTLIGHT</div>
                    <div class="panel-title">师傅战力聚光台</div>
                  </div>
                  <div class="panel-tip">TOP 3 实时表现</div>
                </div>

                <div class="performance-summary">
                  <div v-for="item in performanceSummaryCards" :key="item.label" class="summary-chip">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div v-if="topThreeWorkers.length" class="podium-grid">
                  <div
                    v-for="(worker, index) in topThreeWorkers"
                    :key="worker.id || index"
                    class="podium-card"
                    :class="`podium-card-${index + 1}`"
                  >
                    <div class="podium-rank">TOP {{ index + 1 }}</div>
                    <el-avatar :size="40" :src="worker.avatarUrl">{{ getAvatarInitial(worker) }}</el-avatar>
                    <div class="podium-name">{{ worker.realName || worker.username || '未命名师傅' }}</div>
                    <div class="podium-stats">
                      <span class="podium-meta">完工 {{ worker.completedOrders || 0 }}</span>
                      <span class="podium-meta">¥{{ formatMoney(worker.netIncome) }}</span>
                      <span class="podium-meta">评分 {{ formatScore(worker.rating) }}</span>
                    </div>
                    <div class="podium-power">
                      <span :style="{ width: `${calcPodiumWidth(worker.completedOrders)}%` }"></span>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="暂无师傅绩效数据" />
              </article>

              <article class="panel-card radar-card slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">HEALTH RADAR</div>
                    <div class="panel-title">平台综合战力雷达</div>
                  </div>
                  <div class="panel-tip">履约 / 活跃 / 收益 / 售后</div>
                </div>
                <div class="chart-wrap">
                  <GlowChart :option="radarChartOption" height="100%" />
                </div>
              </article>

              <article class="panel-card ranking-card slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">WORKER RANKING</div>
                    <div class="panel-title">完工与净收入联动榜</div>
                  </div>
                  <div class="panel-tip">TOP 5 师傅</div>
                </div>
                <div class="chart-wrap">
                  <GlowChart :option="workerRankChartOption" height="100%" />
                </div>
              </article>

              <div class="performance-side">
                <article class="panel-card signal-card slide-panel">
                  <div class="panel-head">
                    <div>
                      <div class="panel-kicker">ACTION SIGNALS</div>
                      <div class="panel-title">运营指挥摘要</div>
                    </div>
                    <div class="panel-tip">可执行提示</div>
                  </div>
                  <div class="signal-list">
                    <div v-for="item in signalCards" :key="item.label" class="signal-row">
                      <div class="signal-row-main">
                        <span class="signal-row-label">{{ item.label }}</span>
                        <p>{{ item.desc }}</p>
                      </div>
                      <strong class="signal-row-value">{{ item.value }}</strong>
                    </div>
                  </div>
                </article>

                <article class="panel-card matrix-card slide-panel">
                  <div class="panel-head">
                    <div>
                      <div class="panel-kicker">STATUS SNAPSHOT</div>
                      <div class="panel-title">订单状态矩阵</div>
                    </div>
                    <div class="panel-tip">缩略监控</div>
                  </div>
                  <div class="matrix-grid">
                    <div v-for="item in statusChipList" :key="item.status" class="matrix-item">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.count }}</strong>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </section>

          <section
            class="dashboard-slide product-slide"
            :class="{ 'is-active': slideIndex === 2 }"
            :style="slideStyle"
          >
            <div class="product-layout">
              <article class="product-hero-panel slide-panel">
                <div class="hero-copy">
                  <div class="hero-chip">PRODUCT SALES</div>
                  <h2 class="hero-title">商品销售驾驶舱</h2>
                  <p class="hero-desc">
                    聚焦商城订单成交额、待发货压力与热卖 SKU，让管理员在一屏内快速掌握商品业务节奏。
                  </p>
                  <div class="hero-tags">
                    <span>今日成交 ¥{{ formatMoney(productSales.todaySalesAmount) }}</span>
                    <span>今日支付 {{ productSales.todayPaidOrderCount || 0 }}</span>
                    <span>待发货 {{ productSales.pendingDeliveryOrderCount || 0 }}</span>
                  </div>

                  <div class="hero-metrics">
                    <div
                      v-for="card in productHeroCards"
                      :key="card.label"
                      class="hero-metric"
                      :class="card.className"
                    >
                      <div class="metric-icon">{{ card.icon }}</div>
                      <div class="metric-main">
                        <div class="metric-label">{{ card.label }}</div>
                        <div class="metric-value">{{ card.value }}</div>
                        <div class="metric-extra">{{ card.extra }}</div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="hero-visual">
                  <div class="orbit orbit-main">
                    <span class="orbit-label">累计销售额</span>
                    <strong class="orbit-value">¥{{ formatMoney(productSales.totalSalesAmount) }}</strong>
                  </div>
                  <div class="orbit orbit-top">
                    <span class="orbit-small-label">累计销量</span>
                    <strong class="orbit-small-value">{{ productSales.totalSoldQuantity || 0 }}</strong>
                  </div>
                  <div class="orbit orbit-left">
                    <span class="orbit-small-label">热销贡献</span>
                    <strong class="orbit-small-value">{{ formatPercent(productTopContribution) }}</strong>
                  </div>
                  <div class="orbit orbit-right">
                    <span class="orbit-small-label">退款占比</span>
                    <strong class="orbit-small-value">{{ formatPercent(productRefundRate) }}</strong>
                  </div>
                </div>
              </article>

              <div class="product-side">
                <article class="panel-card product-status-card slide-panel">
                  <div class="panel-head">
                    <div>
                      <div class="panel-kicker">ORDER STATUS</div>
                      <div class="panel-title">商品订单状态</div>
                    </div>
                    <div class="panel-tip">待发货压力直观呈现</div>
                  </div>
                  <div class="mini-panel-body">
                    <div class="mini-chart-wrap">
                      <GlowChart :option="productStatusChartOption" height="100%" />
                    </div>
                    <div class="mini-data-list status-data-list">
                      <div v-for="item in productStatusPreviewList" :key="item.label" class="mini-data-item">
                        <span class="mini-data-dot" :style="{ background: item.color }"></span>
                        <div class="mini-data-main">
                          <strong>{{ item.count }}</strong>
                          <span>{{ item.label }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </article>

                <article class="panel-card product-payment-card slide-panel">
                  <div class="panel-head">
                    <div>
                      <div class="panel-kicker">PAYMENT MIX</div>
                      <div class="panel-title">支付方式结构</div>
                    </div>
                    <div class="panel-tip">成交来源分布</div>
                  </div>
                  <div class="mini-panel-body">
                    <div class="mini-chart-wrap">
                      <GlowChart :option="productPaymentChartOption" height="100%" />
                    </div>
                    <div class="mini-data-list">
                      <div
                        v-for="item in productPaymentPreviewList"
                        :key="`${item.label}-${item.paymentMethod}`"
                        class="mini-data-item"
                      >
                        <span class="mini-data-dot" :style="{ background: item.color }"></span>
                        <div class="mini-data-main">
                          <strong>¥{{ formatMoney(item.amount) }}</strong>
                          <span>{{ item.label }} · {{ item.count }} 笔</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </article>
              </div>

              <article class="panel-card product-trend-card slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">SALES TREND</div>
                    <div class="panel-title">近 7 天销量 / 成交额联动</div>
                  </div>
                  <div class="panel-tip">观察放量与回落</div>
                </div>
                <div class="chart-wrap">
                  <GlowChart :option="productSalesTrendChartOption" height="100%" />
                </div>
              </article>

              <article class="panel-card product-ranking-card slide-panel">
                <div class="panel-head">
                  <div>
                    <div class="panel-kicker">HOT SKU</div>
                    <div class="panel-title">热卖商品排行</div>
                  </div>
                  <div class="panel-tip">销量与成交额双维度</div>
                </div>
                <div v-if="topSellingProducts.length" class="product-ranking-list">
                  <div
                    v-for="(item, index) in topSellingProducts"
                    :key="`${item.productId || item.productName}-${index}`"
                    class="product-ranking-item"
                  >
                    <div class="product-ranking-rank">TOP {{ index + 1 }}</div>
                    <el-avatar :size="42" shape="square" :src="item.productImage" class="product-ranking-avatar">
                      {{ getProductInitial(item) }}
                    </el-avatar>
                    <div class="product-ranking-main">
                      <div class="product-ranking-name">{{ item.productName || '未命名商品' }}</div>
                      <div class="product-ranking-meta">
                        <span>{{ item.orderCount || 0 }} 笔订单</span>
                        <span>销量 {{ item.quantity || 0 }}</span>
                      </div>
                      <div class="product-ranking-bar">
                        <span :style="{ width: `${calcProductRankWidth(item.quantity)}%` }"></span>
                      </div>
                    </div>
                    <div class="product-ranking-value">
                      <strong>¥{{ formatMoney(item.salesAmount) }}</strong>
                      <span>{{ formatPercent(calcProductAmountShare(item.salesAmount)) }}</span>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="暂无商品销售数据" />
              </article>
            </div>
          </section>
        </div>
      </div>
    </div>

    <button
      v-if="slideIndex < screens.length - 1"
      class="edge-switch edge-switch-right"
      type="button"
      aria-label="下一屏"
      @click="goNext"
    >
      <el-icon><ArrowRightBold /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ArrowLeftBold, ArrowRightBold } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import GlowChart from '../../components/charts/GlowChart.vue';
import { fetchAdminDashboardOverview, fetchAdminDashboardProductSales } from '../../api/adminDashboard';
import { fetchAdminWorkerPerformance } from '../../api/adminWorkers';

const loading = ref(false);
const slideIndex = ref(0);
const dashboardRefreshInterval = 60000;
const screens = [
  { key: 'overview', label: '总览大屏' },
  { key: 'performance', label: '绩效大屏' },
  { key: 'product-sales', label: '商品销售大屏' }
];

const overview = reactive({
  totalUsers: 0,
  totalWorkers: 0,
  activeWorkers: 0,
  totalOrders: 0,
  todayOrders: 0,
  pendingOrders: 0,
  todayCompletedOrders: 0,
  pendingAfterSales: 0,
  totalGrossIncome: 0,
  totalRefundAmount: 0,
  totalNetIncome: 0,
  todayIncome: 0,
  recentTrend: [],
  orderStatusDistribution: []
});

const productSales = reactive({
  totalOrderCount: 0,
  totalPaidOrderCount: 0,
  todayPaidOrderCount: 0,
  pendingDeliveryOrderCount: 0,
  refundedOrderCount: 0,
  totalSoldQuantity: 0,
  totalSalesAmount: 0,
  todaySalesAmount: 0,
  totalRefundAmount: 0,
  recentTrend: [],
  orderStatusDistribution: [],
  topProducts: [],
  paymentMethodDistribution: []
});

const topWorkers = ref([]);
let autoRefreshTimer = null;
const statusPalette = ['#62d7ff', '#5b88ff', '#5fdeb1', '#ffc56c', '#ff8d68', '#8b7dff', '#f471ba', '#ff6195'];
const paymentPalette = ['#6bd9ff', '#7e8dff', '#ffc56c', '#63dfb2', '#ff8f79'];
const trackStyle = computed(() => ({
  width: `${screens.length * 100}%`,
  transform: `translateX(-${(100 / screens.length) * slideIndex.value}%)`
}));
const slideStyle = computed(() => ({
  width: `${100 / screens.length}%`
}));

const trendList = computed(() => (Array.isArray(overview.recentTrend) ? overview.recentTrend : []));
const statusList = computed(() => (Array.isArray(overview.orderStatusDistribution) ? overview.orderStatusDistribution : []));
const statusChipList = computed(() => statusList.value.slice(0, 6));
const statusPreviewList = computed(() =>
  statusList.value.map((item, index) => ({
    ...item,
    color: statusPalette[index % statusPalette.length]
  }))
);

const activeWorkerRate = computed(() => toPercent(overview.activeWorkers, overview.totalWorkers));
const pendingOrderRate = computed(() => toPercent(overview.pendingOrders, overview.totalOrders));
const todayCompletionRate = computed(() => toPercent(overview.todayCompletedOrders, overview.todayOrders));
const refundRate = computed(() => toPercent(overview.totalRefundAmount, overview.totalGrossIncome));

const serviceHealthScore = computed(() => {
  const completion = Number(todayCompletionRate.value || 0);
  const active = Number(activeWorkerRate.value || 0);
  const pressure = 100 - Number(pendingOrderRate.value || 0);
  const refund = 100 - Number(refundRate.value || 0);
  const score = completion * 0.36 + active * 0.24 + pressure * 0.24 + refund * 0.16;
  return normalizePercent(score);
});

const heroMetricCards = computed(() => [
  {
    icon: '⚡',
    label: '今日订单',
    value: overview.todayOrders || 0,
    extra: `累计 ${overview.totalOrders || 0} 单`,
    className: 'is-blue'
  },
  {
    icon: '🛠',
    label: '今日完工',
    value: overview.todayCompletedOrders || 0,
    extra: `完工率 ${formatPercent(todayCompletionRate.value)}`,
    className: 'is-green'
  },
  {
    icon: '💰',
    label: '今日收款',
    value: `¥${formatMoney(overview.todayIncome)}`,
    extra: `累计流水 ¥${formatMoney(overview.totalGrossIncome)}`,
    className: 'is-gold'
  },
  {
    icon: '🧰',
    label: '活跃师傅',
    value: `${overview.activeWorkers || 0}/${overview.totalWorkers || 0}`,
    extra: `在岗率 ${formatPercent(activeWorkerRate.value)}`,
    className: 'is-cyan'
  }
]);

const healthCards = computed(() => [
  {
    label: '积压订单',
    value: `${overview.pendingOrders || 0} 单`,
    desc: `占比 ${formatPercent(pendingOrderRate.value)}`
  },
  {
    label: '待处理售后',
    value: `${overview.pendingAfterSales || 0} 单`,
    desc: '售后工单需持续盯盘'
  },
  {
    label: '退款金额',
    value: `¥${formatMoney(overview.totalRefundAmount)}`,
    desc: `退款率 ${formatPercent(refundRate.value)}`
  },
  {
    label: '服务用户',
    value: `${overview.totalUsers || 0} 人`,
    desc: '平台累计触达用户'
  }
]);

const topThreeWorkers = computed(() => topWorkers.value.slice(0, 3));
const performanceSummaryCards = computed(() => {
  const totalNetIncome = topWorkers.value.reduce((sum, item) => sum + Number(item.netIncome || 0), 0);
  const totalCompleted = topWorkers.value.reduce((sum, item) => sum + Number(item.completedOrders || 0), 0);
  const avgRating = topWorkers.value.length
    ? topWorkers.value.reduce((sum, item) => sum + Number(item.rating || 0), 0) / topWorkers.value.length
    : 0;

  return [
    {
      label: '战力冠军',
      value: topWorkers.value[0] ? topWorkers.value[0].realName || topWorkers.value[0].username || '暂无' : '暂无'
    },
    {
      label: 'TOP5净收入',
      value: `¥${formatMoney(totalNetIncome)}`
    },
    {
      label: 'TOP5完工单',
      value: `${totalCompleted} 单`
    },
    {
      label: 'TOP5均分',
      value: formatScore(avgRating)
    }
  ];
});

const signalCards = computed(() => [
  {
    label: '处理效率',
    value: `${serviceHealthScore.value} 分`,
    desc: `今日完工率 ${formatPercent(todayCompletionRate.value)}，积压占比 ${formatPercent(pendingOrderRate.value)}。`
  },
  {
    label: '人力状态',
    value: formatPercent(activeWorkerRate.value),
    desc: `当前在岗 ${overview.activeWorkers || 0} 位师傅，总师傅数 ${overview.totalWorkers || 0}。`
  },
  {
    label: '营收安全',
    value: formatPercent(100 - Number(refundRate.value || 0)),
    desc: `累计流水 ¥${formatMoney(overview.totalGrossIncome)}，退款 ¥${formatMoney(overview.totalRefundAmount)}。`
  },
  {
    label: '售后压力',
    value: `${overview.pendingAfterSales || 0} 单`,
    desc: '优先跟进处理中与待审核售后，避免延迟积压。'
  }
]);

const gaugeInsightList = computed(() => [
  {
    label: '完工率',
    value: formatPercent(todayCompletionRate.value)
  },
  {
    label: '在岗率',
    value: formatPercent(activeWorkerRate.value)
  },
  {
    label: '退款率',
    value: formatPercent(refundRate.value)
  }
]);

const incomeSeries = computed(() => trendList.value.map(item => Number(item.income || 0)));
const orderSeries = computed(() => trendList.value.map(item => Number(item.orderCount || 0)));
const completedSeries = computed(() => trendList.value.map(item => Number(item.completedCount || 0)));

const productTrendList = computed(() => (Array.isArray(productSales.recentTrend) ? productSales.recentTrend : []));
const productStatusList = computed(() =>
  Array.isArray(productSales.orderStatusDistribution) ? productSales.orderStatusDistribution : []
);
const productTopList = computed(() => (Array.isArray(productSales.topProducts) ? productSales.topProducts : []));
const productPaymentList = computed(() =>
  Array.isArray(productSales.paymentMethodDistribution) ? productSales.paymentMethodDistribution : []
);
const productStatusPreviewList = computed(() =>
  productStatusList.value.map((item, index) => ({
    ...item,
    color: statusPalette[index % statusPalette.length]
  }))
);
const productPaymentPreviewList = computed(() =>
  productPaymentList.value.map((item, index) => ({
    ...item,
    color: paymentPalette[index % paymentPalette.length]
  }))
);
const topSellingProducts = computed(() => productTopList.value.slice(0, 5));
const productQuantitySeries = computed(() => productTrendList.value.map(item => Number(item.soldQuantity || 0)));
const productPaidOrderSeries = computed(() => productTrendList.value.map(item => Number(item.paidOrderCount || 0)));
const productAmountSeries = computed(() => productTrendList.value.map(item => Number(item.salesAmount || 0)));
const productPendingRate = computed(() =>
  toPercent(productSales.pendingDeliveryOrderCount, productSales.totalPaidOrderCount || productSales.totalOrderCount)
);
const productRefundRate = computed(() => toPercent(productSales.refundedOrderCount, productSales.totalOrderCount));
const productTopContribution = computed(() => {
  const topQuantity = topSellingProducts.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  return toPercent(topQuantity, productSales.totalSoldQuantity || 1);
});
const productSalesScore = computed(() => {
  const shipmentHealth = normalizePercent(100 - Number(productPendingRate.value || 0));
  const refundHealth = normalizePercent(100 - Number(productRefundRate.value || 0));
  const momentum = normalizePercent(calcProductSalesMomentum());
  const contribution = normalizePercent(productTopContribution.value);
  return normalizePercent(shipmentHealth * 0.34 + refundHealth * 0.3 + momentum * 0.24 + contribution * 0.12);
});

const productHeroCards = computed(() => [
  {
    icon: '🛒',
    label: '累计订单',
    value: `${productSales.totalOrderCount || 0} 单`,
    extra: `已支付 ${productSales.totalPaidOrderCount || 0} 单`,
    className: 'is-blue'
  },
  {
    icon: '📦',
    label: '待发货',
    value: `${productSales.pendingDeliveryOrderCount || 0} 单`,
    extra: `压力占比 ${formatPercent(productPendingRate.value)}`,
    className: 'is-green'
  },
  {
    icon: '💴',
    label: '今日成交',
    value: `¥${formatMoney(productSales.todaySalesAmount)}`,
    extra: `累计 ¥${formatMoney(productSales.totalSalesAmount)}`,
    className: 'is-gold'
  },
  {
    icon: '🔥',
    label: '销售健康',
    value: `${productSalesScore.value} 分`,
    extra: `退款占比 ${formatPercent(productRefundRate.value)}`,
    className: 'is-cyan'
  }
]);

const trendChartOption = computed(() => {
  if (!trendList.value.length) {
    return buildEmptyChartOption('暂无趋势数据');
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 18, 32, 0.92)',
      borderColor: 'rgba(140, 226, 255, 0.16)',
      textStyle: { color: '#eef6ff' }
    },
    legend: {
      top: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#8ca0ba', fontSize: 11 }
    },
    grid: { top: 38, left: 8, right: 10, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: trendList.value.map(item => item.dateLabel),
      axisLine: { lineStyle: { color: 'rgba(166, 185, 207, 0.5)' } },
      axisLabel: { color: '#8ca0ba', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单',
        axisLabel: { color: '#8ca0ba', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(132, 149, 174, 0.12)' } }
      },
      {
        type: 'value',
        name: '营收',
        axisLabel: {
          color: '#8ca0ba',
          fontSize: 11,
          formatter: value => `¥${value}`
        }
      }
    ],
    series: [
      {
        name: '新增订单',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: orderSeries.value,
        lineStyle: { width: 3, color: '#62d7ff' },
        itemStyle: { color: '#62d7ff' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(98, 215, 255, 0.32)' },
              { offset: 1, color: 'rgba(98, 215, 255, 0.02)' }
            ]
          }
        }
      },
      {
        name: '完成订单',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: completedSeries.value,
        lineStyle: { width: 3, color: '#58d69b' },
        itemStyle: { color: '#58d69b' }
      },
      {
        name: '营收',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 16,
        data: incomeSeries.value,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#ffd36a' },
              { offset: 1, color: '#ff8f55' }
            ]
          }
        }
      }
    ]
  };
});

const statusChartOption = computed(() => {
  if (!statusList.value.length) {
    return buildEmptyChartOption('暂无状态分布');
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10, 18, 32, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.08)',
      textStyle: { color: '#eef6ff' }
    },
    legend: { show: false },
    graphic: [],
    series: [
      {
        type: 'pie',
        radius: ['40%', '76%'],
        center: ['48%', '41%'],
        roseType: 'radius',
        itemStyle: {
          borderRadius: 10,
          borderColor: 'rgba(255, 255, 255, 0.9)',
          borderWidth: 2
        },
        label: { show: false },
        labelLine: { show: false },
        emphasis: { scale: true, scaleSize: 8 },
        data: statusList.value.map((item, index) => ({
          name: item.label,
          value: item.count,
          itemStyle: {
            color: statusPalette[index % statusPalette.length]
          }
        }))
      }
    ]
  };
});

const gaugeChartOption = computed(() => ({
  backgroundColor: 'transparent',
  series: [
    {
      type: 'gauge',
      startAngle: 215,
      endAngle: -35,
      min: 0,
      max: 100,
      center: ['50%', '58%'],
      radius: '96%',
      splitNumber: 4,
      progress: {
        show: true,
        roundCap: true,
        width: 16,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#59ddff' },
              { offset: 0.55, color: '#66e4ad' },
              { offset: 1, color: '#ffd56d' }
            ]
          }
        }
      },
      axisLine: {
        roundCap: true,
        lineStyle: {
          width: 16,
          color: [[1, 'rgba(156, 176, 200, 0.18)']]
        }
      },
      pointer: {
        length: '60%',
        width: 5,
        itemStyle: { color: '#284261' }
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: {
        show: false,
        offsetCenter: [0, '28%'],
        color: '#7e8aa0',
        fontSize: 10
      },
      detail: {
        show: false,
        valueAnimation: true,
        offsetCenter: [0, '0%'],
        fontSize: 22,
        fontWeight: 700,
        color: '#16314d',
        formatter: '{value}分'
      },
      data: [{ value: serviceHealthScore.value, name: '运营效率' }]
    }
  ]
}));

const radarChartOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: {
    backgroundColor: 'rgba(10, 18, 32, 0.92)',
    borderColor: 'rgba(255, 255, 255, 0.08)',
    textStyle: { color: '#eef6ff' }
  },
  radar: {
    radius: '63%',
    splitNumber: 5,
    axisName: { color: '#67798f', fontSize: 11 },
    splitLine: { lineStyle: { color: ['rgba(121, 140, 165, 0.14)'] } },
    splitArea: {
      areaStyle: {
        color: ['rgba(93, 123, 163, 0.06)', 'rgba(93, 123, 163, 0.02)']
      }
    },
    axisLine: { lineStyle: { color: 'rgba(121, 140, 165, 0.18)' } },
    indicator: [
      { name: '履约率', max: 100 },
      { name: '师傅活跃', max: 100 },
      { name: '低积压', max: 100 },
      { name: '低退款', max: 100 },
      { name: '售后健康', max: 100 },
      { name: '收入动能', max: 100 }
    ]
  },
  series: [
    {
      type: 'radar',
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 3, color: '#61dfff' },
      itemStyle: { color: '#61dfff' },
      areaStyle: { color: 'rgba(97, 223, 255, 0.22)' },
      data: [
        {
          value: [
            normalizePercent(todayCompletionRate.value),
            normalizePercent(activeWorkerRate.value),
            normalizePercent(100 - Number(pendingOrderRate.value || 0)),
            normalizePercent(100 - Number(refundRate.value || 0)),
            normalizePercent(100 - toPercent(overview.pendingAfterSales, overview.totalOrders || 1)),
            normalizePercent(calcIncomeMomentum())
          ]
        }
      ]
    }
  ]
}));

const workerRankChartOption = computed(() => {
  if (!topWorkers.value.length) {
    return buildEmptyChartOption('暂无师傅榜单数据');
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 18, 32, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.08)',
      textStyle: { color: '#eef6ff' }
    },
    legend: {
      top: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#8ca0ba', fontSize: 11 }
    },
    grid: { top: 38, left: 12, right: 12, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: topWorkers.value.map(item => item.realName || item.username || '未命名'),
      axisLine: { lineStyle: { color: 'rgba(166, 185, 207, 0.5)' } },
      axisLabel: { color: '#8ca0ba', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '完工单',
        axisLabel: { color: '#8ca0ba', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(132, 149, 174, 0.12)' } }
      },
      {
        type: 'value',
        name: '净收入',
        axisLabel: { color: '#8ca0ba', fontSize: 11, formatter: value => `¥${value}` }
      }
    ],
    series: [
      {
        name: '完工单',
        type: 'bar',
        barWidth: 18,
        data: topWorkers.value.map(item => Number(item.completedOrders || 0)),
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#67dcff' },
              { offset: 1, color: '#4f85ff' }
            ]
          }
        }
      },
      {
        name: '净收入',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        data: topWorkers.value.map(item => Number(item.netIncome || 0)),
        lineStyle: { width: 3, color: '#ffc66b' },
        itemStyle: { color: '#ffc66b' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255, 198, 107, 0.24)' },
              { offset: 1, color: 'rgba(255, 198, 107, 0.02)' }
            ]
          }
        }
      }
    ]
  };
});

const productSalesTrendChartOption = computed(() => {
  if (!productTrendList.value.length) {
    return buildEmptyChartOption('暂无商品趋势数据');
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 18, 32, 0.92)',
      borderColor: 'rgba(140, 226, 255, 0.16)',
      textStyle: { color: '#eef6ff' }
    },
    legend: {
      top: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#8ca0ba', fontSize: 11 }
    },
    grid: { top: 38, left: 10, right: 12, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: productTrendList.value.map(item => item.dateLabel),
      axisLine: { lineStyle: { color: 'rgba(166, 185, 207, 0.5)' } },
      axisLabel: { color: '#8ca0ba', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '销量',
        axisLabel: { color: '#8ca0ba', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(132, 149, 174, 0.12)' } }
      },
      {
        type: 'value',
        name: '成交额',
        axisLabel: { color: '#8ca0ba', fontSize: 11, formatter: value => `¥${value}` }
      }
    ],
    series: [
      {
        name: '支付订单',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: productPaidOrderSeries.value,
        lineStyle: { width: 3, color: '#69d9ff' },
        itemStyle: { color: '#69d9ff' }
      },
      {
        name: '销量',
        type: 'bar',
        barWidth: 16,
        data: productQuantitySeries.value,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#73e0c0' },
              { offset: 1, color: '#2ab98a' }
            ]
          }
        }
      },
      {
        name: '成交额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        data: productAmountSeries.value,
        lineStyle: { width: 3, color: '#ffc96d' },
        itemStyle: { color: '#ffc96d' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255, 201, 109, 0.28)' },
              { offset: 1, color: 'rgba(255, 201, 109, 0.03)' }
            ]
          }
        }
      }
    ]
  };
});

const productStatusChartOption = computed(() => {
  if (!productStatusList.value.length) {
    return buildEmptyChartOption('暂无商品订单状态');
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10, 18, 32, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.08)',
      textStyle: { color: '#eef6ff' }
    },
    legend: { show: false },
    series: [
      {
        type: 'pie',
        radius: ['42%', '74%'],
        center: ['48%', '44%'],
        itemStyle: {
          borderRadius: 10,
          borderColor: 'rgba(255, 255, 255, 0.9)',
          borderWidth: 2
        },
        label: { show: false },
        labelLine: { show: false },
        emphasis: { scale: true, scaleSize: 8 },
        data: productStatusPreviewList.value.map(item => ({
          name: item.label,
          value: item.count,
          itemStyle: { color: item.color }
        }))
      }
    ]
  };
});

const productPaymentChartOption = computed(() => {
  if (!productPaymentList.value.length) {
    return buildEmptyChartOption('暂无支付结构数据');
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(10, 18, 32, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.08)',
      textStyle: { color: '#eef6ff' }
    },
    legend: { show: false },
    series: [
      {
        type: 'pie',
        radius: ['46%', '76%'],
        center: ['48%', '44%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 12,
          borderColor: 'rgba(255, 255, 255, 0.88)',
          borderWidth: 2
        },
        label: { show: false },
        labelLine: { show: false },
        data: productPaymentPreviewList.value.map(item => ({
          name: item.label,
          value: item.amount,
          itemStyle: { color: item.color }
        }))
      }
    ]
  };
});

function goPrev() {
  slideIndex.value = Math.max(0, slideIndex.value - 1);
}

function goNext() {
  slideIndex.value = Math.min(screens.length - 1, slideIndex.value + 1);
}

function handleKeydown(event) {
  if (event.key === 'ArrowLeft') {
    goPrev();
  } else if (event.key === 'ArrowRight') {
    goNext();
  }
}

function handleExternalRefresh(event) {
  if (!event || !event.detail || !String(event.detail.path || '').startsWith('/admin/dashboard')) {
    return;
  }
  event.detail.handled = true;
  loadData();
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2);
}

function formatScore(value) {
  return Number(value || 0).toFixed(2);
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function toPercent(value, total) {
  const denominator = Number(total || 0);
  if (!denominator) return 0;
  return (Number(value || 0) / denominator) * 100;
}

function normalizePercent(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number)) return 0;
  return Math.max(0, Math.min(100, Number(number.toFixed(1))));
}

function calcIncomeMomentum() {
  if (incomeSeries.value.length < 2) {
    return toPercent(overview.todayIncome, overview.totalGrossIncome || 1);
  }
  const last = incomeSeries.value[incomeSeries.value.length - 1] || 0;
  const previous = incomeSeries.value[incomeSeries.value.length - 2] || 0;
  if (previous <= 0) {
    return last > 0 ? 100 : 0;
  }
  return Math.min((last / previous) * 100, 100);
}

function calcProductSalesMomentum() {
  if (productAmountSeries.value.length < 2) {
    return toPercent(productSales.todaySalesAmount, productSales.totalSalesAmount || 1);
  }
  const last = productAmountSeries.value[productAmountSeries.value.length - 1] || 0;
  const previous = productAmountSeries.value[productAmountSeries.value.length - 2] || 0;
  if (previous <= 0) {
    return last > 0 ? 100 : 0;
  }
  return Math.min((last / previous) * 100, 100);
}

function getAvatarInitial(worker) {
  if (worker && worker.realName) return String(worker.realName).charAt(0);
  if (worker && worker.username) return String(worker.username).charAt(0).toUpperCase();
  return 'W';
}

function calcPodiumWidth(value) {
  const maxCompleted = Math.max(...topThreeWorkers.value.map(item => Number(item.completedOrders || 0)), 1);
  return Math.max(24, Math.min(100, (Number(value || 0) / maxCompleted) * 100));
}

function getProductInitial(product) {
  const name = product && product.productName ? String(product.productName) : '';
  return name ? name.charAt(0) : '商';
}

function calcProductRankWidth(value) {
  const maxQuantity = Math.max(...topSellingProducts.value.map(item => Number(item.quantity || 0)), 1);
  return Math.max(24, Math.min(100, (Number(value || 0) / maxQuantity) * 100));
}

function calcProductAmountShare(value) {
  return toPercent(value, productSales.totalSalesAmount || 1);
}

function buildEmptyChartOption(text) {
  return {
    backgroundColor: 'transparent',
    xAxis: { show: false },
    yAxis: { show: false },
    series: [],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text,
          fill: '#8ca0ba',
          fontSize: 14
        }
      }
    ]
  };
}

function applyOverview(data) {
  overview.totalUsers = Number(data.totalUsers || 0);
  overview.totalWorkers = Number(data.totalWorkers || 0);
  overview.activeWorkers = Number(data.activeWorkers || 0);
  overview.totalOrders = Number(data.totalOrders || 0);
  overview.todayOrders = Number(data.todayOrders || 0);
  overview.pendingOrders = Number(data.pendingOrders || 0);
  overview.todayCompletedOrders = Number(data.todayCompletedOrders || 0);
  overview.pendingAfterSales = Number(data.pendingAfterSales || 0);
  overview.totalGrossIncome = Number(data.totalGrossIncome || 0);
  overview.totalRefundAmount = Number(data.totalRefundAmount || 0);
  overview.totalNetIncome = Number(data.totalNetIncome || 0);
  overview.todayIncome = Number(data.todayIncome || 0);
  overview.recentTrend = Array.isArray(data.recentTrend) ? data.recentTrend : [];
  overview.orderStatusDistribution = Array.isArray(data.orderStatusDistribution) ? data.orderStatusDistribution : [];
}

function applyProductSales(data) {
  productSales.totalOrderCount = Number(data.totalOrderCount || 0);
  productSales.totalPaidOrderCount = Number(data.totalPaidOrderCount || 0);
  productSales.todayPaidOrderCount = Number(data.todayPaidOrderCount || 0);
  productSales.pendingDeliveryOrderCount = Number(data.pendingDeliveryOrderCount || 0);
  productSales.refundedOrderCount = Number(data.refundedOrderCount || 0);
  productSales.totalSoldQuantity = Number(data.totalSoldQuantity || 0);
  productSales.totalSalesAmount = Number(data.totalSalesAmount || 0);
  productSales.todaySalesAmount = Number(data.todaySalesAmount || 0);
  productSales.totalRefundAmount = Number(data.totalRefundAmount || 0);
  productSales.recentTrend = Array.isArray(data.recentTrend) ? data.recentTrend : [];
  productSales.orderStatusDistribution = Array.isArray(data.orderStatusDistribution) ? data.orderStatusDistribution : [];
  productSales.topProducts = Array.isArray(data.topProducts) ? data.topProducts : [];
  productSales.paymentMethodDistribution = Array.isArray(data.paymentMethodDistribution) ? data.paymentMethodDistribution : [];
}

async function loadData() {
  if (loading.value) return;
  loading.value = true;
  try {
    const [overviewRes, performanceRes, productSalesRes] = await Promise.all([
      fetchAdminDashboardOverview(),
      fetchAdminWorkerPerformance({ pageNum: 1, pageSize: 5 }),
      fetchAdminDashboardProductSales()
    ]);

    if (overviewRes && overviewRes.code === 200 && overviewRes.data) {
      applyOverview(overviewRes.data);
    } else {
      throw new Error((overviewRes && overviewRes.message) || '获取仪表盘数据失败');
    }

    if (performanceRes && performanceRes.code === 200 && performanceRes.data) {
      topWorkers.value = Array.isArray(performanceRes.data.list) ? performanceRes.data.list : [];
    } else {
      topWorkers.value = [];
    }

    if (productSalesRes && productSalesRes.code === 200 && productSalesRes.data) {
      applyProductSales(productSalesRes.data);
    } else {
      throw new Error((productSalesRes && productSalesRes.message) || '获取商品销售数据失败');
    }
  } catch (error) {
    topWorkers.value = [];
    ElMessage.error(error.message || '获取仪表盘数据失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
  window.addEventListener('keydown', handleKeydown);
  window.addEventListener('admin-page-refresh', handleExternalRefresh);
  autoRefreshTimer = window.setInterval(() => {
    loadData();
  }, dashboardRefreshInterval);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
  window.removeEventListener('admin-page-refresh', handleExternalRefresh);
  if (autoRefreshTimer) {
    window.clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }
});
</script>

<style scoped>
.dashboard-page {
  --dashboard-card: rgba(255, 255, 255, 0.8);
  --dashboard-border: rgba(209, 221, 235, 0.86);
  --dashboard-title: #14253d;
  --dashboard-muted: #7d8fa8;
  position: relative;
  height: calc(100vh - 88px);
  overflow: hidden;
  padding: 2px 0;
  background:
    radial-gradient(circle at 0% 0%, rgba(106, 209, 255, 0.16), transparent 24%),
    radial-gradient(circle at 100% 0%, rgba(255, 198, 112, 0.14), transparent 20%),
    radial-gradient(circle at 50% 100%, rgba(101, 214, 182, 0.12), transparent 22%),
    linear-gradient(180deg, #f5f8fc 0%, #eef3f9 100%);
}

.dashboard-shell {
  height: 100%;
  min-height: 0;
}

.header-copy,
.header-meta,
.slider-viewport,
.dashboard-slide,
.overview-layout,
.overview-side,
.performance-layout,
.performance-side,
.product-layout,
.product-side,
.panel-card,
.slide-panel,
.chart-wrap,
.hero-panel,
.health-panel,
.spotlight-panel,
.product-hero-panel,
.product-ranking-card {
  min-width: 0;
  min-height: 0;
}

.panel-kicker,
.hero-chip {
  font-size: 10px;
  letter-spacing: 0.15em;
  color: #55abc9;
}

.slider-viewport {
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: 28px;
}

.slider-track {
  display: flex;
  width: 200%;
  height: 100%;
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.dashboard-slide {
  width: 50%;
  padding: 2px;
}

.overview-layout {
  height: 100%;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 1fr);
  grid-template-rows: minmax(190px, 0.84fr) minmax(0, 1fr);
  grid-template-areas:
    'hero health'
    'trend side';
}

.performance-layout {
  height: 100%;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
  grid-template-rows: minmax(210px, 0.86fr) minmax(0, 1fr);
  grid-template-areas:
    'spotlight radar'
    'ranking side';
}

.product-layout {
  height: 100%;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 1fr);
  grid-template-rows: minmax(210px, 0.88fr) minmax(0, 1fr);
  grid-template-areas:
    'hero side'
    'trend ranking';
}

.slide-panel {
  position: relative;
  overflow: hidden;
  border-radius: 26px;
  border: 1px solid var(--dashboard-border);
  box-shadow: 0 20px 44px rgba(33, 58, 92, 0.08);
}

.dashboard-slide.is-active .slide-panel {
  animation: slideReveal 0.65s ease both;
}

.hero-panel {
  grid-area: hero;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(250px, 0.8fr);
  gap: 12px;
  padding: 16px 18px;
  background:
    radial-gradient(circle at 18% 20%, rgba(134, 232, 255, 0.24), transparent 22%),
    radial-gradient(circle at 78% 16%, rgba(246, 195, 92, 0.2), transparent 18%),
    linear-gradient(140deg, #0b1f3e 0%, #11457a 42%, #15607a 68%, #2f9b74 100%);
  color: #fff;
}

.hero-panel::before {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  right: -100px;
  bottom: -120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.26), transparent 68%);
}

.hero-copy,
.hero-visual {
  position: relative;
  z-index: 1;
}

.hero-title {
  margin: 8px 0 8px;
  font-size: 24px;
  line-height: 1.1;
}

.hero-desc {
  margin: 0;
  max-width: 560px;
  font-size: 12px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.82);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.hero-tags span,
.hero-chip,
.podium-rank {
  border-radius: 999px;
}

.hero-chip {
  display: inline-flex;
  padding: 5px 9px;
  color: rgba(255, 255, 255, 0.84);
  background: rgba(255, 255, 255, 0.1);
}

.hero-tags span {
  padding: 5px 8px;
  font-size: 10px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.hero-metric {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
}

.metric-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 17px;
}

.hero-metric.is-blue .metric-icon { background: linear-gradient(135deg, #4e90ff, #69daff); }
.hero-metric.is-green .metric-icon { background: linear-gradient(135deg, #2ebd85, #76e2b8); }
.hero-metric.is-gold .metric-icon { background: linear-gradient(135deg, #ff9d43, #ffd36d); }
.hero-metric.is-cyan .metric-icon { background: linear-gradient(135deg, #3ec3d9, #78f0e3); }

.metric-main {
  min-width: 0;
}

.metric-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.74);
}

.metric-value {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 700;
}

.metric-extra {
  margin-top: 2px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.68);
}

.hero-visual {
  position: relative;
}

.orbit {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(14px);
  animation: floatPulse 6s ease-in-out infinite;
}

.orbit-main {
  inset: 28px 42px;
}

.orbit-top,
.orbit-left,
.orbit-right {
  width: 74px;
  height: 74px;
}

.orbit-top {
  top: 2px;
  left: calc(50% - 37px);
}

.orbit-left {
  left: 0;
  top: calc(50% - 37px);
  animation-delay: 0.8s;
}

.orbit-right {
  right: 0;
  top: calc(50% - 37px);
  animation-delay: 1.6s;
}

.orbit-label,
.orbit-small-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.72);
}

.orbit-value {
  margin-top: 6px;
  font-size: 21px;
  text-align: center;
}

.orbit-small-value {
  margin-top: 4px;
  font-size: 15px;
  text-align: center;
}

.health-panel {
  grid-area: health;
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 12px;
  padding: 14px;
  background:
    radial-gradient(circle at top right, rgba(115, 221, 255, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 250, 255, 0.9));
}

.panel-card {
  display: grid;
  grid-template-rows: auto 1fr;
  padding: 12px 14px 12px;
  background: var(--dashboard-card);
  backdrop-filter: blur(20px);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  margin-top: 2px;
  font-size: 15px;
  font-weight: 700;
  color: var(--dashboard-title);
}

.panel-tip {
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(238, 244, 250, 0.92);
  color: #61738a;
  white-space: nowrap;
  font-size: 10px;
}

.score-pill {
  padding: 5px 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #15345d, #227ab0);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.health-progress {
  display: grid;
  gap: 8px;
}

.health-progress-bar {
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(216, 227, 239, 0.82);
}

.health-progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #5bddff 0%, #69dfb1 56%, #ffd36b 100%);
  box-shadow: 0 0 20px rgba(91, 221, 255, 0.28);
}

.health-progress-text {
  font-size: 11px;
  line-height: 1.5;
  color: var(--dashboard-muted);
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  align-content: start;
}

.health-card {
  display: grid;
  gap: 4px;
  padding: 10px;
  border-radius: 15px;
  background: linear-gradient(180deg, #ffffff 0%, #f6faff 100%);
  border: 1px solid rgba(225, 234, 244, 0.96);
}

.health-card span,
.matrix-item span,
.summary-chip span {
  font-size: 11px;
  color: var(--dashboard-muted);
}

.health-card strong,
.summary-chip strong {
  font-size: 16px;
  color: var(--dashboard-title);
}

.health-card small {
  font-size: 10px;
  line-height: 1.5;
  color: #8c99aa;
}

.trend-card {
  grid-area: trend;
}

.overview-side {
  grid-area: side;
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 10px;
  min-height: 0;
}

.status-card,
.gauge-card {
  min-height: 0;
}

.mini-panel-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 8px;
  align-items: start;
  height: 100%;
  margin-top: 0;
}

.mini-chart-wrap {
  min-width: 0;
  min-height: 0;
  height: 100%;
  padding: 0 0 12px;
  transform: translateY(-10px);
}

.mini-data-list {
  display: grid;
  gap: 5px;
  align-content: start;
  padding-top: 2px;
  transform: translateY(-6px);
  max-height: 100%;
  overflow: hidden;
}

.status-card .mini-panel-body {
  grid-template-columns: minmax(0, 1.2fr) 132px;
}

.status-card .mini-chart-wrap {
  transform: translateY(0px);
}

.status-data-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
}

.status-data-list .mini-data-item {
  gap: 5px;
  padding: 4px 5px;
}

.status-data-list .mini-data-dot {
  width: 6px;
  height: 6px;
}

.status-data-list .mini-data-main {
  gap: 1px;
}

.status-data-list .mini-data-main strong {
  font-size: 11px;
}

.status-data-list .mini-data-main span {
  font-size: 9px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-data-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 5px 6px;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  border: 1px solid rgba(221, 231, 242, 0.96);
}

.mini-data-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.mini-data-main {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.mini-data-main strong,
.gauge-data-item strong {
  font-size: 12px;
  line-height: 1.1;
  color: var(--dashboard-title);
}

.mini-data-main span,
.gauge-data-item span {
  font-size: 9px;
  line-height: 1.2;
  color: var(--dashboard-muted);
}

.gauge-data-item {
  display: grid;
  gap: 2px;
}

.chart-wrap {
  height: 100%;
  margin-top: 6px;
}

.spotlight-panel {
  grid-area: spotlight;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 8px;
  padding: 12px 14px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.24), transparent 26%),
    linear-gradient(135deg, #112a4a 0%, #1d5583 52%, #2c8c82 100%);
  color: #fff;
}

.product-hero-panel {
  grid-area: hero;
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(250px, 0.82fr);
  gap: 12px;
  padding: 16px 18px;
  background:
    radial-gradient(circle at 14% 18%, rgba(145, 255, 211, 0.24), transparent 22%),
    radial-gradient(circle at 82% 14%, rgba(255, 211, 120, 0.24), transparent 18%),
    linear-gradient(138deg, #0e2b43 0%, #16656d 48%, #248362 76%, #4b9c57 100%);
  color: #fff;
}

.product-hero-panel::before {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  right: -96px;
  bottom: -124px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.24), transparent 68%);
}

.product-hero-panel .panel-kicker,
.product-hero-panel .panel-title,
.product-hero-panel .hero-chip {
  color: rgba(255, 255, 255, 0.88);
}

.spotlight-panel .panel-title,
.spotlight-panel .panel-kicker {
  color: #fff;
}

.spotlight-panel .panel-tip {
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.82);
}

.performance-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.summary-chip {
  display: grid;
  gap: 2px;
  padding: 8px 9px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
}

.summary-chip span {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.7);
}

.summary-chip strong {
  color: #fff;
  font-size: 14px;
}

.podium-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  align-items: stretch;
  min-height: 0;
}

.podium-card {
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 4px;
  padding: 10px 8px 8px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.15);
  text-align: center;
  min-height: 0;
}

.podium-card-1 {
  background: linear-gradient(180deg, rgba(255, 246, 214, 0.26), rgba(255, 215, 90, 0.2));
}

.podium-card-2 {
  background: linear-gradient(180deg, rgba(235, 244, 255, 0.22), rgba(147, 186, 255, 0.18));
}

.podium-card-3 {
  background: linear-gradient(180deg, rgba(255, 233, 223, 0.22), rgba(255, 149, 99, 0.18));
}

.podium-rank {
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 10px;
  font-weight: 700;
}

.podium-name {
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
}

.podium-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
}

.podium-meta {
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 9px;
  line-height: 1.2;
  color: rgba(255, 255, 255, 0.78);
}

.podium-power {
  width: 100%;
  height: 10px;
  margin-top: 2px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  overflow: hidden;
}

.podium-power span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #79e4ff 0%, #4b84ff 100%);
  box-shadow: 0 0 16px rgba(74, 131, 255, 0.28);
}

.radar-card {
  grid-area: radar;
}

.ranking-card {
  grid-area: ranking;
}

.performance-side {
  grid-area: side;
  display: grid;
  grid-template-rows: 0.92fr 1.08fr;
  gap: 10px;
}

.product-side {
  grid-area: side;
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 10px;
}

.product-trend-card {
  grid-area: trend;
}

.product-ranking-card {
  grid-area: ranking;
}

.product-ranking-list {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.product-ranking-item {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f5fbff 100%);
  border: 1px solid rgba(222, 232, 243, 0.96);
}

.product-ranking-rank {
  padding: 5px 9px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(19, 78, 101, 0.14), rgba(60, 151, 112, 0.16));
  font-size: 10px;
  font-weight: 700;
  color: #1d5167;
}

.product-ranking-avatar {
  flex-shrink: 0;
}

.product-ranking-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.product-ranking-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--dashboard-title);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-ranking-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 10px;
  color: var(--dashboard-muted);
}

.product-ranking-bar {
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(208, 221, 235, 0.72);
}

.product-ranking-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #68dfff 0%, #5ed39f 52%, #ffc96d 100%);
  box-shadow: 0 0 16px rgba(104, 223, 255, 0.24);
}

.product-ranking-value {
  display: grid;
  justify-items: end;
  gap: 2px;
  text-align: right;
}

.product-ranking-value strong {
  font-size: 14px;
  color: var(--dashboard-title);
}

.product-ranking-value span {
  font-size: 10px;
  color: var(--dashboard-muted);
}

.signal-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin-top: 6px;
}

.signal-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 6px 10px;
  padding: 7px 8px;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  border: 1px solid rgba(224, 233, 243, 0.96);
}

.signal-row-main {
  min-width: 0;
}

.signal-row-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: var(--dashboard-title);
}

.signal-row-main p {
  margin: 2px 0 0;
  font-size: 10px;
  line-height: 1.25;
  color: var(--dashboard-muted);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.signal-row-value {
  flex-shrink: 0;
  align-self: center;
  font-size: 12px;
  color: var(--dashboard-title);
}

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 6px;
}

.matrix-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  border: 1px solid rgba(221, 231, 242, 0.96);
}

.matrix-item span {
  font-size: 10px;
  line-height: 1.2;
}

.matrix-item strong {
  font-size: 14px;
  flex-shrink: 0;
  color: var(--dashboard-title);
}

.edge-switch {
  position: absolute;
  top: 50%;
  z-index: 6;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 92px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: rgba(18, 45, 75, 0.08);
  color: #fff;
  box-shadow: 0 8px 18px rgba(18, 45, 75, 0.06);
  transform: translateY(-50%);
  cursor: pointer;
  opacity: 0.2;
  backdrop-filter: blur(4px);
  transition: transform 0.25s ease, background 0.25s ease, opacity 0.25s ease, box-shadow 0.25s ease;
}

.edge-switch:hover {
  background: rgba(23, 62, 104, 0.42);
  box-shadow: 0 12px 24px rgba(18, 45, 75, 0.16);
  opacity: 0.82;
  transform: translateY(-50%) scale(1.02);
}

.edge-switch :deep(.el-icon) {
  font-size: 15px;
}

.edge-switch-left {
  left: 6px;
}

.edge-switch-right {
  right: 6px;
}

@keyframes floatPulse {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@keyframes slideReveal {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1500px) {
  .hero-title {
    font-size: 22px;
  }

  .hero-metric {
    padding: 9px;
  }

  .performance-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1280px) {
  .dashboard-page {
    height: auto;
    min-height: calc(100vh - 88px);
    overflow: auto;
    padding: 0;
  }

  .dashboard-shell {
    height: auto;
  }

  .slider-viewport,
  .slider-track {
    overflow: visible;
    height: auto;
  }

  .slider-track {
    display: grid;
    width: 100%;
    transform: none !important;
    transition: none;
    gap: 14px;
  }

  .dashboard-slide {
    width: 100%;
    padding: 0;
  }

  .overview-layout,
  .performance-layout,
  .product-layout,
  .hero-panel,
  .product-hero-panel,
  .overview-side,
  .product-side,
  .matrix-grid,
  .podium-grid {
    grid-template-columns: 1fr;
  }

  .overview-layout,
  .performance-layout,
  .product-layout {
    grid-template-areas: none;
    grid-template-columns: 1fr;
    grid-template-rows: auto;
  }

  .hero-panel,
  .health-panel,
  .trend-card,
  .overview-side,
  .spotlight-panel,
  .radar-card,
  .ranking-card,
  .performance-side,
  .product-hero-panel,
  .product-side,
  .product-trend-card,
  .product-ranking-card {
    grid-area: auto;
  }

  .overview-side {
    grid-template-rows: auto;
  }

  .performance-side,
  .product-side {
    grid-template-rows: auto;
  }

  .edge-switch {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero-panel,
  .panel-card,
  .health-panel,
  .spotlight-panel,
  .product-hero-panel {
    padding: 14px;
  }

  .hero-metrics,
  .health-grid,
  .performance-summary,
  .signal-list,
  .matrix-grid,
  .podium-grid,
  .product-ranking-item {
    grid-template-columns: 1fr;
  }

  .product-ranking-item {
    justify-items: start;
  }

  .product-ranking-value {
    justify-items: start;
    text-align: left;
  }
}
</style>
