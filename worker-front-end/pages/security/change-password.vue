<template>
  <view class="page worker-sec">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">修改密码</text>
      </view>
      <view class="nav-right" />
    </view>

    <view class="card">
      <view class="tip-box">
        <text class="tip-text">修改密码时需要先填写当前旧密码。</text>
      </view>

      <view class="row">
        <text class="label">旧密码</text>
        <view class="right">
          <u-input
            v-model="oldPassword"
            type="password"
            placeholder="请输入旧密码"
            border="none"
            input-align="right"
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
          text="确认修改"
          type="primary"
          shape="circle"
          :loading="saving"
          @click="submit"
        />
      </view>

      <view class="helper">
        <text class="helper-link" @click="goReset">忘记密码？使用邮箱验证码重置</text>
      </view>
    </view>
  </view>
</template>

<script>
import { changeWorkerPassword } from '@/api/workerSecurity';

export default {
  name: 'WorkerChangePasswordPage',
  data() {
    return {
      saving: false,
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    goReset() {
      uni.navigateTo({
        url: '/pages/security/reset-password'
      });
    },
    submit() {
      if (this.saving) return;
      if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
        uni.showToast({ title: '请填写完整', icon: 'none' });
        return;
      }
      if (this.newPassword.length < 6) {
        uni.showToast({ title: '新密码至少6位', icon: 'none' });
        return;
      }
      if (this.oldPassword === this.newPassword) {
        uni.showToast({ title: '新密码不能与旧密码相同', icon: 'none' });
        return;
      }
      if (this.newPassword !== this.confirmPassword) {
        uni.showToast({ title: '两次新密码不一致', icon: 'none' });
        return;
      }
      this.saving = true;
      changeWorkerPassword({
        oldPassword: this.oldPassword,
        newPassword: this.newPassword,
        confirmPassword: this.confirmPassword
      })
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({ title: '修改成功', icon: 'success' });
            this.oldPassword = '';
            this.newPassword = '';
            this.confirmPassword = '';
            setTimeout(() => {
              uni.navigateBack();
            }, 500);
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

.row:last-child {
  border-bottom: none;
}

.label {
  font-size: 26rpx;
  color: #606266;
}

.right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  margin-left: 16rpx;
}

.actions {
  margin-top: 24rpx;
}

.helper {
  margin-top: 18rpx;
  display: flex;
  justify-content: center;
}

.helper-link {
  font-size: 26rpx;
  color: #3c9cff;
}
</style>
