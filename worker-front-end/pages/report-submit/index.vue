<template>
  <view class="page">
    <view class="header">
      <view class="header-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="header-center"><text class="header-title">提交举报</text></view>
      <view class="header-right"></view>
    </view>

    <view class="card">
      <u-form label-position="top">
        <u-form-item label="举报对象类型">
          <u-radio-group v-model="targetType" placement="row">
            <u-radio v-for="t in targetTypes" :key="t.value" :name="t.value" :label="t.label" />
          </u-radio-group>
        </u-form-item>
        <u-form-item label="对象ID">
          <u-input
            v-model="targetId"
            placeholder="请输入要举报的账号/门店/订单/商品ID"
            border="surround"
          />
        </u-form-item>
        <u-form-item label="字段/节点（选填）">
          <u-input v-model="targetField" placeholder="如：昵称、商品描述" border="surround" />
        </u-form-item>
        <u-form-item label="举报原因">
          <u-radio-group v-model="reasonCategory" placement="row">
            <u-radio
              v-for="r in reasonCategories"
              :key="r.value"
              :name="r.value"
              :label="r.label"
            />
          </u-radio-group>
        </u-form-item>
        <u-form-item label="补充说明（选填）">
          <u-textarea
            v-model="description"
            placeholder="请详细描述举报原因"
            maxlength="500"
            height="120"
          />
        </u-form-item>
      </u-form>
    </view>

    <view class="submit-area">
      <u-button type="primary" shape="circle" :loading="submitting" @click="onSubmit"
        >提交举报</u-button
      >
    </view>
  </view>
</template>

<script>
import { submitReport } from '@/api/workerReport';

export default {
  name: 'WorkerReportSubmit',
  data() {
    return {
      targetType: 1,
      targetTypes: [
        { value: 1, label: '账号' },
        { value: 2, label: '门店' },
        { value: 3, label: '订单' },
        { value: 4, label: '商品' }
      ],
      targetId: '',
      targetField: '',
      reasonCategory: '违规内容',
      reasonCategories: [
        { value: '违规内容', label: '违规内容' },
        { value: '虚假信息', label: '虚假信息' },
        { value: '骚扰辱骂', label: '骚扰辱骂' },
        { value: '欺诈', label: '欺诈' },
        { value: '其他', label: '其他' }
      ],
      description: '',
      submitting: false
    };
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    onSubmit() {
      if (!this.targetId.trim()) {
        uni.showToast({ title: '请输入举报对象ID', icon: 'none' });
        return;
      }
      this.submitting = true;
      submitReport({
        targetType: this.targetType,
        targetId: this.targetId.trim(),
        targetField: this.targetField.trim() || null,
        reasonCategory: this.reasonCategory,
        description: this.description.trim() || null
      })
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({ title: '举报提交成功', icon: 'success' });
            setTimeout(() => {
              uni.navigateBack();
            }, 1200);
          } else {
            uni.showToast({ title: res?.message || '提交失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '提交失败', icon: 'none' });
        })
        .finally(() => {
          this.submitting = false;
        });
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 40rpx;
}
.header {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-sizing: border-box;
}
.header-left,
.header-right {
  width: 120rpx;
  display: flex;
  align-items: center;
}
.header-center {
  flex: 1;
  text-align: center;
}
.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}
.card {
  margin: 16rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 20rpx;
}
.submit-area {
  margin: 32rpx 24rpx;
}
</style>
