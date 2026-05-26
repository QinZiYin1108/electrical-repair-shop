<template>
  <view class="page worker-home">
    <view class="top-bar">
      <view class="top-left" @click="openSidebar">
        <u-icon name="list" size="22" color="#303133" />
      </view>
      <view class="top-center" @click.stop="onStatusClick">
        <view class="status-pill" :class="'status-' + (isOnline ? 1 : 0)">
          <text class="status-dot" />
          <text class="status-text">{{ workStatusText }}</text>
          <u-icon name="arrow-down" size="14" color="#606266" />
        </view>
        <view v-if="showStatusDropdown" class="status-dropdown" @click.stop>
          <view class="status-dropdown-item" @click="handleStatusMenuClick">
            <text
              class="status-dropdown-dot"
              :class="statusMenuOption.value === 1 ? 'status-dropdown-dot-online' : 'status-dropdown-dot-offline'"
            />
            <text class="status-dropdown-text">{{ statusMenuOption.name }}</text>
          </view>
        </view>
      </view>
      <view class="top-right" @click="goMessage">
        <view class="message-icon-wrapper">
          <u-icon name="bell-fill" size="22" color="#303133" />
          <view v-if="hasUnread" class="message-badge-dot" />
        </view>
      </view>
    </view>

    <view v-if="showStatusDropdown" class="status-dropdown-mask" @click="closeStatusDropdown" />

    <view class="content-area">
      <view class="welcome-card">
        <view class="welcome-left">
          <image class="avatar" :src="avatarDisplayUrl" mode="aspectFill" @error="onAvatarError" />
          <view class="welcome-texts">
            <text class="welcome-name">{{ workerInfo.username || '维修师傅' }}</text>
          </view>
        </view>
        <view class="pending-summary">
          <text class="pending-count">{{ workerInfo.pendingOrderCount || 0 }}</text>
          <text class="pending-label">待完成单</text>
        </view>
      </view>

      <view v-if="statusNotice" class="account-notice-card">
        <text class="account-notice-text">{{ statusNotice.message }}</text>
        <text
          v-if="statusNotice.actionText"
          class="account-notice-action"
          @click="handleStatusNoticeAction"
        >
          {{ statusNotice.actionText }}
        </text>
      </view>

      <view class="order-board">
        <view class="board-summary">
          <view class="summary-card">
            <text class="summary-value">{{ orderBoard.waitingCount || 0 }}</text>
            <text class="summary-name">待接单</text>
          </view>
          <view class="summary-card">
            <text class="summary-value">{{ orderBoard.inProgressCount || 0 }}</text>
            <text class="summary-name">进行中</text>
          </view>
          <view class="summary-card">
            <text class="summary-value">{{ orderBoard.totalActiveCount || 0 }}</text>
            <text class="summary-name">全部活跃单</text>
          </view>
        </view>

        <view class="order-section">
          <view class="section-head">
            <text class="section-title">待接单</text>
            <text class="section-count">{{ orderBoard.waitingCount || 0 }} 单</text>
          </view>
          <view v-if="orderBoard.waitingOrders && orderBoard.waitingOrders.length" class="order-list">
            <view
              v-for="item in orderBoard.waitingOrders"
              :key="item.id"
              class="order-card order-card-waiting"
              @click="goOrderDetail(item.id)"
            >
              <view class="order-card-top">
                <view class="order-card-main">
                  <text class="order-name">{{ formatServiceTitle(item) }}</text>
                  <text class="order-order-no">{{ item.orderNo }}</text>
                </view>
                <view class="order-card-tags">
                  <text class="order-service-tag" :class="serviceTagClass(item.serviceMode)">
                    {{ serviceTagText(item.serviceMode, item.serviceModeText) }}
                  </text>
                  <text class="order-status order-status-waiting">{{ item.statusText }}</text>
                </view>
              </view>
              <view class="order-core-meta">
                <view class="meta-row-item meta-row-item-address">
                  <text class="meta-row-text">{{ item.serviceAddressShort || item.serviceAddress || '暂无地址' }}</text>
                </view>
                <view class="meta-row-item">
                  <text class="meta-chip-text">预约时间： {{ formatOrderTime(item.appointmentTime) }}</text>
                </view>
              </view>
            </view>
          </view>
          <view v-else class="empty-card">
            <text class="empty-text">当前暂无待接单订单</text>
          </view>
        </view>

        <view class="order-section">
          <view class="section-head">
            <text class="section-title">进行中订单</text>
            <text class="section-count">{{ orderBoard.inProgressCount || 0 }} 单</text>
          </view>
          <view v-if="orderBoard.inProgressOrders && orderBoard.inProgressOrders.length" class="order-list">
            <view
              v-for="item in orderBoard.inProgressOrders"
              :key="item.id"
              class="order-card"
              @click="goOrderDetail(item.id)"
            >
              <view class="order-card-top">
                <view class="order-card-main">
                  <text class="order-name">{{ formatServiceTitle(item) }}</text>
                  <text class="order-order-no">{{ item.orderNo }}</text>
                </view>
                <view class="order-card-tags">
                  <text class="order-service-tag" :class="serviceTagClass(item.serviceMode)">
                    {{ serviceTagText(item.serviceMode, item.serviceModeText) }}
                  </text>
                  <text class="order-status" :class="statusClass(item.status)">{{ item.statusText }}</text>
                </view>
              </view>
              <view class="order-core-meta">
                <view class="meta-row-item meta-row-item-address">
                  <text class="meta-row-text">{{ item.serviceAddressShort || item.serviceAddress || '暂无地址' }}</text>
                </view>
                <view class="meta-row-item">
                  <text class="meta-chip-text">{{ formatOrderTime(item.appointmentTime) }}</text>
                </view>
              </view>
            </view>
          </view>
          <view v-else class="empty-card">
            <text class="empty-text">当前暂无进行中订单</text>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <view class="bottom-left">
        <u-button
          text="接单设置"
          size="small"
          shape="circle"
          :disabled="!canAcceptOrder"
          @click="openOrderSetting"
        />
      </view>
      <view class="bottom-right">
        <u-button
          :text="bottomActionText"
          type="primary"
          shape="circle"
          :disabled="!canAcceptOrder"
          :loading="switching || refreshingOrders"
          @click="handleBottomAction"
        />
      </view>
    </view>

    <u-popup
      v-model:show="showSidebar"
      mode="left"
      :closeable="true"
      :safe-area-inset-bottom="true"
    >
      <view class="sidebar">
        <view class="sidebar-header">
          <image class="sidebar-avatar" :src="avatarDisplayUrl" mode="aspectFill" @error="onAvatarError" />
          <view class="sidebar-meta">
            <view class="sidebar-name">{{ workerInfo.username || '维修师傅' }}</view>
          </view>
        </view>
        <view class="sidebar-menu">
          <view class="sidebar-item" @click="goProfile">
            <u-icon name="account" size="18" />
            <text class="sidebar-text">个人资料</text>
          </view>
          <view class="sidebar-item" @click="goReviews">
            <u-icon name="star" size="18" />
            <text class="sidebar-text">服务评价</text>
          </view>
          <view class="sidebar-item" @click="goFunds">
            <u-icon name="red-packet" size="18" />
            <text class="sidebar-text">资金管理</text>
          </view>
          <view class="sidebar-item" @click="goStats">
            <u-icon name="order" size="18" />
            <text class="sidebar-text">历史订单</text>
          </view>
          <view class="sidebar-item" @click="goSettings">
            <u-icon name="setting" size="18" />
            <text class="sidebar-text">设置</text>
          </view>
        </view>
      </view>
    </u-popup>

    <u-popup
      v-model:show="showOrderSetting"
      mode="bottom"
      round="20"
      :safe-area-inset-bottom="true"
    >
      <view class="order-setting">
        <view class="order-setting-header">
          <text class="order-setting-title">接单设置</text>
        </view>
        <view class="order-setting-body">
          <view class="order-row">
            <text class="order-label">计费策略配置</text>
            <view class="order-action-link" @click="goFeePolicyPage">
              <text class="order-action-text">去设置</text>
              <u-icon name="arrow-right" size="14" color="#909399" />
            </view>
          </view>
          <view class="order-row">
            <text class="order-label">工作时间设置</text>
            <view class="order-action-link" @click="goWorkTimePage">
              <text class="order-action-text">去设置</text>
              <u-icon name="arrow-right" size="14" color="#909399" />
            </view>
          </view>
          <view class="order-row order-row-address">
            <text class="order-label">当前地址</text>
            <view class="order-address-wrapper">
              <text
                class="order-address"
                :class="!workerInfo.address ? 'order-address-placeholder' : ''"
              >
                {{ workerInfo.address || '暂未定位' }}
              </text>
              <u-button
                class="location-btn"
                size="mini"
                type="primary"
                :plain="true"
                shape="circle"
                :loading="locating"
                :disabled="locating || !canAcceptOrder"
                :text="locating ? '定位中' : '定位修改'"
                @click="useGpsLocation"
              />
            </view>
          </view>
        </view>
        <view class="order-setting-footer">
          <u-button
            text="完成"
            type="primary"
            shape="circle"
            @click="showOrderSetting = false"
          />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script>
import { getWorkerAccountInfo, updateWorkerWorkStatus } from '@/api/workerAccount';
import { updateWorkerLocation } from '@/api/workerLocation';
import { fetchWorkerUnreadFlag } from '@/api/workerMessages';
import { fetchWorkerHomeOrders } from '@/api/workerOrders';

function isBrokenText(value) {
  if (value == null) return true;
  const text = String(value).trim();
  if (!text) return true;
  return /^[?？]+$/.test(text) || text.includes('???');
}

function resolveOrderStatusText(status, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(status || 0);
  if (value === 1) return '待接单';
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
  if (value === 3) return '线下维修';
  return '维修服务';
}

function resolveServiceTagText(serviceMode, fallback) {
  const value = Number(serviceMode || 0);
  if (value === 1) return '线上维修';
  if (value === 2) return '线上安装';
  if (value === 3) return '线下维修';
  if (!isBrokenText(fallback)) return fallback;
  return '维修类型';
}

function normalizeMoney(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number : 0;
}

function calculateRemainingAmount(item) {
  const totalAmount = normalizeMoney(item && item.totalAmount);
  const paidAmount = normalizeMoney(item && item.paidAmount);
  const remaining = totalAmount - paidAmount;
  return remaining > 0 ? remaining : 0;
}

function resolveDisplayStatusText(item) {
  const baseText = resolveOrderStatusText(item && item.status, item && item.statusText);
  if (
    Number(item && item.status) === 4 &&
    calculateRemainingAmount(item) <= 0 &&
    normalizeMoney(item && item.paidAmount) > 0
  ) {
    return '已支付';
  }
  return baseText;
}

function normalizeHomeOrderItem(item) {
  const source = item || {};
  return {
    ...source,
    statusText: resolveDisplayStatusText(source),
    serviceModeText: resolveServiceModeText(source.serviceMode, source.serviceModeText)
  };
}

export default {
  data() {
    return {
      workerInfo: {
        id: '',
        username: '',
        email: '',
        accountStatus: null,
        workStatus: 0,
        pendingOrderCount: 0,
        address: '',
        latitude: null,
        longitude: null,
        avatarUrl: ''
      },
      orderBoard: {
        waitingCount: 0,
        inProgressCount: 0,
        totalActiveCount: 0,
        waitingOrders: [],
        inProgressOrders: []
      },
      avatarLoadFailed: false,
      hasUnread: false,
      showSidebar: false,
      showOrderSetting: false,
      showStatusDropdown: false,
      switching: false,
      refreshingOrders: false,
      locating: false
    };
  },
  computed: {
    avatarDisplayUrl() {
      if (this.avatarLoadFailed) return '/static/logo.png';
      const url = (this.workerInfo && this.workerInfo.avatarUrl) ? String(this.workerInfo.avatarUrl).trim() : '';
      return url || '/static/logo.png';
    },
    workStatusText() {
      return this.workerInfo.workStatus === 1 ? '在线' : '离线';
    },
    isOnline() {
      return this.workerInfo.workStatus === 1;
    },
    bottomActionText() {
      return this.isOnline ? '刷新订单' : '上线';
    },
    statusMenuOption() {
      return {
        name: this.isOnline ? '下线' : '上线',
        value: this.isOnline ? 0 : 1
      };
    },
    canAcceptOrder() {
      return this.workerInfo.accountStatus === 1;
    },
    statusNotice() {
      return this.getAccountStatusNotice(this.workerInfo.accountStatus);
    }
  },
  watch: {
    'workerInfo.avatarUrl'() {
      this.avatarLoadFailed = false;
    }
  },
  onShow() {
    this.showStatusDropdown = false;
    this.loadHomeData();
    this.loadUnreadFlag();
  },
  methods: {
    resetOrderBoard() {
      this.orderBoard = {
        waitingCount: 0,
        inProgressCount: 0,
        totalActiveCount: 0,
        waitingOrders: [],
        inProgressOrders: []
      };
    },
    loadHomeOrders() {
      return fetchWorkerHomeOrders()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.orderBoard = {
              waitingCount: Number(res.data.waitingCount || 0),
              inProgressCount: Number(res.data.inProgressCount || 0),
              totalActiveCount: Number(res.data.totalActiveCount || 0),
              waitingOrders: Array.isArray(res.data.waitingOrders) ? res.data.waitingOrders.map(normalizeHomeOrderItem) : [],
              inProgressOrders: Array.isArray(res.data.inProgressOrders) ? res.data.inProgressOrders.map(normalizeHomeOrderItem) : []
            };
            return true;
          }
          this.resetOrderBoard();
          return false;
        })
        .catch(() => {
          this.resetOrderBoard();
          return false;
        });
    },
    loadHomeData() {
      return Promise.all([this.loadWorkerInfo(), this.loadHomeOrders()])
        .then(results => results.every(Boolean));
    },
    onAvatarError() {
      this.avatarLoadFailed = true;
    },
    openSidebar() {
      this.closeStatusDropdown();
      this.showSidebar = true;
    },
    loadWorkerInfo() {
      return getWorkerAccountInfo()
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.workerInfo = {
              ...this.workerInfo,
              ...res.data
            };
            const app = getApp();
            if (app && app.globalData) {
              app.globalData.workerInfo = this.workerInfo;
            }
            return true;
          }
          return false;
        })
        .catch(() => false);
    },
    loadUnreadFlag() {
      fetchWorkerUnreadFlag()
        .then((res) => {
          let hasUnread = false;
          if (res && res.code === 200 && res.data && typeof res.data.totalUnreadCount === 'number') {
            hasUnread = res.data.totalUnreadCount > 0;
          }
          this.hasUnread = hasUnread;
          const app = getApp();
          if (app && app.globalData) {
            app.globalData.workerHasUnread = hasUnread;
          }
        })
        .catch(() => {});
    },
    onStatusClick() {
      if (!this.canAcceptOrder) {
        this.showAccountStatusBlockedToast();
        return;
      }
      if (this.switching || this.refreshingOrders) return;
      this.showStatusDropdown = !this.showStatusDropdown;
    },
    closeStatusDropdown() {
      this.showStatusDropdown = false;
    },
    goMessage() {
      this.closeStatusDropdown();
      uni.navigateTo({
        url: '/pages/message/index'
      });
    },
    openOrderSetting() {
      if (!this.canAcceptOrder) {
        this.showAccountStatusBlockedToast();
        return;
      }
      this.closeStatusDropdown();
      this.showOrderSetting = true;
    },
    goFeePolicyPage() {
      this.showOrderSetting = false;
      uni.navigateTo({
        url: '/pages/fee-policy/index'
      });
    },
    goWorkTimePage() {
      this.showOrderSetting = false;
      uni.navigateTo({
        url: '/pages/work-time/index'
      });
    },
    hasValidLocation() {
      const hasAddress = !!(this.workerInfo.address && String(this.workerInfo.address).trim());
      const rawLatitude = this.workerInfo.latitude;
      const rawLongitude = this.workerInfo.longitude;
      if (rawLatitude === null || rawLatitude === undefined || rawLatitude === '') return false;
      if (rawLongitude === null || rawLongitude === undefined || rawLongitude === '') return false;
      const latitude = Number(rawLatitude);
      const longitude = Number(rawLongitude);
      return hasAddress && Number.isFinite(latitude) && Number.isFinite(longitude);
    },
    ensureLocationBeforeOnline() {
      if (this.hasValidLocation()) return true;
      uni.showToast({
        title: '请先完成定位后再上线',
        icon: 'none'
      });
      return false;
    },
    handleBottomAction() {
      if (!this.canAcceptOrder) {
        this.showAccountStatusBlockedToast();
        return;
      }
      if (this.isOnline) {
        this.refreshOrders();
        return;
      }
      if (!this.ensureLocationBeforeOnline()) return;
      this.changeWorkStatus(1, '已上线');
    },
    refreshOrders() {
      if (this.refreshingOrders) return;
      this.refreshingOrders = true;
      this.loadHomeData()
        .then((ok) => {
          uni.showToast({
            title: ok ? '订单已刷新' : '刷新失败',
            icon: ok ? 'success' : 'none'
          });
        })
        .finally(() => {
          this.refreshingOrders = false;
        });
    },
    formatOrderTime(value) {
      const timestamp = Number(value);
      if (!Number.isFinite(timestamp) || timestamp <= 0) return '待确认';
      const date = new Date(timestamp);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hour}:${minute}`;
    },
    formatServiceTitle(item) {
      if (!item) return '维修订单';
      const category = item.serviceCategoryName || '';
      const type = item.serviceTypeName || item.serviceModeText || '';
      const text = `${category} ${type}`.trim();
      return text || '维修订单';
    },
    serviceTagText(serviceMode, fallback) {
      return resolveServiceTagText(serviceMode, fallback);
    },
    serviceTagClass(serviceMode) {
      const value = Number(serviceMode || 0);
      if (value === 1) return 'order-service-tag-online-repair';
      if (value === 2) return 'order-service-tag-online-install';
      if (value === 3) return 'order-service-tag-offline-repair';
      return '';
    },
    statusClass(status) {
      if (status === 2) return 'order-status-visit';
      if (status === 3) return 'order-status-check';
      if (status === 4) return 'order-status-pay';
      if (status === 5) return 'order-status-work';
      return '';
    },
    handleStatusMenuClick() {
      const target = this.statusMenuOption.value;
      this.closeStatusDropdown();
      if (target === 1 && !this.ensureLocationBeforeOnline()) return;
      this.changeWorkStatus(target, target === 1 ? '已上线' : '已下线');
    },
    changeWorkStatus(target, successText) {
      if (this.switching) return;
      if (target !== 0 && target !== 1) return;
      this.switching = true;
      updateWorkerWorkStatus(target)
        .then((res) => {
          if (res.code === 200) {
            this.workerInfo.workStatus = target;
            const app = getApp();
            if (app && app.globalData) {
              app.globalData.workerInfo = this.workerInfo;
            }
            uni.showToast({
              title: successText || '状态已更新',
              icon: 'success'
            });
          } else {
            uni.showToast({
              title: (res && res.message) || '操作失败',
              icon: 'none'
            });
          }
        })
        .catch(() => {
          uni.showToast({
            title: '操作失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.switching = false;
        });
    },
    getLocationRequestOptions() {
      let type = 'gcj02';
      let coordType = 'gcj02ll';
      // #ifdef APP-PLUS
      type = 'wgs84';
      coordType = 'wgs84ll';
      // #endif
      return { type, coordType };
    },
    getLocationErrorRawMessage(error) {
      if (!error || typeof error !== 'object') return '';
      const message = typeof error.errMsg === 'string' ? error.errMsg : '';
      return message.trim();
    },
    getAppLocationAuthorizedStatus() {
      if (typeof uni.getAppAuthorizeSetting !== 'function') return '';
      try {
        const setting = uni.getAppAuthorizeSetting();
        if (!setting || typeof setting !== 'object') return '';
        return typeof setting.locationAuthorized === 'string' ? setting.locationAuthorized : '';
      } catch (error) {
        return '';
      }
    },
    isLocationPermissionDenied(error) {
      const raw = this.getLocationErrorRawMessage(error).toLowerCase();
      const authorizedStatus = this.getAppLocationAuthorizedStatus();
      if (authorizedStatus === 'denied') {
        return true;
      }
      return raw.includes('auth deny')
        || raw.includes('auth denied')
        || raw.includes('permission denied')
        || raw.includes('system permission denied')
        || raw.includes('without permission')
        || raw.includes('authorize no response');
    },
    buildLocationErrorMessage(error) {
      const raw = this.getLocationErrorRawMessage(error);
      const lower = raw.toLowerCase();
      const authorizedStatus = this.getAppLocationAuthorizedStatus();
      if (authorizedStatus === 'config error') {
        return '当前安装包缺少定位配置，请检查 Android 定位权限和打包配置后重新安装';
      }
      if (this.isLocationPermissionDenied(error)) {
        return '定位权限未开启，请在系统设置里允许定位权限后重试';
      }
      if (
        lower.includes('gps')
        || lower.includes('provider')
        || lower.includes('location service')
        || lower.includes('service unavailable')
        || lower.includes('service not enabled')
      ) {
        return '系统定位服务未开启，请先打开手机定位服务后重试';
      }
      if (
        lower.includes('config')
        || lower.includes('sdk')
        || lower.includes('key')
        || lower.includes('ak')
      ) {
        return '当前安装包定位配置不完整，请重新打包后重试';
      }
      if (raw) {
        return `获取定位失败：${raw}`;
      }
      return '获取定位失败，请稍后重试';
    },
    showLocationError(error) {
      const content = this.buildLocationErrorMessage(error);
      const canOpenSetting = this.isLocationPermissionDenied(error)
        && typeof uni.openAppAuthorizeSetting === 'function';
      uni.showModal({
        title: '定位失败',
        content,
        showCancel: canOpenSetting,
        cancelText: '取消',
        confirmText: canOpenSetting ? '去设置' : '我知道了',
        success: (res) => {
          if (canOpenSetting && res.confirm) {
            uni.openAppAuthorizeSetting({});
          }
        }
      });
    },
    useGpsLocation() {
      if (!this.canAcceptOrder) {
        this.showAccountStatusBlockedToast();
        return;
      }
      if (this.locating) return;
      this.locating = true;
      const locationOptions = this.getLocationRequestOptions();
      uni.getLocation({
        type: locationOptions.type,
        isHighAccuracy: true,
        success: (res) => {
          const payload = {
            latitude: res.latitude,
            longitude: res.longitude,
            coordType: locationOptions.coordType
          };
          this.updateLocation(payload);
        },
        fail: (error) => {
          console.error('[worker-location] getLocation failed', error);
          this.locating = false;
          this.showLocationError(error);
        }
      });
    },
    updateLocation(payload) {
      updateWorkerLocation(payload)
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.workerInfo.address = res.data.address || this.workerInfo.address;
            const nextLatitude = res.data.latitude;
            const nextLongitude = res.data.longitude;
            this.workerInfo.latitude = (nextLatitude === null || nextLatitude === undefined)
              ? this.workerInfo.latitude
              : nextLatitude;
            this.workerInfo.longitude = (nextLongitude === null || nextLongitude === undefined)
              ? this.workerInfo.longitude
              : nextLongitude;
            const app = getApp();
            if (app && app.globalData) {
              app.globalData.workerInfo = this.workerInfo;
            }
            uni.showToast({ title: '定位已更新', icon: 'success' });
          } else {
            uni.showToast({
              title: (res && res.message) || '更新失败',
              icon: 'none'
            });
          }
        })
        .catch((error) => {
          uni.showToast({
            title: (error && error.message) || '更新失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.locating = false;
        });
    },
    getAccountStatusNotice(accountStatus) {
      if (accountStatus === 1) return null;
      if (accountStatus === 2) {
        return {
          message: '账号未实名认证，暂无法接单',
          actionText: '去认证'
        };
      }
      if (accountStatus === 3) {
        return {
          message: '账号已冻结，暂无法接单',
          actionText: ''
        };
      }
      if (accountStatus === 4) {
        return {
          message: '账号已离职，暂无法接单',
          actionText: ''
        };
      }
      return {
        message: '当前账号状态异常，暂无法接单',
        actionText: ''
      };
    },
    showAccountStatusBlockedToast() {
      const notice = this.statusNotice;
      uni.showToast({
        title: (notice && notice.message) || '当前账号状态不可操作',
        icon: 'none'
      });
    },
    handleStatusNoticeAction() {
      if (this.workerInfo.accountStatus === 2) {
        this.goCertification();
      }
    },
    goCertification() {
      this.closeStatusDropdown();
      uni.navigateTo({
        url: '/pages/certification/index'
      });
    },
    goProfile() {
      this.closeStatusDropdown();
      this.showSidebar = false;
      uni.navigateTo({
        url: '/pages/profile/index'
      });
    },
    goFunds() {
      this.closeStatusDropdown();
      this.showSidebar = false;
      uni.navigateTo({
        url: '/pages/funds/index'
      });
    },
    goReviews() {
      this.closeStatusDropdown();
      this.showSidebar = false;
      uni.navigateTo({
        url: '/pages/reviews/index'
      });
    },
    goStats() {
      this.closeStatusDropdown();
      this.showSidebar = false;
      uni.navigateTo({
        url: '/pages/order-history/index'
      });
    },
    goSettings() {
      this.closeStatusDropdown();
      this.showSidebar = false;
      uni.navigateTo({
        url: '/pages/settings/index'
      });
    },
    goOrderDetail(orderId) {
      if (!orderId) return;
      uni.navigateTo({
        url: `/pages/order-detail/index?id=${orderId}`
      });
    }
  }
};
</script>

<style scoped>
.worker-home {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
  box-sizing: border-box;
}

.top-bar {
  position: relative;
  z-index: 30;
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  background-color: #f5f5f5;
}

.top-left,
.top-right {
  width: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-icon-wrapper {
  position: relative;
}

.message-badge-dot {
  position: absolute;
  top: -6rpx;
  right: -8rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background-color: #ff4d4f;
}

.top-center {
  flex: 1;
  display: flex;
  justify-content: center;
  position: relative;
}

.status-pill {
  flex-direction: row;
  display: flex;
  align-items: center;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  background-color: #f2f3f5;
}

.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: #909399;
  margin-right: 8rpx;
}

.status-text {
  font-size: 24rpx;
  color: #606266;
  margin-right: 6rpx;
}

.status-1 .status-dot {
  background-color: #19be6b;
}

.status-dropdown-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
}

.status-dropdown {
  position: absolute;
  top: calc(100% + 12rpx);
  left: 50%;
  transform: translateX(-50%);
  min-width: 160rpx;
  padding: 8rpx;
  border-radius: 12rpx;
  background-color: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
  z-index: 40;
}

.status-dropdown-item {
  height: 68rpx;
  padding: 0 16rpx;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
}

.status-dropdown-item:active {
  background-color: #f5f7fa;
}

.status-dropdown-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin-right: 10rpx;
}

.status-dropdown-dot-online {
  background-color: #19be6b;
}

.status-dropdown-dot-offline {
  background-color: #909399;
}

.status-dropdown-text {
  font-size: 24rpx;
  color: #303133;
}

.content-area {
  padding: 16rpx 24rpx 0;
  box-sizing: border-box;
}

.order-board {
  margin-top: 20rpx;
}

.board-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.summary-card {
  padding: 20rpx 16rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #ffffff 0%, #f7fbff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 8rpx 20rpx rgba(22, 119, 255, 0.06);
}

.summary-value {
  font-size: 34rpx;
  font-weight: 700;
  color: #1677ff;
}

.summary-name {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7a869a;
}

.order-section {
  margin-top: 20rpx;
}

.section-head {
  margin-bottom: 14rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

.section-count {
  font-size: 22rpx;
  color: #909399;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.order-card {
  padding: 22rpx;
  border-radius: 22rpx;
  background-color: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.06);
}

.order-card-waiting {
  border: 2rpx solid #dbeafe;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
}

.order-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.order-card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.order-card-tags {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
}

.order-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.5;
}

.order-order-no {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #98a2b3;
}

.order-service-tag {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  line-height: 1;
}

.order-service-tag-online-repair {
  color: #0f766e;
  background: #ccfbf1;
}

.order-service-tag-online-install {
  color: #7c3aed;
  background: #ede9fe;
}

.order-service-tag-offline-repair {
  color: #b45309;
  background: #fef3c7;
}

.order-status {
  flex: none;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #2563eb;
  background-color: #eef4ff;
}

.order-status-waiting {
  color: #1d4ed8;
  background-color: #dbeafe;
}

.order-status-visit {
  color: #0f766e;
  background-color: #ccfbf1;
}

.order-status-check {
  color: #7c3aed;
  background-color: #ede9fe;
}

.order-status-pay {
  color: #b45309;
  background-color: #fef3c7;
}

.order-status-work {
  color: #be123c;
  background-color: #ffe4e6;
}

.order-core-meta {
  margin-top: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.meta-row-item {
  width: 100%;
}

.meta-row-item-address {
  color: #475467;
}

.meta-row-text,
.meta-chip-text {
  font-size: 24rpx;
  color: #303133;
  line-height: 1.5;
  word-break: break-all;
}

.empty-card {
  padding: 34rpx 24rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 24rpx;
  color: #98a2b3;
}

.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
}

.welcome-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
  background-color: #f3f4f6;
}

.welcome-texts {
  margin-left: 16rpx;
}

.welcome-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}

.welcome-brand {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
}

.welcome-brand-logo {
  width: 32rpx;
  height: 32rpx;
  flex-shrink: 0;
}

.welcome-brand-text {
  margin-left: 10rpx;
  font-size: 22rpx;
  color: #1677ff;
  font-weight: 500;
}

.pending-summary {
  min-width: 120rpx;
  padding-left: 16rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.pending-count {
  font-size: 36rpx;
  font-weight: 700;
  color: #1677ff;
  line-height: 1.2;
}

.pending-label {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #909399;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12rpx 24rpx 24rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.bottom-left {
  flex: 0 0 auto;
}

.bottom-right {
  flex: 1;
  margin-left: 16rpx;
}

.bottom-right :deep(.u-button) {
  width: 100%;
}

.sidebar {
  width: 46vw;
  padding: 32rpx 80rpx 24rpx 24rpx;
  box-sizing: border-box;
}

.sidebar-header {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.sidebar-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
  background-color: #f3f4f6;
}

.sidebar-meta {
  margin-left: 16rpx;
}

.sidebar-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

.sidebar-brand {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
}

.sidebar-brand-logo {
  width: 28rpx;
  height: 28rpx;
  flex-shrink: 0;
}

.sidebar-brand-text {
  margin-left: 10rpx;
  font-size: 22rpx;
  color: #1677ff;
}

.sidebar-menu {
  margin-top: 32rpx;
}

.sidebar-item {
  flex-direction: row;
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.sidebar-text {
  margin-left: 12rpx;
  font-size: 26rpx;
  color: #303133;
}

.order-setting {
  padding: 24rpx 24rpx 32rpx;
}

.order-setting-header {
  margin-bottom: 16rpx;
}

.order-setting-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

.order-setting-body {
  padding: 8rpx 0 24rpx;
}

.order-action-link {
  display: flex;
  align-items: center;
}

.order-action-text {
  font-size: 24rpx;
  color: #1677ff;
  margin-right: 6rpx;
}

.order-row {
  flex-direction: row;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
}

.order-label {
  font-size: 26rpx;
  color: #303133;
}

.account-notice-card {
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  background-color: #fff7e6;
  border: 1rpx solid #ffd591;
}

.account-notice-text {
  font-size: 24rpx;
  color: #d46b08;
}

.account-notice-action {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #1677ff;
}

.order-row-address {
  align-items: flex-start;
}

.order-address-wrapper {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.order-address {
  width: 100%;
  font-size: 24rpx;
  color: #303133;
  line-height: 34rpx;
  text-align: right;
  word-break: break-all;
}

.order-address-placeholder {
  color: #909399;
}

.location-btn {
  margin-top: 12rpx;
}

.order-setting-footer {
  margin-top: 8rpx;
}
</style>
