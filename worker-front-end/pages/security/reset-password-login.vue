<template>
  <view class="page reset-page">
    <view class="page-shell">
      <image
        class="city-bg"
        src="/static/login/worker-login-bg.png"
        mode="widthFix"
      />
      <view class="hero-glow"></view>

      <view class="top-nav">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="18" color="#2E3A4C" />
          <text class="nav-back-text">返回</text>
        </view>
        <text class="nav-title">找回密码</text>
        <view class="nav-placeholder"></view>
      </view>

      <view class="hero-section">
        <view class="hero-copy">
          <text class="hero-title">找回密码</text>
          <view class="hero-badge">
            <text class="hero-badge-text">邮箱验证</text>
          </view>
          <text class="hero-desc">验证账号邮箱后，重新设置登录密码</text>
        </view>
        <image
          class="hero-worker"
          src="/static/login/worker-login-hero.png"
          mode="widthFix"
        />
      </view>

      <view class="form-card">
        <view class="form-stack">
          <view class="field">
            <view class="field-icon">
              <u-icon name="email" size="22" color="#8F99A8" />
            </view>
            <input
              v-model="email"
              class="field-input"
              type="text"
              maxlength="60"
              confirm-type="next"
              placeholder="请输入账号邮箱"
              placeholder-class="input-placeholder"
            />
          </view>

          <view class="field">
            <view class="field-icon">
              <u-icon name="file-text" size="22" color="#8F99A8" />
            </view>
            <input
              v-model="code"
              class="field-input"
              type="number"
              maxlength="6"
              confirm-type="next"
              placeholder="请输入验证码"
              placeholder-class="input-placeholder"
            />
            <view class="field-divider"></view>
            <text
              class="field-action"
              :class="{ disabled: isSendDisabled }"
              @click="sendCode"
            >
              {{ sendText }}
            </text>
          </view>

          <view class="field">
            <view class="field-icon">
              <u-icon name="lock" size="22" color="#8F99A8" />
            </view>
            <input
              v-model="newPassword"
              class="field-input"
              :password="true"
              maxlength="24"
              confirm-type="next"
              placeholder="请输入新密码，至少6位"
              placeholder-class="input-placeholder"
            />
          </view>

          <view class="field">
            <view class="field-icon">
              <u-icon name="lock" size="22" color="#8F99A8" />
            </view>
            <input
              v-model="confirmPassword"
              class="field-input"
              :password="true"
              maxlength="24"
              confirm-type="done"
              placeholder="请再次输入新密码"
              placeholder-class="input-placeholder"
              @confirm="submit"
            />
          </view>
        </view>

        <view
          class="primary-button"
          :class="{ disabled: saving }"
          @click="submit"
        >
          <text class="primary-button-text">
            {{ saving ? '提交中...' : '确认重置' }}
          </text>
        </view>

        <view class="helper-row">
          <text class="helper-text">收不到验证码？请检查垃圾邮件箱</text>
        </view>

        <view class="assist-row">
          <text class="assist-link" @click="goBack">返回登录</text>
        </view>
      </view>

      <view class="bottom-promise">
        <text class="bottom-promise-text">平台保障 · 安全可靠 · 放心接单</text>
      </view>
    </view>
  </view>
</template>

<script>
import {
  workerSendResetPasswordCode,
  workerResetPasswordByEmail
} from '@/api/workerAuth';

export default {
  name: 'WorkerResetPasswordLoginPage',
  data() {
    return {
      email: '',
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
      if (this.sending) {
        return '发送中';
      }
      if (this.countdown > 0) {
        return `${this.countdown}s后重发`;
      }
      return '获取验证码';
    },
    isSendDisabled() {
      return this.sending || this.countdown > 0;
    }
  },
  onUnload() {
    this.clearCountdown(true);
  },
  beforeUnmount() {
    this.clearCountdown(true);
  },
  methods: {
    validateEmail(email) {
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    },
    goBack() {
      uni.navigateBack({
        fail: () => {
          uni.reLaunch({
            url: '/pages/login/index'
          });
        }
      });
    },
    clearCountdown(resetValue = false) {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
      if (resetValue) {
        this.countdown = 0;
      }
    },
    startCountdown() {
      this.clearCountdown();
      let seconds = 60;
      this.countdown = seconds;
      this.timer = setInterval(() => {
        seconds -= 1;
        if (seconds <= 0) {
          this.clearCountdown(true);
          return;
        }
        this.countdown = seconds;
      }, 1000);
    },
    sendCode() {
      if (this.isSendDisabled) {
        return;
      }
      this.email = (this.email || '').trim();
      if (!this.email) {
        uni.showToast({
          title: '请先输入邮箱地址',
          icon: 'none'
        });
        return;
      }
      if (!this.validateEmail(this.email)) {
        uni.showToast({
          title: '请输入正确的邮箱地址',
          icon: 'none'
        });
        return;
      }
      this.sending = true;
      workerSendResetPasswordCode(this.email)
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({
              title: '验证码已发送',
              icon: 'success'
            });
            this.startCountdown();
            return;
          }
          uni.showToast({
            title: (res && res.message) || '发送失败',
            icon: 'none'
          });
        })
        .catch(() => {
          uni.showToast({
            title: '发送失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.sending = false;
        });
    },
    submit() {
      if (this.saving) {
        return;
      }

      this.email = (this.email || '').trim();
      this.code = (this.code || '').trim();

      if (!this.email || !this.code || !this.newPassword || !this.confirmPassword) {
        uni.showToast({
          title: '请填写完整信息',
          icon: 'none'
        });
        return;
      }
      if (!this.validateEmail(this.email)) {
        uni.showToast({
          title: '请输入正确的邮箱地址',
          icon: 'none'
        });
        return;
      }
      if (this.code.length !== 6) {
        uni.showToast({
          title: '请输入6位验证码',
          icon: 'none'
        });
        return;
      }
      if (this.newPassword.length < 6) {
        uni.showToast({
          title: '新密码至少6位',
          icon: 'none'
        });
        return;
      }
      if (this.newPassword !== this.confirmPassword) {
        uni.showToast({
          title: '两次输入的密码不一致',
          icon: 'none'
        });
        return;
      }

      this.saving = true;
      workerResetPasswordByEmail(
        this.email,
        this.code,
        this.newPassword,
        this.confirmPassword
      )
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({
              title: '重置成功',
              icon: 'success'
            });
            setTimeout(() => {
              this.goBack();
            }, 600);
            return;
          }
          uni.showToast({
            title: (res && res.message) || '重置失败',
            icon: 'none'
          });
        })
        .catch(() => {
          uni.showToast({
            title: '重置失败',
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
.reset-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef5ff 0%, #f6f9ff 42%, #fbfcff 100%);
}

.page-shell {
  position: relative;
  min-height: 100vh;
  box-sizing: border-box;
  overflow-x: hidden;
  padding-top: calc(24rpx + constant(safe-area-inset-top));
  padding-top: calc(24rpx + env(safe-area-inset-top));
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}

.city-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 0;
}

.hero-glow {
  position: absolute;
  top: 46rpx;
  right: 58rpx;
  width: 270rpx;
  height: 270rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(45, 120, 255, 0.22) 0%, rgba(45, 120, 255, 0.05) 48%, rgba(45, 120, 255, 0) 76%);
  z-index: 1;
}

.top-nav {
  position: relative;
  z-index: 6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}

.nav-back,
.nav-placeholder {
  width: 116rpx;
  min-height: 44rpx;
}

.nav-back {
  display: flex;
  align-items: center;
}

.nav-back-text {
  margin-left: 8rpx;
  color: #415165;
  font-size: 24rpx;
}

.nav-title {
  color: #1e2736;
  font-size: 30rpx;
  font-weight: 600;
}

.hero-section {
  position: relative;
  min-height: 26vh;
  max-height: 430rpx;
  box-sizing: border-box;
  padding: 18rpx 30rpx 0;
  z-index: 2;
}

.hero-copy {
  position: relative;
  max-width: 360rpx;
  padding-top: 56rpx;
  z-index: 3;
}

.hero-title {
  display: block;
  color: #1c2533;
  font-size: 60rpx;
  font-weight: 900;
  line-height: 1.08;
  letter-spacing: 2rpx;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 18rpx;
  padding: 12rpx 22rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #1f7cff 0%, #0b63fb 100%);
  box-shadow: 0 18rpx 42rpx rgba(22, 109, 255, 0.24);
}

.hero-badge-text {
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1;
}

.hero-desc {
  display: block;
  margin-top: 18rpx;
  color: #506178;
  font-size: 25rpx;
  line-height: 1.46;
}

.hero-worker {
  position: absolute;
  right: 6rpx;
  bottom: -28rpx;
  width: 316rpx;
  opacity: 0.98;
  z-index: 2;
}

.form-card {
  position: relative;
  z-index: 5;
  width: calc(100% - 64rpx);
  margin: -4rpx auto 0;
  box-sizing: border-box;
  padding: 22rpx 24rpx 24rpx;
  border-radius: 38rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 26rpx 80rpx rgba(70, 104, 163, 0.14);
}

.form-stack {
  display: flex;
  flex-direction: column;
}

.form-stack .field + .field {
  margin-top: 16rpx;
}

.field {
  display: flex;
  align-items: center;
  min-height: 88rpx;
  box-sizing: border-box;
  padding: 0 22rpx;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1px solid #edf1f8;
  box-shadow: 0 12rpx 30rpx rgba(74, 100, 145, 0.05);
}

.field-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42rpx;
  margin-right: 18rpx;
  flex-shrink: 0;
}

.field-input {
  flex: 1;
  min-width: 0;
  height: 88rpx;
  color: #263246;
  font-size: 29rpx;
}

.input-placeholder {
  color: #b8c1cf;
}

.field-divider {
  width: 1px;
  height: 40rpx;
  margin: 0 20rpx 0 16rpx;
  background: #e8eef7;
  flex-shrink: 0;
}

.field-action {
  color: #146fff;
  font-size: 28rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.field-action.disabled {
  color: #9ab7e8;
}

.primary-button {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  margin-top: 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #1f7bff 0%, #0b59f1 100%);
  box-shadow: 0 22rpx 40rpx rgba(17, 102, 255, 0.25);
}

.primary-button.disabled {
  opacity: 0.76;
}

.primary-button-text {
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.helper-row {
  margin-top: 18rpx;
  text-align: center;
}

.helper-text {
  color: #95a2b4;
  font-size: 24rpx;
  line-height: 1.5;
}

.assist-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10rpx;
}

.assist-link {
  color: #176fff;
  font-size: 26rpx;
  line-height: 1.4;
}

.bottom-promise {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 12rpx;
  padding: 0 42rpx;
}

.bottom-promise-text {
  color: #99a5b5;
  font-size: 22rpx;
  line-height: 1.5;
}

@media screen and (max-width: 380px) {
  .top-nav {
    padding-left: 20rpx;
    padding-right: 20rpx;
  }

  .hero-section {
    min-height: 24vh;
    padding-left: 24rpx;
    padding-right: 24rpx;
    padding-top: 14rpx;
  }

  .hero-copy {
    max-width: 340rpx;
    padding-top: 48rpx;
  }

  .hero-title {
    font-size: 54rpx;
  }

  .hero-badge-text {
    font-size: 30rpx;
  }

  .hero-desc {
    font-size: 23rpx;
  }

  .hero-worker {
    width: 284rpx;
    bottom: -18rpx;
    right: -8rpx;
  }

  .form-card {
    width: calc(100% - 44rpx);
    margin-top: 0;
    padding-left: 20rpx;
    padding-right: 20rpx;
  }

  .field-input,
  .field-action,
  .helper-text,
  .assist-link,
  .bottom-promise-text {
    font-size: 23rpx;
  }
}
</style>
