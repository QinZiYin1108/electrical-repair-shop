<template>
  <div class="login-page" :style="loginPageStyle">
    <div class="login-shell">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-badge">HOME APPLIANCE SERVICE PLATFORM</span>
          <h1>安修到家</h1>
          <p class="hero-subtitle">家电维修线上预约平台</p>
          <span class="hero-divider"></span>
          <div class="hero-highlights">
            <span v-for="item in brandHighlights" :key="item">{{ item }}</span>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <div class="login-card">
          <div class="login-intro">
            <span class="login-intro-tag">运营后台</span>
            <h2>管理员登录</h2>
            <p>欢迎登录安修到家后台管理系统</p>
          </div>

          <div class="login-tabs">
            <button
              type="button"
              class="login-tab"
              :class="{ active: loginMode === 'password' }"
              @click="loginMode = 'password'"
            >
              密码登录
            </button>
            <button
              type="button"
              class="login-tab"
              :class="{ active: loginMode === 'code' }"
              @click="loginMode = 'code'"
            >
              验证码登录
            </button>
            <div class="login-tab-slider" :class="loginMode"></div>
          </div>

          <transition name="fade-slide" mode="out-in">
            <div :key="loginMode">
              <el-form
                v-if="loginMode === 'password'"
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                class="login-form"
              >
                <el-form-item prop="email">
                  <el-input
                    v-model="passwordForm.email"
                    placeholder="请输入登录邮箱"
                    autocomplete="username"
                  />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="passwordForm.password"
                    type="password"
                    placeholder="请输入密码"
                    autocomplete="current-password"
                    show-password
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    class="login-submit-button"
                    :loading="submitting"
                    @click="handlePasswordLogin"
                  >
                    登录
                  </el-button>
                </el-form-item>
              </el-form>

              <el-form
                v-else
                ref="codeFormRef"
                :model="codeForm"
                :rules="codeRules"
                class="login-form"
              >
                <el-form-item prop="email">
                  <el-input
                    v-model="codeForm.email"
                    placeholder="请输入登录邮箱"
                    autocomplete="username"
                  />
                </el-form-item>
                <el-form-item prop="code" class="code-form-item">
                  <div class="verify-code-group">
                    <el-input
                      v-model="codeForm.code"
                      placeholder="请输入验证码"
                      class="verify-code-input"
                    />
                    <el-button
                      class="verify-code-button"
                      type="primary"
                      :disabled="countdown > 0 || sendingCode"
                      @click="handleSendCode"
                    >
                      <span v-if="countdown === 0">发送验证码</span>
                      <span v-else>{{ countdown }} 秒后重发</span>
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    class="login-submit-button"
                    :loading="submitting"
                    @click="handleCodeLogin"
                  >
                    登录
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </transition>

          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住邮箱</el-checkbox>
            <button type="button" class="forgot-password-link" @click="openForgotPassword">
              忘记密码？
            </button>
          </div>

          <div class="agreement-row">
            <el-checkbox v-model="agreed">
              我已阅读并同意
              <span class="agreement-link" @click.stop="openProtocol('user')">《用户协议》</span>
              和
              <span class="agreement-link" @click.stop="openProtocol('privacy')">《隐私协议》</span>
            </el-checkbox>
          </div>
        </div>
      </section>
    </div>

    <el-dialog v-model="forgotVisible" width="420px" :close-on-click-modal="false">
      <template #title>
        <span v-if="forgotStep === 1">验证邮箱</span>
        <span v-else>重置密码</span>
      </template>
      <div v-if="forgotStep === 1">
        <el-form
          ref="forgotVerifyFormRef"
          :model="forgotVerifyForm"
          :rules="forgotVerifyRules"
          label-position="top"
        >
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="forgotVerifyForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="verify-code-group">
              <el-input
                v-model="forgotVerifyForm.code"
                placeholder="请输入验证码"
                class="verify-code-input"
              />
              <el-button
                class="verify-code-button"
                type="primary"
                :disabled="forgotCountdown > 0 || forgotSendingCode"
                @click="handleForgotSendCode"
              >
                <span v-if="forgotCountdown === 0">发送验证码</span>
                <span v-else>{{ forgotCountdown }} 秒后重发</span>
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <div v-else>
        <el-form
          ref="forgotResetFormRef"
          :model="forgotResetForm"
          :rules="forgotResetRules"
          label-position="top"
        >
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="forgotResetForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="forgotResetForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleForgotCancel">取消</el-button>
          <el-button
            v-if="forgotStep === 1"
            type="primary"
            :loading="forgotSubmitting"
            @click="handleForgotNext"
          >
            下一步
          </el-button>
          <el-button
            v-else
            type="primary"
            :loading="forgotSubmitting"
            @click="handleForgotSubmit"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import {
  adminLoginByCode,
  adminLoginByPassword,
  adminResetPassword,
  adminSendLoginCode,
  adminSendResetCode
} from '../../api/adminAuth';
import { setToken } from '../../utils/auth';

const router = useRouter();

const brandHighlights = ['专业', '高效', '便捷', '安心'];
const rememberedEmailKey = 'admin-remembered-email';
const loginPageStyle = {
  backgroundImage: [
    'linear-gradient(90deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.03) 36%, rgba(239, 246, 255, 0.12) 100%)',
    'url("/admin-login-background.jpg")'
  ].join(', ')
};

const loginMode = ref('password');
const submitting = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const agreed = ref(false);
const rememberMe = ref(false);
let countdownTimer = null;

const passwordFormRef = ref();
const codeFormRef = ref();

const passwordForm = reactive({
  email: '',
  password: ''
});

const codeForm = reactive({
  email: '',
  code: ''
});

const passwordRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const codeRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
};

const forgotVisible = ref(false);
const forgotStep = ref(1);
const forgotVerifyFormRef = ref();
const forgotResetFormRef = ref();
const forgotSubmitting = ref(false);
const forgotSendingCode = ref(false);
const forgotCountdown = ref(0);
let forgotCountdownTimer = null;

const forgotVerifyForm = reactive({
  email: '',
  code: ''
});

const forgotResetForm = reactive({
  newPassword: '',
  confirmPassword: ''
});

const forgotVerifyRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
};

const forgotResetRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }]
};

function applyRememberedEmail(email) {
  passwordForm.email = email;
  codeForm.email = email;
}

function syncRememberedEmail(email) {
  if (rememberMe.value && email) {
    localStorage.setItem(rememberedEmailKey, email);
    return;
  }
  localStorage.removeItem(rememberedEmailKey);
}

function handleLoginSuccess(token, email) {
  if (token) {
    setToken(token);
  }
  syncRememberedEmail(email);
  ElMessage.success('登录成功');
  router.push('/admin/dashboard');
}

function startCountdown() {
  countdown.value = 60;
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
  countdownTimer = setInterval(() => {
    if (countdown.value <= 1) {
      clearInterval(countdownTimer);
      countdownTimer = null;
      countdown.value = 0;
    } else {
      countdown.value -= 1;
    }
  }, 1000);
}

function startForgotCountdown() {
  forgotCountdown.value = 60;
  if (forgotCountdownTimer) {
    clearInterval(forgotCountdownTimer);
  }
  forgotCountdownTimer = setInterval(() => {
    if (forgotCountdown.value <= 1) {
      clearInterval(forgotCountdownTimer);
      forgotCountdownTimer = null;
      forgotCountdown.value = 0;
    } else {
      forgotCountdown.value -= 1;
    }
  }, 1000);
}

function ensureAgreed() {
  if (agreed.value) {
    return true;
  }
  ElMessage.warning('请先阅读并勾选同意《用户协议》和《隐私协议》');
  return false;
}

function openProtocol(type) {
  router.push(`/protocol/${type}`);
}

function handlePasswordLogin() {
  if (!ensureAgreed() || !passwordFormRef.value) return;
  passwordFormRef.value.validate(async valid => {
    if (!valid) return;
    submitting.value = true;
    try {
      const res = await adminLoginByPassword({
        email: passwordForm.email,
        password: passwordForm.password
      });
      if (res.code !== 200) {
        ElMessage.error(res.message || '登录失败');
        return;
      }
      handleLoginSuccess(res.data?.token, passwordForm.email);
    } catch (e) {
      ElMessage.error('登录失败');
    } finally {
      submitting.value = false;
    }
  });
}

function handleSendCode() {
  if (!codeForm.email) {
    ElMessage.warning('请先输入邮箱');
    return;
  }
  if (!ensureAgreed()) {
    return;
  }
  sendingCode.value = true;
  adminSendLoginCode({ email: codeForm.email })
    .then(res => {
      if (res.code !== 200) {
        ElMessage.error(res.message || '发送验证码失败');
        return;
      }
      ElMessage.success('验证码已发送，请查收邮箱');
      startCountdown();
    })
    .catch(() => {
      ElMessage.error('发送验证码失败');
    })
    .finally(() => {
      sendingCode.value = false;
    });
}

function handleCodeLogin() {
  if (!ensureAgreed() || !codeFormRef.value) return;
  codeFormRef.value.validate(async valid => {
    if (!valid) return;
    submitting.value = true;
    try {
      const res = await adminLoginByCode({
        email: codeForm.email,
        code: codeForm.code
      });
      if (res.code !== 200) {
        ElMessage.error(res.message || '登录失败');
        return;
      }
      handleLoginSuccess(res.data?.token, codeForm.email);
    } catch (e) {
      ElMessage.error('登录失败');
    } finally {
      submitting.value = false;
    }
  });
}

function openForgotPassword() {
  forgotVisible.value = true;
  forgotStep.value = 1;
  forgotVerifyForm.email = passwordForm.email || codeForm.email || '';
  forgotVerifyForm.code = '';
  forgotResetForm.newPassword = '';
  forgotResetForm.confirmPassword = '';
}

function handleForgotCancel() {
  forgotVisible.value = false;
}

function handleForgotSendCode() {
  if (!forgotVerifyForm.email) {
    ElMessage.warning('请先输入邮箱');
    return;
  }
  forgotSendingCode.value = true;
  adminSendResetCode({ email: forgotVerifyForm.email })
    .then(res => {
      if (res.code !== 200) {
        ElMessage.error(res.message || '发送验证码失败');
        return;
      }
      ElMessage.success('验证码已发送，请查收邮箱');
      startForgotCountdown();
    })
    .catch(() => {
      ElMessage.error('发送验证码失败');
    })
    .finally(() => {
      forgotSendingCode.value = false;
    });
}

function handleForgotNext() {
  if (!forgotVerifyFormRef.value) return;
  forgotVerifyFormRef.value.validate(valid => {
    if (!valid) return;
    forgotStep.value = 2;
  });
}

function handleForgotSubmit() {
  if (!forgotResetFormRef.value) return;
  forgotResetFormRef.value.validate(async valid => {
    if (!valid) return;
    if (forgotResetForm.newPassword !== forgotResetForm.confirmPassword) {
      ElMessage.error('两次输入的密码不一致');
      return;
    }
    forgotSubmitting.value = true;
    try {
      const res = await adminResetPassword({
        email: forgotVerifyForm.email,
        code: forgotVerifyForm.code,
        newPassword: forgotResetForm.newPassword,
        confirmPassword: forgotResetForm.confirmPassword
      });
      if (res.code !== 200) {
        ElMessage.error(res.message || '重置密码失败');
        return;
      }
      ElMessage.success('重置密码成功，请使用新密码登录');
      passwordForm.email = forgotVerifyForm.email;
      passwordForm.password = '';
      codeForm.email = forgotVerifyForm.email;
      loginMode.value = 'password';
      forgotVisible.value = false;
    } catch (e) {
      ElMessage.error('重置密码失败');
    } finally {
      forgotSubmitting.value = false;
    }
  });
}

onMounted(() => {
  const rememberedEmail = localStorage.getItem(rememberedEmailKey);
  if (!rememberedEmail) {
    return;
  }
  rememberMe.value = true;
  applyRememberedEmail(rememberedEmail);
});

onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
  if (forgotCountdownTimer) {
    clearInterval(forgotCountdownTimer);
  }
});
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background-color: #edf4ff;
  background-position: center center;
  background-repeat: no-repeat;
  background-size: cover;
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.02) 36%, rgba(255, 255, 255, 0.08) 100%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, rgba(255, 255, 255, 0.08) 100%);
  pointer-events: none;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(360px, 470px);
  gap: clamp(56px, 7vw, 136px);
  align-items: center;
  width: min(1440px, calc(100% - 84px));
  min-height: 100vh;
  margin: 0 auto;
  padding: 0;
  box-sizing: border-box;
}

.hero-panel {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  min-height: 100vh;
  padding-top: clamp(78px, 9vh, 118px);
  box-sizing: border-box;
}

.hero-copy {
  max-width: 500px;
  transform: translateY(-24px);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  height: 42px;
  padding: 0 22px;
  border: 1px solid rgba(164, 193, 236, 0.6);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.48);
  box-shadow: 0 16px 40px rgba(110, 145, 202, 0.1);
  color: #6c8fc4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.28em;
}

.hero-copy h1 {
  margin: 30px 0 12px;
  color: #2f73ea;
  font-family: 'Trebuchet MS', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: clamp(52px, 5.9vw, 74px);
  line-height: 1.04;
  letter-spacing: 0.03em;
  text-shadow: 0 16px 36px rgba(75, 124, 205, 0.08);
}

.hero-subtitle {
  margin: 0;
  color: #30445f;
  font-size: clamp(22px, 2.5vw, 30px);
  font-weight: 600;
  line-height: 1.28;
}

.hero-divider {
  display: block;
  width: 82px;
  height: 4px;
  margin: 24px 0 20px;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f73ea 0%, rgba(47, 115, 234, 0.18) 100%);
}

.hero-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 20px;
  margin-top: 24px;
  color: rgba(113, 134, 167, 0.82);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.1em;
}

.hero-highlights span::after {
  content: '·';
  margin-left: 20px;
  color: rgba(137, 160, 194, 0.56);
}

.hero-highlights span:last-child::after {
  display: none;
}

.login-panel {
  display: flex;
  justify-content: flex-end;
}

.login-card {
  width: min(100%, 470px);
  padding: 50px 50px 42px;
  border: 1px solid rgba(224, 233, 244, 0.95);
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 30px 72px rgba(117, 145, 193, 0.14),
    0 1px 0 rgba(255, 255, 255, 0.84) inset;
  animation: card-enter 0.7s cubic-bezier(0.19, 1, 0.22, 1);
}

.login-intro {
  margin-bottom: 30px;
  text-align: center;
}

.login-intro-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(233, 242, 255, 0.96);
  color: #6c86ad;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2em;
}

.login-intro h2 {
  margin: 26px 0 12px;
  color: #233857;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.login-intro p {
  margin: 0;
  color: #7f91ab;
  font-size: 14px;
  line-height: 1.7;
}

.login-tabs {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 5px;
  margin-bottom: 28px;
  border-radius: 999px;
  background: rgba(239, 244, 255, 0.96);
}

.login-tab {
  position: relative;
  z-index: 1;
  height: 50px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #6e7f98;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.25s ease;
}

.login-tab.active {
  color: #ffffff;
}

.login-tab-slider {
  position: absolute;
  top: 5px;
  bottom: 5px;
  left: 5px;
  width: calc(50% - 5px);
  border-radius: 999px;
  background: linear-gradient(90deg, #2f74ff 0%, #76acff 100%);
  box-shadow: 0 12px 24px rgba(75, 132, 230, 0.24);
  transition: transform 0.28s ease;
}

.login-tab-slider.password {
  transform: translateX(0);
}

.login-tab-slider.code {
  transform: translateX(100%);
}

.login-form {
  margin-top: 4px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 58px;
  padding: 0 18px;
  border-radius: 18px;
  background: rgba(251, 253, 255, 0.98);
  box-shadow: inset 0 0 0 1px rgba(207, 220, 238, 0.95);
  transition: box-shadow 0.25s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px rgba(170, 195, 229, 0.95);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px rgba(78, 133, 225, 0.96),
    0 0 0 5px rgba(82, 138, 232, 0.1);
}

.login-form :deep(.el-input__inner) {
  color: #253d5f;
  font-size: 14px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #a1afc3;
}

.login-submit-button {
  width: 100%;
  height: 58px;
  margin-top: 6px;
  border: 0;
  border-radius: 18px;
  background: linear-gradient(90deg, #2f74ff 0%, #7cb1ff 100%);
  box-shadow: 0 18px 32px rgba(80, 129, 222, 0.2);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.login-submit-button:hover {
  transform: translateY(-1px);
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  color: #6a7d98;
  font-size: 13px;
}

.login-options :deep(.el-checkbox__label) {
  color: #6a7d98;
  font-size: 13px;
}

.login-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  border-color: #4b86e8;
  background-color: #4b86e8;
}

.forgot-password-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #6a99df;
  font-size: 13px;
  cursor: pointer;
}

.agreement-row {
  margin-top: 22px;
  color: #7a8ca6;
  font-size: 13px;
  line-height: 1.8;
}

.agreement-row :deep(.el-checkbox) {
  align-items: flex-start;
  white-space: normal;
}

.agreement-row :deep(.el-checkbox__label) {
  color: #7a8ca6;
  line-height: 1.8;
  white-space: normal;
}

.agreement-link {
  color: #4d84e2;
  cursor: pointer;
}

.verify-code-group {
  display: flex;
  width: 100%;
  gap: 12px;
}

.verify-code-input {
  flex: 1;
}

.verify-code-button {
  width: 132px;
  height: 58px;
  border: 0;
  border-radius: 18px;
  background: linear-gradient(90deg, #4d8cff 0%, #7bb0ff 100%);
  box-shadow: 0 14px 26px rgba(85, 135, 229, 0.18);
  font-weight: 600;
}

.verify-code-button.is-disabled,
.verify-code-button:disabled {
  background: linear-gradient(90deg, #b5c8e8 0%, #cfdbef 100%);
  box-shadow: none;
}

.code-form-item :deep(.el-form-item__content) {
  width: 100%;
}

.dialog-footer {
  display: inline-flex;
  gap: 12px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 1180px) {
  .login-shell {
    grid-template-columns: minmax(340px, 1fr) minmax(340px, 430px);
    gap: 44px;
    width: min(1160px, calc(100% - 52px));
  }

  .hero-panel {
    padding-top: clamp(72px, 8vh, 102px);
  }

  .hero-copy h1 {
    font-size: clamp(44px, 5vw, 60px);
  }

  .hero-subtitle {
    font-size: clamp(20px, 2.2vw, 26px);
  }
}

@media (max-width: 920px) {
  .login-page {
    background-position: 32% center;
  }

  .login-shell {
    grid-template-columns: 1fr;
    width: min(560px, calc(100% - 32px));
    gap: 28px;
    padding: 28px 0 34px;
  }

  .hero-panel {
    min-height: auto;
    align-items: flex-start;
    padding-top: 0;
  }

  .hero-copy {
    max-width: none;
    transform: none;
  }

  .login-panel {
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .hero-copy h1 {
    margin-top: 26px;
    font-size: 48px;
  }

  .hero-subtitle {
    font-size: 24px;
  }

  .hero-description {
    font-size: 15px;
    line-height: 1.75;
  }

  .hero-highlights {
    gap: 10px 14px;
    margin-top: 30px;
    font-size: 16px;
    letter-spacing: 0.08em;
  }

  .hero-highlights span::after {
    margin-left: 14px;
  }

  .login-card {
    padding: 32px 22px 28px;
    border-radius: 24px;
  }

  .login-intro h2 {
    font-size: 28px;
  }

  .verify-code-group {
    flex-direction: column;
  }

  .verify-code-button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card,
  .fade-slide-enter-active,
  .fade-slide-leave-active {
    animation: none;
    transition: none;
  }
}
</style>
