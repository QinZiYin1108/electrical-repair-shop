<template>
  <view class="page login-page">
    <view class="page-shell">
      <image
        class="city-bg"
        src="/static/login/worker-login-bg.png"
        mode="widthFix"
      />
      <view class="hero-glow"></view>

      <view class="hero-section">
        <view class="hero-copy">
          <text class="brand-title">安修到家</text>
          <view class="hero-badge">
            <text class="hero-badge-text">维修师傅端</text>
          </view>
          <text class="hero-desc">专业维修 · 接单自由 · 收入保障</text>
        </view>
        <image
          class="hero-worker"
          src="/static/login/worker-login-hero.png"
          mode="widthFix"
        />
      </view>

      <view class="login-card">
        <view class="mode-tabs">
          <view
            class="mode-tab"
            :class="{ active: mode === 'code' }"
            @click="switchMode('code')"
          >
            <text class="mode-tab-text">验证码登录</text>
            <view class="mode-tab-indicator"></view>
          </view>
          <view
            class="mode-tab"
            :class="{ active: mode === 'password' }"
            @click="switchMode('password')"
          >
            <text class="mode-tab-text">密码登录</text>
            <view class="mode-tab-indicator"></view>
          </view>
        </view>

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
              placeholder="请输入邮箱地址"
              placeholder-class="input-placeholder"
            />
          </view>

          <view v-if="mode === 'code'" class="field field-code">
            <view class="field-icon">
              <u-icon name="file-text" size="22" color="#8F99A8" />
            </view>
            <input
              v-model="codeInput"
              class="field-input"
              type="number"
              maxlength="6"
              confirm-type="done"
              placeholder="请输入验证码"
              placeholder-class="input-placeholder"
              @confirm="handleSubmit"
            />
            <view class="field-divider"></view>
            <text
              class="field-action"
              :class="{ disabled: isCodeActionDisabled }"
              @click="handleSendCode"
            >
              {{ codeActionText }}
            </text>
          </view>

          <view v-else class="field">
            <view class="field-icon">
              <u-icon name="lock" size="22" color="#8F99A8" />
            </view>
            <input
              v-model="password"
              class="field-input"
              :password="true"
              maxlength="24"
              confirm-type="done"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
              @confirm="handleSubmit"
            />
          </view>
        </view>

        <view
          class="primary-button"
          :class="{ disabled: submitting }"
          @click="handleSubmit"
        >
          <text class="primary-button-text">
            {{ submitting ? '登录中...' : '登录' }}
          </text>
        </view>

        <view class="agreement-row" @click="toggleAgreement">
          <view class="agreement-box" :class="{ checked: agreed }">
            <u-icon
              v-if="agreed"
              name="checkmark"
              size="12"
              color="#FFFFFF"
            />
          </view>
          <view class="agreement-copy">
            <text class="agreement-text">我已阅读并同意</text>
            <text
              class="agreement-link"
              @click.stop="openProtocol('user')"
            >
              《用户协议》
            </text>
            <text class="agreement-text">和</text>
            <text
              class="agreement-link"
              @click.stop="openProtocol('privacy')"
            >
              《隐私政策》
            </text>
          </view>
        </view>

        <view class="assist-row">
          <text class="assist-link muted" @click="goResetPassword">
            忘记密码
          </text>
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
  workerLoginByPassword,
  workerSendLoginCode,
  workerLoginByCode
} from '@/api/workerAuth';

export default {
  name: 'WorkerLoginPage',
  data() {
    return {
      mode: 'code',
      email: '',
      password: '',
      codeInput: '',
      countdown: 0,
      countdownTimer: null,
      submitting: false,
      sending: false,
      agreed: false
    };
  },
  computed: {
    codeActionText() {
      if (this.sending) {
        return '发送中';
      }
      if (this.countdown > 0) {
        return `${this.countdown}s后重发`;
      }
      return '获取验证码';
    },
    isCodeActionDisabled() {
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
    formatTimestamp(ts) {
      if (!ts) return '';
      const date = new Date(ts);
      const pad = (value) => String(value).padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },
    validateEmail(email) {
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    },
    ensureAgreed() {
      if (this.agreed) {
        return true;
      }
      uni.showToast({
        title: '请先阅读并同意用户协议和隐私政策',
        icon: 'none'
      });
      return false;
    },
    toggleAgreement() {
      this.agreed = !this.agreed;
    },
    openProtocol(type) {
      uni.navigateTo({
        url: `/pages/protocol/index?type=${type}`
      });
    },
    switchMode(nextMode) {
      if (this.mode === nextMode) {
        return;
      }
      this.mode = nextMode;
      this.password = '';
      this.codeInput = '';
    },
    clearCountdown(resetValue = false) {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer);
        this.countdownTimer = null;
      }
      if (resetValue) {
        this.countdown = 0;
      }
    },
    startCountdown() {
      this.clearCountdown();
      let seconds = 60;
      this.countdown = seconds;
      this.countdownTimer = setInterval(() => {
        seconds -= 1;
        if (seconds <= 0) {
          this.clearCountdown(true);
          return;
        }
        this.countdown = seconds;
      }, 1000);
    },
    handleCancelConfirmIfNeeded(data, onConfirm) {
      if (!data || !data.needCancelConfirm) {
        return false;
      }
      const deadlineText = this.formatTimestamp(data.cancelDeadlineTime);
      const content = deadlineText
        ? `账号已提交注销申请，反悔期截止：${deadlineText}。\n继续登录将取消注销，是否继续登录？`
        : '账号已提交注销申请。\n继续登录将取消注销，是否继续登录？';
      uni.showModal({
        title: '注销确认',
        content,
        confirmText: '继续登录',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm && typeof onConfirm === 'function') {
            onConfirm();
          }
        }
      });
      return true;
    },
    handleLoginSuccess(data) {
      uni.setStorageSync('workerToken', data.token);
      const app = getApp();
      if (app && app.globalData) {
        app.globalData.workerIsLogin = true;
      }
      uni.showToast({
        title: data.cancelRevoked ? '已取消注销，登录成功' : '登录成功',
        icon: 'success'
      });
      uni.reLaunch({
        url: '/pages/index/index'
      });
    },
    goResetPassword() {
      uni.navigateTo({
        url: '/pages/security/reset-password-login'
      });
    },
    handleSendCode() {
      if (this.isCodeActionDisabled) {
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
      if (!this.ensureAgreed()) {
        return;
      }
      this.sending = true;
      workerSendLoginCode(this.email)
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
    handleSubmit() {
      if (this.submitting) {
        return;
      }

      this.email = (this.email || '').trim();
      this.codeInput = (this.codeInput || '').trim();

      if (!this.email) {
        uni.showToast({
          title: '请输入邮箱地址',
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
      if (!this.ensureAgreed()) {
        return;
      }

      if (this.mode === 'password') {
        if (!this.password) {
          uni.showToast({
            title: '请输入密码',
            icon: 'none'
          });
          return;
        }
        this.submitting = true;
        workerLoginByPassword(this.email, this.password, false)
          .then((res) => {
            if (res && res.code === 200 && res.data) {
              if (this.handleCancelConfirmIfNeeded(res.data, () => {
                this.submitting = true;
                workerLoginByPassword(this.email, this.password, true)
                  .then((res2) => {
                    if (res2 && res2.code === 200 && res2.data && res2.data.token) {
                      this.handleLoginSuccess(res2.data);
                      return;
                    }
                    uni.showToast({
                      title: (res2 && res2.message) || '登录失败',
                      icon: 'none'
                    });
                  })
                  .catch(() => {
                    uni.showToast({
                      title: '登录失败',
                      icon: 'none'
                    });
                  })
                  .finally(() => {
                    this.submitting = false;
                  });
              })) {
                return;
              }
              if (res.data.token) {
                this.handleLoginSuccess(res.data);
                return;
              }
            }
            uni.showToast({
              title: (res && res.message) || '登录失败',
              icon: 'none'
            });
          })
          .catch(() => {
            uni.showToast({
              title: '登录失败',
              icon: 'none'
            });
          })
          .finally(() => {
            this.submitting = false;
          });
        return;
      }

      if (!this.codeInput || this.codeInput.length !== 6) {
        uni.showToast({
          title: '请输入6位验证码',
          icon: 'none'
        });
        return;
      }

      this.submitting = true;
      workerLoginByCode(this.email, this.codeInput, false)
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            if (this.handleCancelConfirmIfNeeded(res.data, () => {
              this.submitting = true;
              workerLoginByCode(this.email, this.codeInput, true)
                .then((res2) => {
                  if (res2 && res2.code === 200 && res2.data && res2.data.token) {
                    this.handleLoginSuccess(res2.data);
                    return;
                  }
                  uni.showToast({
                    title: (res2 && res2.message) || '登录失败',
                    icon: 'none'
                  });
                })
                .catch(() => {
                  uni.showToast({
                    title: '登录失败',
                    icon: 'none'
                  });
                })
                .finally(() => {
                  this.submitting = false;
                });
            })) {
              return;
            }
            if (res.data.token) {
              this.handleLoginSuccess(res.data);
              return;
            }
          }
          uni.showToast({
            title: (res && res.message) || '登录失败',
            icon: 'none'
          });
        })
        .catch(() => {
          uni.showToast({
            title: '登录失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.submitting = false;
        });
    }
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef5ff 0%, #f6f9ff 42%, #fbfcff 100%);
}

.page-shell {
  position: relative;
  min-height: 100vh;
  box-sizing: border-box;
  overflow-x: hidden;
  padding-top: calc(28rpx + constant(safe-area-inset-top));
  padding-top: calc(28rpx + env(safe-area-inset-top));
  padding-bottom: calc(18rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
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
  top: 56rpx;
  right: 56rpx;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(45, 120, 255, 0.22) 0%, rgba(45, 120, 255, 0.06) 45%, rgba(45, 120, 255, 0) 76%);
  z-index: 1;
}

.hero-section {
  position: relative;
  min-height: 32vh;
  max-height: 520rpx;
  box-sizing: border-box;
  padding: 22rpx 36rpx 0;
  z-index: 2;
}

.hero-copy {
  position: relative;
  max-width: 380rpx;
  padding-top: 86rpx;
  z-index: 3;
}

.brand-title {
  display: block;
  color: #1c2533;
  font-size: 66rpx;
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: 2rpx;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 22rpx;
  padding: 14rpx 26rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #1f7cff 0%, #0b63fb 100%);
  box-shadow: 0 18rpx 42rpx rgba(22, 109, 255, 0.28);
}

.hero-badge-text {
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1;
}

.hero-desc {
  display: block;
  margin-top: 24rpx;
  color: #4e5d72;
  font-size: 27rpx;
  line-height: 1.42;
  letter-spacing: 1rpx;
}

.hero-worker {
  position: absolute;
  right: 6rpx;
  bottom: -30rpx;
  width: 388rpx;
  z-index: 2;
}

.login-card {
  position: relative;
  z-index: 5;
  width: calc(100% - 64rpx);
  margin: 0.8125rem auto 0;
  box-sizing: border-box;
  padding: 22rpx 26rpx 24rpx;
  border-radius: 38rpx;
  background: #ffffff;
  box-shadow: 0 26rpx 80rpx rgba(70, 104, 163, 0.14);
}

.mode-tabs {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 4rpx 10rpx 0;
}

.mode-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 0 14rpx;
}

.mode-tab-text {
  color: #2b3444;
  font-size: 32rpx;
  font-weight: 600;
  line-height: 1.2;
}

.mode-tab.active .mode-tab-text {
  color: #146fff;
  font-weight: 700;
}

.mode-tab-indicator {
  width: 72rpx;
  height: 7rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  background: transparent;
}

.mode-tab.active .mode-tab-indicator {
  background: linear-gradient(90deg, #1a7bff 0%, #0d60fa 100%);
  box-shadow: 0 8rpx 18rpx rgba(23, 113, 255, 0.28);
}

.form-stack {
  display: flex;
  flex-direction: column;
  margin-top: 10rpx;
}

.form-stack .field + .field {
  margin-top: 16rpx;
}

.field {
  display: flex;
  align-items: center;
  min-height: 90rpx;
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
  height: 90rpx;
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

.agreement-row {
  display: flex;
  align-items: flex-start;
  margin-top: 20rpx;
  padding: 0 6rpx;
}

.agreement-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34rpx;
  height: 34rpx;
  margin-top: 4rpx;
  border-radius: 50%;
  border: 2rpx solid #d8dfeb;
  background: #ffffff;
  flex-shrink: 0;
}

.agreement-box.checked {
  border-color: #166fff;
  background: #166fff;
  box-shadow: 0 10rpx 24rpx rgba(22, 111, 255, 0.2);
}

.agreement-copy {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  margin-left: 14rpx;
  line-height: 1.7;
}

.agreement-text,
.agreement-link {
  font-size: 26rpx;
}

.agreement-text {
  color: #8f9bad;
}

.agreement-link {
  color: #176fff;
  font-weight: 600;
}

.assist-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 16rpx;
  padding: 0 6rpx;
}

.assist-link {
  color: #176fff;
  font-size: 26rpx;
  line-height: 1.4;
}

.assist-link.muted {
  color: #8994a6;
}

.bottom-promise {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 14rpx;
  padding: 0 42rpx;
}

.bottom-promise-text {
  color: #99a5b5;
  font-size: 24rpx;
  line-height: 1.5;
}

@media screen and (max-width: 380px) {
  .hero-section {
    min-height: 30vh;
    padding-left: 28rpx;
    padding-right: 28rpx;
    padding-top: 18rpx;
  }

  .hero-copy {
    max-width: 340rpx;
    padding-top: 76rpx;
  }

  .brand-title {
    font-size: 60rpx;
  }

  .hero-badge-text {
    font-size: 34rpx;
  }

  .hero-desc {
    font-size: 25rpx;
  }

  .hero-worker {
    width: 346rpx;
    bottom: -26rpx;
    right: -8rpx;
  }

  .login-card {
    width: calc(100% - 44rpx);
    margin-top: -2rpx;
    padding-left: 22rpx;
    padding-right: 22rpx;
  }

  .mode-tab-text {
    font-size: 30rpx;
  }

  .field-input,
  .field-action,
  .agreement-text,
  .agreement-link,
  .assist-link,
  .bottom-promise-text {
    font-size: 24rpx;
  }
}
</style>
