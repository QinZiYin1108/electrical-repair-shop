<template>
  <view class="page fee-policy-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">计费策略</text>
      </view>
      <view class="nav-right" />
    </view>

    <scroll-view class="content" scroll-y>
      <view class="tip-card">
        <text class="tip-text">按上门类型分别配置最低上门费、基础半径和超区计费规则。</text>
      </view>

      <view
        v-for="(policy, index) in feePolicies"
        :key="policy.serviceKind || index"
        class="policy-card"
      >
        <view class="policy-head">
          <text class="policy-name">
            {{ policy.serviceKind === 1 ? '上门维修' : '上门安装' }}
          </text>
          <u-switch v-model="policy.enabled" />
        </view>

        <view class="field-row">
          <text class="field-label">最低上门费（元）</text>
          <u-input
            class="field-input"
            v-model="policy.minVisitFee"
            type="digit"
            border="bottom"
            inputAlign="right"
            placeholder="请输入金额"
          />
        </view>
        <view class="field-row">
          <text class="field-label">基础服务半径（公里）</text>
          <u-input
            class="field-input"
            v-model="policy.baseRadiusKm"
            type="digit"
            border="bottom"
            inputAlign="right"
            placeholder="请输入公里数"
          />
        </view>
        <view class="field-row">
          <text class="field-label">超区每公里费用（元）</text>
          <u-input
            class="field-input"
            v-model="policy.extraFeePerKm"
            type="digit"
            border="bottom"
            inputAlign="right"
            placeholder="请输入金额"
          />
        </view>
        <view class="field-row">
          <text class="field-label">封顶公里数（可空）</text>
          <u-input
            class="field-input"
            v-model="policy.maxVisitFee"
            type="digit"
            border="bottom"
            inputAlign="right"
            placeholder="不填表示不限公里"
          />
        </view>
        <view class="field-row">
          <text class="field-label">距离计算方式</text>
          <picker
            class="field-picker"
            mode="selector"
            :range="distanceCalcOptions"
            range-key="label"
            :value="getDistanceOptionIndex(policy.distanceCalcType)"
            @change="onDistanceCalcChange(index, $event)"
          >
            <view class="picker-text">{{ getDistanceCalcText(policy.distanceCalcType) }}</view>
          </picker>
        </view>
        <view class="field-row">
          <text class="field-label">公里取整规则</text>
          <picker
            class="field-picker"
            mode="selector"
            :range="roundingRuleOptions"
            range-key="label"
            :value="getRoundingOptionIndex(policy.roundingRule)"
            @change="onRoundingRuleChange(index, $event)"
          >
            <view class="picker-text">{{ getRoundingRuleText(policy.roundingRule) }}</view>
          </picker>
        </view>
      </view>
    </scroll-view>

    <view class="bottom-actions">
      <u-button
        text="保存设置"
        type="primary"
        shape="circle"
        :loading="saving"
        @click="savePolicies"
      />
    </view>
  </view>
</template>

<script>
import { getWorkerVisitFeePolicies, updateWorkerVisitFeePolicies } from '@/api/workerVisitFeePolicy';

export default {
  name: 'WorkerVisitFeePolicyPage',
  data() {
    return {
      loading: false,
      saving: false,
      distanceCalcOptions: [
        { label: '驾车', value: 1 },
        { label: '骑行', value: 2 }
      ],
      roundingRuleOptions: [
        { label: '向上取整', value: 1 },
        { label: '四舍五入', value: 2 }
      ],
      feePolicies: [
        this.createDefaultPolicy(1),
        this.createDefaultPolicy(2)
      ]
    };
  },
  onShow() {
    this.loadPolicies();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    createDefaultPolicy(serviceKind) {
      return {
        id: '',
        serviceKind,
        minVisitFee: '0',
        baseRadiusKm: '0',
        extraFeePerKm: '0',
        maxVisitFee: '',
        distanceCalcType: 1,
        roundingRule: 1,
        enabled: true
      };
    },
    normalizePolicy(policy, fallbackServiceKind) {
      const serviceKind = policy && (policy.serviceKind === 1 || policy.serviceKind === 2)
        ? policy.serviceKind
        : fallbackServiceKind;
      return {
        id: (policy && policy.id) || '',
        serviceKind,
        minVisitFee: policy && policy.minVisitFee != null ? String(policy.minVisitFee) : '0',
        baseRadiusKm: policy && policy.baseRadiusKm != null ? String(policy.baseRadiusKm) : '0',
        extraFeePerKm: policy && policy.extraFeePerKm != null ? String(policy.extraFeePerKm) : '0',
        maxVisitFee: policy && policy.maxVisitFee != null ? String(policy.maxVisitFee) : '',
        distanceCalcType: policy && policy.distanceCalcType === 2 ? 2 : 1,
        roundingRule: policy && policy.roundingRule === 2 ? 2 : 1,
        enabled: !(policy && policy.isActive === 0)
      };
    },
    loadPolicies() {
      if (this.loading) return;
      this.loading = true;
      getWorkerVisitFeePolicies()
        .then((res) => {
          if (res && res.code === 200 && Array.isArray(res.data)) {
            const policyMap = new Map();
            res.data.forEach((item) => {
              if (item && (item.serviceKind === 1 || item.serviceKind === 2) && !policyMap.has(item.serviceKind)) {
                policyMap.set(item.serviceKind, item);
              }
            });
            this.feePolicies = [1, 2].map((kind) => this.normalizePolicy(policyMap.get(kind), kind));
            return;
          }
          uni.showToast({
            title: (res && res.message) || '获取计费策略失败',
            icon: 'none'
          });
        })
        .catch(() => {
          uni.showToast({
            title: '获取计费策略失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    getDistanceCalcText(value) {
      return value === 2 ? '骑行' : '驾车';
    },
    getRoundingRuleText(value) {
      return value === 2 ? '四舍五入' : '向上取整';
    },
    getDistanceOptionIndex(value) {
      return value === 2 ? 1 : 0;
    },
    getRoundingOptionIndex(value) {
      return value === 2 ? 1 : 0;
    },
    onDistanceCalcChange(index, event) {
      const selected = this.distanceCalcOptions[Number(event && event.detail ? event.detail.value : 0)];
      if (!selected) return;
      this.feePolicies[index].distanceCalcType = selected.value;
    },
    onRoundingRuleChange(index, event) {
      const selected = this.roundingRuleOptions[Number(event && event.detail ? event.detail.value : 0)];
      if (!selected) return;
      this.feePolicies[index].roundingRule = selected.value;
    },
    validatePolicies() {
      for (const policy of this.feePolicies) {
        const minVisitFee = Number(policy.minVisitFee);
        const baseRadiusKm = Number(policy.baseRadiusKm);
        const extraFeePerKm = Number(policy.extraFeePerKm);
        const maxVisitFee = policy.maxVisitFee === '' ? null : Number(policy.maxVisitFee);
        if (!Number.isFinite(minVisitFee) || minVisitFee < 0) return '最低上门费不能小于0';
        if (!Number.isFinite(baseRadiusKm) || baseRadiusKm < 0) return '基础服务半径不能小于0';
        if (!Number.isFinite(extraFeePerKm) || extraFeePerKm < 0) return '超区每公里费用不能小于0';
        if (maxVisitFee !== null && (!Number.isFinite(maxVisitFee) || maxVisitFee < 0)) return '封顶公里数不能小于0';
      }
      return '';
    },
    buildPayload() {
      return this.feePolicies.map((policy) => ({
        id: policy.id || '',
        serviceKind: policy.serviceKind,
        minVisitFee: Number(policy.minVisitFee || 0),
        baseRadiusKm: Number(policy.baseRadiusKm || 0),
        extraFeePerKm: Number(policy.extraFeePerKm || 0),
        distanceCalcType: policy.distanceCalcType === 2 ? 2 : 1,
        roundingRule: policy.roundingRule === 2 ? 2 : 1,
        maxVisitFee: policy.maxVisitFee === '' ? null : Number(policy.maxVisitFee),
        isActive: policy.enabled ? 1 : 0
      }));
    },
    savePolicies() {
      if (this.saving) return;
      const validateMessage = this.validatePolicies();
      if (validateMessage) {
        uni.showToast({
          title: validateMessage,
          icon: 'none'
        });
        return;
      }
      this.saving = true;
      updateWorkerVisitFeePolicies(this.buildPayload())
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({
              title: '保存成功',
              icon: 'success'
            });
            return;
          }
          uni.showToast({
            title: (res && res.message) || '保存失败',
            icon: 'none'
          });
        })
        .catch(() => {
          uni.showToast({
            title: '保存失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.saving = false;
        });
    }
  }
};
</script>

<style scoped>
.fee-policy-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.nav-bar {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  box-sizing: border-box;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-left,
.nav-right {
  width: 120rpx;
  display: flex;
  align-items: center;
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
  color: #303133;
}

.content {
  height: calc(100vh - var(--status-bar-height) - 88rpx - 120rpx);
  padding: 16rpx 24rpx 0;
  box-sizing: border-box;
}

.tip-card {
  padding: 16rpx 20rpx;
  border-radius: 14rpx;
  background-color: #eef5ff;
  margin-bottom: 16rpx;
}

.tip-text {
  font-size: 24rpx;
  color: #4e5969;
}

.policy-card {
  border-radius: 16rpx;
  background-color: #ffffff;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.policy-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.policy-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
}

.field-label {
  width: 250rpx;
  font-size: 24rpx;
  color: #606266;
}

.field-input {
  width: 260rpx;
}

.field-picker {
  width: 260rpx;
}

.picker-text {
  font-size: 24rpx;
  color: #303133;
  text-align: right;
}

.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx 24rpx;
  background-color: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}
</style>
