<template>
  <div class="admin-profile-page">
    <el-card class="profile-card" shadow="never" v-loading="loading">
      <div class="profile-header">
        <div class="avatar-section" @click="handleAvatarClick">
          <el-avatar :size="96" :src="avatarPreview || form.avatarUrl">
            <span v-if="!form.avatarUrl && !avatarPreview">{{ initial }}</span>
          </el-avatar>
          <div class="avatar-tip">点击头像更换图片</div>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            class="hidden-file-input"
            @change="handleFileChange"
          />
          <el-button
            v-if="avatarFile"
            class="avatar-save-button"
            type="primary"
            size="small"
            :loading="savingAvatar"
            @click.stop="handleSaveAvatar"
          >
            保存头像
          </el-button>
        </div>
        <div class="header-info">
          <div class="header-title">个人中心</div>
          <div class="header-subtitle">查看并管理您的账号、资料和安全设置</div>
          <div class="header-actions">
            <el-button v-if="!editMode" type="primary" @click="handleEdit">
              编辑信息
            </el-button>
            <template v-else>
              <el-button @click="handleCancelEdit">取消</el-button>
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存
              </el-button>
            </template>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="profile-body">
        <el-form :model="form" label-width="100px" class="profile-form">
          <div class="section-title">账号信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="账号ID">
                <el-input v-model="form.id" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="登录邮箱">
                <el-input v-model="form.email" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="form.username" :disabled="!editMode" placeholder="请输入用户名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号">
                <el-input v-model="form.phone" :disabled="!editMode" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="管理员类型">
                <el-tag type="info">{{ adminTypeText }}</el-tag>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账号状态">
                <el-tag :type="statusTagType">{{ accountStatusText }}</el-tag>
              </el-form-item>
            </el-col>
          </el-row>

          <div class="section-title">个人信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="真实姓名">
                <el-input v-model="form.realName" :disabled="!editMode" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="部门">
                <el-input v-model="form.department" :disabled="!editMode" placeholder="请输入部门" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="职位">
                <el-input v-model="form.position" :disabled="!editMode" placeholder="请输入职位" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="section-title security-title">账号安全</div>
        <div class="security-panel">
          <div class="security-item">
            <div>
              <div class="security-name">修改密码</div>
              <div class="security-desc">修改时需要填写当前旧密码。</div>
            </div>
            <el-button type="primary" plain @click="openPasswordDialog">
              去修改
            </el-button>
          </div>
          <div class="security-item">
            <div>
              <div class="security-name">修改邮箱</div>
              <div class="security-desc">新邮箱需要验证码确认，且不能使用已注册邮箱。</div>
            </div>
            <el-button type="primary" plain @click="openEmailDialog">
              去修改
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="420px"
      :close-on-click-modal="false"
      @closed="resetPasswordDialog"
    >
      <el-form :model="passwordForm" label-position="top">
        <el-form-item label="旧密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前旧密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码，至少6位"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handleChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="emailDialogVisible"
      title="修改邮箱"
      width="460px"
      :close-on-click-modal="false"
      @closed="resetEmailDialog"
    >
      <el-form :model="emailForm" label-position="top">
        <el-form-item label="当前邮箱">
          <el-input :model-value="form.email || '未绑定邮箱'" disabled />
        </el-form-item>
        <el-form-item label="新邮箱">
          <el-input v-model="emailForm.newEmail" placeholder="请输入新邮箱" />
        </el-form-item>
        <el-form-item label="确认邮箱">
          <el-input v-model="emailForm.confirmEmail" placeholder="请再次输入新邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="verify-code-row">
            <el-input v-model="emailForm.code" placeholder="请输入验证码" />
            <el-button
              type="primary"
              :disabled="emailCountdown > 0 || emailSendingCode"
              :loading="emailSendingCode"
              @click="handleSendEmailCode"
            >
              <span v-if="emailCountdown === 0">发送验证码</span>
              <span v-else>{{ emailCountdown }} 秒后重发</span>
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="emailSubmitting" @click="handleChangeEmail">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getAdminProfileDetail, updateAdminProfile, uploadAdminAvatar } from '../../api/adminProfile';
import { changeAdminEmail, changeAdminPassword, sendAdminChangeEmailCode } from '../../api/adminSecurity';
import { useAdminStore } from '../../stores/admin';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import { showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';

const adminStore = useAdminStore();

const form = reactive({
  id: '',
  username: '',
  phone: '',
  email: '',
  adminType: null,
  accountStatus: null,
  realName: '',
  department: '',
  position: '',
  avatarUrl: ''
});

const originalForm = reactive({
  id: '',
  username: '',
  phone: '',
  email: '',
  adminType: null,
  accountStatus: null,
  realName: '',
  department: '',
  position: '',
  avatarUrl: ''
});

const editMode = ref(false);
const loading = ref(false);
const saving = ref(false);

const fileInputRef = ref(null);
const avatarFile = ref(null);
const avatarPreview = ref('');
const savingAvatar = ref(false);

const passwordDialogVisible = ref(false);
const passwordSubmitting = ref(false);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const emailDialogVisible = ref(false);
const emailSubmitting = ref(false);
const emailSendingCode = ref(false);
const emailCountdown = ref(0);
let emailCountdownTimer = null;
const emailForm = reactive({
  newEmail: '',
  confirmEmail: '',
  code: ''
});

const initial = computed(() => {
  if (form.username) return form.username.charAt(0).toUpperCase();
  if (form.email) return form.email.charAt(0).toUpperCase();
  return 'A';
});

const adminTypeText = computed(() => {
  if (form.adminType === 1) return '超级管理员';
  if (form.adminType === 2) return '普通管理员';
  if (form.adminType === 3) return '客服';
  return '未知';
});

const accountStatusText = computed(() => {
  if (form.accountStatus === 1) return '正常';
  if (form.accountStatus === 2) return '冻结';
  if (form.accountStatus === 3) return '禁用';
  return '未知';
});

const statusTagType = computed(() => {
  if (form.accountStatus === 1) return 'success';
  if (form.accountStatus === 2) return 'warning';
  if (form.accountStatus === 3) return 'danger';
  return 'info';
});

function assignForm(target, source) {
  target.id = source.id || '';
  target.username = source.username || '';
  target.phone = source.phone || '';
  target.email = source.email || '';
  target.adminType = source.adminType ?? null;
  target.accountStatus = source.accountStatus ?? null;
  target.realName = source.realName || '';
  target.department = source.department || '';
  target.position = source.position || '';
  target.avatarUrl = source.avatarUrl || '';
}

function syncStore() {
  adminStore.setInfo({
    id: form.id,
    username: form.username,
    email: form.email,
    adminType: form.adminType,
    accountStatus: form.accountStatus,
    avatarUrl: form.avatarUrl
  });
}

function normalizeEmail(value) {
  return String(value || '').trim().toLowerCase();
}

function isValidEmail(value) {
  return /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(String(value || '').trim());
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getAdminProfileDetail();
    if (res.code === 200 && res.data) {
      assignForm(form, res.data);
      assignForm(originalForm, res.data);
      syncStore();
    } else {
      ElMessage.error(res.message || '获取个人信息失败');
    }
  } catch (e) {
    ElMessage.error('获取个人信息失败');
  } finally {
    loading.value = false;
  }
}

function handleEdit() {
  editMode.value = true;
}

function handleCancelEdit() {
  assignForm(form, originalForm);
  editMode.value = false;
}

async function handleSave() {
  if (!form.username) {
    ElMessage.warning('用户名不能为空');
    return;
  }
  if (!form.realName) {
    ElMessage.warning('真实姓名不能为空');
    return;
  }
  saving.value = true;
  try {
    const res = await updateAdminProfile({
      username: form.username,
      phone: form.phone,
      realName: form.realName,
      department: form.department,
      position: form.position
    });
    if (res.code === 200) {
      assignForm(originalForm, form);
      syncStore();
      ElMessage.success('保存成功');
      editMode.value = false;
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  } catch (e) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

function handleAvatarClick() {
  if (fileInputRef.value) {
    fileInputRef.value.click();
  }
}

function handleFileChange(event) {
  const files = event.target.files;
  if (!files || !files.length) return;
  const file = files[0];
  if (!file.type || !file.type.startsWith('image/')) {
    showUploadLimitDialog('请上传图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    showUploadLimitDialog('头像大小不能超过 5MB');
    return;
  }
  avatarFile.value = file;
  const reader = new FileReader();
  reader.onload = e => {
    avatarPreview.value = e.target.result;
  };
  reader.readAsDataURL(file);
}

async function handleSaveAvatar() {
  if (!avatarFile.value) return;
  savingAvatar.value = true;
  try {
    const res = await uploadAdminAvatar(avatarFile.value);
    if (res.code === 200 && res.data) {
      form.avatarUrl = res.data;
      originalForm.avatarUrl = res.data;
      syncStore();
      avatarFile.value = null;
      avatarPreview.value = '';
      if (fileInputRef.value) {
        fileInputRef.value.value = '';
      }
      ElMessage.success('头像已更新');
    } else {
      showUploadErrorDialog(res.message || '头像上传失败', '头像上传失败', '头像上传失败');
    }
  } catch (e) {
    showUploadErrorDialog(e, '头像上传失败', '头像上传失败');
  } finally {
    savingAvatar.value = false;
  }
}

function openPasswordDialog() {
  passwordDialogVisible.value = true;
}

function resetPasswordDialog() {
  passwordSubmitting.value = false;
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
}

async function handleChangePassword() {
  if (passwordSubmitting.value) return;
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整');
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位');
    return;
  }
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    ElMessage.warning('新密码不能与旧密码相同');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次新密码不一致');
    return;
  }
  passwordSubmitting.value = true;
  try {
    const res = await changeAdminPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    });
    if (res.code === 200) {
      ElMessage.success('密码修改成功');
      passwordDialogVisible.value = false;
    } else {
      ElMessage.error(res.message || '密码修改失败');
    }
  } catch (e) {
    ElMessage.error('密码修改失败');
  } finally {
    passwordSubmitting.value = false;
  }
}

function openEmailDialog() {
  emailDialogVisible.value = true;
}

function clearEmailCountdown() {
  if (emailCountdownTimer) {
    clearInterval(emailCountdownTimer);
    emailCountdownTimer = null;
  }
  emailCountdown.value = 0;
}

function resetEmailDialog() {
  emailSubmitting.value = false;
  emailSendingCode.value = false;
  emailForm.newEmail = '';
  emailForm.confirmEmail = '';
  emailForm.code = '';
  clearEmailCountdown();
}

function startEmailCountdown() {
  clearEmailCountdown();
  emailCountdown.value = 60;
  emailCountdownTimer = setInterval(() => {
    if (emailCountdown.value <= 1) {
      clearEmailCountdown();
    } else {
      emailCountdown.value -= 1;
    }
  }, 1000);
}

function validateEmailForm(checkCode = false) {
  const newEmail = normalizeEmail(emailForm.newEmail);
  const confirmEmail = normalizeEmail(emailForm.confirmEmail);
  if (!newEmail || !confirmEmail) {
    ElMessage.warning('请填写完整');
    return null;
  }
  if (!isValidEmail(newEmail)) {
    ElMessage.warning('邮箱格式不正确');
    return null;
  }
  if (newEmail !== confirmEmail) {
    ElMessage.warning('两次邮箱不一致');
    return null;
  }
  if (newEmail === normalizeEmail(form.email)) {
    ElMessage.warning('新邮箱不能与当前邮箱相同');
    return null;
  }
  if (checkCode && !emailForm.code) {
    ElMessage.warning('请输入验证码');
    return null;
  }
  return newEmail;
}

async function handleSendEmailCode() {
  if (emailSendingCode.value || emailCountdown.value > 0) return;
  const newEmail = validateEmailForm(false);
  if (!newEmail) return;
  emailSendingCode.value = true;
  try {
    const res = await sendAdminChangeEmailCode(newEmail);
    if (res.code === 200) {
      ElMessage.success('验证码已发送，请查收新邮箱');
      startEmailCountdown();
    } else {
      ElMessage.error(res.message || '验证码发送失败');
    }
  } catch (e) {
    ElMessage.error('验证码发送失败');
  } finally {
    emailSendingCode.value = false;
  }
}

async function handleChangeEmail() {
  if (emailSubmitting.value) return;
  const newEmail = validateEmailForm(true);
  if (!newEmail) return;
  emailSubmitting.value = true;
  try {
    const res = await changeAdminEmail({
      newEmail,
      code: emailForm.code.trim()
    });
    if (res.code === 200) {
      form.email = newEmail;
      originalForm.email = newEmail;
      syncStore();
      ElMessage.success('邮箱修改成功');
      emailDialogVisible.value = false;
    } else {
      ElMessage.error(res.message || '邮箱修改失败');
    }
  } catch (e) {
    ElMessage.error('邮箱修改失败');
  } finally {
    emailSubmitting.value = false;
  }
}

onMounted(() => {
  loadData();
});

onBeforeUnmount(() => {
  clearEmailCountdown();
});

useAdminPageRefresh(async () => {
  await loadData();
});
</script>

<style scoped>
.admin-profile-page {
  padding: 16px;
  box-sizing: border-box;
}

.profile-card {
  max-width: 900px;
  margin: 0 auto;
}

.profile-header {
  display: flex;
  align-items: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 32px;
  cursor: pointer;
}

.avatar-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.avatar-save-button {
  margin-top: 12px;
}

.hidden-file-input {
  display: none;
}

.header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.header-actions {
  margin-top: 12px;
}

.profile-body {
  margin-top: 8px;
}

.profile-form {
  max-width: 800px;
}

.section-title {
  margin: 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.security-title {
  margin-top: 24px;
}

.security-panel {
  display: grid;
  gap: 12px;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}

.security-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.security-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.verify-code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  width: 100%;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar-section {
    margin-right: 0;
    margin-bottom: 20px;
  }

  .security-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .verify-code-row {
    grid-template-columns: 1fr;
  }
}
</style>
