<template>
  <view class="page worker-certification">
    <view class="nav-bar">
      <view class="nav-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="nav-center">
        <text class="nav-title">实名认证</text>
      </view>
      <view class="nav-right" />
    </view>

    <view class="card">
      <view class="row">
        <text class="label">手机号</text>
        <view class="right">
          <u-input
            v-model="form.phone"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            border="none"
            input-align="right"
          />
        </view>
      </view>
      <view class="row">
        <text class="label">真实姓名</text>
        <view class="right">
          <u-input
            v-model="form.realName"
            placeholder="请输入真实姓名"
            border="none"
            input-align="right"
          />
        </view>
      </view>
      <view class="row">
        <text class="label">身份证号</text>
        <view class="right">
          <u-input
            v-model="form.idCard"
            placeholder="请输入身份证号"
            border="none"
            input-align="right"
          />
        </view>
      </view>

      <u-button
        class="submit-btn"
        text="提交认证"
        type="primary"
        shape="circle"
        :loading="submitting"
        @click="submit"
      />

      <view class="tips">
        <text class="tips-text">说明：当前为模拟认证流程，仅校验手机号和身份证号格式。</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getWorkerProfile, submitWorkerCertification } from '@/api/workerProfile';

export default {
  name: 'WorkerCertificationPage',
  data() {
    return {
      submitting: false,
      form: {
        phone: '',
        realName: '',
        idCard: ''
      }
    };
  },
  onShow() {
    this.loadProfile();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    loadProfile() {
      getWorkerProfile()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.form.phone = res.data.phone || '';
            this.form.realName = res.data.realName || '';
            this.form.idCard = res.data.idCard || '';
          }
        })
        .catch(() => {});
    },
    isPhone(phone) {
      return /^1[3-9]\d{9}$/.test(phone);
    },
    isIdCard(idCard) {
      return /^[1-9]\d{5}(19\d{2}|20\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/.test(idCard);
    },
    submit() {
      if (this.submitting) return;
      const phone = (this.form.phone || '').trim();
      const realName = (this.form.realName || '').trim();
      const idCard = (this.form.idCard || '').trim();
      if (!phone || !realName || !idCard) {
        uni.showToast({ title: '请填写完整信息', icon: 'none' });
        return;
      }
      if (!this.isPhone(phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' });
        return;
      }
      if (!this.isIdCard(idCard)) {
        uni.showToast({ title: '身份证号格式不正确', icon: 'none' });
        return;
      }
      this.submitting = true;
      submitWorkerCertification({
        phone,
        realName,
        idCard
      })
        .then((res) => {
          if (res && res.code === 200) {
            const app = getApp();
            if (app && app.globalData && app.globalData.workerInfo) {
              app.globalData.workerInfo.accountStatus = 1;
              app.globalData.workerInfo.phone = phone;
            }
            uni.showToast({ title: '实名认证成功', icon: 'success' });
            setTimeout(() => {
              uni.navigateBack();
            }, 600);
          } else {
            uni.showToast({ title: res?.message || '认证失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '认证失败', icon: 'none' });
        })
        .finally(() => {
          this.submitting = false;
        });
    }
  }
};
</script>

<style scoped>
.worker-certification {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.nav-bar {
  height: calc(var(--status-bar-height) + 88rpx);
  padding: var(--status-bar-height) 24rpx 0;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
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

.right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  margin-left: 16rpx;
}

.submit-btn {
  margin-top: 28rpx;
}

.tips {
  margin-top: 16rpx;
}

.tips-text {
  font-size: 24rpx;
  color: #909399;
  line-height: 34rpx;
}
</style>
