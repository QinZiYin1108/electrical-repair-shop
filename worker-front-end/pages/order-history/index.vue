<template>
  <view class="page worker-order-history">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#0f172a" />
      </view>
      <view class="nav-center">
        <text class="nav-title">历史订单</text>
      </view>
      <view class="nav-right">
        <view class="nav-action" @click="loadOrders">
          <u-icon name="reload" size="18" color="#475569" />
        </view>
        <view class="nav-action nav-action-filter" @click="openFilterDrawer">
          <u-icon name="list-dot" size="18" color="#155eef" />
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="page-scroll">
      <view class="hero-card">
        <view class="hero-texts">
          <text class="hero-title">已接单历史记录</text>
          <text class="hero-subtitle">只要师傅接过单，这里都会保留记录</text>
        </view>
        <view class="hero-badge">
          <text class="hero-badge-value">{{ filteredOrders.length }}</text>
          <text class="hero-badge-label">单</text>
        </view>
      </view>

      <view class="toolbar-card">
        <view class="toolbar-info">
          <text class="toolbar-title">共 {{ allOrders.length }} 单</text>
          <text class="toolbar-subtitle">
            {{ appliedFilterCount ? `已筛选 ${filteredOrders.length} 单` : '当前展示全部历史订单' }}
          </text>
        </view>
        
      </view>

      <view v-if="activeFilterTags.length" class="filter-tags">
        <view
          v-for="tag in activeFilterTags"
          :key="tag.key"
          class="filter-tag"
          @click="clearFilter(tag.key)"
        >
          <text class="filter-tag-text">{{ tag.label }}</text>
          <u-icon name="close" size="12" color="#475569" />
        </view>
      </view>

      <view v-if="loading" class="state-card">
        <text class="state-title">正在加载历史订单...</text>
      </view>

      <view v-else-if="loadError" class="state-card">
        <text class="state-title">历史订单加载失败</text>
        <text class="state-desc">{{ loadError }}</text>
        <u-button text="重新加载" type="primary" shape="circle" @click="loadOrders" />
      </view>

      <view v-else-if="filteredOrders.length" class="order-list">
        <view
          v-for="item in filteredOrders"
          :key="item.id"
          class="order-card"
          @click="goOrderDetail(item.id)"
        >
          <view class="order-head">
            <view class="order-head-main">
              <text class="order-title">{{ item.serviceTitle }}</text>
              <text class="order-no">{{ item.orderNo }}</text>
            </view>
            <text class="order-status" :class="statusClass(item.status)">{{ item.statusText }}</text>
          </view>

          <view class="info-row">
            <text class="info-label">服务分类</text>
            <text class="info-value">{{ item.serviceCategoryPath || item.serviceCategoryName || '未分类' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">服务方式</text>
            <text class="info-value">{{ item.serviceModeText || '维修服务' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">用户信息</text>
            <text class="info-value">{{ item.userName || '用户' }} {{ item.contactPhone || '' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">下单时间</text>
            <text class="info-value">{{ formatTime(item.createdTime) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">服务地址</text>
            <text class="info-value">{{ item.serviceAddressShort || item.serviceAddress || '暂无地址' }}</text>
          </view>
          <view v-if="item.faultSummary" class="info-row">
            <text class="info-label">故障概述</text>
            <text class="info-value">{{ item.faultSummary }}</text>
          </view>

          <view class="order-footer">
            <view class="price-block">
              <text class="price-label">订单金额</text>
              <text class="price-value">￥{{ item.totalAmountText }}</text>
            </view>
            <view class="time-block">
              <text class="time-label">最近更新</text>
              <text class="time-value">{{ formatTime(item.updatedTime || item.createdTime) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-card">
        <u-icon name="file-text" size="30" color="#cbd5e1" />
        <text class="empty-title">{{ appliedFilterCount ? '没有匹配订单' : '暂无历史订单' }}</text>
        <text class="empty-desc">
          {{ appliedFilterCount ? '请调整筛选条件后再试' : '当前还没有已接单的历史记录' }}
        </text>
      </view>
    </scroll-view>

    <u-popup
      v-model:show="showFilterDrawer"
      mode="right"
      :safe-area-inset-bottom="true"
      :overlay-style="{ backgroundColor: 'rgba(15, 23, 42, 0.35)' }"
    >
      <view class="filter-drawer">
        <view class="filter-header">
          <text class="filter-title">筛选历史订单</text>
          <text class="filter-subtitle">支持多条件组合查询</text>
        </view>

        <scroll-view scroll-y class="filter-scroll">
          <view class="filter-section">
            <text class="filter-section-title">关键词</text>
            <input
              v-model.trim="draftFilters.keyword"
              class="filter-input"
              maxlength="50"
              placeholder="订单号 / 用户 / 手机号 / 故障描述"
              placeholder-class="filter-input-placeholder"
            />
          </view>

          <view class="filter-section">
            <text class="filter-section-title">订单状态</text>
            <view class="option-group">
              <view
                v-for="option in statusOptions"
                :key="option.value"
                class="option-chip"
                :class="{ 'option-chip-active': draftFilters.status === option.value }"
                @click="draftFilters.status = option.value"
              >
                <text
                  class="option-chip-text"
                  :class="{ 'option-chip-text-active': draftFilters.status === option.value }"
                >
                  {{ option.label }}
                </text>
              </view>
            </view>
          </view>

          <view class="filter-section">
            <text class="filter-section-title">服务方式</text>
            <view class="option-group">
              <view
                v-for="option in serviceModeOptions"
                :key="option.value"
                class="option-chip"
                :class="{ 'option-chip-active': draftFilters.serviceMode === option.value }"
                @click="draftFilters.serviceMode = option.value"
              >
                <text
                  class="option-chip-text"
                  :class="{ 'option-chip-text-active': draftFilters.serviceMode === option.value }"
                >
                  {{ option.label }}
                </text>
              </view>
            </view>
          </view>

          <view class="filter-section">
            <text class="filter-section-title">下单时间</text>
            <view class="option-group">
              <view
                v-for="option in timePresetOptions"
                :key="option.value"
                class="option-chip"
                :class="{ 'option-chip-active': draftFilters.createTimePreset === option.value }"
                @click="selectTimePreset(option.value)"
              >
                <text
                  class="option-chip-text"
                  :class="{ 'option-chip-text-active': draftFilters.createTimePreset === option.value }"
                >
                  {{ option.label }}
                </text>
              </view>
            </view>
            <view class="date-range">
              <picker mode="date" :value="draftFilters.createTimeStart" @change="handleDateChange('createTimeStart', $event)">
                <view class="date-picker">
                  <text :class="draftFilters.createTimeStart ? 'date-picker-text' : 'date-picker-placeholder'">
                    {{ draftFilters.createTimeStart || '开始日期' }}
                  </text>
                  <u-icon name="calendar" size="16" color="#94a3b8" />
                </view>
              </picker>
              <text class="date-separator">至</text>
              <picker mode="date" :value="draftFilters.createTimeEnd" @change="handleDateChange('createTimeEnd', $event)">
                <view class="date-picker">
                  <text :class="draftFilters.createTimeEnd ? 'date-picker-text' : 'date-picker-placeholder'">
                    {{ draftFilters.createTimeEnd || '结束日期' }}
                  </text>
                  <u-icon name="calendar" size="16" color="#94a3b8" />
                </view>
              </picker>
            </view>
          </view>
        </scroll-view>

        <view class="filter-footer">
          <u-button text="重置" shape="circle" :plain="true" @click="resetDraftFilters" />
          <u-button text="应用筛选" type="primary" shape="circle" @click="applyFilters" />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script>
import { fetchWorkerHistoryOrders } from '@/api/workerOrders';

const DEFAULT_FILTERS = Object.freeze({
  keyword: '',
  status: '',
  serviceMode: '',
  createTimePreset: '',
  createTimeStart: '',
  createTimeEnd: ''
});

function createFilters() {
  return {
    keyword: '',
    status: '',
    serviceMode: '',
    createTimePreset: '',
    createTimeStart: '',
    createTimeEnd: ''
  };
}

function formatDateOnly(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function isBrokenText(value) {
  if (value == null) return true;
  const text = String(value).trim();
  if (!text) return true;
  return /^[?]+$/.test(text) || text.includes('???');
}

function resolveOrderStatusText(status, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(status || 0);
  if (value === 2) return '待上门';
  if (value === 3) return '待检查';
  if (value === 4) return '待支付';
  if (value === 5) return '服务中';
  if (value === 6) return '已完成';
  if (value === 7) return '已取消';
  if (value === 8) return '已退款';
  return '订单状态';
}

function resolveServiceModeText(serviceMode, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(serviceMode || 0);
  if (value === 1) return '上门维修';
  if (value === 2) return '上门安装';
  if (value === 3) return '到店维修';
  return '维修服务';
}

function normalizeMoney(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number.toFixed(2) : '0.00';
}

function buildServiceTitle(item) {
  const source = item || {};
  const category = source.serviceCategoryName || '';
  const type = source.serviceTypeName || '';
  const text = `${category} ${type}`.trim();
  return text || '维修订单';
}

function normalizeOrderItem(item) {
  const source = item || {};
  return {
    ...source,
    statusText: resolveOrderStatusText(source.status, source.statusText),
    serviceModeText: resolveServiceModeText(source.serviceMode, source.serviceModeText),
    serviceTitle: buildServiceTitle(source),
    totalAmountText: normalizeMoney(source.totalAmount)
  };
}

function includesText(source, keyword) {
  if (!keyword) return true;
  return String(source || '').toLowerCase().includes(keyword);
}

export default {
  name: 'WorkerOrderHistoryPage',
  data() {
    return {
      loading: false,
      loadError: '',
      showFilterDrawer: false,
      allOrders: [],
      appliedFilters: createFilters(),
      draftFilters: createFilters(),
      statusOptions: [
        { value: '', label: '全部' },
        { value: '2', label: '待上门' },
        { value: '3', label: '待检查' },
        { value: '4', label: '待支付' },
        { value: '5', label: '服务中' },
        { value: '6', label: '已完成' },
        { value: '7', label: '已取消' },
        { value: '8', label: '已退款' }
      ],
      serviceModeOptions: [
        { value: '', label: '全部' },
        { value: '1', label: '上门维修' },
        { value: '2', label: '上门安装' },
        { value: '3', label: '到店维修' }
      ],
      timePresetOptions: [
        { value: '7', label: '最近7天' },
        { value: '30', label: '最近30天' },
        { value: '90', label: '最近90天' }
      ]
    };
  },
  computed: {
    filteredOrders() {
      const keyword = String(this.appliedFilters.keyword || '').trim().toLowerCase();
      return this.allOrders.filter((item) => {
        if (this.appliedFilters.status && String(item.status) !== this.appliedFilters.status) {
          return false;
        }
        if (this.appliedFilters.serviceMode && String(item.serviceMode) !== this.appliedFilters.serviceMode) {
          return false;
        }
        if (!this.matchCreateTime(item.createdTime)) {
          return false;
        }
        if (!keyword) {
          return true;
        }
        return [
          item.orderNo,
          item.userName,
          item.contactPhone,
          item.serviceTitle,
          item.serviceCategoryName,
          item.serviceCategoryPath,
          item.faultSummary,
          item.serviceAddressShort,
          item.serviceAddress
        ].some((field) => includesText(field, keyword));
      });
    },
    appliedFilterCount() {
      return Object.keys(DEFAULT_FILTERS).filter((key) => {
        if (key === 'createTimePreset') return false;
        return String(this.appliedFilters[key] || '').trim() !== String(DEFAULT_FILTERS[key] || '').trim();
      }).length;
    },
    activeFilterTags() {
      const tags = [];
      if (this.appliedFilters.keyword) {
        tags.push({ key: 'keyword', label: `关键词：${this.appliedFilters.keyword}` });
      }
      if (this.appliedFilters.status) {
        const matchedStatus = this.statusOptions.find((item) => item.value === this.appliedFilters.status);
        if (matchedStatus) {
          tags.push({ key: 'status', label: `状态：${matchedStatus.label}` });
        }
      }
      if (this.appliedFilters.serviceMode) {
        const matchedMode = this.serviceModeOptions.find((item) => item.value === this.appliedFilters.serviceMode);
        if (matchedMode) {
          tags.push({ key: 'serviceMode', label: `方式：${matchedMode.label}` });
        }
      }
      if (this.appliedFilters.createTimeStart || this.appliedFilters.createTimeEnd) {
        tags.push({
          key: 'createTimeRange',
          label: `下单时间：${this.appliedFilters.createTimeStart || '不限'} - ${this.appliedFilters.createTimeEnd || '不限'}`
        });
      }
      return tags;
    }
  },
  onShow() {
    this.loadOrders();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    openFilterDrawer() {
      this.draftFilters = {
        ...this.appliedFilters
      };
      this.showFilterDrawer = true;
    },
    resetDraftFilters() {
      this.draftFilters = createFilters();
    },
    handleDateChange(field, event) {
      const value = event && event.detail ? event.detail.value : '';
      this.draftFilters = {
        ...this.draftFilters,
        createTimePreset: '',
        [field]: value || ''
      };
    },
    selectTimePreset(value) {
      const endDate = new Date();
      const startDate = new Date(endDate.getTime());
      startDate.setDate(startDate.getDate() - Number(value || 0) + 1);
      this.draftFilters = {
        ...this.draftFilters,
        createTimePreset: value,
        createTimeStart: formatDateOnly(startDate),
        createTimeEnd: formatDateOnly(endDate)
      };
    },
    applyFilters() {
      if (
        this.draftFilters.createTimeStart &&
        this.draftFilters.createTimeEnd &&
        this.draftFilters.createTimeStart > this.draftFilters.createTimeEnd
      ) {
        uni.showToast({
          title: '开始日期不能晚于结束日期',
          icon: 'none'
        });
        return;
      }
      this.appliedFilters = {
        ...this.draftFilters
      };
      this.showFilterDrawer = false;
    },
    clearFilter(key) {
      if (key === 'createTimeRange') {
        this.appliedFilters = {
          ...this.appliedFilters,
          createTimePreset: '',
          createTimeStart: '',
          createTimeEnd: ''
        };
        this.draftFilters = {
          ...this.appliedFilters
        };
        return;
      }
      if (!Object.prototype.hasOwnProperty.call(this.appliedFilters, key)) return;
      this.appliedFilters = {
        ...this.appliedFilters,
        [key]: ''
      };
      this.draftFilters = {
        ...this.appliedFilters
      };
    },
    loadOrders() {
      if (this.loading) return;
      this.loading = true;
      this.loadError = '';
      fetchWorkerHistoryOrders()
        .then((res) => {
          if (!res || res.code !== 200) {
            this.allOrders = [];
            this.loadError = (res && res.message) || '请稍后重试';
            return;
          }
          this.allOrders = Array.isArray(res.data) ? res.data.map(normalizeOrderItem) : [];
        })
        .catch((error) => {
          this.allOrders = [];
          this.loadError = (error && error.message) || '请检查网络后重试';
        })
        .finally(() => {
          this.loading = false;
        });
    },
    goOrderDetail(orderId) {
      if (!orderId) return;
      uni.navigateTo({
        url: `/pages/order-detail/index?id=${orderId}`
      });
    },
    statusClass(status) {
      const value = Number(status || 0);
      if (value === 2) return 'status-visit';
      if (value === 3) return 'status-check';
      if (value === 4) return 'status-pay';
      if (value === 5) return 'status-working';
      if (value === 6) return 'status-finished';
      if (value === 7) return 'status-cancelled';
      if (value === 8) return 'status-refunded';
      return '';
    },
    formatTime(value) {
      const timestamp = Number(value);
      if (!Number.isFinite(timestamp) || timestamp <= 0) return '暂无';
      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hour}:${minute}`;
    },
    matchCreateTime(value) {
      if (!this.appliedFilters.createTimeStart && !this.appliedFilters.createTimeEnd) {
        return true;
      }
      const timestamp = Number(value || 0);
      if (!Number.isFinite(timestamp) || timestamp <= 0) {
        return false;
      }
      if (this.appliedFilters.createTimeStart) {
        const start = new Date(`${this.appliedFilters.createTimeStart}T00:00:00`).getTime();
        if (timestamp < start) {
          return false;
        }
      }
      if (this.appliedFilters.createTimeEnd) {
        const end = new Date(`${this.appliedFilters.createTimeEnd}T23:59:59`).getTime();
        if (timestamp > end) {
          return false;
        }
      }
      return true;
    }
  }
};
</script>

<style scoped>
.worker-order-history {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.12) 0, rgba(14, 165, 233, 0) 28%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08) 0, rgba(16, 185, 129, 0) 22%),
    linear-gradient(180deg, #f3f7fb 0%, #f8fbff 44%, #f6f8fc 100%);
}

.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(248, 251, 255, 0.92);
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(16rpx);
}

.nav-left,
.nav-right {
  width: 140rpx;
  display: flex;
  align-items: center;
}

.nav-right {
  justify-content: flex-end;
  gap: 12rpx;
}

.nav-action {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(226, 232, 240, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-action-filter {
  background: rgba(21, 94, 239, 0.08);
  border-color: rgba(21, 94, 239, 0.18);
}

.nav-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.page-scroll {
  box-sizing: border-box;
  padding: 18rpx 20rpx 36rpx;
}

.hero-card,
.toolbar-card,
.state-card,
.order-card,
.empty-card {
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(226, 232, 240, 0.88);
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.05);
}

.hero-card {
  padding: 24rpx 22rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  background: linear-gradient(135deg, #155eef 0%, #0ea5e9 56%, #14b8a6 100%);
  border: none;
}

.hero-texts {
  flex: 1;
  min-width: 0;
}

.hero-title {
  display: block;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 700;
}

.hero-subtitle {
  display: block;
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.86);
  font-size: 22rpx;
  line-height: 1.5;
}

.hero-badge {
  width: 112rpx;
  height: 112rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.16);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-badge-value {
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.1;
}

.hero-badge-label {
  margin-top: 6rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 20rpx;
}

.toolbar-card {
  margin-top: 16rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.toolbar-info {
  flex: 1;
  min-width: 0;
}

.toolbar-title {
  display: block;
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
}

.toolbar-subtitle {
  display: block;
  margin-top: 6rpx;
  color: #64748b;
  font-size: 22rpx;
}

.filter-tags {
  margin-top: 14rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(191, 219, 254, 0.92);
  box-shadow: 0 6rpx 18rpx rgba(21, 94, 239, 0.06);
}

.filter-tag-text {
  color: #334155;
  font-size: 20rpx;
}

.state-card {
  margin-top: 18rpx;
  padding: 32rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  text-align: center;
}

.state-title,
.empty-title {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 600;
}

.state-desc,
.empty-desc {
  color: #64748b;
  font-size: 22rpx;
  line-height: 1.5;
}

.order-list {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.order-card {
  padding: 22rpx 20rpx;
}

.order-head,
.info-row,
.order-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
}

.order-head {
  margin-bottom: 10rpx;
}

.order-head-main {
  flex: 1;
  min-width: 0;
}

.order-title {
  display: block;
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.45;
}

.order-no {
  display: block;
  margin-top: 8rpx;
  color: #94a3b8;
  font-size: 22rpx;
  word-break: break-all;
}

.order-status {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #eff6ff;
  color: #2563eb;
  font-size: 22rpx;
  font-weight: 600;
}

.status-visit {
  background: #d1fae5;
  color: #0f766e;
}

.status-check {
  background: #ede9fe;
  color: #7c3aed;
}

.status-pay {
  background: #fef3c7;
  color: #b45309;
}

.status-working {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-finished {
  background: #dcfce7;
  color: #15803d;
}

.status-cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.status-refunded {
  background: #ffe4e6;
  color: #be123c;
}

.info-row {
  padding: 10rpx 0;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.7);
}

.info-row:last-of-type {
  border-bottom: none;
}

.info-label {
  width: 132rpx;
  flex-shrink: 0;
  color: #64748b;
  font-size: 22rpx;
}

.info-value {
  flex: 1;
  color: #334155;
  font-size: 22rpx;
  line-height: 1.5;
  text-align: right;
  word-break: break-all;
}

.order-footer {
  margin-top: 14rpx;
  padding-top: 14rpx;
  border-top: 1rpx dashed rgba(203, 213, 225, 0.9);
  align-items: center;
}

.price-block,
.time-block {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.time-block {
  align-items: flex-end;
}

.price-label,
.time-label {
  color: #94a3b8;
  font-size: 20rpx;
}

.price-value {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
}

.time-value {
  color: #334155;
  font-size: 22rpx;
}

.empty-card {
  margin-top: 18rpx;
  padding: 56rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  text-align: center;
}

.filter-drawer {
  width: 78vw;
  max-width: 620rpx;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 16%, #f8fafc 100%);
  display: flex;
  flex-direction: column;
}

.filter-header {
  flex-shrink: 0;
  padding: calc(var(--status-bar-height) + 26rpx) 24rpx 20rpx;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.88);
}

.filter-title {
  display: block;
  color: #0f172a;
  font-size: 32rpx;
  font-weight: 700;
}

.filter-subtitle {
  display: block;
  margin-top: 8rpx;
  color: #64748b;
  font-size: 22rpx;
}

.filter-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 24rpx 24rpx;
  box-sizing: border-box;
}

.filter-section {
  margin-top: 22rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.95);
  border: 1rpx solid rgba(226, 232, 240, 0.88);
  box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.04);
}

.filter-section-title {
  display: block;
  color: #0f172a;
  font-size: 26rpx;
  font-weight: 700;
}

.filter-input {
  width: 100%;
  height: 82rpx;
  margin-top: 16rpx;
  padding: 0 22rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid rgba(203, 213, 225, 0.86);
  box-sizing: border-box;
  color: #0f172a;
  font-size: 24rpx;
}

.filter-input-placeholder {
  color: #94a3b8;
}

.option-group {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.option-chip {
  min-width: 132rpx;
  min-height: 68rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid rgba(203, 213, 225, 0.86);
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.option-chip-active {
  background: linear-gradient(135deg, rgba(21, 94, 239, 0.12) 0%, rgba(14, 165, 233, 0.12) 100%);
  border-color: rgba(21, 94, 239, 0.4);
  box-shadow: 0 8rpx 18rpx rgba(21, 94, 239, 0.08);
}

.option-chip-text {
  color: #475569;
  font-size: 22rpx;
  font-weight: 500;
}

.option-chip-text-active {
  color: #155eef;
  font-weight: 600;
}

.date-range {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.date-range picker {
  flex: 1;
  min-width: 0;
}

.date-picker {
  height: 76rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid rgba(203, 213, 225, 0.86);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.date-picker-text {
  color: #0f172a;
  font-size: 22rpx;
}

.date-picker-placeholder {
  color: #94a3b8;
  font-size: 22rpx;
}

.date-separator {
  flex-shrink: 0;
  color: #64748b;
  font-size: 22rpx;
}

.filter-footer {
  flex-shrink: 0;
  padding: 16rpx 24rpx calc(env(safe-area-inset-bottom) + 20rpx);
  border-top: 1rpx solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.96);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}
</style>
