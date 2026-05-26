<template>
  <div class="announcement-page">
    <el-card class="switch-card" shadow="never">
      <div class="switch-row">
        <div class="switch-title">公告管理</div>
        <el-radio-group v-model="activePanel">
          <el-radio-button label="banners">轮播图</el-radio-button>
          <el-radio-button label="notices">公告栏</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openCreateDialog">新增{{ panelLabel }}</el-button>
        <span v-if="isBannerPanel" class="toolbar-tip">轮播图图片会先按 702:250 比例裁剪，再上传。</span>
      </div>

      <el-table :data="currentRows" border class="main-table">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column v-if="isBannerPanel" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.contentType === 1 ? 'success' : 'info'">
              {{ row.contentType === 1 ? '图片' : '文字' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isBannerPanel" label="图片" width="120">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              fit="cover"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              class="banner-image"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="subtitle" label="副标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
              {{ row.isActive === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="有效期" min-width="260">
          <template #default="{ row }">
            {{ formatDateRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-dropdown @command="command => handleRowCommand(command, row)">
              <el-button size="small">操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item v-if="isBannerPanel" command="upload">上传图片</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? `新增${panelLabel}` : `编辑${panelLabel}`"
      width="620px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item v-if="isBannerPanel" label="类型">
          <el-select v-model="form.contentType" style="width: 100%">
            <el-option :value="1" label="图片" />
            <el-option :value="2" label="文字" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isBannerPanel && form.contentType === 2" label="表情">
          <el-input v-model="form.emoji" maxlength="16" placeholder="例如：🔥" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="可不填"
            value-format="x"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="可不填"
            value-format="x"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="cropDialogVisible"
      title="裁剪轮播图"
      width="760px"
      destroy-on-close
      :close-on-click-modal="false"
      @closed="handleCropDialogClosed"
    >
      <div class="crop-dialog">
        <div class="crop-tip">
          用户端首页轮播区使用固定比例。这里会按 {{ BANNER_RECOMMEND_WIDTH }}:{{ BANNER_RECOMMEND_HEIGHT }} 裁剪后再上传。
        </div>
        <div class="crop-preview-shell">
          <div class="crop-preview-box" :style="cropPreviewBoxStyle">
            <img
              v-if="cropSourceUrl"
              :src="cropSourceUrl"
              :style="cropImageStyle"
              class="crop-preview-image"
              alt="轮播图裁剪预览"
              draggable="false"
            >
          </div>
        </div>
        <div class="crop-meta">
          <span>原图尺寸：{{ cropSourceImage.width || 0 }} × {{ cropSourceImage.height || 0 }}</span>
          <span>导出尺寸：{{ BANNER_OUTPUT_WIDTH }} × {{ BANNER_OUTPUT_HEIGHT }}</span>
          <span>可用缩放和位移调整显示范围</span>
        </div>
        <div class="crop-control">
          <div class="crop-control-label">缩放</div>
          <el-slider v-model="cropState.zoom" :min="100" :max="300" />
        </div>
        <div class="crop-control">
          <div class="crop-control-label">左右位置</div>
          <el-slider
            v-model="cropState.offsetX"
            :min="-cropOffsetLimitX"
            :max="cropOffsetLimitX"
            :disabled="cropOffsetLimitX === 0"
          />
        </div>
        <div class="crop-control">
          <div class="crop-control-label">上下位置</div>
          <el-slider
            v-model="cropState.offsetY"
            :min="-cropOffsetLimitY"
            :max="cropOffsetLimitY"
            :disabled="cropOffsetLimitY === 0"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="cropDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="cropUploading" @click="confirmCropUpload">裁剪并上传</el-button>
      </template>
    </el-dialog>

    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onImageFileChange"
    >
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import { showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';
import {
  createAnnouncement,
  deleteAnnouncement,
  fetchAnnouncements,
  updateAnnouncement,
  uploadAnnouncementImage
} from '../../api/adminAnnouncements';

const CHANNEL_BANNER = 1;
const CHANNEL_NOTICE = 2;
const BANNER_RECOMMEND_WIDTH = 702;
const BANNER_RECOMMEND_HEIGHT = 250;
const BANNER_RECOMMEND_RATIO = BANNER_RECOMMEND_WIDTH / BANNER_RECOMMEND_HEIGHT;
const BANNER_PREVIEW_WIDTH = 560;
const BANNER_PREVIEW_HEIGHT = BANNER_PREVIEW_WIDTH / BANNER_RECOMMEND_RATIO;
const BANNER_OUTPUT_WIDTH = BANNER_RECOMMEND_WIDTH * 2;
const BANNER_OUTPUT_HEIGHT = BANNER_RECOMMEND_HEIGHT * 2;
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

const activePanel = ref('banners');
const bannerRows = ref([]);
const noticeRows = ref([]);

const dialogVisible = ref(false);
const dialogMode = ref('create');
const saving = ref(false);
const imageInputRef = ref(null);
const pendingUploadRow = ref(null);
const pendingUploadFile = ref(null);
const cropDialogVisible = ref(false);
const cropUploading = ref(false);
const cropSourceUrl = ref('');

const form = reactive({
  id: '',
  title: '',
  contentType: 1,
  subtitle: '',
  content: '',
  emoji: '',
  isActive: 1,
  sortOrder: 0,
  startTime: '',
  endTime: ''
});

const cropSourceImage = reactive({
  width: 0,
  height: 0
});

const cropState = reactive({
  zoom: 100,
  offsetX: 0,
  offsetY: 0
});

const isBannerPanel = computed(() => activePanel.value === 'banners');
const panelLabel = computed(() => (isBannerPanel.value ? '轮播图' : '公告'));
const currentRows = computed(() => (isBannerPanel.value ? bannerRows.value : noticeRows.value));
const cropPreviewBoxStyle = computed(() => ({
  width: `${BANNER_PREVIEW_WIDTH}px`,
  height: `${BANNER_PREVIEW_HEIGHT}px`
}));
const cropPreviewMetrics = computed(() => {
  const naturalWidth = cropSourceImage.width || 0;
  const naturalHeight = cropSourceImage.height || 0;
  if (!naturalWidth || !naturalHeight) {
    return {
      scale: 1,
      renderWidth: BANNER_PREVIEW_WIDTH,
      renderHeight: BANNER_PREVIEW_HEIGHT,
      maxOffsetX: 0,
      maxOffsetY: 0
    };
  }
  const scale = Math.max(
    BANNER_PREVIEW_WIDTH / naturalWidth,
    BANNER_PREVIEW_HEIGHT / naturalHeight
  ) * (cropState.zoom / 100);
  const renderWidth = naturalWidth * scale;
  const renderHeight = naturalHeight * scale;
  return {
    scale,
    renderWidth,
    renderHeight,
    maxOffsetX: Math.max(0, (renderWidth - BANNER_PREVIEW_WIDTH) / 2),
    maxOffsetY: Math.max(0, (renderHeight - BANNER_PREVIEW_HEIGHT) / 2)
  };
});
const cropImageStyle = computed(() => {
  const metrics = cropPreviewMetrics.value;
  return {
    width: `${metrics.renderWidth}px`,
    height: `${metrics.renderHeight}px`,
    transform: `translate(calc(-50% + ${cropState.offsetX}px), calc(-50% + ${cropState.offsetY}px))`
  };
});
const cropOffsetLimitX = computed(() => Math.round(cropPreviewMetrics.value.maxOffsetX));
const cropOffsetLimitY = computed(() => Math.round(cropPreviewMetrics.value.maxOffsetY));

watch(activePanel, () => {
  reloadCurrentPanel();
});

watch(cropPreviewMetrics, (metrics) => {
  cropState.offsetX = clamp(cropState.offsetX, -metrics.maxOffsetX, metrics.maxOffsetX);
  cropState.offsetY = clamp(cropState.offsetY, -metrics.maxOffsetY, metrics.maxOffsetY);
});

onMounted(() => {
  reloadCurrentPanel();
});

async function reloadCurrentPanel() {
  const channel = isBannerPanel.value ? CHANNEL_BANNER : CHANNEL_NOTICE;
  try {
    const res = await fetchAnnouncements(channel);
    if (res.code !== 200) {
      ElMessage.error(res.message || '加载公告失败');
      return;
    }
    if (channel === CHANNEL_BANNER) {
      bannerRows.value = res.data || [];
    } else {
      noticeRows.value = res.data || [];
    }
  } catch (e) {
    ElMessage.error('加载公告失败');
  }
}

function openCreateDialog() {
  dialogMode.value = 'create';
  form.id = '';
  form.title = '';
  form.contentType = isBannerPanel.value ? 1 : 2;
  form.subtitle = '';
  form.content = '';
  form.emoji = '';
  form.isActive = 1;
  form.sortOrder = 0;
  form.startTime = '';
  form.endTime = '';
  dialogVisible.value = true;
}

function openEditDialog(row) {
  dialogMode.value = 'edit';
  form.id = row.id;
  form.title = row.title || '';
  form.contentType = row.contentType || (isBannerPanel.value ? 1 : 2);
  form.subtitle = row.subtitle || '';
  form.content = row.content || '';
  form.emoji = row.emoji || '';
  form.isActive = row.isActive ?? 1;
  form.sortOrder = row.sortOrder ?? 0;
  form.startTime = row.startTime ? String(row.startTime) : '';
  form.endTime = row.endTime ? String(row.endTime) : '';
  dialogVisible.value = true;
}

function buildPayload() {
  return {
    channel: isBannerPanel.value ? CHANNEL_BANNER : CHANNEL_NOTICE,
    contentType: isBannerPanel.value ? form.contentType : 2,
    title: form.title ? form.title.trim() : '',
    subtitle: form.subtitle ? form.subtitle.trim() : '',
    content: form.content ? form.content.trim() : '',
    emoji: form.emoji ? form.emoji.trim() : '',
    isActive: form.isActive,
    sortOrder: form.sortOrder,
    startTime: form.startTime ? Number(form.startTime) : null,
    endTime: form.endTime ? Number(form.endTime) : null
  };
}

async function submitForm() {
  const payload = buildPayload();
  if (!payload.title) {
    ElMessage.warning('请输入标题');
    return;
  }
  if (payload.startTime && payload.endTime && payload.endTime < payload.startTime) {
    ElMessage.warning('结束时间不能早于开始时间');
    return;
  }

  saving.value = true;
  try {
    const res = dialogMode.value === 'create'
      ? await createAnnouncement(payload)
      : await updateAnnouncement(form.id, payload);
    if (res.code !== 200) {
      ElMessage.error(res.message || '保存失败');
      return;
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await reloadCurrentPanel();
  } catch (e) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.title || row.id}”吗？`, '删除确认', { type: 'warning' });
  } catch (e) {
    return;
  }
  try {
    const res = await deleteAnnouncement(row.id);
    if (res.code !== 200) {
      ElMessage.error(res.message || '删除失败');
      return;
    }
    ElMessage.success('删除成功');
    await reloadCurrentPanel();
  } catch (e) {
    ElMessage.error('删除失败');
  }
}

function triggerUpload(row) {
  pendingUploadRow.value = row;
  if (!imageInputRef.value) {
    return;
  }
  imageInputRef.value.value = '';
  imageInputRef.value.click();
}

async function onImageFileChange(event) {
  const file = event?.target?.files?.[0];
  const row = pendingUploadRow.value;
  pendingUploadRow.value = null;
  if (!file || !row) {
    return;
  }
  if (!file.type || !file.type.startsWith('image/')) {
    showUploadLimitDialog('请上传图片文件');
    return;
  }
  if (file.size > MAX_IMAGE_SIZE) {
    showUploadLimitDialog('图片大小不能超过 5MB');
    return;
  }
  try {
    await openCropDialog(row, file);
  } catch (e) {
    showUploadErrorDialog(e, '读取图片失败', '读取图片失败');
    handleCropDialogClosed();
  }
}

async function openCropDialog(row, file) {
  const objectUrl = URL.createObjectURL(file);
  try {
    const image = await loadImageElement(objectUrl);
    if (image.naturalWidth < BANNER_RECOMMEND_WIDTH || image.naturalHeight < BANNER_RECOMMEND_HEIGHT) {
      throw new Error(`图片尺寸不能小于 ${BANNER_RECOMMEND_WIDTH}×${BANNER_RECOMMEND_HEIGHT}`);
    }
    revokeCropSourceUrl();
    pendingUploadRow.value = row;
    pendingUploadFile.value = file;
    cropSourceUrl.value = objectUrl;
    cropSourceImage.width = image.naturalWidth;
    cropSourceImage.height = image.naturalHeight;
    cropState.zoom = 100;
    cropState.offsetX = 0;
    cropState.offsetY = 0;
    cropDialogVisible.value = true;
  } catch (error) {
    URL.revokeObjectURL(objectUrl);
    throw error;
  }
}

async function confirmCropUpload() {
  if (!pendingUploadRow.value || !pendingUploadFile.value) {
    ElMessage.warning('请先选择图片');
    return;
  }
  cropUploading.value = true;
  try {
    const croppedFile = await createCroppedBannerFile(pendingUploadFile.value);
    const res = await uploadAnnouncementImage(pendingUploadRow.value.id, croppedFile);
    if (res.code !== 200) {
      showUploadErrorDialog(res.message || '上传失败', '上传失败', '上传失败');
      return;
    }
    ElMessage.success('上传成功');
    cropDialogVisible.value = false;
    await reloadCurrentPanel();
  } catch (e) {
    showUploadErrorDialog(e, '上传失败', '上传失败');
  } finally {
    cropUploading.value = false;
  }
}

async function createCroppedBannerFile(file) {
  if (!cropSourceUrl.value) {
    throw new Error('未找到待上传图片');
  }
  const metrics = cropPreviewMetrics.value;
  if (!metrics.scale) {
    throw new Error('裁剪参数无效');
  }
  const image = await loadImageElement(cropSourceUrl.value);
  const renderLeft = BANNER_PREVIEW_WIDTH / 2 - metrics.renderWidth / 2 + cropState.offsetX;
  const renderTop = BANNER_PREVIEW_HEIGHT / 2 - metrics.renderHeight / 2 + cropState.offsetY;
  const sourceX = clamp((0 - renderLeft) / metrics.scale, 0, image.naturalWidth);
  const sourceY = clamp((0 - renderTop) / metrics.scale, 0, image.naturalHeight);
  const sourceWidth = Math.min(BANNER_PREVIEW_WIDTH / metrics.scale, image.naturalWidth - sourceX);
  const sourceHeight = Math.min(BANNER_PREVIEW_HEIGHT / metrics.scale, image.naturalHeight - sourceY);
  const canvas = document.createElement('canvas');
  canvas.width = BANNER_OUTPUT_WIDTH;
  canvas.height = BANNER_OUTPUT_HEIGHT;
  const context = canvas.getContext('2d');
  if (!context) {
    throw new Error('浏览器不支持图片裁剪');
  }
  context.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, canvas.width, canvas.height);
  const mimeType = resolveCropMimeType(file.type);
  const blob = await canvasToBlob(canvas, mimeType);
  return new File([blob], buildCroppedFileName(file.name, mimeType), { type: mimeType });
}

function handleCropDialogClosed() {
  revokeCropSourceUrl();
  pendingUploadRow.value = null;
  pendingUploadFile.value = null;
  cropSourceImage.width = 0;
  cropSourceImage.height = 0;
  cropState.zoom = 100;
  cropState.offsetX = 0;
  cropState.offsetY = 0;
}

function revokeCropSourceUrl() {
  if (cropSourceUrl.value) {
    URL.revokeObjectURL(cropSourceUrl.value);
    cropSourceUrl.value = '';
  }
}

function loadImageElement(url) {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('读取图片失败'));
    image.src = url;
  });
}

function resolveCropMimeType(mimeType) {
  return mimeType === 'image/png' ? 'image/png' : 'image/jpeg';
}

function buildCroppedFileName(fileName, mimeType) {
  const baseName = (fileName || 'announcement').replace(/\.[^.]+$/, '') || 'announcement';
  const ext = mimeType === 'image/png' ? '.png' : '.jpg';
  return `${baseName}-banner${ext}`;
}

function canvasToBlob(canvas, mimeType) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error('生成裁剪图片失败'));
          return;
        }
        resolve(blob);
      },
      mimeType,
      mimeType === 'image/jpeg' ? 0.92 : undefined
    );
  });
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function handleRowCommand(command, row) {
  if (command === 'edit') {
    openEditDialog(row);
    return;
  }
  if (command === 'delete') {
    confirmDelete(row);
    return;
  }
  if (command === 'upload') {
    triggerUpload(row);
  }
}

function formatDateRange(startTime, endTime) {
  const start = formatTime(startTime);
  const end = formatTime(endTime);
  if (!start && !end) {
    return '长期有效';
  }
  if (start && !end) {
    return `${start} 起`;
  }
  if (!start && end) {
    return `截止 ${end}`;
  }
  return `${start} - ${end}`;
}

function formatTime(timestamp) {
  if (!timestamp) {
    return '';
  }
  const date = new Date(Number(timestamp));
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mm}`;
}

useAdminPageRefresh(async () => {
  await reloadCurrentPanel();
});
</script>

<style scoped>
.announcement-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.switch-card,
.panel-card {
  border-radius: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.switch-title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.toolbar-tip {
  font-size: 13px;
  color: #909399;
}

.main-table {
  width: 100%;
}

.banner-image {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.crop-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.crop-tip {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}

.crop-preview-shell {
  display: flex;
  justify-content: center;
}

.crop-preview-box {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid #dcdfe6;
  background: #f4f6fa;
}

.crop-preview-image {
  position: absolute;
  left: 50%;
  top: 50%;
  user-select: none;
}

.crop-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  font-size: 13px;
  color: #909399;
}

.crop-control {
  display: flex;
  align-items: center;
  gap: 16px;
}

.crop-control-label {
  width: 72px;
  flex: none;
  color: #606266;
}

.crop-control .el-slider {
  flex: 1;
}
</style>
