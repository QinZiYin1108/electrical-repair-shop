<template>
  <view class="page worker-profile">
    <view class="header">
      <view class="header-left" @click="goBack">
        <u-icon name="arrow-left" size="20" />
      </view>
      <view class="header-center">
        <text class="header-title">个人资料</text>
      </view>
      <view class="header-right">
        <text
          v-if="!editing"
          class="header-action"
          @click="startEdit"
        >编辑</text>
        <text
          v-else
          class="header-action"
          @click="saveEdit"
        >保存</text>
      </view>
    </view>

    <view class="card avatar-card">
      <image
        class="avatar"
        :src="avatarDisplayUrl"
        mode="aspectFill"
        @error="onAvatarError"
        @click="changeAvatar"
      />
      <text class="avatar-tip">点击更换头像</text>
      <text
        v-if="uploadingAvatar"
        class="avatar-subtip"
      >上传中...</text>
    </view>

    <view class="card">
      <text class="section-title">账号信息</text>

      <view class="row">
        <text class="label">账号ID</text>
        <text class="value">{{ profile.id || '-' }}</text>
      </view>

      <view class="row">
        <text class="label">昵称</text>
        <view class="right">
          <u-input
            v-if="editing"
            v-model="form.username"
            placeholder="请输入昵称"
            border="none"
            input-align="right"
          />
          <text v-else class="value">{{ profile.username || '-' }}</text>
        </view>
      </view>

      <view class="row">
        <text class="label">手机号</text>
        <view class="right">
          <text class="value">{{ profile.phone || '-' }}</text>
        </view>
      </view>

      <view class="row">
        <text class="label">邮箱</text>
        <view class="right">
          <text class="value">{{ profile.email || '未绑定邮箱' }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <text class="section-title">账号安全</text>

      <view class="row clickable" @click="goChangeEmail">
        <text class="label">修改邮箱</text>
        <text class="value link">去修改</text>
      </view>

      <view class="row clickable" @click="goChangePassword">
        <text class="label">修改密码</text>
        <text class="value link">去修改</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">个人信息</text>

      <view class="row">
        <text class="label">性别</text>
        <view class="right">
          <picker
            v-if="editing"
            mode="selector"
            :range="genderOptions"
            :value="genderPickerIndex"
            @change="onGenderPick"
          >
            <text class="value picker-value">{{ genderText(form.gender) }}</text>
          </picker>
          <text v-else class="value">{{ genderText(profile.gender) }}</text>
        </view>
      </view>

      <view class="row">
        <text class="label">生日</text>
        <view class="right">
          <picker
            v-if="editing"
            mode="date"
            :value="birthdayPickerValue"
            @change="onBirthdayPick"
          >
            <text class="value picker-value">
              {{ birthdayText(form.birthday) }}
            </text>
          </picker>
          <text v-else class="value">{{ birthdayText(profile.birthday) }}</text>
        </view>
      </view>

      <view class="row">
        <text class="label">工作年限</text>
        <view class="right">
          <u-input
            v-if="editing"
            v-model="form.workYears"
            type="number"
            placeholder="例如 3"
            border="none"
            input-align="right"
          />
          <text v-else class="value">{{ typeof profile.workYears === 'number' ? profile.workYears : '-' }}</text>
        </view>
      </view>

      <view class="row">
        <text class="label">学历</text>
        <view class="right">
          <u-input
            v-if="editing"
            v-model="form.education"
            placeholder="例如 大专/本科"
            border="none"
            input-align="right"
          />
          <text v-else class="value">{{ profile.education || '-' }}</text>
        </view>
      </view>

      <view class="row column">
        <text class="label">个人介绍</text>
        <view class="right column-right">
          <u-textarea
            v-if="editing"
            v-model="form.introduction"
            placeholder="简单介绍一下自己"
            height="120"
          />
          <text v-else class="value multi">{{ profile.introduction || '-' }}</text>
        </view>
      </view>
    </view>

    <view
      v-if="editing"
      class="footer"
    >
      <u-button
        text="取消"
        shape="circle"
        @click="cancelEdit"
      />
      <u-button
        text="保存"
        type="primary"
        shape="circle"
        :loading="saving"
        @click="saveEdit"
      />
    </view>
  </view>
</template>

<script>
import {
  getWorkerProfile,
  updateWorkerProfile,
  uploadWorkerAvatar
} from '@/api/workerProfile';
import { showUploadErrorModal } from '@/utils/uploadFeedback';

export default {
  name: 'WorkerProfilePage',
  data() {
    return {
      loading: false,
      saving: false,
      uploadingAvatar: false,
      avatarLoadFailed: false,
      editing: false,
      profile: {
        id: '',
        username: '',
        phone: '',
        email: '',
        avatarUrl: '',
        gender: null,
        birthday: null,
        workYears: null,
        education: '',
        introduction: ''
      },
      form: {
        username: '',
        email: '',
        gender: null,
        birthday: null,
        workYears: '',
        education: '',
        introduction: ''
      },
      genderOptions: ['未知', '男', '女']
    };
  },
  computed: {
    avatarDisplayUrl() {
      if (this.avatarLoadFailed) return '/static/logo.png';
      const url = this.profile && this.profile.avatarUrl ? String(this.profile.avatarUrl).trim() : '';
      return url || '/static/logo.png';
    },
    genderPickerIndex() {
      const g = this.form.gender;
      if (g === 1) return 1;
      if (g === 2) return 2;
      return 0;
    },
    birthdayPickerValue() {
      const ts = this.form.birthday;
      if (typeof ts === 'number' && ts > 0) {
        return this.formatDate(ts);
      }
      return '';
    }
  },
  onShow() {
    this.loadProfile();
  },
  methods: {
    goBack() {
      uni.navigateBack();
    },
    goChangePassword() {
      uni.navigateTo({
        url: '/pages/security/change-password'
      });
    },
    goChangeEmail() {
      uni.navigateTo({
        url: '/pages/security/change-email'
      });
    },
    genderText(g) {
      if (g === 1) return '男';
      if (g === 2) return '女';
      return '未知';
    },
    birthdayText(ts) {
      if (typeof ts !== 'number' || ts <= 0) return '请选择';
      return this.formatDate(ts);
    },
    formatDate(ts) {
      const d = new Date(ts);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    },
    dateStringToTimestamp(val) {
      if (!val) return null;
      const parts = String(val).split('-').map((x) => Number(x));
      if (parts.length !== 3 || parts.some((n) => !Number.isFinite(n))) return null;
      const [y, m, d] = parts;
      if (!y || !m || !d) return null;
      return new Date(y, m - 1, d).getTime();
    },
    onGenderPick(e) {
      const idx = Number(e.detail.value);
      if (idx === 1) this.form.gender = 1;
      else if (idx === 2) this.form.gender = 2;
      else this.form.gender = null;
    },
    onBirthdayPick(e) {
      const val = e && e.detail ? e.detail.value : '';
      const ts = this.dateStringToTimestamp(val);
      this.form.birthday = ts;
    },
    onAvatarError() {
      this.avatarLoadFailed = true;
    },
    loadProfile() {
      this.loading = true;
      getWorkerProfile()
        .then((res) => {
          if (res && res.code === 200 && res.data) {
            this.profile = {
              ...this.profile,
              ...res.data
            };
            this.avatarLoadFailed = false;
            if (this.editing) {
              this.fillForm();
            }
          }
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false;
        });
    },
    fillForm() {
      const p = this.profile || {};
      this.form.username = p.username || '';
      this.form.email = p.email || '';
      this.form.gender = p.gender ?? null;
      this.form.birthday = typeof p.birthday === 'number' ? p.birthday : null;
      this.form.workYears =
        typeof p.workYears === 'number' ? String(p.workYears) : '';
      this.form.education = p.education || '';
      this.form.introduction = p.introduction || '';
    },
    startEdit() {
      this.editing = true;
      this.fillForm();
    },
    cancelEdit() {
      this.editing = false;
      this.fillForm();
    },
    saveEdit() {
      if (this.saving) return;
      this.saving = true;
      const payload = {
        username: this.form.username,
        gender: this.form.gender,
        birthday:
          typeof this.form.birthday === 'number' && this.form.birthday > 0
            ? this.form.birthday
            : null,
        workYears:
          this.form.workYears === '' || this.form.workYears == null
            ? null
            : Number(this.form.workYears),
        education: this.form.education,
        introduction: this.form.introduction
      };
      updateWorkerProfile(payload)
        .then((res) => {
          if (res && res.code === 200) {
            uni.showToast({ title: '保存成功', icon: 'success' });
            this.editing = false;
            this.loadProfile();
          } else {
            uni.showToast({ title: res?.message || '保存失败', icon: 'none' });
          }
        })
        .catch(() => {
          uni.showToast({ title: '保存失败', icon: 'none' });
        })
        .finally(() => {
          this.saving = false;
        });
    },
    changeAvatar() {
      if (this.uploadingAvatar) return;
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0];
          if (!path) return;
          this.uploadingAvatar = true;
          uploadWorkerAvatar(path)
            .then((body) => {
              if (body && body.code === 200) {
                this.profile.avatarUrl = body.data || '';
                this.avatarLoadFailed = false;
                uni.showToast({ title: '头像已更新', icon: 'success' });
            } else {
                showUploadErrorModal((body && body.message) || '头像上传失败', {
                  title: '头像上传失败',
                  fallback: '头像上传失败'
                });
              }
            })
            .catch((error) => {
              showUploadErrorModal(error, {
                title: '头像上传失败',
                fallback: '头像上传失败'
              });
            })
            .finally(() => {
              this.uploadingAvatar = false;
            });
        }
      });
    }
  }
};
</script>

<style scoped>
.worker-profile {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 140rpx;
  box-sizing: border-box;
}

.header {
  height: calc(88rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 24rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  background-color: #ffffff;
}

.header-left,
.header-right {
  width: 120rpx;
  display: flex;
  align-items: center;
}

.header-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-right {
  justify-content: flex-end;
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}

.header-action {
  font-size: 28rpx;
  color: #3c9cff;
}

.card {
  margin: 16rpx 24rpx 0;
  padding: 24rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
  box-sizing: border-box;
}

.avatar-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 24rpx;
  background-color: #f2f3f5;
}

.avatar-tip {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #606266;
}

.avatar-subtip {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #909399;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16rpx;
  display: block;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.row.clickable:active {
  opacity: 0.7;
}

.row:last-child {
  border-bottom: none;
}

.row.column {
  align-items: flex-start;
  flex-direction: column;
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

.right.column-right {
  width: 100%;
  margin-left: 0;
  margin-top: 12rpx;
}

.value {
  font-size: 26rpx;
  color: #303133;
}

.value.link {
  color: #3c9cff;
}

.value.multi {
  width: 100%;
  white-space: pre-wrap;
  line-height: 1.6;
}

.picker-value {
  color: #3c9cff;
}

.email-right {
  flex: 1;
}

.email-suffix {
  display: flex;
  align-items: center;
  padding-left: 12rpx;
  padding-right: 4rpx;
}

.email-suffix-text {
  font-size: 24rpx;
  color: #3c9cff;
  padding: 8rpx 12rpx;
  border-radius: 999rpx;
  background-color: rgba(60, 156, 255, 0.08);
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12rpx 24rpx 24rpx;
  background-color: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
  display: flex;
  gap: 16rpx;
  box-sizing: border-box;
}

.footer :deep(.u-button) {
  flex: 1;
}

</style>
