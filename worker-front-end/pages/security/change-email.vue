<template>
  <view class="page worker-sec">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">修改邮箱</text>
      </view>
      <view class="nav-right" />
    </view>

    <view class="card">
      <view class="tip-box">
        <text class="tip-text">验证码会发送到新邮箱，且新邮箱不能是已注册邮箱。</text>
      </view>

      <view class="row">
        <text class="label">当前邮箱</text>
        <view class="right">
          <text class="value">{{ currentEmail || '未绑定邮箱' }}</text>
        </view>
      </view>

      <view class="row">
        <text class="label">新邮箱</text>
        <view class="right">
          <u-input
            v-model="newEmail"
            placeholder="请输入新邮箱"
            border="none"
            input-align="right"
          />
        </view>
      </view>

      <view class="row">
        <text class="label">确认邮箱</text>
        <view class="right">
          <u-input
            v-model="confirmEmail"
            placeholder="再次输入新邮箱"
            border="none"
            input-align="right"
          />
        </view>
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
            :disabled="countdown > 0 || !canSend"
            @click="sendCode"
          />
        </view>
      </view>

      <view class="actions">
        <u-button
          text="确认修改"
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
import { sendWorkerChangeEmailCode, changeWorkerEmail } from '@/api/workerSecurity';
import { getWorkerProfile } from '@/api/workerProfile';

export default {
  name: 'WorkerChangeEmailPage',
  data() {
    return {
      currentEmail: '',
      newEmail: '',
      confirmEmail: '',
      code: '',
      sending: false,
      saving: false,
      countdown: 0,
      timer: null
    };
  },
  computed: {
    canSend() {
      const email = this.normalizeEmail(this.newEmail);
      const confirmEmail = this.normalizeEmail(this.confirmEmail);
      return !!email &&
        !!confirmEmail &&
        email === confirmEmail &&
        this.isValidEmail(email) &&
        (!this.currentEmail || email !== this.normalizeEmail(this.currentEmail));
    },
    sendText() {
      return this.countdown > 0 ? `${this.countdown}s` : '获取验证码';
    }
  },
  onShow() {
    this.loadCurrentEmail();
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer);
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    normalizeEmail(value) {
      return String(value || '').trim().toLowerCase();
    },
    isValidEmail(value) {
      return /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(String(value || '').trim());
    },
    loadCurrentEmail() {
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
      const nextEmail = this.normalizeEmail(this.newEmail);
      const confirmedEmail = this.normalizeEmail(this.confirmEmail);
      if (!nextEmail || !confirmedEmail) {
        uni.showToast({ title: '请先填写新邮箱', icon: 'none' });
        return;
      }
      if (!this.isValidEmail(nextEmail)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
        return;
      }
      if (nextEmail !== confirmedEmail) {
        uni.showToast({ title: '两次邮箱不一致', icon: 'none' });
        return;
      }
      if (this.currentEmail && nextEmail === this.normalizeEmail(this.currentEmail)) {
        uni.showToast({ title: '新邮箱不能与当前邮箱相同', icon: 'none' });
        return;
      }
      if (!this.canSend) {
        uni.showToast({ title: '请先确认新邮箱', icon: 'none' });
        return;
      }
      this.sending = true;
      sendWorkerChangeEmailCode(nextEmail)
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
      const nextEmail = this.normalizeEmail(this.newEmail);
      const confirmedEmail = this.normalizeEmail(this.confirmEmail);
      if (!nextEmail || !confirmedEmail) {
        uni.showToast({ title: '请填写完整', icon: 'none' });
        return;
      }
      if (!this.isValidEmail(nextEmail)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
        return;
      }
      if (nextEmail !== confirmedEmail) {
        uni.showToast({ title: '两次邮箱不一致', icon: 'none' });
        return;
      }
      if (this.currentEmail && nextEmail === this.normalizeEmail(this.currentEmail)) {
        uni.showToast({ title: '新邮箱不能与当前邮箱相同', icon: 'none' });
        return;
      }
      if (!this.code) {
        uni.showToast({ title: '请输入验证码', icon: 'none' });
        return;
      }
      this.saving = true;
      changeWorkerEmail({
        newEmail: nextEmail,
        code: this.code
      })
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({ title: '修改成功', icon: 'success' });
            this.currentEmail = nextEmail;
            const app = getApp();
            if (app && app.globalData && app.globalData.workerInfo) {
              app.globalData.workerInfo.email = nextEmail;
            }
            setTimeout(() => {
              uni.navigateBack();
            }, 600);
          } else {
            uni.showToast({ title: res?.message || '修改失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '修改失败', icon: 'none' });
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

.tip-box {
  margin-bottom: 12rpx;
  padding: 18rpx 20rpx;
  border-radius: 16rpx;
  background: #f7f8fa;
}

.tip-text {
  font-size: 24rpx;
  line-height: 1.6;
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
