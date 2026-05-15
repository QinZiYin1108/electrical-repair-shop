<template>
  <div class="system-settings-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-content">
        <div>
          <div class="eyebrow">System Settings</div>
          <h2>{{ ui.pageTitle }}</h2>
          <p>{{ ui.pageDescription }}</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" :loading="saving" @click="handleSave">{{ ui.saveButton }}</el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loading" class="group-list">
      <el-card
        v-for="group in groups"
        :key="group.groupName"
        class="group-card"
        shadow="never"
      >
        <template #header>
          <div class="group-header">
            <div>
              <div class="group-title">{{ group.groupLabel }}</div>
              <div class="group-subtitle">{{ group.groupName }}</div>
            </div>
            <el-tag type="info">{{ formatCount((group.items || []).length, ui.itemUnit) }}</el-tag>
          </div>
        </template>

        <div
          v-for="item in group.items"
          :key="item.configKey"
          class="setting-item"
        >
          <div class="setting-meta">
            <div class="setting-title-row">
              <div class="setting-title">{{ item.label }}</div>
              <el-tag v-if="item.usingDefault" size="small" type="success">{{ ui.defaultTag }}</el-tag>
            </div>
            <div class="setting-desc">{{ item.description }}</div>
            <div class="setting-extra">
              <span>{{ ui.configKeyLabel }}{{ item.configKey }}</span>
              <span>{{ ui.defaultValueLabel }}{{ formatValue(item.defaultValue, item.unit) }}</span>
            </div>
          </div>

          <div class="setting-input">
            <el-input-number
              v-if="item.configType === 2"
              v-model="formValues[item.configKey]"
              :min="item.minValue ?? 0"
              :max="item.maxValue ?? Number.MAX_SAFE_INTEGER"
              controls-position="right"
            />
            <el-input
              v-else
              v-model="formValues[item.configKey]"
              clearable
            />
            <span v-if="item.unit" class="unit-text">{{ item.unit }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="protocols-card" shadow="never" v-loading="protocolLoading">
      <template #header>
        <div class="group-header">
          <div>
            <div class="group-title">{{ ui.protocolSectionTitle }}</div>
            <div class="group-subtitle">{{ ui.protocolSectionDesc }}</div>
          </div>
          <el-tag type="warning">{{ formatCount(protocols.length, ui.fileUnit) }}</el-tag>
        </div>
      </template>

      <div class="asset-grid">
        <div v-for="item in protocols" :key="item.type" class="asset-item">
          <div class="asset-item-main">
            <div class="asset-item-head">
              <div class="asset-name">{{ item.title }}</div>
              <el-tag :type="item.uploaded ? 'success' : 'info'">
                {{ item.uploaded ? ui.uploaded : ui.notUploaded }}
              </el-tag>
            </div>
            <div class="asset-desc">
              {{ item.uploaded ? ui.protocolUploadedDesc : ui.protocolEmptyDesc }}
            </div>
            <div class="asset-meta">
              <span>{{ ui.fileNameLabel }}{{ item.fileName || '-' }}</span>
              <span>{{ ui.updatedTimeLabel }}{{ formatTime(item.updatedTime) }}</span>
            </div>
          </div>

          <div class="asset-actions">
            <el-upload
              :show-file-list="false"
              accept=".md,.markdown,text/markdown"
              :http-request="options => handleProtocolUpload(item.type, options)"
            >
              <el-button type="primary" :loading="uploadingProtocolType === item.type">
                {{ ui.uploadMarkdown }}
              </el-button>
            </el-upload>
            <el-button plain @click="viewProtocol(item.type)">{{ ui.viewContent }}</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="email-templates-card" shadow="never" v-loading="emailTemplateLoading">
      <template #header>
        <div class="group-header">
          <div>
            <div class="group-title">{{ ui.emailTemplateSectionTitle }}</div>
            <div class="group-subtitle">{{ ui.emailTemplateSectionDesc }}</div>
          </div>
          <el-tag type="danger">{{ formatCount(emailTemplates.length, ui.fileUnit) }}</el-tag>
        </div>
      </template>

      <div class="asset-grid">
        <div v-for="item in emailTemplates" :key="item.type" class="asset-item">
          <div class="asset-item-main">
            <div class="asset-item-head">
              <div class="asset-name">{{ item.title }}</div>
              <el-tag :type="item.uploaded ? 'success' : 'info'">
                {{ item.uploaded ? ui.uploaded : ui.notUploaded }}
              </el-tag>
            </div>
            <div class="asset-desc">
              {{ item.uploaded ? ui.emailTemplateUploadedDesc : ui.emailTemplateEmptyDesc }}
            </div>
            <div class="asset-meta">
              <span>{{ ui.fileNameLabel }}{{ item.fileName || '-' }}</span>
              <span>{{ ui.updatedTimeLabel }}{{ formatTime(item.updatedTime) }}</span>
            </div>
            <div class="template-tip">
              {{ ui.placeholderLabel }}
              <code v-pre>{{code}}</code>
              {{ ui.placeholderSeparator }}
              <code v-pre>{{expireMinutes}}</code>
            </div>
          </div>

          <div class="asset-actions">
            <el-upload
              :show-file-list="false"
              accept=".html,.htm,text/html"
              :http-request="options => handleEmailTemplateUpload(item.type, options)"
            >
              <el-button type="primary" :loading="uploadingEmailTemplateType === item.type">
                {{ ui.uploadHtml }}
              </el-button>
            </el-upload>
            <el-button plain :disabled="!item.fileUrl" @click="openAsset(item.fileUrl)">{{ ui.viewFile }}</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  fetchAdminEmailTemplates,
  fetchAdminProtocols,
  fetchAdminSystemSettings,
  updateAdminSystemSettings,
  uploadAdminEmailTemplate,
  uploadAdminProtocol
} from '../../api/adminSystemSettings';

const ui = {
  pageTitle: '\u7ba1\u7406\u5458\u57fa\u7840\u8bbe\u7f6e',
  pageDescription: '\u7edf\u4e00\u7ef4\u62a4\u552e\u540e\u3001\u8d26\u53f7\u6ce8\u9500\u3001\u9a8c\u8bc1\u7801\u3001\u9884\u7ea6\u3001\u4e0a\u95e8\u4e8c\u7ef4\u7801\u3001\u534f\u8bae\u6587\u6863\u4e0e\u90ae\u7bb1\u6a21\u677f\u7b49\u7cfb\u7edf\u57fa\u7840\u53c2\u6570\u3002',
  saveButton: '\u4fdd\u5b58\u914d\u7f6e',
  itemUnit: '\u9879',
  fileUnit: '\u4efd',
  defaultTag: '\u9ed8\u8ba4\u503c',
  configKeyLabel: '\u914d\u7f6e\u952e\uff1a',
  defaultValueLabel: '\u9ed8\u8ba4\u503c\uff1a',
  protocolSectionTitle: '\u534f\u8bae\u6587\u6863',
  protocolSectionDesc: '\u901a\u8fc7 OSS \u6258\u7ba1 Markdown \u6587\u4ef6\uff0c\u524d\u7aef\u7edf\u4e00\u7ecf\u540e\u7aef\u8bfb\u53d6\u5c55\u793a\u3002',
  emailTemplateSectionTitle: '\u90ae\u7bb1\u6a21\u677f\u8bbe\u7f6e',
  emailTemplateSectionDesc: '\u901a\u8fc7 OSS \u6258\u7ba1 HTML \u90ae\u4ef6\u6a21\u677f\uff0c\u6a21\u677f\u6587\u4ef6\u8bb0\u5f55\u5199\u5165\u6587\u4ef6\u8868\uff0c\u5f53\u524d\u542f\u7528\u6a21\u677f ID \u5199\u5165\u7cfb\u7edf\u914d\u7f6e\u8868\u3002',
  uploaded: '\u5df2\u4e0a\u4f20',
  notUploaded: '\u672a\u4e0a\u4f20',
  protocolUploadedDesc: '\u5f53\u524d\u542f\u7528\u6587\u4ef6\u5c06\u7528\u4e8e\u7ba1\u7406\u5458\u3001\u7528\u6237\u548c\u5e08\u5085\u7aef\u534f\u8bae\u5185\u5bb9\u5c55\u793a\u3002',
  protocolEmptyDesc: '\u6682\u672a\u4e0a\u4f20\uff0c\u5c06\u663e\u793a\u9ed8\u8ba4\u5360\u4f4d\u5185\u5bb9\u3002',
  emailTemplateUploadedDesc: '\u9a8c\u8bc1\u7801\u53d1\u9001\u65f6\u5c06\u4f18\u5148\u4f7f\u7528\u5f53\u524d\u542f\u7528\u7684 OSS \u6a21\u677f\u3002',
  emailTemplateEmptyDesc: '\u6682\u672a\u4e0a\u4f20\u65f6\uff0c\u5c06\u56de\u9000\u5230\u540e\u7aef\u9ed8\u8ba4\u9a8c\u8bc1\u7801\u6a21\u677f\u3002',
  fileNameLabel: '\u6587\u4ef6\u540d\uff1a',
  updatedTimeLabel: '\u66f4\u65b0\u65f6\u95f4\uff1a',
  uploadMarkdown: '\u4e0a\u4f20 Markdown',
  uploadHtml: '\u4e0a\u4f20 HTML',
  viewContent: '\u67e5\u770b\u5185\u5bb9',
  viewFile: '\u67e5\u770b\u6587\u4ef6',
  placeholderLabel: '\u6a21\u677f\u5360\u4f4d\u7b26\uff1a',
  placeholderSeparator: '\u3001',
  loadSettingsFailed: '\u52a0\u8f7d\u57fa\u7840\u8bbe\u7f6e\u5931\u8d25',
  loadProtocolsFailed: '\u52a0\u8f7d\u534f\u8bae\u5217\u8868\u5931\u8d25',
  loadEmailTemplatesFailed: '\u52a0\u8f7d\u90ae\u7bb1\u6a21\u677f\u5217\u8868\u5931\u8d25',
  saveSettingsFailed: '\u4fdd\u5b58\u57fa\u7840\u8bbe\u7f6e\u5931\u8d25',
  saveSettingsSuccess: '\u57fa\u7840\u8bbe\u7f6e\u5df2\u4fdd\u5b58',
  invalidMarkdown: '\u4ec5\u652f\u6301\u4e0a\u4f20 .md \u6216 .markdown \u6587\u4ef6',
  invalidHtml: '\u4ec5\u652f\u6301\u4e0a\u4f20 .html \u6216 .htm \u6587\u4ef6',
  uploadProtocolFailed: '\u4e0a\u4f20\u534f\u8bae\u5931\u8d25',
  uploadProtocolSuccess: '\u534f\u8bae\u6587\u4ef6\u5df2\u4e0a\u4f20',
  uploadEmailTemplateFailed: '\u4e0a\u4f20\u90ae\u7bb1\u6a21\u677f\u5931\u8d25',
  uploadEmailTemplateSuccess: '\u90ae\u7bb1\u6a21\u677f\u5df2\u4e0a\u4f20',
  refreshSuccess: '\u5237\u65b0\u6210\u529f'
};

const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const protocolLoading = ref(false);
const emailTemplateLoading = ref(false);
const uploadingProtocolType = ref('');
const uploadingEmailTemplateType = ref('');
const groups = ref([]);
const protocols = ref([]);
const emailTemplates = ref([]);
const formValues = reactive({});

onMounted(() => {
  loadAll();
  window.addEventListener('admin-page-refresh', handlePageRefresh);
});

onBeforeUnmount(() => {
  window.removeEventListener('admin-page-refresh', handlePageRefresh);
});

async function loadAll() {
  await Promise.all([loadSettings(), loadProtocols(), loadEmailTemplates()]);
}

async function loadSettings() {
  loading.value = true;
  try {
    const res = await fetchAdminSystemSettings();
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || ui.loadSettingsFailed);
      return;
    }
    applyResponse(res.data);
  } catch (error) {
    ElMessage.error(ui.loadSettingsFailed);
  } finally {
    loading.value = false;
  }
}

async function loadProtocols() {
  protocolLoading.value = true;
  try {
    const res = await fetchAdminProtocols();
    if (res.code !== 200 || !Array.isArray(res.data)) {
      ElMessage.error(res.message || ui.loadProtocolsFailed);
      return;
    }
    protocols.value = res.data;
  } catch (error) {
    ElMessage.error(ui.loadProtocolsFailed);
  } finally {
    protocolLoading.value = false;
  }
}

async function loadEmailTemplates() {
  emailTemplateLoading.value = true;
  try {
    const res = await fetchAdminEmailTemplates();
    if (res.code !== 200 || !Array.isArray(res.data)) {
      ElMessage.error(res.message || ui.loadEmailTemplatesFailed);
      return;
    }
    emailTemplates.value = res.data;
  } catch (error) {
    ElMessage.error(ui.loadEmailTemplatesFailed);
  } finally {
    emailTemplateLoading.value = false;
  }
}

async function handleSave() {
  const items = [];
  groups.value.forEach(group => {
    (group.items || []).forEach(item => {
      const value = formValues[item.configKey];
      items.push({
        configKey: item.configKey,
        configValue: String(value ?? item.defaultValue ?? '')
      });
    });
  });

  saving.value = true;
  try {
    const res = await updateAdminSystemSettings({ items });
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || ui.saveSettingsFailed);
      return;
    }
    applyResponse(res.data);
    ElMessage.success(ui.saveSettingsSuccess);
  } catch (error) {
    ElMessage.error(ui.saveSettingsFailed);
  } finally {
    saving.value = false;
  }
}

async function handleProtocolUpload(type, options) {
  const file = options.file;
  const fileName = String(file?.name || '').toLowerCase();
  if (!fileName.endsWith('.md') && !fileName.endsWith('.markdown')) {
    ElMessage.warning(ui.invalidMarkdown);
    options.onError?.(new Error('invalid file'));
    return;
  }

  uploadingProtocolType.value = type;
  try {
    const res = await uploadAdminProtocol(type, file);
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || ui.uploadProtocolFailed);
      options.onError?.(new Error(res.message || 'upload failed'));
      return;
    }
    await loadProtocols();
    ElMessage.success(ui.uploadProtocolSuccess);
    options.onSuccess?.(res.data);
  } catch (error) {
    ElMessage.error(ui.uploadProtocolFailed);
    options.onError?.(error);
  } finally {
    uploadingProtocolType.value = '';
  }
}

async function handleEmailTemplateUpload(type, options) {
  const file = options.file;
  const fileName = String(file?.name || '').toLowerCase();
  if (!fileName.endsWith('.html') && !fileName.endsWith('.htm')) {
    ElMessage.warning(ui.invalidHtml);
    options.onError?.(new Error('invalid file'));
    return;
  }

  uploadingEmailTemplateType.value = type;
  try {
    const res = await uploadAdminEmailTemplate(type, file);
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || ui.uploadEmailTemplateFailed);
      options.onError?.(new Error(res.message || 'upload failed'));
      return;
    }
    await loadEmailTemplates();
    ElMessage.success(ui.uploadEmailTemplateSuccess);
    options.onSuccess?.(res.data);
  } catch (error) {
    ElMessage.error(ui.uploadEmailTemplateFailed);
    options.onError?.(error);
  } finally {
    uploadingEmailTemplateType.value = '';
  }
}

function viewProtocol(type) {
  router.push(`/protocol/${type}`);
}

function openAsset(url) {
  if (!url) {
    return;
  }
  window.open(url, '_blank', 'noopener,noreferrer');
}

function applyResponse(data) {
  groups.value = data.groups || [];
  Object.keys(formValues).forEach(key => {
    delete formValues[key];
  });
  groups.value.forEach(group => {
    (group.items || []).forEach(item => {
      formValues[item.configKey] = normalizeItemValue(item);
    });
  });
}

function normalizeItemValue(item) {
  if (!item) {
    return '';
  }
  if (item.configType === 2) {
    const value = Number(item.configValue ?? item.defaultValue ?? 0);
    return Number.isFinite(value) ? value : 0;
  }
  return item.configValue ?? item.defaultValue ?? '';
}

function formatValue(value, unit) {
  const text = value == null || value === '' ? '-' : value;
  return unit ? `${text}${unit}` : text;
}

function formatTime(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(Number(value));
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return date.toLocaleString();
}

function formatCount(count, unit) {
  return `${count} ${unit}`;
}

async function handlePageRefresh(event) {
  if (!event || !event.detail || event.detail.path !== '/admin/system/settings') {
    return;
  }
  event.detail.handled = true;
  await loadAll();
  ElMessage.success(ui.refreshSuccess);
}
</script>

<style scoped>
.system-settings-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px;
}

.hero-card {
  border: none;
  background:
    radial-gradient(circle at top left, rgba(214, 232, 255, 0.9), transparent 38%),
    linear-gradient(135deg, #f7fbff 0%, #eef5fb 100%);
}

.hero-content {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #6c8193;
  margin-bottom: 6px;
}

.hero-content h2 {
  margin: 0;
  font-size: 28px;
  color: #183247;
}

.hero-content p {
  margin: 10px 0 0;
  color: #5d7284;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.group-list {
  display: grid;
  gap: 16px;
}

.group-card,
.protocols-card,
.email-templates-card {
  border: 1px solid #e1ebf2;
  background: #ffffff;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.group-title {
  font-size: 18px;
  font-weight: 600;
  color: #163047;
}

.group-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #8495a6;
}

.setting-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
  padding: 18px 0;
  border-bottom: 1px solid #edf2f6;
}

.setting-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.setting-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.setting-title {
  font-size: 16px;
  font-weight: 600;
  color: #20384c;
}

.setting-desc {
  color: #5e7383;
  line-height: 1.7;
}

.setting-extra {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 10px;
  font-size: 12px;
  color: #8b9cab;
}

.setting-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.setting-input :deep(.el-input-number) {
  width: 100%;
}

.setting-input :deep(.el-input) {
  width: 100%;
}

.unit-text {
  color: #5f7385;
  white-space: nowrap;
}

.asset-grid {
  display: grid;
  gap: 16px;
}

.asset-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  padding: 20px 0;
  border-bottom: 1px solid #edf2f6;
}

.asset-item:first-child {
  padding-top: 0;
}

.asset-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.asset-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.asset-name {
  font-size: 16px;
  font-weight: 600;
  color: #20384c;
}

.asset-desc {
  margin-top: 8px;
  color: #5e7383;
  line-height: 1.7;
}

.asset-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 12px;
  color: #8b9cab;
}

.template-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #4b6477;
}

.template-tip code {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f2f6fc;
  color: #245d91;
}

.asset-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .setting-item,
  .asset-item {
    grid-template-columns: 1fr;
  }

  .setting-input,
  .asset-actions {
    width: 100%;
  }
}
</style>