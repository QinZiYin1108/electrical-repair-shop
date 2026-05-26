<template>
  <div class="user-detail-page" v-loading="loading">
    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left-head">
            <el-button text @click="goBack">{{ text.backToList }}</el-button>
            <span class="title">{{ text.userDetail }}</span>
          </div>
          <div class="header-actions">
            <el-button
              v-if="detail.status === 1 || detail.status === 2"
              :type="detail.status === 1 ? 'danger' : 'success'"
              :loading="statusSaving"
              @click="toggleStatus"
            >
              {{ detail.status === 1 ? text.freezeAccount : text.unfreezeAccount }}
            </el-button>
            <el-button type="warning" :loading="passwordInitSaving" @click="initPassword">
              {{ text.initPassword }}
            </el-button>
            <el-button type="primary" :loading="saving" @click="saveInfo">{{ text.saveInfo }}</el-button>
          </div>
        </div>
      </template>

      <div class="summary">
        <div class="avatar-wrap" @click="triggerAvatarUpload">
          <el-avatar :size="88" :src="detail.avatarUrl">{{ avatarInitial }}</el-avatar>
          <div v-if="avatarUploading" class="avatar-mask">{{ text.uploading }}</div>
        </div>
        <div class="summary-main">
          <div class="name-row">
            <span class="name">{{ detail.username || '-' }}</span>
            <el-tag :type="getStatusTagType(detail.status)">{{ getStatusText(detail.status) }}</el-tag>
            <el-tag :type="detail.isVerified === 1 ? 'success' : 'info'">
              {{ detail.isVerified === 1 ? text.verified : text.unverified }}
            </el-tag>
          </div>
          <div class="meta-row">
            <span>{{ text.phoneLabel }}{{ detail.phone || '-' }}</span>
            <span>{{ text.emailLabel }}{{ detail.email || '-' }}</span>
            <span>{{ text.balanceLabel }}{{ formatBalance(detail.balance) }}</span>
          </div>
          <div class="tip-row">{{ text.avatarTip }}</div>
        </div>
      </div>

      <el-form label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="text.userId">
              <el-input :model-value="detail.id" disabled />
            </el-form-item>
            <el-form-item :label="text.username">
              <el-input v-model="form.username" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.phone">
              <el-input v-model="form.phone" maxlength="20" />
            </el-form-item>
            <el-form-item :label="text.email">
              <el-input :model-value="detail.email || '-'" disabled />
            </el-form-item>
            <el-form-item :label="text.realName">
              <el-input :model-value="detail.realName || '-'" disabled />
            </el-form-item>
            <el-form-item :label="text.idCard">
              <el-input :model-value="detail.idCard || '-'" disabled />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="text.gender">
              <el-input :model-value="getGenderText(detail.gender)" disabled />
            </el-form-item>
            <el-form-item :label="text.birthday">
              <el-input :model-value="detail.birthday || '-'" disabled />
            </el-form-item>
            <el-form-item :label="text.profession">
              <el-input v-model="form.profession" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.emergencyContact">
              <el-input v-model="form.emergencyContact" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.emergencyPhone">
              <el-input v-model="form.emergencyPhone" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="card-header card-header--address">
          <div class="address-header">
            <span class="section-title">{{ text.addressList }}</span>
            <span class="address-count">{{ text.totalPrefix }} {{ addressList.length }} {{ text.totalSuffix }}</span>
          </div>
          <span class="address-hint">{{ text.listHint }}</span>
        </div>
      </template>

      <div class="address-section" v-loading="addressLoading">
        <div v-if="addressList.length" class="address-list">
          <div
            v-for="item in addressList"
            :key="item.id"
            class="address-item"
            :class="{
              'is-default': item.isDefault === 1,
              'is-active': addressDrawerVisible && selectedAddressId === item.id
            }"
          >
            <div class="address-card-head">
              <div class="address-card-main">
                <div class="contact-row">
                  <span class="contact-name">{{ item.contactName || text.contactFallback }}</span>
                  <span class="contact-phone">{{ item.contactPhone || '-' }}</span>
                </div>
                <div class="tag-row">
                  <span v-if="item.isDefault === 1" class="mini-tag mini-tag--default">{{ text.defaultAddress }}</span>
                  <span class="mini-tag">{{ getAddressTypeText(item) }}</span>
                </div>
              </div>
              <div class="time-text">{{ text.updatedTimeLabel }}{{ formatDateTime(item.updatedTime || item.createdTime) }}</div>
            </div>

            <div class="address-brief">{{ getFullAddress(item) || '-' }}</div>
            <div class="address-detail-brief">{{ item.detailedAddress || '-' }}</div>

            <div class="address-meta-row">
              <span>{{ text.postalCodeLabel }}{{ item.postalCode || '-' }}</span>
              <span>{{ text.coordinateLabel }}{{ formatCoordinate(item) }}</span>
            </div>

            <div class="action-row action-row--list">
              <button type="button" class="mini-action" @click="openAddressDrawer(item, 'view')">{{ text.viewDetail }}</button>
              <button type="button" class="mini-action mini-action--primary" @click="openAddressDrawer(item, 'edit')">{{ text.editAddress }}</button>
              <button
                v-if="item.isDefault !== 1"
                type="button"
                class="mini-action"
                :disabled="isAddressActionPending(item.id, 'default') || isAnyAddressActionRunning"
                @click="setDefaultAddress(item)"
              >
                {{ isAddressActionPending(item.id, 'default') ? text.processing : text.setDefault }}
              </button>
              <button
                type="button"
                class="mini-action mini-action--danger"
                :disabled="isAddressActionPending(item.id, 'delete') || isAnyAddressActionRunning"
                @click="deleteAddress(item)"
              >
                {{ isAddressActionPending(item.id, 'delete') ? text.deleting : text.deleteAddress }}
              </button>
            </div>
          </div>
        </div>
        <el-empty v-else :description="text.emptyAddress" />
      </div>
    </el-card>

    <el-drawer
      v-model="addressDrawerVisible"
      :title="addressDrawerTitle"
      size="520px"
      @closed="handleAddressDrawerClosed"
    >
      <template v-if="currentAddress">
        <div class="drawer-shell">
          <div class="drawer-summary">
            <div class="drawer-top-row">
              <div class="drawer-title-wrap">
                <div class="drawer-name">{{ currentAddress.contactName || text.contactFallback }}</div>
                <div class="drawer-phone">{{ currentAddress.contactPhone || '-' }}</div>
              </div>
              <div class="drawer-mode-tag">{{ addressDrawerMode === 'edit' ? text.drawerModeEdit : text.drawerModeView }}</div>
            </div>
            <div class="tag-row tag-row--drawer">
              <span v-if="currentAddress.isDefault === 1" class="mini-tag mini-tag--default">{{ text.defaultAddress }}</span>
              <span class="mini-tag">{{ getAddressTypeText(currentAddress) }}</span>
            </div>
          </div>

          <template v-if="addressDrawerMode === 'view'">
            <div class="drawer-section">
              <div class="drawer-section-title">{{ text.contactSection }}</div>
              <div class="drawer-grid">
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.contactName }}</span>
                  <span class="drawer-value">{{ currentAddress.contactName || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.contactPhone }}</span>
                  <span class="drawer-value">{{ currentAddress.contactPhone || '-' }}</span>
                </div>
              </div>
            </div>
            <div class="drawer-section">
              <div class="drawer-section-title">{{ text.addressSection }}</div>
              <div class="drawer-grid drawer-grid--single">
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.fullAddress }}</span>
                  <span class="drawer-value">{{ getFullAddress(currentAddress) }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.detailAddress }}</span>
                  <span class="drawer-value">{{ currentAddress.detailedAddress || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.province }}</span>
                  <span class="drawer-value">{{ currentAddress.province || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.city }}</span>
                  <span class="drawer-value">{{ currentAddress.city || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.district }}</span>
                  <span class="drawer-value">{{ currentAddress.district || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.street }}</span>
                  <span class="drawer-value">{{ currentAddress.street || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.postalCodeLabel }}</span>
                  <span class="drawer-value">{{ currentAddress.postalCode || '-' }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.coordinateLabel }}</span>
                  <span class="drawer-value">{{ formatCoordinate(currentAddress) }}</span>
                </div>
              </div>
            </div>

            <div class="drawer-section">
              <div class="drawer-section-title">{{ text.metaSection }}</div>
              <div class="drawer-grid">
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.addressType }}</span>
                  <span class="drawer-value">{{ getAddressTypeText(currentAddress) }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.defaultFlag }}</span>
                  <span class="drawer-value">{{ currentAddress.isDefault === 1 ? text.yes : text.no }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.createdTime }}</span>
                  <span class="drawer-value">{{ formatDateTime(currentAddress.createdTime) }}</span>
                </div>
                <div class="drawer-field">
                  <span class="drawer-label">{{ text.updatedTime }}</span>
                  <span class="drawer-value">{{ formatDateTime(currentAddress.updatedTime) }}</span>
                </div>
              </div>
            </div>
          </template>

          <el-form v-else label-width="84px" class="drawer-form">
            <el-form-item :label="text.contactName">
              <el-input v-model="addressEditForm.contactName" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.contactPhone">
              <el-input v-model="addressEditForm.contactPhone" maxlength="11" />
            </el-form-item>
            <el-form-item :label="text.province">
              <el-input v-model="addressEditForm.province" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.city">
              <el-input v-model="addressEditForm.city" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.district">
              <el-input v-model="addressEditForm.district" maxlength="50" />
            </el-form-item>
            <el-form-item :label="text.street">
              <el-input v-model="addressEditForm.street" maxlength="100" />
            </el-form-item>
            <el-form-item :label="text.detailAddress">
              <el-input v-model="addressEditForm.detailedAddress" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item :label="text.postalCodeLabel">
              <el-input v-model="addressEditForm.postalCode" maxlength="10" />
            </el-form-item>
            <el-form-item :label="text.addressType">
              <el-select v-model="addressEditForm.addressType" class="full-width">
                <el-option :label="text.home" :value="1" />
                <el-option :label="text.company" :value="2" />
                <el-option :label="text.other" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item :label="text.defaultFlag">
              <el-switch v-model="addressEditForm.isDefault" />
            </el-form-item>
            <el-form-item :label="text.addressCoordinates">
              <div class="coordinate-grid">
                <el-input v-model="addressEditForm.longitude" :placeholder="text.longitude" />
                <el-input v-model="addressEditForm.latitude" :placeholder="text.latitude" />
              </div>
            </el-form-item>
          </el-form>
        </div>
      </template>
      <el-empty v-else :description="text.viewEmpty" />

      <template #footer>
        <div class="drawer-footer">
          <template v-if="addressDrawerMode === 'view'">
            <el-button @click="addressDrawerVisible = false">{{ text.closeDrawer }}</el-button>
            <el-button v-if="currentAddress && currentAddress.isDefault !== 1" @click="setDefaultAddress(currentAddress)">{{ text.setDefault }}</el-button>
            <el-button type="primary" @click="enterAddressEditMode">{{ text.editInDrawer }}</el-button>
          </template>
          <template v-else>
            <el-button @click="exitAddressEditMode">{{ text.cancelEdit }}</el-button>
            <el-button type="primary" :loading="addressDrawerSaving" @click="saveAddressEdit">{{ text.saveAddress }}</el-button>
          </template>
        </div>
      </template>
    </el-drawer>

    <input ref="avatarInputRef" class="hidden-file" type="file" accept="image/*" @change="handleAvatarChange" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useAdminPageRefresh } from '../../utils/adminPageRefresh';
import {
  deleteAdminUserAddress,
  fetchAdminUserAddresses,
  fetchAdminUserDetail,
  initAdminUserPassword,
  setAdminUserDefaultAddress,
  updateAdminUserAddress,
  updateAdminUserInfo,
  updateAdminUserStatus,
  uploadAdminUserAvatar
} from '../../api/adminUsers';
import { showUploadErrorDialog, showUploadLimitDialog } from '../../utils/uploadFeedback';

const text = Object.freeze({
  backToList: '返回列表',
  userDetail: '用户详情',
  freezeAccount: '冻结账号',
  unfreezeAccount: '解除冻结',
  initPassword: '初始化密码',
  saveInfo: '保存信息',
  uploading: '上传中',
  verified: '已实名认证',
  unverified: '未实名认证',
  phoneLabel: '手机号：',
  emailLabel: '邮箱：',
  balanceLabel: '账户余额：',
  avatarTip: '点击头像可上传新的用户头像。',
  userId: '用户ID',
  username: '昵称',
  phone: '手机号',
  email: '邮箱',
  realName: '真实姓名',
  idCard: '身份证号',
  gender: '性别',
  birthday: '生日',
  profession: '职业',
  emergencyContact: '紧急联系人',
  emergencyPhone: '紧急电话',
  addressList: '地址列表',
  totalPrefix: '共',
  totalSuffix: '条',
  contactFallback: '未填写联系人',
  defaultAddress: '默认地址',
  updatedTimeLabel: '更新时间：',
  processing: '处理中...',
  setDefault: '设为默认',
  deleting: '删除中...',
  deleteAddress: '删除地址',
  viewDetail: '查看详情',
  editAddress: '编辑地址',
  addressSummary: '地址概览',
  fullAddress: '完整地址',
  detailAddress: '详细地址',
  postalCodeLabel: '邮编：',
  coordinateLabel: '坐标：',
  emptyAddress: '该用户暂无地址记录',
  statusNormal: '正常',
  statusFrozen: '冻结',
  statusClosed: '注销',
  unknown: '未知',
  genderMale: '男',
  genderFemale: '女',
  home: '家庭',
  company: '公司',
  other: '其他',
  loadDetailFailed: '获取用户详情失败',
  loadAddressFailed: '获取地址列表失败',
  inputUsername: '请输入昵称',
  saveInfoSuccess: '用户信息已保存',
  saveInfoFailed: '保存用户信息失败',
  freezeConfirm: '确认冻结该用户账号吗？',
  unfreezeConfirm: '确认解除该用户账号冻结吗？',
  initPasswordConfirm: '确认将该用户登录密码初始化为固定值 123456 吗？',
  initPasswordNoEmail: '用户未绑定邮箱，无法初始化密码',
  initPasswordSuccess: '密码已初始化为 123456',
  initPasswordFailed: '初始化密码失败',
  prompt: '提示',
  statusUpdated: '账号状态已更新',
  updateStatusFailed: '更新账号状态失败',
  defaultUpdated: '默认地址已更新',
  setDefaultFailed: '设置默认地址失败',
  deleteConfirm: '确认删除这条地址记录吗？删除后不可恢复。',
  deleteSuccess: '地址已删除',
  deleteFailed: '删除地址失败',
  avatarTooLarge: '头像大小不能超过 5MB',
  avatarUpdated: '头像已更新',
  avatarUploadFailed: '头像上传失败',
  addressDetail: '地址详情',
  drawerViewTitle: '地址详情',
  drawerEditTitle: '编辑地址',
  closeDrawer: '关闭',
  editInDrawer: '编辑地址',
  cancelEdit: '取消编辑',
  saveAddress: '保存地址',
  saveAddressSuccess: '地址已保存',
  saveAddressFailed: '保存地址失败',
  contactSection: '联系信息',
  addressSection: '地址信息',
  metaSection: '其他信息',
  contactName: '联系人',
  contactPhone: '联系电话',
  province: '省份',
  city: '城市',
  district: '区县',
  street: '街道',
  addressType: '地址类型',
  defaultFlag: '默认地址',
  createdTime: '创建时间',
  updatedTime: '更新时间',
  longitude: '经度',
  latitude: '纬度',
  yes: '是',
  no: '否',
  viewEmpty: '暂无可展示的地址信息',
  requiredContactName: '联系人不能为空',
  invalidPhone: '联系电话格式不正确',
  requiredProvince: '省份不能为空',
  requiredCity: '城市不能为空',
  requiredDistrict: '区县不能为空',
  requiredDetailAddress: '详细地址不能为空',
  invalidCoordinatePair: '经纬度需要同时填写',
  invalidLongitude: '经度格式不正确',
  invalidLatitude: '纬度格式不正确',
  invalidLongitudeRange: '经度范围应在 -180 到 180 之间',
  invalidLatitudeRange: '纬度范围应在 -90 到 90 之间',
  drawerModeView: '查看模式',
  drawerModeEdit: '编辑模式',
  addressCoordinates: '坐标信息',
  listHint: '点击查看详情或编辑地址，右侧抽屉展示完整信息。'
});

const route = useRoute();
const router = useRouter();
const userId = computed(() => route.params.id);

const loading = ref(false);
const saving = ref(false);
const statusSaving = ref(false);
const passwordInitSaving = ref(false);
const avatarUploading = ref(false);
const addressLoading = ref(false);
const addressDrawerSaving = ref(false);
const addressActionId = ref('');
const addressActionType = ref('');
const addressDrawerVisible = ref(false);
const addressDrawerMode = ref('view');
const selectedAddressId = ref('');
const avatarInputRef = ref();
const addressList = ref([]);

const detail = reactive({
  id: '',
  username: '',
  phone: '',
  email: '',
  status: null,
  isVerified: 0,
  balance: null,
  realName: '',
  gender: null,
  profession: '',
  emergencyContact: '',
  emergencyPhone: '',
  birthday: '',
  idCard: '',
  avatarUrl: ''
});

const form = reactive({
  username: '',
  phone: '',
  profession: '',
  emergencyContact: '',
  emergencyPhone: ''
});

function createAddressEditForm() {
  return {
    contactName: '',
    contactPhone: '',
    province: '',
    city: '',
    district: '',
    street: '',
    detailedAddress: '',
    postalCode: '',
    longitude: '',
    latitude: '',
    addressType: 1,
    isDefault: false
  };
}

const addressEditForm = reactive(createAddressEditForm());

const avatarInitial = computed(() => {
  if (detail.username) return String(detail.username).charAt(0).toUpperCase();
  if (detail.email) return String(detail.email).charAt(0).toUpperCase();
  return 'U';
});

const isAnyAddressActionRunning = computed(() => Boolean(addressActionId.value));
const currentAddress = computed(() => addressList.value.find(item => item.id === selectedAddressId.value) || null);
const addressDrawerTitle = computed(() => (
  addressDrawerMode.value === 'edit' ? text.drawerEditTitle : text.drawerViewTitle
));

function isAddressActionPending(id, type) {
  return addressActionId.value === id && addressActionType.value === type;
}

function getStatusText(status) {
  if (status === 1) return text.statusNormal;
  if (status === 2) return text.statusFrozen;
  if (status === 3) return text.statusClosed;
  return text.unknown;
}

function getStatusTagType(status) {
  if (status === 1) return 'success';
  if (status === 2) return 'danger';
  if (status === 3) return 'info';
  return 'warning';
}

function getGenderText(gender) {
  if (gender === 1) return text.genderMale;
  if (gender === 2) return text.genderFemale;
  return text.unknown;
}
function getAddressTypeText(item) {
  if (item && item.addressTypeName) return item.addressTypeName;
  if (item && item.addressType === 1) return text.home;
  if (item && item.addressType === 2) return text.company;
  return text.other;
}

function getFullAddress(item) {
  if (!item) return '-';
  if (item.fullAddress) return item.fullAddress;
  return [item.province, item.city, item.district, item.street]
    .filter(Boolean)
    .join('') || '-';
}

function formatBalance(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '-';
  return amount.toFixed(2);
}

function formatCoordinate(item) {
  if (!item || !item.longitude || !item.latitude) return '-';
  return `${item.longitude}, ${item.latitude}`;
}

function formatDateTime(value) {
  const timestamp = Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) return '-';
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function trimValue(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function applyDetail(data) {
  detail.id = data.id || '';
  detail.username = data.username || '';
  detail.phone = data.phone || '';
  detail.email = data.email || '';
  detail.status = data.status ?? null;
  detail.isVerified = data.isVerified === 1 ? 1 : 0;
  detail.balance = data.balance ?? null;
  detail.realName = data.realName || '';
  detail.gender = data.gender ?? null;
  detail.profession = data.profession || '';
  detail.emergencyContact = data.emergencyContact || '';
  detail.emergencyPhone = data.emergencyPhone || '';
  detail.birthday = data.birthday || '';
  detail.idCard = data.idCard || '';
  detail.avatarUrl = data.avatarUrl || '';

  form.username = detail.username;
  form.phone = detail.phone;
  form.profession = detail.profession;
  form.emergencyContact = detail.emergencyContact;
  form.emergencyPhone = detail.emergencyPhone;
}

function applyAddresses(list) {
  addressList.value = Array.isArray(list) ? list : [];
}

function fillAddressEditForm(item) {
  const source = item || {};
  addressEditForm.contactName = source.contactName || '';
  addressEditForm.contactPhone = source.contactPhone || '';
  addressEditForm.province = source.province || '';
  addressEditForm.city = source.city || '';
  addressEditForm.district = source.district || '';
  addressEditForm.street = source.street || '';
  addressEditForm.detailedAddress = source.detailedAddress || '';
  addressEditForm.postalCode = source.postalCode || '';
  addressEditForm.longitude = source.longitude || '';
  addressEditForm.latitude = source.latitude || '';
  addressEditForm.addressType = Number(source.addressType) || 1;
  addressEditForm.isDefault = source.isDefault === 1;
}

function resetAddressEditForm() {
  Object.assign(addressEditForm, createAddressEditForm());
}

function openAddressDrawer(item, mode) {
  if (!item || !item.id) return;
  selectedAddressId.value = item.id;
  addressDrawerMode.value = mode === 'edit' ? 'edit' : 'view';
  if (addressDrawerMode.value === 'edit') {
    fillAddressEditForm(item);
  }
  addressDrawerVisible.value = true;
}

function handleAddressDrawerClosed() {
  addressDrawerMode.value = 'view';
  selectedAddressId.value = '';
  addressDrawerSaving.value = false;
  resetAddressEditForm();
}

function enterAddressEditMode() {
  if (!currentAddress.value) return;
  fillAddressEditForm(currentAddress.value);
  addressDrawerMode.value = 'edit';
}

function exitAddressEditMode() {
  if (currentAddress.value) {
    fillAddressEditForm(currentAddress.value);
    addressDrawerMode.value = 'view';
    return;
  }
  addressDrawerVisible.value = false;
}

function buildAddressPayload() {
  const longitudeText = trimValue(addressEditForm.longitude);
  const latitudeText = trimValue(addressEditForm.latitude);
  return {
    contactName: trimValue(addressEditForm.contactName),
    contactPhone: trimValue(addressEditForm.contactPhone),
    province: trimValue(addressEditForm.province),
    city: trimValue(addressEditForm.city),
    district: trimValue(addressEditForm.district),
    street: trimValue(addressEditForm.street),
    detailedAddress: trimValue(addressEditForm.detailedAddress),
    postalCode: trimValue(addressEditForm.postalCode),
    longitude: longitudeText ? Number(longitudeText) : null,
    latitude: latitudeText ? Number(latitudeText) : null,
    addressType: Number(addressEditForm.addressType) || 1,
    isDefault: addressEditForm.isDefault ? 1 : 0
  };
}

function validateAddressPayload(payload) {
  if (!payload.contactName) return text.requiredContactName;
  if (!/^1\d{10}$/.test(payload.contactPhone)) return text.invalidPhone;
  if (!payload.province) return text.requiredProvince;
  if (!payload.city) return text.requiredCity;
  if (!payload.district) return text.requiredDistrict;
  if (!payload.detailedAddress) return text.requiredDetailAddress;

  const hasLongitude = trimValue(addressEditForm.longitude) !== '';
  const hasLatitude = trimValue(addressEditForm.latitude) !== '';
  if (hasLongitude !== hasLatitude) return text.invalidCoordinatePair;
  if (hasLongitude && !Number.isFinite(payload.longitude)) return text.invalidLongitude;
  if (hasLatitude && !Number.isFinite(payload.latitude)) return text.invalidLatitude;
  if (hasLongitude && (payload.longitude < -180 || payload.longitude > 180)) return text.invalidLongitudeRange;
  if (hasLatitude && (payload.latitude < -90 || payload.latitude > 90)) return text.invalidLatitudeRange;
  return '';
}

async function loadDetail() {
  if (!userId.value) return;
  loading.value = true;
  try {
    const res = await fetchAdminUserDetail(userId.value);
    if (res && res.code === 200 && res.data) {
      applyDetail(res.data);
      return;
    }
    ElMessage.error((res && res.message) || text.loadDetailFailed);
  } catch {
    ElMessage.error(text.loadDetailFailed);
  } finally {
    loading.value = false;
  }
}

async function loadAddresses() {
  if (!userId.value) return;
  addressLoading.value = true;
  try {
    const res = await fetchAdminUserAddresses(userId.value);
    if (res && res.code === 200) {
      applyAddresses(res.data);
      return;
    }
    ElMessage.error((res && res.message) || text.loadAddressFailed);
  } catch {
    ElMessage.error(text.loadAddressFailed);
  } finally {
    addressLoading.value = false;
  }
}

async function saveInfo() {
  if (!userId.value) return;
  if (!form.username || !form.username.trim()) {
    ElMessage.warning(text.inputUsername);
    return;
  }
  saving.value = true;
  try {
    const res = await updateAdminUserInfo(userId.value, {
      username: form.username.trim(),
      phone: form.phone || '',
      profession: form.profession || '',
      emergencyContact: form.emergencyContact || '',
      emergencyPhone: form.emergencyPhone || ''
    });
    if (res && res.code === 200) {
      ElMessage.success(text.saveInfoSuccess);
      await loadDetail();
      return;
    }
    ElMessage.error((res && res.message) || text.saveInfoFailed);
  } catch {
    ElMessage.error(text.saveInfoFailed);
  } finally {
    saving.value = false;
  }
}

async function toggleStatus() {
  if (!userId.value) return;
  const targetStatus = detail.status === 1 ? 2 : 1;
  const confirmText = targetStatus === 2 ? text.freezeConfirm : text.unfreezeConfirm;
  try {
    await ElMessageBox.confirm(confirmText, text.prompt, { type: 'warning' });
  } catch {
    return;
  }
  statusSaving.value = true;
  try {
    const res = await updateAdminUserStatus(userId.value, targetStatus);
    if (res && res.code === 200) {
      ElMessage.success(text.statusUpdated);
      await loadDetail();
      return;
    }
    ElMessage.error((res && res.message) || text.updateStatusFailed);
  } catch {
    ElMessage.error(text.updateStatusFailed);
  } finally {
    statusSaving.value = false;
  }
}

async function initPassword() {
  if (!userId.value) return;
  if (!detail.email) {
    ElMessage.warning(text.initPasswordNoEmail);
    return;
  }
  try {
    await ElMessageBox.confirm(text.initPasswordConfirm, text.prompt, { type: 'warning' });
  } catch {
    return;
  }
  passwordInitSaving.value = true;
  try {
    const res = await initAdminUserPassword(userId.value);
    if (res && res.code === 200) {
      ElMessage.success(text.initPasswordSuccess);
      return;
    }
    ElMessage.error((res && res.message) || text.initPasswordFailed);
  } catch (error) {
    ElMessage.error((error && error.message) || text.initPasswordFailed);
  } finally {
    passwordInitSaving.value = false;
  }
}

async function setDefaultAddress(item) {
  if (!userId.value || !item || !item.id || isAnyAddressActionRunning.value) return;
  addressActionId.value = item.id;
  addressActionType.value = 'default';
  try {
    const res = await setAdminUserDefaultAddress(userId.value, item.id);
    if (res && res.code === 200) {
      ElMessage.success(text.defaultUpdated);
      await loadAddresses();
      return;
    }
    ElMessage.error((res && res.message) || text.setDefaultFailed);
  } catch {
    ElMessage.error(text.setDefaultFailed);
  } finally {
    addressActionId.value = '';
    addressActionType.value = '';
  }
}
async function deleteAddress(item) {
  if (!userId.value || !item || !item.id || isAnyAddressActionRunning.value) return;
  try {
    await ElMessageBox.confirm(text.deleteConfirm, text.prompt, { type: 'warning' });
  } catch {
    return;
  }
  addressActionId.value = item.id;
  addressActionType.value = 'delete';
  try {
    const res = await deleteAdminUserAddress(userId.value, item.id);
    if (res && res.code === 200) {
      ElMessage.success(text.deleteSuccess);
      await loadAddresses();
      if (selectedAddressId.value === item.id) {
        addressDrawerVisible.value = false;
      }
      return;
    }
    ElMessage.error((res && res.message) || text.deleteFailed);
  } catch {
    ElMessage.error(text.deleteFailed);
  } finally {
    addressActionId.value = '';
    addressActionType.value = '';
  }
}

async function saveAddressEdit() {
  if (!userId.value || !selectedAddressId.value) return;
  const payload = buildAddressPayload();
  const errorText = validateAddressPayload(payload);
  if (errorText) {
    ElMessage.warning(errorText);
    return;
  }
  addressDrawerSaving.value = true;
  try {
    const res = await updateAdminUserAddress(userId.value, selectedAddressId.value, payload);
    if (res && res.code === 200) {
      ElMessage.success(text.saveAddressSuccess);
      await loadAddresses();
      addressDrawerMode.value = 'view';
      return;
    }
    ElMessage.error((res && res.message) || text.saveAddressFailed);
  } catch {
    ElMessage.error(text.saveAddressFailed);
  } finally {
    addressDrawerSaving.value = false;
  }
}

function triggerAvatarUpload() {
  if (avatarUploading.value || !avatarInputRef.value) return;
  avatarInputRef.value.value = '';
  avatarInputRef.value.click();
}

async function handleAvatarChange(event) {
  const files = event.target && event.target.files;
  if (!files || !files.length || !userId.value) return;
  const file = files[0];
  if (!file.type || !file.type.startsWith('image/')) {
    showUploadLimitDialog('请上传图片文件');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    showUploadLimitDialog(text.avatarTooLarge);
    return;
  }
  avatarUploading.value = true;
  try {
    const res = await uploadAdminUserAvatar(userId.value, file);
    if (res && res.code === 200) {
      ElMessage.success(text.avatarUpdated);
      await loadDetail();
      return;
    }
    showUploadErrorDialog((res && res.message) || text.avatarUploadFailed, text.avatarUploadFailed, text.avatarUploadFailed);
  } catch (error) {
    showUploadErrorDialog(error, text.avatarUploadFailed, text.avatarUploadFailed);
  } finally {
    avatarUploading.value = false;
  }
}

function goBack() {
  router.push('/admin/users/list');
}

watch(
  userId,
  value => {
    if (!value) return;
    loadDetail();
    loadAddresses();
  },
  { immediate: true }
);

watch(currentAddress, value => {
  if (!value && addressDrawerVisible.value) {
    addressDrawerVisible.value = false;
  }
});

useAdminPageRefresh(async () => {
  await Promise.all([loadDetail(), loadAddresses()]);
});
</script>

<style scoped>
.user-detail-page {
  padding: 16px;
  box-sizing: border-box;
}

.detail-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header--address {
  align-items: flex-start;
}

.left-head,
.header-actions,
.address-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.address-count {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f0f7ff;
  color: #2f6bff;
  font-size: 12px;
}

.address-hint {
  font-size: 12px;
  color: #909399;
}

.summary {
  display: flex;
  gap: 18px;
  margin-bottom: 18px;
}

.avatar-wrap {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  cursor: pointer;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-main {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.meta-row {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: #606266;
  font-size: 13px;
}

.tip-row {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.address-section {
  min-height: 120px;
}

.address-list {
  display: grid;
  gap: 14px;
}

.address-item {
  border: 1px solid #e6ebf2;
  border-radius: 14px;
  padding: 16px 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.address-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.address-item.is-default {
  border-color: #9bc2ff;
}

.address-item.is-active {
  border-color: #2f6bff;
  box-shadow: 0 10px 24px rgba(47, 107, 255, 0.14);
}

.address-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.address-card-main {
  min-width: 0;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.contact-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.contact-phone,
.time-text {
  font-size: 12px;
  color: #7b8794;
}

.tag-row,
.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-row {
  margin-top: 10px;
}

.tag-row--drawer {
  margin-top: 12px;
}

.mini-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 12px;
}

.mini-tag--default {
  background: #eaf3ff;
  color: #2563eb;
}

.address-brief {
  margin-top: 14px;
  font-size: 14px;
  color: #1f2937;
  line-height: 1.7;
}

.address-detail-brief {
  margin-top: 6px;
  color: #667085;
  line-height: 1.7;
}

.address-meta-row {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #7b8794;
}

.action-row--list {
  margin-top: 14px;
}

.mini-action {
  border: none;
  padding: 7px 12px;
  border-radius: 999px;
  background: #eef2f7;
  color: #334155;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, background 0.2s ease;
}

.mini-action:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #e2e8f0;
}

.mini-action--primary {
  background: #eef4ff;
  color: #2563eb;
}

.mini-action--primary:hover:not(:disabled) {
  background: #dce9ff;
}

.mini-action--danger {
  background: #fff1f2;
  color: #e11d48;
}

.mini-action--danger:hover:not(:disabled) {
  background: #ffe4e6;
}

.mini-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.drawer-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-summary {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f7faff 0%, #edf4ff 100%);
  border: 1px solid #dbeafe;
}
.drawer-top-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.drawer-title-wrap {
  min-width: 0;
}

.drawer-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.drawer-phone {
  margin-top: 6px;
  color: #64748b;
}

.drawer-mode-tag {
  flex: none;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
}

.drawer-section {
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.drawer-section-title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #475467;
}

.drawer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.drawer-grid--single {
  grid-template-columns: 1fr;
}

.drawer-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.drawer-label {
  font-size: 12px;
  color: #98a2b3;
}

.drawer-value {
  color: #1f2937;
  line-height: 1.7;
  word-break: break-all;
}

.drawer-form {
  padding-top: 4px;
}

.full-width,
.full-width :deep(.el-select__wrapper) {
  width: 100%;
}

.coordinate-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

.hidden-file {
  display: none;
}
</style>
