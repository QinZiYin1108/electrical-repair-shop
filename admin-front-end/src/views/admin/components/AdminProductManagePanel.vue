<template>
  <div class="product-manage-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-content">
        <div>
          <div class="hero-title">{{ title }}</div>
          <div class="hero-desc">{{ description }}</div>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="openCreateDialog">新增商品</el-button>
        </div>
      </div>
    </el-card>

    <div class="summary-grid">
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">商品总数</div>
        <div class="summary-value">{{ rows.length }}</div>
      </el-card>
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">上架中</div>
        <div class="summary-value">{{ onShelfCount }}</div>
      </el-card>
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">库存预警</div>
        <div class="summary-value">{{ warningCount }}</div>
      </el-card>
      <el-card class="summary-card" shadow="never">
        <div class="summary-label">已下架</div>
        <div class="summary-value">{{ offShelfCount }}</div>
      </el-card>
    </div>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="filter.keyword"
            clearable
            placeholder="搜索商品名称 / 编号 / 品牌 / 型号"
            style="width: 280px"
            @keyup.enter="loadProducts"
          />
          <el-cascader
            v-model="filter.categoryId"
            :options="categoryOptions"
            :props="categoryCascaderProps"
            clearable
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部分类"
            style="width: 240px"
          />
          <el-select v-model="filter.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option :value="1" label="上架" />
            <el-option :value="2" label="下架" />
            <el-option :value="3" label="缺货" />
          </el-select>
          <el-button type="primary" @click="loadProducts">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border class="main-table">
        <el-table-column label="商品信息" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                v-if="row.mainImageUrl"
                :src="row.mainImageUrl"
                fit="cover"
                :preview-src-list="[row.mainImageUrl]"
                preview-teleported
                class="product-thumb"
              />
              <div class="product-meta">
                <div class="product-name-row">
                  <span class="product-name">{{ row.name }}</span>
                  <el-tag v-if="row.isHot === 1" size="small" type="danger">热销</el-tag>
                  <el-tag v-if="row.isNew === 1" size="small" type="success">新品</el-tag>
                  <el-tag v-if="row.isRecommended === 1" size="small" type="warning">推荐</el-tag>
                </div>
                <div class="product-subline">编号：{{ row.productNo || '-' }}</div>
                <div class="product-subline">品牌 / 型号：{{ row.brand || '-' }} / {{ row.model || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.categoryPath || row.categoryName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="价格" width="180">
          <template #default="{ row }">
            <div class="price-line">售价：{{ formatMoney(row.sellingPrice) }}</div>
            <div class="price-subline">原价：{{ formatMoney(row.originalPrice) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <div>{{ row.stockQuantity ?? 0 }}</div>
            <div class="stock-subline">预警：{{ row.warningStock ?? 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ row.statusText || formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.updatedTime || row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? `新增${title}` : `编辑${title}`"
      width="980px"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <el-form label-width="110px" class="product-form">
        <div class="form-grid">
          <el-form-item label="商品名称" required>
            <el-input v-model="form.name" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="商品分类" required>
            <el-cascader
              v-model="form.categoryId"
              :options="categoryOptions"
              :props="categoryCascaderProps"
              clearable
              filterable
              placeholder="请选择商品分类"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="品牌" required>
            <el-input v-model="form.brand" maxlength="100" />
          </el-form-item>
          <el-form-item label="型号" required>
            <el-input v-model="form.model" maxlength="100" />
          </el-form-item>
          <el-form-item label="原价" required>
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="售价" required>
            <el-input-number v-model="form.sellingPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="成本价" required>
            <el-input-number v-model="form.costPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="库存数量" required>
            <el-input-number v-model="form.stockQuantity" :min="0" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="预警库存">
            <el-input-number v-model="form.warningStock" :min="0" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="商品状态" required>
            <el-select v-model="form.status" style="width: 100%">
              <el-option :value="1" label="上架" />
              <el-option :value="2" label="下架" />
              <el-option :value="3" label="缺货" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="保修期(月)">
            <el-input-number v-model="form.warrantyPeriod" :min="0" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="重量(kg)">
            <el-input-number v-model="form.weight" :min="0" :precision="2" :step="0.1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="尺寸">
            <el-input v-model="form.dimensions" maxlength="100" placeholder="例：60x45x80cm" />
          </el-form-item>
          <el-form-item label="是否包邮">
            <el-switch v-model="form.isFreeShipping" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="运费">
            <el-input-number
              v-model="form.shippingFee"
              :min="0"
              :precision="2"
              :step="5"
              :disabled="form.isFreeShipping === 1"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="热销商品">
            <el-switch v-model="form.isHot" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="新品商品">
            <el-switch v-model="form.isNew" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="推荐商品">
            <el-switch v-model="form.isRecommended" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>

        <el-form-item label="主图素材" required>
          <div class="media-field">
            <div class="media-toolbar">
              <div class="media-toolbar-left">
                <el-button type="primary" plain @click="triggerMainImageSelect">选择主图</el-button>
                <el-button v-if="mainImageFile" @click="removeMainImage">移除主图</el-button>
              </div>
              <div class="upload-tip">先在前端预览，点击保存时再上传到阿里 OSS</div>
            </div>
            <div v-if="mainImageFile" class="media-grid media-grid-main">
              <div class="media-card media-card-main">
                <img :src="resolveMediaUrl(mainImageFile)" alt="主图预览" class="media-image media-image-main">
                <div class="media-card-footer">
                  <div class="media-card-name">{{ mainImageFile.name || '商品主图' }}</div>
                  <div class="media-card-actions">
                    <el-button link type="primary" @click="openMediaPreview(mainImageFile, 'image')">预览</el-button>
                    <el-button link type="danger" @click="removeMainImage">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="media-empty">请选择商品主图</div>
          </div>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" show-word-limit />
        </el-form-item>

        <el-form-item label="商品图片">
          <div class="media-field">
            <div class="media-toolbar">
              <div class="media-toolbar-left">
                <el-button type="primary" plain @click="triggerImageSelect">选择图片</el-button>
                <el-button v-if="imageFileList.length" @click="clearImageFiles">清空图片</el-button>
              </div>
              <div class="upload-tip">支持多选，单张不超过 5MB，保存时统一上传</div>
            </div>
            <div v-if="imageFileList.length" class="media-grid">
              <div v-for="file in imageFileList" :key="file.uid" class="media-card">
                <img :src="resolveMediaUrl(file)" :alt="file.name || '商品图片'" class="media-image">
                <div class="media-card-footer">
                  <div class="media-card-name">{{ file.name || '商品图片' }}</div>
                  <div class="media-card-actions">
                    <el-button link type="primary" @click="openMediaPreview(file, 'image')">预览</el-button>
                    <el-button link type="danger" @click="removeImageFile(file.uid)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="media-empty">暂未选择商品图片</div>
          </div>
        </el-form-item>

        <el-form-item label="商品视频">
          <div class="media-field">
            <div class="media-toolbar">
              <div class="media-toolbar-left">
                <el-button type="primary" plain @click="triggerVideoSelect">选择视频</el-button>
                <el-button v-if="videoFileList.length" @click="clearVideoFiles">清空视频</el-button>
              </div>
              <div class="upload-tip">支持多选，单个视频不超过 30MB，保存时统一上传</div>
            </div>
            <div v-if="videoFileList.length" class="media-grid">
              <div v-for="file in videoFileList" :key="file.uid" class="media-card media-card-video">
                <video :src="resolveMediaUrl(file)" class="media-video" controls preload="metadata" />
                <div class="media-card-footer">
                  <div class="media-card-name">{{ file.name || '商品视频' }}</div>
                  <div class="media-card-actions">
                    <el-button link type="primary" @click="openMediaPreview(file, 'video')">预览</el-button>
                    <el-button link type="danger" @click="removeVideoFile(file.uid)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="media-empty">暂未选择商品视频</div>
          </div>
        </el-form-item>

        <el-form-item label="规格参数">
          <div class="dynamic-list">
            <div v-for="(item, index) in form.specifications" :key="`spec-${index}`" class="spec-row">
              <el-input v-model="item.key" maxlength="100" placeholder="参数名" />
              <el-input v-model="item.value" maxlength="200" placeholder="参数值" />
              <el-button :disabled="form.specifications.length === 1" @click="removeSpecItem(index)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addSpecItem">+ 添加规格</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="mediaPreview.visible"
      class="media-preview-dialog"
      :title="mediaPreview.name || '素材预览'"
      width="760px"
      destroy-on-close
      @closed="resetMediaPreview"
    >
      <div class="media-preview-wrap">
        <img
          v-if="mediaPreview.type === 'image' && mediaPreview.url"
          :src="mediaPreview.url"
          alt="素材预览"
          class="media-preview-image"
        >
        <video
          v-else-if="mediaPreview.type === 'video' && mediaPreview.url"
          :src="mediaPreview.url"
          class="media-preview-video"
          controls
          autoplay
        />
        <div v-else class="media-preview-empty">暂无可预览内容</div>
      </div>
    </el-dialog>

    <input
      ref="mainImageInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleMainImageChange"
    >
    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      multiple
      style="display: none"
      @change="handleImageChange"
    >
    <input
      ref="videoInputRef"
      type="file"
      accept="video/*"
      multiple
      style="display: none"
      @change="handleVideoChange"
    >
  </div>
</template>

<script setup>
/* eslint-disable no-undef */
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createAdminProduct,
  deleteAdminProduct,
  fetchAdminProductCategories,
  fetchAdminProducts,
  updateAdminProduct,
  uploadAdminProductMedia
} from '../../../api/adminProducts';
import { isUploadRelatedError, showUploadErrorDialog, showUploadLimitDialog } from '../../../utils/uploadFeedback';

const props = defineProps({
  productType: {
    type: Number,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
});

const route = useRoute();
const typeKey = computed(() => (props.productType === 2 ? 'second-hand' : 'main'));

const loading = ref(false);
const saving = ref(false);
const rows = ref([]);
const categories = ref([]);
const dialogVisible = ref(false);
const dialogMode = ref('create');
const mainImageInputRef = ref(null);
const imageInputRef = ref(null);
const videoInputRef = ref(null);
const mainImageFileList = ref([]);
const imageFileList = ref([]);
const videoFileList = ref([]);

const filter = reactive({
  keyword: '',
  categoryId: '',
  status: undefined
});

const mediaPreview = reactive({
  visible: false,
  type: 'image',
  url: '',
  name: ''
});

const form = reactive(createEmptyForm());

const categoryCascaderProps = {
  value: 'id',
  label: 'name',
  emitPath: false,
  checkStrictly: true
};

const categoryOptions = computed(() => normalizeCategoryOptions(categories.value));
const onShelfCount = computed(() => rows.value.filter(item => Number(item.status) === 1).length);
const offShelfCount = computed(() => rows.value.filter(item => Number(item.status) === 2).length);
const warningCount = computed(() => rows.value.filter(item => isWarningStock(item)).length);
const mainImageFile = computed(() => mainImageFileList.value[0] || null);

watch(
  () => form.isFreeShipping,
  value => {
    if (value === 1) {
      form.shippingFee = 0;
    }
  }
);

onMounted(() => {
  loadPageData();
  window.addEventListener('admin-page-refresh', handleExternalRefresh);
});

onBeforeUnmount(() => {
  window.removeEventListener('admin-page-refresh', handleExternalRefresh);
  clearAllMediaFiles();
});

async function loadPageData() {
  await Promise.all([loadCategories(), loadProducts()]);
}

async function loadCategories() {
  try {
    const res = await fetchAdminProductCategories();
    if (res.code !== 200) {
      ElMessage.error(res.message || '加载商品分类失败');
      return;
    }
    categories.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载商品分类失败');
  }
}

async function loadProducts() {
  loading.value = true;
  try {
    const res = await fetchAdminProducts(typeKey.value, {
      keyword: filter.keyword || undefined,
      categoryId: filter.categoryId || undefined,
      status: filter.status
    });
    if (res.code !== 200) {
      ElMessage.error(res.message || '加载商品列表失败');
      return;
    }
    rows.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载商品列表失败');
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filter.keyword = '';
  filter.categoryId = '';
  filter.status = undefined;
  loadProducts();
}

function openCreateDialog() {
  dialogMode.value = 'create';
  assignForm(createEmptyForm());
  dialogVisible.value = true;
}

function openEditDialog(row) {
  dialogMode.value = 'edit';
  assignForm({
    id: row.id || '',
    name: row.name || '',
    categoryId: row.categoryId || '',
    brand: row.brand || '',
    model: row.model || '',
    description: row.description || '',
    mainImageUrl: row.mainImageUrl || '',
    imageUrls: sanitizeStringList(row.imageUrls),
    videoUrls: sanitizeStringList(row.videoUrls),
    specifications: ensureSpecValue(row.specifications),
    originalPrice: toNumber(row.originalPrice),
    sellingPrice: toNumber(row.sellingPrice),
    costPrice: toNumber(row.costPrice),
    stockQuantity: row.stockQuantity ?? 0,
    warningStock: row.warningStock ?? 0,
    weight: row.weight == null ? null : toNumber(row.weight),
    dimensions: row.dimensions || '',
    warrantyPeriod: row.warrantyPeriod ?? 0,
    shippingFee: toNumber(row.shippingFee),
    isFreeShipping: row.isFreeShipping ?? 0,
    status: row.status ?? 1,
    isHot: row.isHot ?? 0,
    isNew: row.isNew ?? 0,
    isRecommended: row.isRecommended ?? 0,
    sortOrder: row.sortOrder ?? 0
  });
  dialogVisible.value = true;
}

async function submitForm() {
  const payload = buildPayloadBase();
  if (!payload) {
    return;
  }
  if (!mainImageFile.value) {
    ElMessage.warning('请先选择商品主图');
    return;
  }

  saving.value = true;
  try {
    const [mainImages, images, videos] = await Promise.all([
      uploadMediaFileList(mainImageFileList.value, 'image'),
      uploadMediaFileList(imageFileList.value, 'image'),
      uploadMediaFileList(videoFileList.value, 'video')
    ]);
    payload.mainImageUrl = mainImages[0]?.url || '';
    payload.imageUrls = images.map(item => item.url).filter(Boolean);
    payload.videoUrls = videos.map(item => item.url).filter(Boolean);

    if (!payload.mainImageUrl) {
      showUploadErrorDialog('商品主图上传失败，请重试', '商品主图上传失败，请重试', '上传失败');
      return;
    }

    const res = dialogMode.value === 'create'
      ? await createAdminProduct(typeKey.value, payload)
      : await updateAdminProduct(typeKey.value, form.id, payload);
    if (res.code !== 200) {
      ElMessage.error(res.message || '保存商品失败');
      return;
    }
    ElMessage.success(dialogMode.value === 'create' ? '商品新增成功' : '商品更新成功');
    dialogVisible.value = false;
    await loadProducts();
  } catch (error) {
    const message = getErrorMessage(error) || '保存商品失败';
    if (isUploadRelatedError(error, message)) {
      showUploadErrorDialog(message, message, '上传失败');
    } else {
      ElMessage.error(message);
    }
  } finally {
    saving.value = false;
  }
}

function buildPayloadBase() {
  const payload = {
    name: (form.name || '').trim(),
    categoryId: form.categoryId || '',
    brand: (form.brand || '').trim(),
    model: (form.model || '').trim(),
    description: (form.description || '').trim(),
    specifications: sanitizeSpecifications(form.specifications),
    originalPrice: form.originalPrice,
    sellingPrice: form.sellingPrice,
    costPrice: form.costPrice,
    stockQuantity: form.stockQuantity,
    warningStock: form.warningStock ?? 0,
    weight: form.weight,
    dimensions: (form.dimensions || '').trim(),
    warrantyPeriod: form.warrantyPeriod ?? 0,
    shippingFee: form.isFreeShipping === 1 ? 0 : (form.shippingFee ?? 0),
    isFreeShipping: form.isFreeShipping,
    status: form.status,
    isHot: form.isHot,
    isNew: form.isNew,
    isRecommended: form.isRecommended,
    sortOrder: form.sortOrder ?? 0
  };

  if (!payload.name) {
    ElMessage.warning('请输入商品名称');
    return null;
  }
  if (!payload.categoryId) {
    ElMessage.warning('请选择商品分类');
    return null;
  }
  if (!payload.brand) {
    ElMessage.warning('请输入商品品牌');
    return null;
  }
  if (!payload.model) {
    ElMessage.warning('请输入商品型号');
    return null;
  }
  if (payload.originalPrice == null || payload.sellingPrice == null || payload.costPrice == null) {
    ElMessage.warning('请完整填写价格信息');
    return null;
  }
  if (payload.sellingPrice > payload.originalPrice) {
    ElMessage.warning('售价不能高于原价');
    return null;
  }
  if (payload.stockQuantity == null) {
    ElMessage.warning('请输入库存数量');
    return null;
  }
  return payload;
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除商品“${row.name || row.id}”吗？`, '删除确认', { type: 'warning' });
  } catch (error) {
    return;
  }
  try {
    const res = await deleteAdminProduct(typeKey.value, row.id);
    if (res.code !== 200) {
      ElMessage.error(res.message || '删除商品失败');
      return;
    }
    ElMessage.success('商品删除成功');
    await loadProducts();
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '删除商品失败');
  }
}

function triggerMainImageSelect() {
  triggerFileInput(mainImageInputRef);
}

function triggerImageSelect() {
  triggerFileInput(imageInputRef);
}

function triggerVideoSelect() {
  triggerFileInput(videoInputRef);
}

function triggerFileInput(targetRef) {
  if (!targetRef.value) {
    return;
  }
  targetRef.value.value = '';
  targetRef.value.click();
}

function handleMainImageChange(event) {
  const file = event?.target?.files?.[0];
  if (!file || !beforeImageSelect(file, '主图')) {
    return;
  }
  replaceMainImage(file);
}

function handleImageChange(event) {
  const files = Array.from(event?.target?.files || []);
  if (!files.length) {
    return;
  }
  const nextList = [...imageFileList.value];
  for (const file of files) {
    if (!beforeImageSelect(file, '图片')) {
      continue;
    }
    nextList.push(createLocalUploadFile(file));
  }
  imageFileList.value = nextList;
}

function handleVideoChange(event) {
  const files = Array.from(event?.target?.files || []);
  if (!files.length) {
    return;
  }
  const nextList = [...videoFileList.value];
  for (const file of files) {
    if (!beforeVideoSelect(file)) {
      continue;
    }
    nextList.push(createLocalUploadFile(file));
  }
  videoFileList.value = nextList;
}

function beforeImageSelect(file, label) {
  if (!file?.type || !file.type.startsWith('image/')) {
    showUploadLimitDialog(`请选择${label}图片文件`);
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    showUploadLimitDialog(`${label}大小不能超过 5MB`);
    return false;
  }
  return true;
}

function beforeVideoSelect(file) {
  if (!file?.type || !file.type.startsWith('video/')) {
    showUploadLimitDialog('请选择视频文件');
    return false;
  }
  if (file.size > 30 * 1024 * 1024) {
    showUploadLimitDialog('视频大小不能超过 30MB');
    return false;
  }
  return true;
}

function replaceMainImage(file) {
  clearMainImage();
  mainImageFileList.value = [createLocalUploadFile(file)];
}

function removeMainImage() {
  clearMainImage();
}

function removeImageFile(uid) {
  const target = imageFileList.value.find(item => item.uid === uid);
  revokeLocalPreviewUrl(target);
  imageFileList.value = imageFileList.value.filter(item => item.uid !== uid);
  closePreviewIfTargetRemoved(target);
}

function removeVideoFile(uid) {
  const target = videoFileList.value.find(item => item.uid === uid);
  revokeLocalPreviewUrl(target);
  videoFileList.value = videoFileList.value.filter(item => item.uid !== uid);
  closePreviewIfTargetRemoved(target);
}

function clearMainImage() {
  if (mainImageFile.value) {
    revokeLocalPreviewUrl(mainImageFile.value);
  }
  closePreviewIfTargetRemoved(mainImageFile.value);
  mainImageFileList.value = [];
}

function clearImageFiles() {
  imageFileList.value.forEach(revokeLocalPreviewUrl);
  if (mediaPreview.visible && imageFileList.value.some(item => resolveMediaUrl(item) === mediaPreview.url)) {
    resetMediaPreview();
  }
  imageFileList.value = [];
}

function clearVideoFiles() {
  videoFileList.value.forEach(revokeLocalPreviewUrl);
  if (mediaPreview.visible && videoFileList.value.some(item => resolveMediaUrl(item) === mediaPreview.url)) {
    resetMediaPreview();
  }
  videoFileList.value = [];
}

function clearAllMediaFiles() {
  clearMainImage();
  clearImageFiles();
  clearVideoFiles();
}

function closePreviewIfTargetRemoved(target) {
  if (!target) {
    return;
  }
  if (mediaPreview.visible && mediaPreview.url === resolveMediaUrl(target)) {
    resetMediaPreview();
  }
}

function openMediaPreview(file, mediaType) {
  const previewUrl = resolveMediaUrl(file);
  if (!previewUrl) {
    ElMessage.warning('当前素材暂无可预览地址');
    return;
  }
  mediaPreview.type = mediaType || 'image';
  mediaPreview.url = previewUrl;
  mediaPreview.name = file?.name || '';
  mediaPreview.visible = true;
}

function resetMediaPreview() {
  mediaPreview.visible = false;
  mediaPreview.type = 'image';
  mediaPreview.url = '';
  mediaPreview.name = '';
}

async function uploadMediaFileList(fileList, mediaType) {
  const tasks = (Array.isArray(fileList) ? fileList : []).map(file => uploadSingleMediaFile(file, mediaType));
  return Promise.all(tasks);
}

async function uploadSingleMediaFile(file, mediaType) {
  if (file?.responseData?.url) {
    return file.responseData;
  }
  if (!file?.rawFile) {
    const resolved = {
      url: resolveMediaUrl(file),
      name: file?.name || '',
      mediaType
    };
    file.responseData = resolved;
    return resolved;
  }
  const res = await uploadAdminProductMedia(file.rawFile, mediaType);
  if (res?.code !== 200 || !res.data?.url) {
    throw new Error(res?.message || '上传商品素材失败');
  }
  file.responseData = res.data;
  file.url = res.data.url;
  revokeLocalPreviewUrl(file);
  file.previewUrl = '';
  return res.data;
}

function assignForm(nextValue) {
  clearAllMediaFiles();
  Object.assign(form, createEmptyForm(), nextValue);
  mainImageFileList.value = buildMediaFileList(nextValue.mainImageUrl ? [nextValue.mainImageUrl] : [], 'image');
  imageFileList.value = buildMediaFileList(nextValue.imageUrls, 'image');
  videoFileList.value = buildMediaFileList(nextValue.videoUrls, 'video');
}

function buildMediaFileList(urlList, mediaType) {
  return sanitizeStringList(urlList).map(url => createRemoteUploadFile(url, mediaType));
}

function createRemoteUploadFile(url, mediaType) {
  const name = extractFileName(url);
  return {
    uid: `remote_${Date.now()}_${Math.random().toString(16).slice(2)}`,
    name,
    status: 'success',
    url,
    previewUrl: '',
    rawFile: null,
    responseData: {
      url,
      name,
      mediaType
    }
  };
}

function createLocalUploadFile(rawFile) {
  const previewUrl = URL.createObjectURL(rawFile);
  return {
    uid: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    name: rawFile.name,
    status: 'success',
    url: previewUrl,
    previewUrl,
    rawFile,
    responseData: null
  };
}

function revokeLocalPreviewUrl(file) {
  if (file?.previewUrl) {
    URL.revokeObjectURL(file.previewUrl);
  }
}

function resolveMediaUrl(file) {
  return file?.url || file?.previewUrl || file?.responseData?.url || '';
}

function extractFileName(url) {
  const value = url == null ? '' : String(url).trim();
  if (!value) {
    return '素材文件';
  }
  const cleanUrl = value.split('?')[0];
  const index = cleanUrl.lastIndexOf('/');
  return index >= 0 ? cleanUrl.slice(index + 1) || '素材文件' : cleanUrl;
}

function addSpecItem() {
  form.specifications.push({ key: '', value: '' });
}

function removeSpecItem(index) {
  if (form.specifications.length === 1) {
    form.specifications[0] = { key: '', value: '' };
    return;
  }
  form.specifications.splice(index, 1);
}

function handleDialogClosed() {
  resetMediaPreview();
}

async function handleExternalRefresh(event) {
  if (event?.detail?.path !== route.path) {
    return;
  }
  event.detail.handled = true;
  await loadPageData();
  ElMessage.success('刷新成功');
}

function createEmptyForm() {
  return {
    id: '',
    name: '',
    categoryId: '',
    brand: '',
    model: '',
    description: '',
    mainImageUrl: '',
    imageUrls: [],
    videoUrls: [],
    specifications: [{ key: '', value: '' }],
    originalPrice: 0,
    sellingPrice: 0,
    costPrice: 0,
    stockQuantity: 0,
    warningStock: 0,
    weight: null,
    dimensions: '',
    warrantyPeriod: 0,
    shippingFee: 0,
    isFreeShipping: 0,
    status: 1,
    isHot: 0,
    isNew: 0,
    isRecommended: 0,
    sortOrder: 0
  };
}

function normalizeCategoryOptions(nodes) {
  return (nodes || []).map(item => ({
    id: item.id,
    name: item.name,
    children: normalizeCategoryOptions(item.children || [])
  }));
}

function ensureSpecValue(value) {
  const list = sanitizeSpecifications(value);
  return list.length ? list : [{ key: '', value: '' }];
}

function sanitizeStringList(list) {
  return (Array.isArray(list) ? list : [])
    .map(item => (item == null ? '' : String(item).trim()))
    .filter(Boolean);
}

function sanitizeSpecifications(list) {
  return (Array.isArray(list) ? list : [])
    .map(item => ({
      key: item?.key == null ? '' : String(item.key).trim(),
      value: item?.value == null ? '' : String(item.value).trim()
    }))
    .filter(item => item.key || item.value);
}

function toNumber(value) {
  if (value == null || value === '') {
    return 0;
  }
  return Number(value);
}

function formatMoney(value) {
  if (value == null || value === '') {
    return '-';
  }
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return String(value);
  }
  return `¥${amount.toFixed(2)}`;
}

function formatStatus(status) {
  if (Number(status) === 1) return '上架';
  if (Number(status) === 2) return '下架';
  if (Number(status) === 3) return '缺货';
  return '未知';
}

function statusTagType(status) {
  if (Number(status) === 1) return 'success';
  if (Number(status) === 2) return 'info';
  if (Number(status) === 3) return 'warning';
  return '';
}

function formatTime(timestamp) {
  if (!timestamp) {
    return '-';
  }
  const date = new Date(Number(timestamp));
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function isWarningStock(item) {
  const stock = Number(item?.stockQuantity ?? 0);
  const warning = Number(item?.warningStock ?? 0);
  return warning > 0 && stock <= warning;
}

function getErrorMessage(error) {
  if (typeof error?.response?.data?.message === 'string' && error.response.data.message) {
    return error.response.data.message;
  }
  if (typeof error?.message === 'string' && error.message) {
    return error.message;
  }
  return '';
}
</script>

<style scoped>
.product-manage-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-card,
.panel-card,
.summary-card {
  border-radius: 14px;
}

.hero-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.hero-desc {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-label {
  color: #6b7280;
  font-size: 13px;
}

.summary-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.main-table {
  width: 100%;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  flex-shrink: 0;
}

.product-meta {
  min-width: 0;
}

.product-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.product-name {
  font-weight: 600;
  color: #111827;
}

.product-subline,
.price-subline,
.stock-subline {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.price-line {
  font-weight: 600;
  color: #111827;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-form {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.dynamic-list,
.media-field {
  width: 100%;
}

.media-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.media-toolbar-left {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.upload-tip {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.6;
}

.media-empty {
  padding: 18px 16px;
  border: 1px dashed #d1d5db;
  border-radius: 12px;
  color: #6b7280;
  background: #f9fafb;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}

.media-grid-main {
  grid-template-columns: minmax(0, 240px);
}

.media-card {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.media-card-main {
  max-width: 240px;
}

.media-image,
.media-video {
  display: block;
  width: 100%;
  height: 148px;
  object-fit: cover;
  background: #111827;
}

.media-image-main {
  height: 220px;
}

.media-card-footer {
  padding: 10px 12px 12px;
}

.media-card-name {
  color: #111827;
  font-size: 13px;
  font-weight: 500;
  word-break: break-all;
}

.media-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.spec-row {
  display: grid;
  grid-template-columns: 180px 1fr auto;
  gap: 8px;
  margin-bottom: 8px;
}

.media-preview-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
}

.media-preview-image,
.media-preview-video {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 12px;
  background: #000;
}

.media-preview-empty {
  color: #6b7280;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-filters {
    width: 100%;
  }

  .toolbar-filters :deep(.el-input),
  .toolbar-filters :deep(.el-cascader),
  .toolbar-filters :deep(.el-select) {
    width: 100% !important;
  }

  .media-grid,
  .media-grid-main,
  .spec-row {
    grid-template-columns: 1fr;
  }

  .media-card-main {
    max-width: none;
  }
}
</style>
