<template>
  <div class="offline-order-page" v-loading="pageLoading">
    <el-card class="page-card intro-card" shadow="never">
      <div class="page-header">
        <div>
          <div class="page-title">线下订单代录</div>
          <div class="page-subtitle">按步骤完成用户选择、服务分配、故障补充与订单确认，减少管理员误操作。</div>
        </div>
        <el-tag type="success" effect="plain">流程录入</el-tag>
      </div>
    </el-card>

    <el-card class="page-card steps-card" shadow="never">
      <div class="steps-wrap">
        <el-steps :active="activeStep" finish-status="success" simple>
          <el-step title="选择用户" description="确认申请账号" />
          <el-step title="服务分配" description="分类、服务、师傅" />
          <el-step title="故障材料" description="现象、图片、视频" />
          <el-step title="确认提交" description="设备信息与备注" />
        </el-steps>
      </div>
    </el-card>

    <el-card class="page-card content-card" shadow="never">
      <template v-if="activeStep === 0">
        <div class="section-head">
          <div>
            <div class="section-title">第一步：选择申请用户</div>
            <div class="section-desc">管理员代顾客提交申请时，先明确当前订单归属的用户账号。</div>
          </div>
          <div class="section-side-text">已选用户：{{ selectedUser ? (selectedUser.username || selectedUser.phone || '-') : '未选择' }}</div>
        </div>

        <div class="toolbar">
          <el-input
            v-model="userKeyword"
            clearable
            class="toolbar-search"
            placeholder="搜索昵称、手机号或邮箱"
            @keyup.enter="handleUserSearch"
            @clear="handleUserSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" :loading="userLoading" @click="handleUserSearch">查询用户</el-button>
        </div>

        <div v-if="selectedUser" class="selected-user-panel">
          <div class="selected-user-label">当前已选</div>
          <div class="selected-user-main">
            <el-avatar :size="52" :src="selectedUser.avatarUrl">
              {{ getUserInitial(selectedUser) }}
            </el-avatar>
            <div class="selected-user-info">
              <div class="selected-user-name">{{ selectedUser.username || '-' }}</div>
              <div class="selected-user-meta">
                <span>{{ selectedUser.phone || '-' }}</span>
                <span>{{ selectedUser.email || '-' }}</span>
                <span>{{ getUserStatusText(selectedUser.status) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="user-grid" v-loading="userLoading">
          <button
            v-for="item in userList"
            :key="item.id"
            type="button"
            class="user-card"
            :class="{ 'is-active': form.userId === item.id }"
            :disabled="Number(item.status) !== 1"
            @click="selectUser(item)"
          >
            <div class="user-card-top">
              <el-avatar :size="42" :src="item.avatarUrl">
                {{ getUserInitial(item) }}
              </el-avatar>
              <div class="user-card-title">
                <div class="user-name">{{ item.username || '-' }}</div>
                <div class="user-phone">{{ item.phone || item.email || '-' }}</div>
              </div>
              <el-tag size="small" :type="form.userId === item.id ? 'success' : 'info'">
                {{ form.userId === item.id ? '已选择' : (Number(item.status) === 1 ? '可选择' : '不可用') }}
              </el-tag>
            </div>
            <div class="user-card-bottom">
              <span>实名：{{ item.realName || '未填写' }}</span>
              <span>认证：{{ item.isVerified === 1 ? '已实名' : '未实名' }}</span>
            </div>
          </button>
        </div>

        <el-empty v-if="!userLoading && !userList.length" description="暂无可选用户" />

        <div class="pagination-wrap">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="userTotal"
            :page-size="userPageSize"
            :current-page="userPage"
            @current-change="handleUserPageChange"
          />
        </div>
      </template>

      <template v-else-if="activeStep === 1">
        <div class="section-head">
          <div>
            <div class="section-title">第二步：选择服务分类、服务类型与师傅</div>
            <div class="section-desc">先定位服务分类，再选择线下维修服务类型，并给本次订单指派合适的维修师傅。</div>
          </div>
          <div class="section-side-text">
            {{ selectedServiceType ? `已选服务：${selectedServiceType.name}` : '请先选择服务类型' }}
          </div>
        </div>

        <div class="service-layout">
          <aside class="category-panel">
            <div class="panel-title-row">
              <span class="panel-title">服务分类</span>
              <el-button text @click="clearCategoryFilter">全部分类</el-button>
            </div>
            <div
              class="all-category-entry"
              :class="{ 'is-active': !selectedCategoryId }"
              @click="clearCategoryFilter"
            >
              全部分类
            </div>
            <el-tree
              :data="categoryTree"
              node-key="id"
              :props="categoryTreeProps"
              class="category-tree"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              empty-text="暂无分类"
              @node-click="handleCategoryNodeClick"
            />
          </aside>

          <div class="service-main">
            <div class="service-types-section">
              <div class="panel-title-row">
                <span class="panel-title">线下维修服务类型</span>
                <span class="panel-hint">仅展示已启用的线下维修项目</span>
              </div>
              <div class="service-type-grid">
                <button
                  v-for="item in filteredOfflineServiceTypes"
                  :key="item.id"
                  type="button"
                  class="service-type-card"
                  :class="{ 'is-active': form.serviceTypeId === item.id }"
                  @click="selectServiceType(item)"
                >
                  <div class="service-type-name">{{ item.name }}</div>
                  <div class="service-type-path">{{ resolveCategoryPath(item.categoryId) || item.categoryName || '-' }}</div>
                  <div class="service-type-foot">
                    <span>基础价：{{ formatPrice(item.basePrice) }}</span>
                    <el-tag size="small" :type="form.serviceTypeId === item.id ? 'success' : 'info'">
                      {{ form.serviceTypeId === item.id ? '已选择' : '可选' }}
                    </el-tag>
                  </div>
                </button>
              </div>
              <el-empty
                v-if="!filteredOfflineServiceTypes.length"
                description="当前分类下暂无已启用的线下维修服务类型"
              />
            </div>

            <div class="technician-section">
              <div class="panel-title-row">
                <span class="panel-title">服务师傅</span>
                <span class="panel-hint">师傅卡片已与搜索区分开，避免重叠</span>
              </div>

              <div class="technician-toolbar">
                <el-input
                  v-model="technicianKeyword"
                  clearable
                  class="toolbar-search"
                  placeholder="搜索师傅姓名、手机号或邮箱"
                  :disabled="!form.serviceTypeId"
                  @keyup.enter="handleTechnicianSearch"
                  @clear="handleTechnicianSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button
                  type="primary"
                  :disabled="!form.serviceTypeId"
                  :loading="technicianLoading"
                  @click="handleTechnicianSearch"
                >
                  查询师傅
                </el-button>
              </div>

              <div v-if="!form.serviceTypeId" class="placeholder-panel">
                请先选择服务类型，再筛选可接单师傅。
              </div>

              <div v-else class="technician-grid" v-loading="technicianLoading">
                <button
                  v-for="item in technicianList"
                  :key="item.id"
                  type="button"
                  class="technician-card"
                  :class="{ 'is-active': form.technicianId === item.id }"
                  @click="selectTechnician(item)"
                >
                  <div class="technician-card-head">
                    <el-avatar :size="46" :src="item.avatarUrl">
                      {{ getTechnicianInitial(item) }}
                    </el-avatar>
                    <div class="technician-card-title">
                      <div class="technician-name">{{ item.name || '-' }}</div>
                      <div class="technician-contact">{{ item.phone || item.email || '-' }}</div>
                    </div>
                    <el-tag size="small" :type="getTechnicianTagType(item.workStatus)">
                      {{ item.workStatusText || '-' }}
                    </el-tag>
                  </div>
                  <div class="technician-card-meta">
                    <span>评分：{{ item.rating || '0.0' }}</span>
                    <span>接单：{{ item.orderCount || 0 }}</span>
                  </div>
                </button>
              </div>

              <el-empty
                v-if="form.serviceTypeId && !technicianLoading && !technicianList.length"
                description="当前服务类型暂无可分配师傅"
              />
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="activeStep === 2">
        <div class="section-head">
          <div>
            <div class="section-title">第三步：填写故障现象并补充图片/视频</div>
            <div class="section-desc">每个故障现象支持上传最多 5 张图片和 1 段视频，便于师傅提前判断问题。</div>
          </div>
          <div class="section-side-text">
            已选故障：{{ selectedFaultOptions.length }} 项
          </div>
        </div>

        <div class="fault-select-panel">
          <div class="panel-title-row">
            <span class="panel-title">可选故障现象</span>
            <span class="panel-hint">
              当前服务类型：{{ selectedServiceType ? selectedServiceType.name : '-' }}
            </span>
          </div>
          <div class="fault-grid">
            <label
              v-for="item in availableFaultOptions"
              :key="item.id"
              class="fault-option-card"
              :class="{ 'is-active': selectedFaultIds.includes(item.id) }"
            >
              <input
                :checked="selectedFaultIds.includes(item.id)"
                type="checkbox"
                class="fault-checkbox"
                @change="toggleFaultSelection(item, $event.target.checked)"
              />
              <div class="fault-option-main">
                <div class="fault-option-name">{{ item.name }}</div>
                <div class="fault-option-desc">{{ item.description || '未配置说明' }}</div>
                <div class="fault-option-price">{{ formatFaultPrice(item) }}</div>
              </div>
              <el-tag size="small" :type="selectedFaultIds.includes(item.id) ? 'success' : 'info'">
                {{ selectedFaultIds.includes(item.id) ? '已选择' : '未选择' }}
              </el-tag>
            </label>
          </div>
          <el-empty v-if="!availableFaultOptions.length" description="当前服务类型暂无已启用故障现象" />
        </div>

        <div v-if="selectedFaultOptions.length" class="fault-media-list">
          <div
            v-for="item in selectedFaultOptions"
            :key="item.id"
            class="fault-media-card"
          >
            <div class="fault-media-head">
              <div>
                <div class="fault-media-title">{{ item.name }}</div>
                <div class="fault-media-subtitle">{{ item.description || '可补充更详细的故障描述和现场材料。' }}</div>
              </div>
              <el-tag type="success" effect="plain">已选择</el-tag>
            </div>

            <el-form label-width="98px" class="fault-media-form">
              <el-form-item label="补充描述">
                <el-input
                  v-model="faultDescriptionMap[item.id]"
                  type="textarea"
                  :rows="3"
                  maxlength="200"
                  show-word-limit
                  placeholder="可补充顾客描述、故障表现、已尝试处理方式等"
                />
              </el-form-item>

              <el-form-item label="故障图片">
                <div class="upload-block">
                  <div class="upload-tip">最多 5 张，单张不超过 5MB，先暂存在前端，提交订单时统一上传到系统</div>
                  <el-upload
                    :file-list="getImageFileList(item.id)"
                    list-type="picture-card"
                    accept="image/*"
                    :limit="5"
                    :multiple="true"
                    :show-file-list="true"
                    :http-request="options => handleImageUpload(item.id, options)"
                    :before-upload="file => beforeImageUpload(item.id, file)"
                    :on-preview="file => handleMediaPreview(file, 'image')"
                    :on-remove="file => handleImageRemove(item.id, file)"
                    :on-exceed="() => handleImageExceed()"
                  >
                    <el-icon><Picture /></el-icon>
                  </el-upload>
                </div>
              </el-form-item>

              <el-form-item label="故障视频">
                <div class="upload-block">
                  <div class="upload-tip">最多 1 段，视频不超过 30MB，先暂存在前端，提交订单时统一上传到系统</div>
                  <el-upload
                    :file-list="getVideoFileList(item.id)"
                    accept="video/*"
                    :limit="1"
                    :multiple="false"
                    :show-file-list="false"
                    :http-request="options => handleVideoUpload(item.id, options)"
                    :before-upload="file => beforeVideoUpload(item.id, file)"
                    :on-preview="file => handleMediaPreview(file, 'video')"
                    :on-remove="file => handleVideoRemove(item.id, file)"
                    :on-exceed="() => handleVideoExceed()"
                  >
                    <el-button plain>
                      <el-icon><VideoPlay /></el-icon>
                      上传视频
                    </el-button>
                  </el-upload>
                  <div v-if="getVideoFileList(item.id).length" class="video-preview-list">
                    <div
                      v-for="file in getVideoFileList(item.id)"
                      :key="file.uid"
                      class="video-preview-card"
                    >
                      <button
                        type="button"
                        class="video-delete-button"
                        title="删除视频"
                        @click.stop.prevent="removeVideoByUid(item.id, file.uid)"
                      >
                        <el-icon><Delete /></el-icon>
                      </button>
                      <video :src="file.url" controls preload="metadata" class="video-preview-player" />
                      <div class="video-preview-foot">
                        <span class="video-preview-name">{{ file.name }}</span>
                        <el-button link type="primary" @click="handleMediaPreview(file, 'video')">放大预览</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <el-empty v-else description="请至少选择一个故障现象" />
      </template>

      <template v-else>
        <div class="section-head">
          <div>
            <div class="section-title">第四步：确认订单信息并提交</div>
            <div class="section-desc">补充设备资料和备注，确认无误后提交线下代录订单。</div>
          </div>
          <div class="section-side-text">提交后将按正常申请流程进入订单流转</div>
        </div>

        <div class="summary-grid">
          <div class="summary-card">
            <div class="summary-label">申请用户</div>
            <div class="summary-value">{{ selectedUser ? (selectedUser.username || '-') : '-' }}</div>
            <div class="summary-meta">{{ selectedUser ? (selectedUser.phone || selectedUser.email || '-') : '未选择' }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">服务类型</div>
            <div class="summary-value">{{ selectedServiceType ? selectedServiceType.name : '-' }}</div>
            <div class="summary-meta">{{ selectedServiceType ? (resolveCategoryPath(selectedServiceType.categoryId) || '-') : '未选择' }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">服务师傅</div>
            <div class="summary-value">{{ selectedTechnician ? (selectedTechnician.name || '-') : '-' }}</div>
            <div class="summary-meta">{{ selectedTechnician ? (selectedTechnician.phone || selectedTechnician.email || '-') : '未选择' }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">故障现象</div>
            <div class="summary-value">{{ selectedFaultOptions.length }} 项</div>
            <div class="summary-meta">{{ selectedFaultOptions.map(item => item.name).join('、') || '未选择' }}</div>
          </div>
        </div>

        <el-form label-width="110px" class="confirm-form">
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="设备品牌">
                <el-input v-model="form.applianceBrand" maxlength="50" placeholder="例如：美的、海尔、格力" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="设备型号">
                <el-input v-model="form.applianceModel" maxlength="50" placeholder="请输入设备型号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="购买日期">
                <el-date-picker
                  v-model="form.purchaseDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择购买日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="24">
              <el-form-item label="管理员备注">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="4"
                  maxlength="300"
                  show-word-limit
                  placeholder="可填写线下沟通记录、设备状态、到店说明等"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </template>
    </el-card>

    <div class="step-footer">
      <div class="step-footer-tip">
        <span class="tip-strong">当前步骤：</span>
        <span>{{ stepLabels[activeStep] }}</span>
      </div>
      <div class="step-footer-actions">
        <el-button v-if="activeStep > 0" @click="goPrevStep">上一步</el-button>
        <el-button v-if="activeStep < stepLabels.length - 1" type="primary" @click="goNextStep">
          下一步
          <el-icon class="button-icon"><ArrowRight /></el-icon>
        </el-button>
        <el-button v-else type="primary" :loading="submitting" @click="submitOrder">
          确认提交
        </el-button>
      </div>
    </div>

    <el-dialog
      v-model="mediaPreview.visible"
      class="media-preview-dialog"
      :title="mediaPreview.name || '附件预览'"
      width="760px"
      append-to-body
      @closed="resetMediaPreview"
    >
      <div class="media-preview-wrap">
        <img
          v-if="mediaPreview.type === 'image' && mediaPreview.url"
          :src="mediaPreview.url"
          alt="preview"
          class="media-preview-image"
        />
        <video
          v-else-if="mediaPreview.type === 'video' && mediaPreview.url"
          :src="mediaPreview.url"
          controls
          autoplay
          preload="metadata"
          class="media-preview-video"
        />
        <div v-else class="media-preview-empty">暂无可预览内容</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowRight, Delete, Picture, Search, VideoPlay } from '@element-plus/icons-vue';
import { fetchAdminUserList } from '../../api/adminUsers';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import { isUploadRelatedError, showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';
import {
  createAdminOfflineOrder,
  fetchAdminOfflineOrderTechnicians,
  uploadAdminOfflineOrderMedia
} from '../../api/adminOrders';
import {
  fetchFaultPhenomena,
  fetchServiceCategories,
  fetchServiceTypes
} from '../../api/adminServiceConfig';

const activeStep = ref(0);
const pageLoading = ref(false);
const userLoading = ref(false);
const technicianLoading = ref(false);
const submitting = ref(false);

const stepLabels = ['选择用户', '服务分配', '故障材料', '确认提交'];

const form = reactive({
  userId: '',
  categoryId: '',
  serviceTypeId: '',
  technicianId: '',
  applianceBrand: '',
  applianceModel: '',
  purchaseDate: '',
  remark: ''
});

const userKeyword = ref('');
const userList = ref([]);
const userPage = ref(1);
const userPageSize = ref(8);
const userTotal = ref(0);
const userMap = reactive({});

const categoryList = ref([]);
const serviceTypeList = ref([]);
const faultList = ref([]);

const selectedCategoryId = ref('');

const technicianKeyword = ref('');
const technicianList = ref([]);
const technicianMap = reactive({});

const selectedFaultIds = ref([]);
const faultDescriptionMap = reactive({});
const faultMediaMap = reactive({});
const mediaPreview = reactive({
  visible: false,
  type: 'image',
  url: '',
  name: ''
});

const categoryTreeProps = {
  label: 'name',
  children: 'children'
};

const categoryTree = computed(() => buildCategoryTree(categoryList.value));

const categoryPathMap = computed(() => {
  const map = {};
  buildCategoryPathMap(categoryTree.value, [], map);
  return map;
});

const offlineServiceTypes = computed(() => {
  return (serviceTypeList.value || []).filter(item => Number(item.type) === 3 && Number(item.isActive) === 1);
});

const filteredOfflineServiceTypes = computed(() => {
  const filterId = selectedCategoryId.value;
  if (!filterId) {
    return offlineServiceTypes.value;
  }
  return offlineServiceTypes.value.filter(item => matchesCategory(item.categoryId, filterId, categoryList.value));
});

const availableFaultOptions = computed(() => {
  return (faultList.value || []).filter(item => (
    item.serviceTypeId === form.serviceTypeId && Number(item.isActive) === 1
  ));
});

const selectedFaultOptions = computed(() => {
  const optionMap = {};
  availableFaultOptions.value.forEach(item => {
    optionMap[item.id] = item;
  });
  return selectedFaultIds.value
    .map(id => optionMap[id])
    .filter(Boolean);
});

const selectedUser = computed(() => userMap[form.userId] || null);
const selectedServiceType = computed(() => (
  offlineServiceTypes.value.find(item => item.id === form.serviceTypeId) || null
));
const selectedTechnician = computed(() => technicianMap[form.technicianId] || null);

async function loadPageData() {
  pageLoading.value = true;
  try {
    const [categoryRes, typeRes, faultRes] = await Promise.all([
      fetchServiceCategories(),
      fetchServiceTypes(),
      fetchFaultPhenomena()
    ]);

    categoryList.value = categoryRes?.code === 200 && Array.isArray(categoryRes.data) ? categoryRes.data : [];
    serviceTypeList.value = typeRes?.code === 200 && Array.isArray(typeRes.data) ? typeRes.data : [];
    faultList.value = faultRes?.code === 200 && Array.isArray(faultRes.data) ? faultRes.data : [];

    await loadUsers();
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '初始化线下订单页面失败');
  } finally {
    pageLoading.value = false;
  }
}

async function loadUsers() {
  userLoading.value = true;
  try {
    const params = {
      pageNum: userPage.value,
      pageSize: userPageSize.value
    };
    if (userKeyword.value && userKeyword.value.trim()) {
      params.keyword = userKeyword.value.trim();
    }
    const res = await fetchAdminUserList(params);
    if (res?.code === 200 && res.data) {
      const data = res.data;
      const records = data.records || data.list || [];
      userList.value = records;
      userTotal.value = data.total || 0;
      records.forEach(item => {
        if (item?.id) {
          userMap[item.id] = item;
        }
      });
      return;
    }
    throw new Error(res?.message || '获取用户列表失败');
  } catch (error) {
    userList.value = [];
    userTotal.value = 0;
    ElMessage.error(getErrorMessage(error) || '获取用户列表失败');
  } finally {
    userLoading.value = false;
  }
}

async function loadTechnicians() {
  if (!form.serviceTypeId) {
    technicianList.value = [];
    return;
  }
  technicianLoading.value = true;
  try {
    const params = {
      serviceTypeId: form.serviceTypeId
    };
    if (technicianKeyword.value && technicianKeyword.value.trim()) {
      params.keyword = technicianKeyword.value.trim();
    }
    const res = await fetchAdminOfflineOrderTechnicians(params);
    if (res?.code === 200 && Array.isArray(res.data)) {
      technicianList.value = res.data;
      res.data.forEach(item => {
        if (item?.id) {
          technicianMap[item.id] = item;
        }
      });
      return;
    }
    throw new Error(res?.message || '获取师傅列表失败');
  } catch (error) {
    technicianList.value = [];
    ElMessage.error(getErrorMessage(error) || '获取师傅列表失败');
  } finally {
    technicianLoading.value = false;
  }
}

function handleUserSearch() {
  userPage.value = 1;
  loadUsers();
}

function handleUserPageChange(page) {
  userPage.value = page;
  loadUsers();
}

function selectUser(user) {
  if (!user?.id || Number(user.status) !== 1) {
    ElMessage.warning('只能选择状态正常的用户');
    return;
  }
  form.userId = user.id;
  userMap[user.id] = user;
}

function handleCategoryNodeClick(node) {
  selectedCategoryId.value = node?.id || '';
}

function clearCategoryFilter() {
  selectedCategoryId.value = '';
}

async function selectServiceType(serviceType) {
  if (!serviceType?.id) {
    return;
  }
  const changed = form.serviceTypeId !== serviceType.id;
  form.serviceTypeId = serviceType.id;
  form.categoryId = serviceType.categoryId || '';

  if (changed) {
    form.technicianId = '';
    technicianKeyword.value = '';
    technicianList.value = [];
    selectedFaultIds.value = [];
    clearFaultExtraData();
    await loadTechnicians();
  }
}

function selectTechnician(technician) {
  if (!technician?.id) {
    return;
  }
  form.technicianId = technician.id;
  technicianMap[technician.id] = technician;
}

function handleTechnicianSearch() {
  loadTechnicians();
}

function clearFaultExtraData() {
  Object.values(faultMediaMap).forEach(state => {
    (state?.images || []).forEach(file => revokeLocalPreviewUrl(file));
    (state?.videos || []).forEach(file => revokeLocalPreviewUrl(file));
  });
  Object.keys(faultDescriptionMap).forEach(key => {
    delete faultDescriptionMap[key];
  });
  Object.keys(faultMediaMap).forEach(key => {
    delete faultMediaMap[key];
  });
}

function ensureFaultMediaState(faultId) {
  if (!faultMediaMap[faultId]) {
    faultMediaMap[faultId] = {
      images: [],
      videos: []
    };
  }
  return faultMediaMap[faultId];
}

function toggleFaultSelection(item, checked) {
  if (!item?.id) {
    return;
  }
  if (checked) {
    if (!selectedFaultIds.value.includes(item.id)) {
      selectedFaultIds.value = [...selectedFaultIds.value, item.id];
    }
    ensureFaultMediaState(item.id);
    if (faultDescriptionMap[item.id] == null) {
      faultDescriptionMap[item.id] = '';
    }
    return;
  }

  selectedFaultIds.value = selectedFaultIds.value.filter(id => id !== item.id);
  (ensureFaultMediaState(item.id).images || []).forEach(file => revokeLocalPreviewUrl(file));
  (ensureFaultMediaState(item.id).videos || []).forEach(file => revokeLocalPreviewUrl(file));
  delete faultDescriptionMap[item.id];
  delete faultMediaMap[item.id];
}

function getImageFileList(faultId) {
  return ensureFaultMediaState(faultId).images;
}

function getVideoFileList(faultId) {
  return ensureFaultMediaState(faultId).videos;
}

function beforeImageUpload(faultId, file) {
  if (!file?.type || !file.type.startsWith('image/')) {
    showUploadLimitDialog('请上传图片文件');
    return false;
  }
  if (getImageFileList(faultId).length >= 5) {
    ElMessage.warning('每个故障现象最多上传 5 张图片');
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    showUploadLimitDialog('单张图片不能超过 5MB');
    return false;
  }
  return true;
}

function beforeVideoUpload(faultId, file) {
  if (!file?.type || !file.type.startsWith('video/')) {
    showUploadLimitDialog('请上传视频文件');
    return false;
  }
  if (getVideoFileList(faultId).length >= 1) {
    ElMessage.warning('每个故障现象最多上传 1 段视频');
    return false;
  }
  if (file.size > 30 * 1024 * 1024) {
    showUploadLimitDialog('视频大小不能超过 30MB');
    return false;
  }
  return true;
}

function handleImageExceed() {
  ElMessage.warning('每个故障现象最多上传 5 张图片');
}

function handleVideoExceed() {
  ElMessage.warning('每个故障现象最多上传 1 段视频');
}

function handleMediaPreview(file, mediaType) {
  const previewUrl = file?.url || file?.previewUrl || file?.responseData?.url || '';
  if (!previewUrl) {
    ElMessage.warning('当前文件暂无可预览地址');
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

async function handleImageUpload(faultId, options) {
  try {
    const fileItem = createLocalUploadFile(options.file);
    ensureFaultMediaState(faultId).images = [...getImageFileList(faultId), fileItem];
    options.onSuccess?.({ cached: true });
    ElMessage.success('图片已暂存在前端，提交订单时再上传');
  } catch (error) {
    options.onError?.(error);
    ElMessage.error(getErrorMessage(error) || '暂存图片失败');
  }
}

async function handleVideoUpload(faultId, options) {
  try {
    const fileItem = createLocalUploadFile(options.file);
    ensureFaultMediaState(faultId).videos = [fileItem];
    options.onSuccess?.({ cached: true });
    ElMessage.success('视频已暂存在前端，提交订单时再上传');
  } catch (error) {
    options.onError?.(error);
    ElMessage.error(getErrorMessage(error) || '暂存视频失败');
  }
}

function handleImageRemove(faultId, file) {
  const target = getImageFileList(faultId).find(item => item.uid === file.uid);
  revokeLocalPreviewUrl(target);
  ensureFaultMediaState(faultId).images = getImageFileList(faultId).filter(item => item.uid !== file.uid);
}

function handleVideoRemove(faultId, file) {
  const target = getVideoFileList(faultId).find(item => item.uid === file.uid);
  revokeLocalPreviewUrl(target);
  ensureFaultMediaState(faultId).videos = getVideoFileList(faultId).filter(item => item.uid !== file.uid);
}

function removeVideoByUid(faultId, uid) {
  const target = getVideoFileList(faultId).find(item => item.uid === uid);
  if (!target) {
    return;
  }
  revokeLocalPreviewUrl(target);
  ensureFaultMediaState(faultId).videos = getVideoFileList(faultId).filter(item => item.uid !== uid);
  if (mediaPreview.visible && mediaPreview.url === target.url) {
    resetMediaPreview();
  }
  ElMessage.success('视频已删除');
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

async function uploadCachedFile(file, mediaType) {
  if (file?.responseData?.url) {
    return file.responseData;
  }
  if (!file?.rawFile) {
    throw new Error('存在未识别的待上传文件');
  }
  const res = await uploadAdminOfflineOrderMedia(file.rawFile, mediaType);
  if (res?.code !== 200 || !res.data) {
    throw new Error(res?.message || '上传附件失败');
  }
  file.responseData = res.data;
  return res.data;
}

async function buildFaultSubmitList() {
  const faultSubmitList = [];
  for (const item of selectedFaultOptions.value) {
    const state = ensureFaultMediaState(item.id);
    const images = [];
    for (const file of (state.images || [])) {
      const uploaded = await uploadCachedFile(file, 'image');
      images.push({
        url: uploaded.url || '',
        name: uploaded.name || file.name || '',
        fileSize: uploaded.fileSize ?? file.rawFile?.size ?? 0,
        mimeType: uploaded.mimeType || file.rawFile?.type || '',
        width: uploaded.width ?? null,
        height: uploaded.height ?? null
      });
    }
    const videoFile = (state.videos || [])[0];
    const uploadedVideo = videoFile ? await uploadCachedFile(videoFile, 'video') : null;
    const video = uploadedVideo ? {
      url: uploadedVideo.url || '',
      name: uploadedVideo.name || videoFile.name || '',
      fileSize: uploadedVideo.fileSize ?? videoFile.rawFile?.size ?? 0,
      mimeType: uploadedVideo.mimeType || videoFile.rawFile?.type || '',
      duration: uploadedVideo.duration ?? null,
      width: uploadedVideo.width ?? null,
      height: uploadedVideo.height ?? null,
      thumbnailUrl: uploadedVideo.thumbnailUrl || ''
    } : null;
    faultSubmitList.push({
      faultId: item.id,
      faultDescription: (faultDescriptionMap[item.id] || '').trim(),
      images,
      video
    });
  }
  return faultSubmitList;
}

function validateCurrentStep() {
  if (activeStep.value === 0) {
    if (!form.userId) {
      ElMessage.warning('请先选择申请用户');
      return false;
    }
    return true;
  }

  if (activeStep.value === 1) {
    if (!form.serviceTypeId) {
      ElMessage.warning('请先选择服务类型');
      return false;
    }
    if (!form.technicianId) {
      ElMessage.warning('请先选择服务师傅');
      return false;
    }
    return true;
  }

  if (activeStep.value === 2) {
    if (!selectedFaultIds.value.length) {
      ElMessage.warning('请至少选择一个故障现象');
      return false;
    }
    return true;
  }

  return true;
}

function goNextStep() {
  if (!validateCurrentStep()) {
    return;
  }
  if (activeStep.value < stepLabels.length - 1) {
    activeStep.value += 1;
  }
}

function goPrevStep() {
  if (activeStep.value > 0) {
    activeStep.value -= 1;
  }
}

async function submitOrder() {
  if (!validateAll()) {
    return;
  }

  submitting.value = true;
  try {
    const faultList = await buildFaultSubmitList();
    const payload = {
      userId: form.userId,
      categoryId: selectedServiceType.value?.categoryId || form.categoryId || '',
      serviceTypeId: form.serviceTypeId,
      technicianId: form.technicianId,
      applianceBrand: normalizeText(form.applianceBrand),
      applianceModel: normalizeText(form.applianceModel),
      purchaseDate: form.purchaseDate || '',
      remark: normalizeText(form.remark),
      faultList
    };

    const res = await createAdminOfflineOrder(payload);
    if (res?.code === 200 && res.data) {
      ElMessage.success(`线下订单录入成功，订单号：${res.data.orderNo || '-'}`);
      resetForm();
      return;
    }
    throw new Error(res?.message || '提交线下订单失败');
  } catch (error) {
    const message = getErrorMessage(error) || '提交线下订单失败';
    if (isUploadRelatedError(error, message)) {
      showUploadErrorDialog(message, message, '上传失败');
    } else {
      ElMessage.error(message);
    }
  } finally {
    submitting.value = false;
  }
}

function validateAll() {
  const current = activeStep.value;
  for (let step = 0; step < stepLabels.length - 1; step += 1) {
    activeStep.value = step;
    if (!validateCurrentStep()) {
      return false;
    }
  }
  activeStep.value = current;
  return true;
}

function resetForm() {
  activeStep.value = 0;
  form.userId = '';
  form.categoryId = '';
  form.serviceTypeId = '';
  form.technicianId = '';
  form.applianceBrand = '';
  form.applianceModel = '';
  form.purchaseDate = '';
  form.remark = '';
  selectedCategoryId.value = '';
  technicianKeyword.value = '';
  technicianList.value = [];
  selectedFaultIds.value = [];
  clearFaultExtraData();
}

function normalizeText(value) {
  if (value == null) {
    return '';
  }
  const text = String(value).trim();
  return text || '';
}

function getUserInitial(user) {
  if (user?.username) {
    return String(user.username).slice(0, 1).toUpperCase();
  }
  if (user?.email) {
    return String(user.email).slice(0, 1).toUpperCase();
  }
  return 'U';
}

function getTechnicianInitial(technician) {
  if (technician?.name) {
    return String(technician.name).slice(0, 1).toUpperCase();
  }
  if (technician?.email) {
    return String(technician.email).slice(0, 1).toUpperCase();
  }
  return 'T';
}

function getUserStatusText(status) {
  if (Number(status) === 1) return '正常';
  if (Number(status) === 2) return '冻结';
  if (Number(status) === 3) return '注销';
  return '未知';
}

function getTechnicianTagType(status) {
  if (Number(status) === 1) return 'success';
  if (Number(status) === 2) return 'warning';
  return 'info';
}

function formatPrice(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return '￥0.00';
  }
  return `￥${amount.toFixed(2)}`;
}

function formatFaultPrice(item) {
  const min = item?.estimatedPriceMin;
  const max = item?.estimatedPriceMax;
  if (min == null && max == null) return '价格待评估';
  if (min != null && max == null) return `${formatPrice(min)} 起`;
  if (min == null && max != null) return `不高于 ${formatPrice(max)}`;
  return `${formatPrice(min)} - ${formatPrice(max)}`;
}

function resolveCategoryPath(categoryId) {
  return categoryPathMap.value[categoryId] || '';
}

function getErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '';
}

function buildCategoryTree(list) {
  const map = {};
  const roots = [];

  (list || []).forEach(item => {
    if (!item?.id || Number(item.isActive) !== 1) {
      return;
    }
    map[item.id] = {
      ...item,
      children: []
    };
  });

  Object.keys(map).forEach(key => {
    const node = map[key];
    if (node.parentId && map[node.parentId]) {
      map[node.parentId].children.push(node);
    } else {
      roots.push(node);
    }
  });

  const sortNodes = nodes => {
    nodes.sort((a, b) => {
      const sortA = Number(a.sortOrder || 0);
      const sortB = Number(b.sortOrder || 0);
      if (sortA !== sortB) {
        return sortA - sortB;
      }
      return String(a.name || '').localeCompare(String(b.name || ''));
    });
    nodes.forEach(node => {
      if (node.children?.length) {
        sortNodes(node.children);
      }
    });
  };

  sortNodes(roots);
  return roots;
}

function buildCategoryPathMap(nodes, parentNames, result) {
  (nodes || []).forEach(node => {
    const names = [...parentNames, node.name].filter(Boolean);
    result[node.id] = names.join(' / ');
    if (node.children?.length) {
      buildCategoryPathMap(node.children, names, result);
    }
  });
}

function matchesCategory(categoryId, filterId, list) {
  if (!categoryId || !filterId) {
    return true;
  }
  if (categoryId === filterId) {
    return true;
  }
  const map = {};
  (list || []).forEach(item => {
    if (item?.id) {
      map[item.id] = item;
    }
  });
  let currentId = categoryId;
  while (currentId) {
    if (currentId === filterId) {
      return true;
    }
    currentId = map[currentId]?.parentId || '';
  }
  return false;
}

onMounted(() => {
  loadPageData();
});

useAdminPageRefresh(async () => {
  await loadPageData();
  if (form.serviceTypeId) {
    await loadTechnicians();
  }
});
</script>

<style scoped>
.offline-order-page {
  padding: 16px;
  box-sizing: border-box;
}

.page-card {
  border-radius: 14px;
}

.intro-card,
.steps-card,
.content-card {
  margin-bottom: 16px;
}

.page-header,
.section-head,
.panel-title-row,
.selected-user-main,
.service-type-foot,
.technician-card-head,
.fault-media-head,
.step-footer,
.step-footer-actions,
.step-footer-tip,
.toolbar {
  display: flex;
  align-items: center;
}

.page-header,
.section-head,
.panel-title-row,
.step-footer {
  justify-content: space-between;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.page-subtitle,
.section-desc,
.panel-hint,
.section-side-text,
.upload-tip {
  color: #909399;
  font-size: 13px;
}

.page-subtitle {
  margin-top: 6px;
}

.steps-wrap {
  overflow-x: auto;
}

.section-head {
  gap: 16px;
  margin-bottom: 18px;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.section-desc {
  margin-top: 6px;
}

.toolbar {
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-search {
  width: 320px;
  max-width: 100%;
}

.selected-user-panel {
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f7fbff, #f5f7fa);
  border: 1px solid #e4edf6;
}

.selected-user-label {
  margin-bottom: 10px;
  font-size: 12px;
  color: #909399;
}

.selected-user-main {
  gap: 14px;
}

.selected-user-info {
  min-width: 0;
}

.selected-user-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.selected-user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.user-grid,
.service-type-grid,
.technician-grid,
.fault-grid,
.summary-grid {
  display: grid;
  gap: 14px;
}

.user-grid,
.service-type-grid,
.technician-grid,
.summary-grid {
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}

.fault-grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.user-card,
.service-type-card,
.technician-card {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #ffffff;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-card:hover,
.service-type-card:hover,
.technician-card:hover,
.fault-option-card:hover,
.summary-card:hover,
.fault-media-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.08);
}

.user-card.is-active,
.service-type-card.is-active,
.technician-card.is-active,
.fault-option-card.is-active {
  border-color: #409eff;
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.14);
}

.user-card:disabled {
  cursor: not-allowed;
  opacity: 0.68;
  transform: none;
  box-shadow: none;
}

.user-card-top,
.user-card-bottom,
.technician-card-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.user-card-top {
  align-items: center;
}

.user-card-title,
.technician-card-title {
  flex: 1;
  min-width: 0;
}

.user-name,
.service-type-name,
.technician-name,
.fault-option-name,
.fault-media-title,
.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-phone,
.service-type-path,
.technician-contact,
.fault-option-desc,
.summary-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.user-card-bottom,
.technician-card-meta {
  margin-top: 14px;
  color: #606266;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.service-layout {
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.category-panel {
  width: 260px;
  flex-shrink: 0;
  padding: 16px 14px;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  background: #fafbfc;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.all-category-entry {
  display: flex;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  margin-top: 12px;
  margin-bottom: 8px;
  border-radius: 10px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
}

.all-category-entry:hover,
.all-category-entry.is-active {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.category-tree {
  background: transparent;
}

.category-tree :deep(.el-tree-node__content) {
  height: 38px;
  border-radius: 8px;
}

.service-main {
  flex: 1;
  min-width: 0;
}

.service-types-section {
  padding: 18px;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  background: #ffffff;
}

.technician-section {
  margin-top: 22px;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  background: #ffffff;
}

.technician-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.placeholder-panel {
  padding: 32px 18px;
  border-radius: 12px;
  border: 1px dashed #dcdfe6;
  background: #fafafa;
  color: #909399;
  text-align: center;
}

.fault-select-panel,
.fault-media-card,
.summary-card,
.confirm-form {
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #ffffff;
}

.fault-select-panel {
  padding: 18px;
}

.fault-option-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.fault-checkbox {
  margin-top: 3px;
}

.fault-option-main {
  flex: 1;
  min-width: 0;
}

.fault-option-price {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}

.fault-media-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
}

.fault-media-card {
  padding: 18px;
  transition: all 0.2s ease;
}

.fault-media-head {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.fault-media-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.fault-media-form {
  margin-top: 12px;
}

.upload-block {
  width: 100%;
}

.upload-tip {
  margin-bottom: 10px;
}

.video-preview-list {
  margin-top: 12px;
}

.video-preview-card {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 100%;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}

.video-delete-button {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 20;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.62);
  color: #ffffff;
  cursor: pointer;
  pointer-events: auto;
  transition: background 0.2s ease;
}

.video-delete-button:hover {
  background: rgba(245, 108, 108, 0.92);
}

.video-preview-player {
  position: relative;
  z-index: 1;
  display: block;
  width: auto;
  max-height: 260px;
  max-width: min(100%, 520px);
  height: auto;
  border-radius: 10px;
  background: #000000;
}

.video-preview-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.video-preview-name {
  min-width: 0;
  color: #606266;
  font-size: 13px;
  word-break: break-all;
}

.summary-grid {
  margin-bottom: 18px;
}

.summary-card {
  padding: 16px 18px;
  transition: all 0.2s ease;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  margin-top: 10px;
}

.summary-meta {
  line-height: 1.6;
}

.confirm-form {
  padding: 18px;
}

.step-footer {
  position: sticky;
  bottom: 0;
  z-index: 5;
  gap: 16px;
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid #ebeef5;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -6px 18px rgba(31, 45, 61, 0.06);
  backdrop-filter: blur(8px);
}

.step-footer-tip {
  gap: 6px;
  color: #606266;
  font-size: 13px;
}

.tip-strong {
  color: #303133;
  font-weight: 600;
}

.step-footer-actions {
  gap: 12px;
}

.button-icon {
  margin-left: 4px;
}

.media-preview-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

:deep(.media-preview-dialog) {
  width: fit-content !important;
  max-width: min(96vw, 1200px) !important;
}

:deep(.media-preview-dialog .el-dialog__body) {
  display: flex;
  justify-content: center;
}

.media-preview-image,
.media-preview-video {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 70vh;
  border-radius: 12px;
  background: #000000;
}

.media-preview-empty {
  color: #909399;
  font-size: 14px;
}

@media (max-width: 992px) {
  .service-layout {
    flex-direction: column;
  }

  .category-panel {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .offline-order-page {
    padding: 12px;
  }

  .toolbar-search {
    width: 100%;
  }

  .toolbar,
  .technician-toolbar,
  .page-header,
  .section-head,
  .step-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .step-footer-actions,
  .toolbar .el-button,
  .technician-toolbar .el-button {
    width: 100%;
  }

  .step-footer-actions {
    width: 100%;
  }

  .step-footer-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
