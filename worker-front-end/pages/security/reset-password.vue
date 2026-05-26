<template>
  <view class="page worker-sec">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">重置密码</text>
      </view>
      <view class="nav-right" />
    </view>

    <view class="card">
      <view class="tip">
        <text class="tip-text">将向当前账号邮箱发送验证码</text>
      </view>

      <view class="row">
        <text class="label">当前邮箱</text>
        <text class="value">{{ currentEmail || '-' }}</text>
      </view>

      <view class="row">
        <text class="label">验证码</text>
        <view class="right">
          <u-input
            v-model="code"
            placeholder="请输入验证码"
            border="none"
            input-align="right"
          />
        </view>
        <view class="send-btn">
          <u-button
            :text="sendText"
            size="mini"
            shape="circle"
            :loading="sending"
            :disabled="countdown > 0 || !currentEmail"
            @click="sendCode"
          />
        </view>
      </view>

      <view class="row">
        <text class="label">新密码</text>
        <view class="right">
          <u-input
            v-model="newPassword"
            type="password"
            placeholder="至少6位"
            border="none"
            input-align="right"
          />
        </view>
      </view>

      <view class="row">
        <text class="label">确认密码</text>
        <view class="right">
          <u-input
            v-model="confirmPassword"
            type="password"
            placeholder="再次输入新密码"
            border="none"
            input-align="right"
          />
        </view>
      </view>

      <view class="actions">
        <u-button
          text="确认重置"
          type="primary"
          shape="circle"
          :loading="saving"
          @click="submit"
        />
      </view>
    </view>
  </view>
</template>

<script>
import { getWorkerProfile } from '@/api/workerProfile';
import {
  sendWorkerResetPasswordCode,
  resetWorkerPasswordByCode
} from '@/api/workerSecurity';

export default {
  name: 'WorkerResetPasswordPage',
  data() {
    return {
      currentEmail: '',
      code: '',
      newPassword: '',
      confirmPassword: '',
      sending: false,
      saving: false,
      countdown: 0,
      timer: null
    };
  },
  computed: {
    sendText() {
      return this.countdown > 0 ? `${this.countdown}s` : '获取验证码';
    }
  },
  onShow() {
    this.loadEmail();
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer);
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadEmail() {
      const app = getApp();
      const cached =
        app &&
        app.globalData &&
        app.globalData.workerInfo &&
        app.globalData.workerInfo.email;
      if (cached) {
        this.currentEmail = cached;
        return;
      }
      getWorkerProfile()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.currentEmail = res.data.email || '';
          }
        })
        .catch(() => {});
    },
    startCountdown() {
      this.countdown = 60;
      if (this.timer) clearInterval(this.timer);
      this.timer = setInterval(() => {
        this.countdown -= 1;
        if (this.countdown <= 0) {
          clearInterval(this.timer);
          this.timer = null;
          this.countdown = 0;
        }
      }, 1000);
    },
    sendCode() {
      if (this.sending || this.countdown > 0) return;
      if (!this.currentEmail) {
        uni.showToast({ title: '当前账号未绑定邮箱', icon: 'none' });
        return;
      }
      this.sending = true;
      sendWorkerResetPasswordCode()
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({ title: '已发送', icon: 'success' });
            this.startCountdown();
          } else {
            uni.showToast({ title: res?.message || '发送失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '发送失败', icon: 'none' });
        })
        .finally(() => {
          this.sending = false;
        });
    },
    submit() {
      if (this.saving) return;
      if (!this.code || !this.newPassword || !this.confirmPassword) {
        uni.showToast({ title: '请填写完整', icon: 'none' });
        return;
      }
      if (this.newPassword !== this.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' });
        return;
      }
      this.saving = true;
      resetWorkerPasswordByCode({
        code: this.code,
        newPassword: this.newPassword,
        confirmPassword: this.confirmPassword
      })
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({ title: '重置成功', icon: 'success' });
            setTimeout(() => {
              uni.navigateBack();
            }, 600);
          } else {
            uni.showToast({ title: res?.message || '重置失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '重置失败', icon: 'none' });
        })
        .finally(() => {
          this.saving = false;
        });
    }
  }
};
</script>

<style scoped>
.worker-sec {
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

.card {
  margin: 16rpx 24rpx 0;
  padding: 24rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
  box-sizing: border-box;
}

.tip {
  margin-bottom: 10rpx;
}

.tip-text {
  font-size: 24rpx;
  color: #909399;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.label {
  font-size: 26rpx;
  color: #606266;
}

.value {
  font-size: 26rpx;
  color: #303133;
}

.right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  margin-left: 16rpx;
}

.send-btn {
  margin-left: 12rpx;
}

.actions {
  margin-top: 24rpx;
}
</style>

