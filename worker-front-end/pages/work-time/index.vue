<template>
  <view class="page work-time-page">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">工作时间</text>
      </view>
      <view class="nav-right" />
    </view>

    <scroll-view class="content" scroll-y>
      <view class="tip-card">
        <text class="tip-text">设置每周可接单时间。关闭某一天后，该天将暂停自动接单。</text>
      </view>

      <view
        v-for="(item, index) in workTimes"
        :key="item.dayOfWeek"
        class="day-card"
      >
        <view class="day-head">
          <text class="day-name">{{ item.dayLabel }}</text>
          <u-switch v-model="item.enabled" />
        </view>

        <view class="time-row" :class="item.enabled ? '' : 'time-row-disabled'">
          <text class="time-label">开始时间</text>
          <picker
            class="time-picker"
            mode="time"
            :value="item.startTime"
            @change="onTimeChange(index, 'startTime', $event)"
          >
            <view class="picker-text">{{ item.startTime || '请选择' }}</view>
          </picker>
        </view>

        <view class="time-row" :class="item.enabled ? '' : 'time-row-disabled'">
          <text class="time-label">结束时间</text>
          <picker
            class="time-picker"
            mode="time"
            :value="item.endTime"
            @change="onTimeChange(index, 'endTime', $event)"
          >
            <view class="picker-text">{{ item.endTime || '请选择' }}</view>
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
        @click="saveWorkTimes"
      />
    </view>
  </view>
</template>

<script>
import { getWorkerWorkTimes, updateWorkerWorkTimes } from '@/api/workerWorkTime';

const WEEK_DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

export default {
  name: 'WorkerWorkTimePage',
  data() {
    return {
      loading: false,
      saving: false,
      workTimes: Array.from({ length: 7 }, (_, index) => this.createDefaultItem(index + 1))
    };
  },
  onShow() {
    this.loadWorkTimes();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    createDefaultItem(dayOfWeek) {
      return {
        id: '',
        dayOfWeek,
        dayLabel: WEEK_DAY_LABELS[dayOfWeek - 1],
        enabled: true,
        startTime: '09:00',
        endTime: '18:00'
      };
    },
    normalizePickerTime(value, fallback) {
      if (value === null || value === undefined) return fallback;
      const text = String(value).trim();
      if (!text) return fallback;
      if (/^\d{2}:\d{2}$/.test(text)) return text;
      if (/^\d{2}:\d{2}:\d{2}$/.test(text)) return text.slice(0, 5);
      return fallback;
    },
    normalizeWorkTime(item, dayOfWeek) {
      const normalized = this.createDefaultItem(dayOfWeek);
      if (!item || typeof item !== 'object') {
        return normalized;
      }
      normalized.id = item.id || '';
      normalized.enabled = !(item.isAvailable === 0);
      normalized.startTime = this.normalizePickerTime(item.startTime, normalized.startTime);
      normalized.endTime = this.normalizePickerTime(item.endTime, normalized.endTime);
      return normalized;
    },
    loadWorkTimes() {
      if (this.loading) return;
      this.loading = true;
      getWorkerWorkTimes()
        .then((res) => {
          if (res && res.code === 200 && Array.isArray(res.data)) {
            const map = new Map();
            res.data.forEach((item) => {
              if (!item || map.has(item.dayOfWeek)) return;
              if (item.dayOfWeek >= 1 && item.dayOfWeek <= 7) {
                map.set(item.dayOfWeek, item);
              }
            });
            this.workTimes = Array.from({ length: 7 }, (_, index) => {
              const dayOfWeek = index + 1;
              return this.normalizeWorkTime(map.get(dayOfWeek), dayOfWeek);
            });
            return;
          }
          uni.showToast({
            title: (res && res.message) || '获取工作时间失败',
            icon: 'none'
          });
        })
        .catch(() => {
          uni.showToast({
            title: '获取工作时间失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    onTimeChange(index, field, event) {
      const value = event && event.detail ? event.detail.value : '';
      if (!value || !this.workTimes[index]) return;
      this.workTimes[index][field] = value;
    },
    toMinutes(value) {
      if (!value) return NaN;
      const parts = String(value).split(':');
      if (parts.length < 2) return NaN;
      const hour = Number(parts[0]);
      const minute = Number(parts[1]);
      if (!Number.isInteger(hour) || !Number.isInteger(minute)) return NaN;
      return hour * 60 + minute;
    },
    toApiTime(value, fallback) {
      const base = this.normalizePickerTime(value, fallback);
      return `${base}:00`;
    },
    validateWorkTimes() {
      for (const item of this.workTimes) {
        if (!item.enabled) continue;
        if (!item.startTime || !item.endTime) {
          return `${item.dayLabel}请设置开始和结束时间`;
        }
        const start = this.toMinutes(item.startTime);
        const end = this.toMinutes(item.endTime);
        if (!Number.isFinite(start) || !Number.isFinite(end)) {
          return `${item.dayLabel}时间格式不正确`;
        }
        if (start >= end) {
          return `${item.dayLabel}开始时间需要早于结束时间`;
        }
      }
      return '';
    },
    buildPayload() {
      return this.workTimes.map((item) => ({
        id: item.id || '',
        dayOfWeek: item.dayOfWeek,
        startTime: this.toApiTime(item.startTime, '09:00'),
        endTime: this.toApiTime(item.endTime, '18:00'),
        isAvailable: item.enabled ? 1 : 0
      }));
    },
    saveWorkTimes() {
      if (this.saving) return;
      const validateMessage = this.validateWorkTimes();
      if (validateMessage) {
        uni.showToast({
          title: validateMessage,
          icon: 'none'
        });
        return;
      }
      this.saving = true;
      updateWorkerWorkTimes(this.buildPayload())
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({
              title: '保存成功',
              icon: 'success'
            });
            this.loadWorkTimes();
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
.work-time-page {
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
  line-height: 34rpx;
}

.day-card {
  border-radius: 16rpx;
  background-color: #ffffff;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.day-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
}

.time-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 0;
}

.time-row-disabled {
  opacity: 0.6;
}

.time-label {
  font-size: 24rpx;
  color: #606266;
}

.time-picker {
  min-width: 180rpx;
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
