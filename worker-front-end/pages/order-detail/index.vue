<template>
  <view class="page worker-order-detail">
    <view class="header">
      <view class="header-side" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#1f2937" />
      </view>
      <view class="header-title">订单详情</view>
      <view class="header-side header-side-right" @click="refreshDetail">
        <u-icon :name="refreshing ? 'reload' : 'reload'" size="20" color="#1f2937" />
      </view>
    </view>

    <scroll-view scroll-y class="page-scroll">
      <view v-if="loading" class="state-card">
        <text class="state-title">正在加载订单详情...</text>
      </view>

      <view v-else-if="loadError" class="state-card">
        <text class="state-title">订单详情加载失败</text>
        <text class="state-desc">{{ loadError }}</text>
        <view class="state-action" @click="loadDetail">重新加载</view>
      </view>

      <view v-else-if="detail" class="detail-content">
        <view class="hero-card">
          <view class="hero-head">
            <view class="hero-status-wrap">
              <view class="hero-status-icon">
                <u-icon name="checkmark-circle-fill" size="16" color="#ffffff" />
              </view>
              <text class="hero-status">{{ detail.statusText || '待处理' }}</text>
            </view>
            <text class="hero-mode">{{ detail.serviceModeText || '维修服务' }}</text>
          </view>
          <text class="hero-title">{{ detail.serviceTitle }}</text>
          <view class="hero-order-row">
            <u-icon name="file-text" size="14" color="rgba(255,255,255,0.88)" />
            <text class="hero-order-no">{{ detail.orderNo }}</text>
          </view>
          <text v-if="detail.actionHint" class="hero-hint">{{ detail.actionHint }}</text>
          <view class="hero-metrics">
            <view class="hero-metric-card">
              <text class="hero-metric-label">支付状态</text>
              <text class="hero-metric-value">{{ detail.paymentDisplayText || '待支付' }}</text>
            </view>
            <view class="hero-metric-card">
              <text class="hero-metric-label">已支付</text>
              <text class="hero-metric-value">￥{{ detail.paidAmountText }}</text>
            </view>
            <view class="hero-metric-card">
              <text class="hero-metric-label">订单合计</text>
              <text class="hero-metric-value">￥{{ detail.totalAmountText }}</text>
            </view>
          </view>
        </view>

        <view class="section-card" :class="{ 'section-card-collapsed': isSectionCollapsed('overview') }">
          <view class="section-headline" @click="toggleSection('overview')">
            <view class="section-title-wrap">
              <view class="section-icon section-icon-overview">
                <u-icon name="file-text" size="16" color="#1677ff" />
              </view>
              <view class="section-texts">
                <text class="section-title">订单概览</text>
                <text class="section-subtitle">预约、设备与联系信息</text>
              </view>
            </view>
            <view class="section-toggle">
              <text class="section-toggle-text">{{ isSectionCollapsed('overview') ? '展开' : '收起' }}</text>
              <u-icon :name="isSectionCollapsed('overview') ? 'arrow-down' : 'arrow-up'" size="14" color="#64748b" />
            </view>
          </view>
          <view v-show="!isSectionCollapsed('overview')">
            <view
              v-for="item in detail.summaryRows"
              :key="item.label"
              class="info-row"
            >
              <text class="info-label">{{ item.label }}</text>
              <text class="info-value" :class="{ 'info-value-strong': item.strong }">{{ item.value }}</text>
            </view>
          </view>
        </view>

        <view class="section-card" :class="{ 'section-card-collapsed': isSectionCollapsed('fault') }">
          <view class="section-headline" @click="toggleSection('fault')">
            <view class="section-title-wrap">
              <view class="section-icon section-icon-fault">
                <u-icon name="warning-fill" size="16" color="#f97316" />
              </view>
              <view class="section-texts">
                <text class="section-title">故障信息</text>
                <text class="section-subtitle">用户反馈与现场凭证</text>
              </view>
            </view>
            <view class="section-actions">
              <text v-if="detail.faultList.length" class="section-tag">{{ detail.faultList.length }} 条</text>
              <view class="section-toggle">
                <text class="section-toggle-text">{{ isSectionCollapsed('fault') ? '展开' : '收起' }}</text>
                <u-icon :name="isSectionCollapsed('fault') ? 'arrow-down' : 'arrow-up'" size="14" color="#64748b" />
              </view>
            </view>
          </view>
          <view v-show="!isSectionCollapsed('fault') && detail.faultList.length">
            <view
              v-for="(fault, faultIndex) in detail.faultList"
              :key="fault.id"
              class="fault-card"
            >
              <view class="fault-card-head">
                <view class="fault-chip">
                  <u-icon name="warning" size="14" color="#f97316" />
                  <text class="fault-chip-text">故障 {{ faultIndex + 1 }}</text>
                </view>
              </view>
              <view class="info-row">
                <text class="info-label">故障现象</text>
                <text class="info-value">{{ fault.phenomenonName || '未填写' }}</text>
              </view>
              <view v-if="fault.phenomenonDescription" class="info-row">
                <text class="info-label">现象说明</text>
                <text class="info-value">{{ fault.phenomenonDescription }}</text>
              </view>
              <view v-if="fault.faultDescription" class="info-row">
                <text class="info-label">补充描述</text>
                <text class="info-value">{{ fault.faultDescription }}</text>
              </view>

              <view v-if="fault.images.length" class="media-group">
                <view class="media-title-row">
                  <u-icon name="photo" size="14" color="#1677ff" />
                  <text class="media-title">图片</text>
                </view>
                <view class="image-list">
                  <image
                    v-for="(image, index) in fault.images"
                    :key="image.id || index"
                    class="media-image"
                    :src="image.thumbnailUrl || image.url"
                    mode="aspectFill"
                    @click="previewImages(fault.images, index)"
                  />
                </view>
              </view>

              <view v-if="fault.videos.length" class="media-group">
                <view class="media-title-row">
                  <u-icon name="play-circle" size="14" color="#14b8a6" />
                  <text class="media-title">视频</text>
                </view>
                <view
                  v-for="(video, index) in fault.videos"
                  :key="video.id || index"
                  class="video-card"
                >
                  <view class="video-preview" @click="previewVideo(video)">
                    <image
                      v-if="video.thumbnailUrl"
                      class="media-video-poster"
                      :src="video.thumbnailUrl"
                      mode="aspectFill"
                    />
                    <view v-else class="media-video-fallback">
                      <u-icon name="play-right-fill" size="34" color="#ffffff" />
                    </view>
                    <view class="video-play-mask">
                      <view class="video-play-badge">
                        <u-icon name="play-right-fill" size="18" color="#ffffff" />
                        <text class="video-play-text">预览视频</text>
                      </view>
                    </view>
                  </view>
                  <view class="video-meta">
                    <text v-if="video.durationText" class="video-duration">{{ video.durationText }}</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <text v-show="!isSectionCollapsed('fault') && !detail.faultList.length" class="empty-text">暂无故障详情</text>
        </view>

        <view class="section-card" :class="{ 'section-card-collapsed': isSectionCollapsed('inspection') }">
          <view class="section-headline" @click="toggleSection('inspection')">
            <view class="section-title-wrap">
              <view class="section-icon section-icon-inspection">
                <u-icon name="search" size="16" color="#0f766e" />
              </view>
              <view class="section-texts">
                <text class="section-title">检查结果</text>
                <text class="section-subtitle">师傅检查结论与佐证资料</text>
              </view>
            </view>
            <view class="section-toggle">
              <text class="section-toggle-text">{{ isSectionCollapsed('inspection') ? '展开' : '收起' }}</text>
              <u-icon :name="isSectionCollapsed('inspection') ? 'arrow-down' : 'arrow-up'" size="14" color="#64748b" />
            </view>
          </view>
          <view v-show="!isSectionCollapsed('inspection')">
            <view class="info-row">
              <text class="info-label">检查时间</text>
              <text class="info-value">{{ detail.inspectionTimeText || '未提交' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">问题说明</text>
              <text class="info-value">{{ detail.inspectionDiagnosis || '未填写' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">维修方案</text>
              <text class="info-value">{{ detail.repairPlan || '未填写' }}</text>
            </view>

            <view v-if="detail.inspectionImages.length" class="media-group">
              <view class="media-title-row">
                <u-icon name="photo" size="14" color="#1677ff" />
                <text class="media-title">检查图片</text>
              </view>
              <view class="image-list">
                <image
                  v-for="(image, index) in detail.inspectionImages"
                  :key="image.id || index"
                  class="media-image"
                  :src="image.thumbnailUrl || image.url"
                  mode="aspectFill"
                  @click="previewImages(detail.inspectionImages, index)"
                />
              </view>
            </view>

            <view v-if="detail.inspectionVideos.length" class="media-group">
              <view class="media-title-row">
                <u-icon name="play-circle" size="14" color="#14b8a6" />
                <text class="media-title">检查视频</text>
              </view>
              <view
                v-for="(video, index) in detail.inspectionVideos"
                :key="video.id || index"
                class="video-card"
              >
                <view class="video-preview" @click="previewVideo(video)">
                  <image
                    v-if="video.thumbnailUrl"
                    class="media-video-poster"
                    :src="video.thumbnailUrl"
                    mode="aspectFill"
                  />
                  <view v-else class="media-video-fallback">
                    <u-icon name="play-right-fill" size="34" color="#ffffff" />
                  </view>
                  <view class="video-play-mask">
                    <view class="video-play-badge">
                      <u-icon name="play-right-fill" size="18" color="#ffffff" />
                      <text class="video-play-text">预览视频</text>
                    </view>
                  </view>
                </view>
                <view class="video-meta">
                  <text v-if="video.durationText" class="video-duration">{{ video.durationText }}</text>
                </view>
              </view>
            </view>

            <text
              v-if="!detail.inspectionImages.length && !detail.inspectionVideos.length && !detail.inspectionDiagnosis && !detail.repairPlan"
              class="empty-text"
            >
              暂无检查结果
            </text>
          </view>
        </view>

        <view class="section-card" :class="{ 'section-card-collapsed': isSectionCollapsed('fee') }">
          <view class="section-headline" @click="toggleSection('fee')">
            <view class="section-title-wrap">
              <view class="section-icon section-icon-fee">
                <u-icon name="rmb-circle-fill" size="16" color="#ca8a04" />
              </view>
              <view class="section-texts">
                <text class="section-title">费用信息</text>
                <text class="section-subtitle">服务费用与支付情况</text>
              </view>
            </view>
            <view class="section-toggle">
              <text class="section-toggle-text">{{ isSectionCollapsed('fee') ? '展开' : '收起' }}</text>
              <u-icon :name="isSectionCollapsed('fee') ? 'arrow-down' : 'arrow-up'" size="14" color="#64748b" />
            </view>
          </view>
          <view v-show="!isSectionCollapsed('fee')">
            <view
              v-for="item in detail.feeRows"
              :key="item.label"
              class="info-row"
            >
              <text class="info-label">{{ item.label }}</text>
              <text class="info-value" :class="{ 'info-value-strong': item.strong }">{{ item.value }}</text>
            </view>
          </view>
        </view>

        <view class="section-card" :class="{ 'section-card-collapsed': isSectionCollapsed('progress') }">
          <view class="section-headline" @click="toggleSection('progress')">
            <view class="section-title-wrap">
              <view class="section-icon section-icon-progress">
                <u-icon name="clock-fill" size="16" color="#7c3aed" />
              </view>
              <view class="section-texts">
                <text class="section-title">订单进度</text>
                <text class="section-subtitle">处理节点与状态变化</text>
              </view>
            </view>
            <view class="section-actions">
              <text v-if="detail.progressList.length" class="section-tag">{{ detail.progressList.length }} 条</text>
              <view class="section-toggle">
                <text class="section-toggle-text">{{ isSectionCollapsed('progress') ? '展开' : '收起' }}</text>
                <u-icon :name="isSectionCollapsed('progress') ? 'arrow-down' : 'arrow-up'" size="14" color="#64748b" />
              </view>
            </view>
          </view>
          <view v-show="!isSectionCollapsed('progress') && detail.progressList.length">
            <view
              v-for="item in detail.progressList"
              :key="item.id"
              class="progress-item"
            >
              <view class="progress-dot" />
              <view class="progress-main">
                <view class="progress-head">
                  <text class="progress-status">{{ item.statusText }}</text>
                  <text class="progress-time">{{ item.createdTimeText }}</text>
                </view>
                <text v-if="item.description" class="progress-desc">{{ item.description }}</text>
                <text v-if="item.operatorName" class="progress-operator">{{ item.operatorName }}</text>
              </view>
            </view>
          </view>
          <text v-show="!isSectionCollapsed('progress') && !detail.progressList.length" class="empty-text">暂无进度记录</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="detail" class="action-bar">
      <view class="action-hint-block">
        <text class="action-hint-title">{{ detail.statusText || '订单状态' }}</text>
        <text class="action-hint-text">{{ detail.actionHint || '可刷新查看订单最新状态' }}</text>
      </view>
      <view class="action-buttons" :class="{ 'action-buttons-single': !detail.actionAvailable }">
        <u-button
          text="刷新"
          shape="circle"
          :plain="true"
          :disabled="refreshing || actionLoading || submittingInspection"
          @click="refreshDetail"
        />
        <u-button
          v-if="detail.actionAvailable"
          :text="detail.primaryActionText || '立即处理'"
          type="primary"
          shape="circle"
          :loading="actionLoading"
          :disabled="refreshing || actionLoading || submittingInspection"
          @click="handlePrimaryAction"
        />
      </view>
    </view>

    <u-popup
      v-model:show="showInspectionPopup"
      mode="bottom"
      round="20"
      :safe-area-inset-bottom="true"
    >
      <view class="inspection-popup">
        <view class="inspection-header">
          <text class="inspection-title">{{ inspectionPopupTitle }}</text>
          <text class="inspection-subtitle">{{ inspectionPopupSubtitle }}</text>
        </view>

        <scroll-view scroll-y class="inspection-scroll">
          <view v-if="!isFeeEditMode" class="form-card">
            <text class="form-label">问题说明</text>
            <textarea
              v-model="inspectionForm.inspectionDiagnosis"
              class="form-textarea"
              maxlength="300"
              placeholder="例如：压缩机异响，启动后 2 分钟自动停机。"
            />
          </view>

          <view v-if="!isFeeEditMode" class="form-card">
            <text class="form-label">维修方案</text>
            <textarea
              v-model="inspectionForm.repairPlan"
              class="form-textarea"
              maxlength="300"
              placeholder="例如：建议更换启动电容并清洗冷凝器，预计 1 小时完成。"
            />
          </view>

          <view class="form-card">
            <text class="form-label">费用设置</text>
            <view class="fee-grid">
              <view class="fee-item">
                <text class="fee-label">服务费（手动填写）</text>
                <input
                  v-model="inspectionForm.serviceFee"
                  class="fee-input"
                  type="digit"
                  placeholder="0.00"
                />
              </view>
              <view class="fee-item">
                <text class="fee-label">材料费（手动填写）</text>
                <input
                  v-model="inspectionForm.materialFee"
                  class="fee-input"
                  type="digit"
                  placeholder="0.00"
                />
              </view>
            </view>
            <view class="fee-note">
              <text class="fee-note-text">{{ isFeeEditMode ? '仅可调整服务费和材料费，用户支付后不可再修改。' : '上门费、路程费、加班费等费用由系统自动带入，师傅无需手动填写。' }}</text>
            </view>
            <view class="auto-fee-card">
              <view class="auto-fee-head">
                <text class="auto-fee-title">系统自动计算项</text>
                <text class="auto-fee-subtitle">{{ isFeeEditMode ? '保存后会同步更新待支付金额' : '提交后会自动汇总为待支付金额' }}</text>
              </view>
              <view
                v-for="item in inspectionAutoFeeRows"
                :key="item.label"
                class="auto-fee-row"
              >
                <text class="auto-fee-label">{{ item.label }}</text>
                <text class="auto-fee-value">{{ item.value }}</text>
              </view>
              <view class="auto-fee-row auto-fee-row-strong">
                <text class="auto-fee-label auto-fee-label-strong">待支付合计预览</text>
                <text class="auto-fee-value auto-fee-value-strong">￥{{ inspectionPreviewTotalText }}</text>
              </view>
            </view>
          </view>

          <view v-if="!isFeeEditMode" class="form-card">
            <view class="form-head">
              <text class="form-label">检查图片</text>
              <text class="form-tip">最多 {{ maxInspectionImageCount }} 张</text>
            </view>
            <view class="upload-grid">
              <view
                v-for="(image, index) in inspectionForm.images"
                :key="image.url || index"
                class="upload-image-card"
              >
                <image
                  class="upload-image"
                  :src="image.thumbnailUrl || image.url"
                  mode="aspectFill"
                  @click="previewImages(inspectionForm.images, index)"
                />
                <view class="upload-remove" @click.stop="removeInspectionImage(index)">
                  <u-icon name="close" size="12" color="#ffffff" />
                </view>
              </view>

              <view
                v-if="inspectionForm.images.length < maxInspectionImageCount"
                class="upload-add-card"
                @click="chooseInspectionImages"
              >
                <u-icon :name="uploadingImage ? 'reload' : 'plus'" size="22" color="#64748b" />
                <text class="upload-add-text">{{ uploadingImage ? '选择中' : '选择图片' }}</text>
              </view>
            </view>
          </view>

          <view v-if="!isFeeEditMode" class="form-card">
            <view class="form-head">
              <text class="form-label">检查视频</text>
              <text class="form-tip">最多 1 段</text>
            </view>
            <view v-if="inspectionForm.video" class="video-card video-card-popup">
              <view class="video-preview" @click="previewVideo(inspectionForm.video)">
                <image
                  v-if="inspectionForm.video.thumbnailUrl"
                  class="media-video-poster"
                  :src="inspectionForm.video.thumbnailUrl"
                  mode="aspectFill"
                />
                <view v-else class="media-video-fallback">
                  <u-icon name="play-right-fill" size="34" color="#ffffff" />
                </view>
                <view class="video-play-mask">
                  <view class="video-play-badge">
                    <u-icon name="play-right-fill" size="18" color="#ffffff" />
                    <text class="video-play-text">预览视频</text>
                  </view>
                </view>
              </view>
              <view class="video-meta">
                <text v-if="inspectionForm.video.durationText" class="video-duration">{{ inspectionForm.video.durationText }}</text>
              </view>
              <view class="video-actions">
                <text class="video-action-link" @click="removeInspectionVideo">删除视频</text>
                <text class="video-action-link" @click="chooseInspectionVideo">重新上传</text>
              </view>
            </view>
            <view
              v-else
              class="upload-video-card"
              @click="chooseInspectionVideo"
            >
              <u-icon :name="uploadingVideo ? 'reload' : 'video'" size="22" color="#64748b" />
              <text class="upload-add-text">{{ uploadingVideo ? '选择中' : '选择视频' }}</text>
            </view>
          </view>

          <view v-if="!isFeeEditMode" class="form-card form-card-tip">
            <text class="tip-text">请至少上传 1 份检查凭证，凭证可为图片或视频。</text>
          </view>
        </scroll-view>

        <view class="inspection-footer">
          <u-button
            text="取消"
            shape="circle"
            :plain="true"
            :disabled="submittingInspection"
            @click="showInspectionPopup = false"
          />
          <u-button
            :text="inspectionPopupSubmitText"
            type="primary"
            shape="circle"
            :loading="submittingInspection"
            :disabled="submittingInspection"
            @click="submitInspection"
          />
        </view>
      </view>
    </u-popup>

    <u-popup
      v-model:show="showVideoPreviewPopup"
      mode="center"
      :safe-area-inset-bottom="true"
      @close="closeVideoPreview"
    >
      <view class="video-preview-popup">
        <video
          v-if="previewVideoItem && previewVideoItem.url"
          class="video-preview-player"
          :src="previewVideoItem.url"
          :poster="previewVideoItem.thumbnailUrl"
          controls
          autoplay
          object-fit="contain"
        />
        <view class="video-preview-footer">
          <text v-if="previewVideoItem && previewVideoItem.durationText" class="video-duration">{{ previewVideoItem.durationText }}</text>
          <u-button
            text="关闭"
            shape="circle"
            :plain="true"
            @click="closeVideoPreview"
          />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script>
import {
  acceptWorkerOrder,
  advanceWorkerOrderStatus,
  consumeWorkerDoorQr,
  fetchWorkerOrderDetail,
  submitWorkerInspection,
  updateWorkerInspectionFees,
  uploadWorkerInspectionMedia
} from '@/api/workerOrders';
import { showUploadErrorModal } from '@/utils/uploadFeedback';

const MAX_INSPECTION_IMAGE_COUNT = 5;

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function isBrokenText(value) {
  if (value == null) return true;
  const text = String(value).trim();
  if (!text) return true;
  return /^[?？]+$/.test(text) || text.includes('???');
}

function resolveOrderStatusText(status, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(status || 0);
  if (value === 1) return '待接单';
  if (value === 2) return '待上门';
  if (value === 3) return '待检查';
  if (value === 4) return '待支付';
  if (value === 5) return '服务中';
  if (value === 6) return '已完成';
  if (value === 7) return '已取消';
  if (value === 8) return '已退款';
  return '订单状态';
}

function resolveServiceModeText(serviceMode, fallback) {
  if (!isBrokenText(fallback)) return fallback;
  const value = Number(serviceMode || 0);
  if (value === 1) return '上门维修';
  if (value === 2) return '上门安装';
  if (value === 3) return '线下维修';
  return '维修服务';
}

function normalizeMoney(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number : 0;
}

function calculateRemainingAmount(detail) {
  const totalAmount = normalizeMoney(detail && detail.totalAmount);
  const paidAmount = normalizeMoney(detail && detail.paidAmount);
  const remaining = totalAmount - paidAmount;
  return remaining > 0 ? remaining : 0;
}

function resolvePaymentStatusText(detail) {
  const fallback = detail && detail.paymentStatusText;
  if (!isBrokenText(fallback)) return fallback;
  const paidAmount = normalizeMoney(detail && detail.paidAmount);
  const remainingAmount = calculateRemainingAmount(detail);
  if (remainingAmount <= 0 && paidAmount > 0) return '已支付';
  if (paidAmount > 0) return '待补尾款';
  return '待支付';
}

function resolveProgressStatusText(item) {
  const description = String((item && item.description) || '').trim();
  if (Number(item && item.status) === 4 && /已支付|支付尾款|尾款已支付/.test(description)) {
    return '已支付';
  }
  if (!isBrokenText(item && item.statusName)) return item.statusName;
  if (!isBrokenText(item && item.statusText)) return item.statusText;
  return resolveOrderStatusText(item && item.status, item && item.statusText);
}

function formatProgressDescription(value) {
  const text = String(value || '').trim();
  if (!text) return '';
  const formatted = text
    .replace(/(问题|维修建议|服务费|材料费|支付方式|支付金额)\s*[=:：]/g, '$1：')
    .replace(/师傅已完成检查[:：]?\s*(?=问题：)/g, '师傅已完成检查\n')
    .replace(/[；;]\s*/g, '\n')
    .replace(/[，,]\s*(?=(支付方式：|支付金额：))/g, '\n')
    .replace(/\n{2,}/g, '\n');
  return formatted;
}

function resolveActionHint(data) {
  if (!isBrokenText(data && data.actionHint)) return data.actionHint;
  const actionType = data && data.primaryActionType;
  if (actionType === 'accept') return '确认接单后进入下一处理阶段';
  if (actionType === 'scanDoorQr') return '上门后扫描用户提供的上门码';
  if (actionType === 'submitInspection') return '请上传检查凭证，并仅填写服务费和材料费，其余费用由系统自动计算';
  if (actionType === 'editInspectionFee') return '已提交费用，用户支付前可调整服务费和材料费';
  if (actionType === 'advance') {
    return Number(data && data.status) === 5
      ? '提交完工后需等待用户确认完成'
      : '用户已支付，可开始后续服务';
  }
  return '可刷新查看订单最新状态';
}

function resolvePrimaryActionText(data) {
  if (!isBrokenText(data && data.primaryActionText)) return data.primaryActionText;
  const actionType = data && data.primaryActionType;
  if (actionType === 'accept') return '接单';
  if (actionType === 'scanDoorQr') return Number(data && data.serviceMode) === 2 ? '扫码开始安装' : '扫码上门';
  if (actionType === 'submitInspection') return '提交检查';
  if (actionType === 'editInspectionFee') return '修改费用';
  if (actionType === 'advance') return Number(data && data.status) === 5 ? '提交完工' : (Number(data && data.serviceMode) === 2 ? '开始安装' : '开始维修');
  return '';
}

function formatDateTime(timestamp, emptyText = '暂无') {
  const value = Number(timestamp || 0);
  if (!value) return emptyText;
  const date = new Date(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatMoney(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number)) return '0.00';
  return number.toFixed(2);
}

function toEditableFeeText(value) {
  const text = String(value == null ? '' : value).trim();
  if (!text) return '';
  const number = Number(text);
  if (!Number.isFinite(number) || number < 0) return '';
  return number.toFixed(2);
}

function buildServiceTitle(detail) {
  if (!detail) return '维修订单';
  const parts = [detail.serviceCategoryName, detail.serviceTypeName].filter(Boolean);
  return parts.join(' / ') || detail.serviceModeText || '维修订单';
}

function buildContactText(name, phone) {
  return [name, phone].filter(Boolean).join(' / ') || '暂无';
}

function buildDurationText(duration) {
  const seconds = Number(duration || 0);
  if (!Number.isFinite(seconds) || seconds <= 0) return '';
  if (seconds < 60) return `${seconds}s`;
  const minute = Math.floor(seconds / 60);
  const second = seconds % 60;
  return `${minute}m ${String(second).padStart(2, '0')}s`;
}

function normalizeAmountInput(value) {
  const text = String(value == null ? '' : value).trim();
  if (!text) return '0.00';
  const number = Number(text);
  if (!Number.isFinite(number) || number < 0) return null;
  return number.toFixed(2);
}

function resolvePreviewFeeAmount(value) {
  const text = String(value == null ? '' : value).trim();
  if (!text) return 0;
  const number = Number(text);
  return Number.isFinite(number) && number > 0 ? number : 0;
}

function getFileNameFromPath(filePath, fallback) {
  const path = String(filePath || '').trim();
  if (!path) return fallback;
  const parts = path.split('/');
  return parts[parts.length - 1] || fallback;
}

function createLocalInspectionImageItem(file, index) {
  const path = file.path || file.tempFilePath || '';
  return {
    id: `local-inspection-image-${Date.now()}-${index}`,
    url: path,
    thumbnailUrl: path,
    name: getFileNameFromPath(path, `inspection-image-${index + 1}.jpg`),
    mimeType: file.type || 'image/jpeg',
    duration: 0,
    durationText: '',
    fileSize: file.size || 0,
    width: file.width || null,
    height: file.height || null,
    localPath: path,
    isLocal: true
  };
}

function createLocalInspectionVideoItem(file) {
  const path = file.tempFilePath || '';
  const duration = Number(file.duration || 0);
  return {
    id: `local-inspection-video-${Date.now()}`,
    url: path,
    thumbnailUrl: file.thumbTempFilePath || '',
    name: getFileNameFromPath(path, 'inspection-video.mp4'),
    mimeType: 'video/mp4',
    duration,
    durationText: buildDurationText(duration),
    fileSize: file.size || 0,
    width: file.width || null,
    height: file.height || null,
    localPath: path,
    isLocal: true
  };
}

export default {
  data() {
    return {
      orderId: '',
      loading: true,
      refreshing: false,
      loadError: '',
      detail: null,
      actionLoading: false,
      sectionCollapse: {
        overview: false,
        fault: false,
        inspection: false,
        fee: false,
        progress: false
      },
      showInspectionPopup: false,
      inspectionPopupMode: 'submit',
      showVideoPreviewPopup: false,
      previewVideoItem: null,
      submittingInspection: false,
      uploadingImage: false,
      uploadingVideo: false,
      maxInspectionImageCount: MAX_INSPECTION_IMAGE_COUNT,
      inspectionForm: this.createInspectionForm()
    };
  },
  computed: {
    isFeeEditMode() {
      return this.inspectionPopupMode === 'editFee';
    },
    inspectionPopupTitle() {
      return this.isFeeEditMode ? '修改费用' : '提交检查结果';
    },
    inspectionPopupSubtitle() {
      return this.isFeeEditMode
        ? '仅可调整服务费和材料费，保存后会同步更新用户待支付金额'
        : '请上传检查凭证，并仅填写服务费和材料费，其余费用由系统自动计算';
    },
    inspectionPopupSubmitText() {
      return this.isFeeEditMode ? '保存费用' : '提交检查结果';
    },
    inspectionAutoFeeRows() {
      const detail = this.detail || {};
      return [
        { label: '上门费', value: `￥${formatMoney(detail.doorFee)}` },
        { label: '路程费', value: `￥${formatMoney(detail.distanceFee)}` },
        { label: '加班费', value: `￥${formatMoney(detail.overtimeFee)}` }
      ];
    },
    inspectionPreviewTotalText() {
      const detail = this.detail || {};
      const baseAmount = Math.max(
        0,
        normalizeMoney(detail.totalAmount) - normalizeMoney(detail.serviceFee) - normalizeMoney(detail.materialFee)
      );
      const serviceFee = resolvePreviewFeeAmount(this.inspectionForm.serviceFee);
      const materialFee = resolvePreviewFeeAmount(this.inspectionForm.materialFee);
      return formatMoney(baseAmount + serviceFee + materialFee);
    }
  },
  onLoad(options) {
    this.orderId = options && options.id ? options.id : '';
    if (!this.orderId) {
      this.loading = false;
      this.loadError = '缺少订单编号';
      return;
    }
    this.loadDetail();
  },
  methods: {
    createInspectionForm(source = null) {
      return {
        inspectionDiagnosis: (source && source.inspectionDiagnosis) || '',
        repairPlan: (source && source.repairPlan) || '',
        serviceFee: toEditableFeeText(source && source.serviceFee),
        materialFee: toEditableFeeText(source && source.materialFee),
        images: [],
        video: null
      };
    },
    goBack() {
      if (getCurrentPages().length > 1) {
        uni.navigateBack();
        return;
      }
      uni.reLaunch({
        url: '/pages/index/index'
      });
    },
    loadDetail(showLoading = true) {
      if (!this.orderId) return;
      if (showLoading) {
        this.loading = true;
      } else {
        this.refreshing = true;
      }
      this.loadError = '';
      fetchWorkerOrderDetail(this.orderId)
        .then((res) => {
          if (!res || res.code !== 200 || !res.data) {
            throw new Error((res && res.message) || '订单详情加载失败');
          }
          this.detail = this.normalizeDetail(res.data);
          if (this.showInspectionPopup) {
            const expectedActionType = this.inspectionPopupMode === 'editFee' ? 'editInspectionFee' : 'submitInspection';
            if (this.detail.primaryActionType !== expectedActionType) {
              this.showInspectionPopup = false;
            }
          }
        })
        .catch((error) => {
          const message = (error && error.message) || '订单详情加载失败';
          if (!this.detail) {
            this.loadError = message;
          } else {
            uni.showToast({
              title: message,
              icon: 'none'
            });
          }
        })
        .finally(() => {
          this.loading = false;
          this.refreshing = false;
        });
    },
    refreshDetail() {
      if (this.loading || this.refreshing) return;
      this.loadDetail(false);
    },
    toggleSection(key) {
      if (!key) return;
      this.sectionCollapse = {
        ...this.sectionCollapse,
        [key]: !this.sectionCollapse[key]
      };
    },
    isSectionCollapsed(key) {
      return !!(this.sectionCollapse && this.sectionCollapse[key]);
    },
    normalizeDetail(data) {
      const faultList = this.mapFaultList(data.faultList);
      const inspectionImages = this.mapMediaList(data.inspectionImages);
      const inspectionVideos = this.mapMediaList(data.inspectionVideos, false);
      const statusText = resolveOrderStatusText(data.status, data.statusText);
      const serviceModeText = resolveServiceModeText(data.serviceMode, data.serviceModeText);
      const actionHint = resolveActionHint(data);
      const primaryActionText = resolvePrimaryActionText(data);
      const paymentDisplayText = resolvePaymentStatusText(data);
      const totalAmountText = formatMoney(data.totalAmount);
      const paidAmountText = formatMoney(data.paidAmount);

      return {
        ...data,
        statusText,
        serviceModeText,
        actionHint,
        primaryActionText,
        paymentDisplayText,
        totalAmountText,
        paidAmountText,
        serviceTitle: buildServiceTitle({
          ...data,
          serviceModeText
        }),
        inspectionTimeText: formatDateTime(data.inspectionTime, '未提交'),
        summaryRows: [
          { label: '订单号', value: data.orderNo || '-' },
          { label: '服务项目', value: buildServiceTitle({ ...data, serviceModeText }) },
          { label: '服务方式', value: serviceModeText || '暂无' },
          { label: '预约时间', value: formatDateTime(data.appointmentTime, '暂未安排') },
          { label: '客户信息', value: buildContactText(data.contactName, data.contactPhone) },
          { label: '服务地址', value: data.serviceAddress || '线下维修无需上门地址' },
          { label: '设备品牌', value: data.applianceBrand || '未填写' },
          { label: '设备型号', value: data.applianceModel || '未填写' },
          { label: '支付状态', value: paymentDisplayText },
          { label: '订单备注', value: data.remark || '暂无' }
        ],
        feeRows: [
          { label: '上门费', value: `￥${formatMoney(data.doorFee)}` },
          { label: '路程费', value: `￥${formatMoney(data.distanceFee)}` },
          { label: '服务费', value: `￥${formatMoney(data.serviceFee)}` },
          { label: '材料费', value: `￥${formatMoney(data.materialFee)}` },
          { label: '加班费', value: `￥${formatMoney(data.overtimeFee)}` },
          { label: '订单合计', value: `￥${totalAmountText}`, strong: true },
          { label: '已支付', value: `￥${paidAmountText}` }
        ],
        faultList,
        inspectionImages,
        inspectionVideos,
        progressList: this.mapProgressList(data.progressList)
      };
    },
    mapFaultList(list) {
      return (Array.isArray(list) ? list : []).map((item) => ({
        id: item.id || '',
        phenomenonName: item.faultPhenomenonName || '',
        phenomenonDescription: item.faultPhenomenonDescription || '',
        faultDescription: item.faultDescription || '',
        images: this.mapMediaList(item.images),
        videos: this.mapMediaList(item.videos, false)
      }));
    },
    mapMediaList(list, fallbackToUrl = true) {
      return (Array.isArray(list) ? list : [])
        .map((item) => ({
          id: item.id || '',
          url: item.url || '',
          thumbnailUrl: item.thumbnailUrl || (fallbackToUrl ? item.url || '' : ''),
          name: item.name || '',
          mimeType: item.mimeType || '',
          duration: item.duration || 0,
          durationText: buildDurationText(item.duration),
          fileSize: item.fileSize || 0,
          width: item.width || null,
          height: item.height || null,
          localPath: '',
          isLocal: false
        }))
        .filter((item) => !!item.url);
    },
    mapProgressList(list) {
      return (Array.isArray(list) ? list : []).map((item) => ({
        id: item.id || '',
        statusText: resolveProgressStatusText(item),
        description: formatProgressDescription(item.description),
        operatorName: item.operatorName || '',
        createdTimeText: formatDateTime(item.createdTime)
      }));
    },
    previewImages(list, index) {
      const items = Array.isArray(list) ? list : [];
      const urls = items.map((item) => item.url).filter(Boolean);
      if (!urls.length) return;
      const currentIndex = Number(index || 0);
      uni.previewImage({
        current: urls[currentIndex] || urls[0],
        urls
      });
    },
    previewVideo(video) {
      if (!video || !video.url) return;
      this.previewVideoItem = {
        url: video.url,
        thumbnailUrl: video.thumbnailUrl || '',
        durationText: video.durationText || ''
      };
      this.showVideoPreviewPopup = true;
    },
    closeVideoPreview() {
      this.showVideoPreviewPopup = false;
      this.previewVideoItem = null;
    },
    handlePrimaryAction() {
      if (!this.detail || !this.detail.primaryActionType) return;
      if (this.detail.primaryActionType === 'accept') {
        this.handleAccept();
        return;
      }
      if (this.detail.primaryActionType === 'advance') {
        this.handleAdvance();
        return;
      }
      if (this.detail.primaryActionType === 'scanDoorQr') {
        this.handleScanDoorQr();
        return;
      }
      if (this.detail.primaryActionType === 'submitInspection') {
        this.openInspectionPopup('submit');
        return;
      }
      if (this.detail.primaryActionType === 'editInspectionFee') {
        this.openInspectionPopup('editFee');
      }
    },
    withActionLoading(action) {
      if (this.actionLoading) return;
      this.actionLoading = true;
      action()
        .finally(() => {
          this.actionLoading = false;
        });
    },
    handleAccept() {
      this.withActionLoading(() => new Promise((resolve) => {
        uni.showModal({
          title: '确认接单',
          content: '确认接受当前订单并进入下一处理阶段吗？',
          success: ({ confirm }) => {
            if (!confirm) {
              resolve();
              return;
            }
            acceptWorkerOrder(this.orderId)
              .then((res) => {
                this.applyDetailResponse(res, '接单成功');
              })
              .finally(resolve);
          },
          fail: resolve
        });
      }));
    },
    handleAdvance() {
      const buttonText = (this.detail && this.detail.primaryActionText) || '继续处理';
      const isSubmitCompletion = this.detail && Number(this.detail.status) === 5;
      this.withActionLoading(() => new Promise((resolve) => {
        uni.showModal({
          title: '确认操作',
          content: isSubmitCompletion
            ? '确认提交完工吗？提交后需等待用户确认完成，订单才会结束。'
            : `确认执行“${buttonText}”吗？`,
          success: ({ confirm }) => {
            if (!confirm) {
              resolve();
              return;
            }
            advanceWorkerOrderStatus(this.orderId)
              .then((res) => {
                this.applyDetailResponse(res, `${buttonText}成功`);
              })
              .finally(resolve);
          },
          fail: resolve
        });
      }));
    },
    handleScanDoorQr() {
      if (this.actionLoading) return;
      this.actionLoading = true;
      uni.scanCode({
        onlyFromCamera: false,
        success: (scanRes) => {
          const token = this.extractDoorQrToken(scanRes && scanRes.result);
          if (!token) {
            uni.showToast({
              title: '未识别到有效上门码',
              icon: 'none'
            });
            this.actionLoading = false;
            return;
          }
          consumeWorkerDoorQr(token)
            .then((res) => {
              this.applyDetailResponse(res, '核销成功');
            })
            .finally(() => {
              this.actionLoading = false;
            });
        },
        fail: () => {
          this.actionLoading = false;
        }
      });
    },
    extractDoorQrToken(result) {
      const text = String(result || '').trim();
      if (!text) return '';
      const queryIndex = text.indexOf('?');
      if (queryIndex < 0) return text;
      const query = text.slice(queryIndex + 1);
      const pairs = query.split('&');
      for (let i = 0; i < pairs.length; i += 1) {
        const [key, value] = pairs[i].split('=');
        if (key === 'token' && value) {
          return decodeURIComponent(value);
        }
      }
      return text;
    },
    openInspectionPopup(mode = 'submit') {
      this.inspectionPopupMode = mode;
      this.inspectionForm = mode === 'editFee'
        ? this.createInspectionForm(this.detail || {})
        : this.createInspectionForm();
      this.showInspectionPopup = true;
    },
    chooseInspectionImages() {
      if (this.uploadingImage) return;
      const remain = MAX_INSPECTION_IMAGE_COUNT - this.inspectionForm.images.length;
      if (remain <= 0) {
        uni.showToast({
          title: `最多上传 ${MAX_INSPECTION_IMAGE_COUNT} 张图片`,
          icon: 'none'
        });
        return;
      }
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const tempFiles = Array.isArray(res.tempFiles) && res.tempFiles.length
            ? res.tempFiles
            : (Array.isArray(res.tempFilePaths) ? res.tempFilePaths.map((path) => ({ path })) : []);
          if (!tempFiles.length) return;
          this.uploadingImage = true;
          this.inspectionForm.images = this.inspectionForm.images
            .concat(tempFiles.slice(0, remain).map((item, index) => createLocalInspectionImageItem(item, this.inspectionForm.images.length + index)))
            .slice(0, MAX_INSPECTION_IMAGE_COUNT);
          this.uploadingImage = false;
        }
      });
    },
    chooseInspectionVideo() {
      if (this.uploadingVideo) return;
      uni.chooseVideo({
        sourceType: ['album', 'camera'],
        compressed: true,
        maxDuration: 120,
        success: (res) => {
          if (!res || !res.tempFilePath) return;
          this.uploadingVideo = true;
          this.inspectionForm.video = createLocalInspectionVideoItem(res);
          this.uploadingVideo = false;
        }
      });
    },
    removeInspectionImage(index) {
      const next = this.inspectionForm.images.slice();
      next.splice(index, 1);
      this.inspectionForm.images = next;
    },
    removeInspectionVideo() {
      this.inspectionForm.video = null;
    },
    buildInspectionPayloadDraft() {
      const serviceFee = normalizeAmountInput(this.inspectionForm.serviceFee);
      const materialFee = normalizeAmountInput(this.inspectionForm.materialFee);
      if (serviceFee == null || materialFee == null) {
        uni.showToast({
          title: '费用格式不正确',
          icon: 'none'
        });
        return null;
      }
      const inspectionDiagnosis = String(this.inspectionForm.inspectionDiagnosis || '').trim();
      const repairPlan = String(this.inspectionForm.repairPlan || '').trim();
      if (!inspectionDiagnosis) {
        uni.showToast({
          title: '请填写问题说明',
          icon: 'none'
        });
        return null;
      }
      if (!repairPlan) {
        uni.showToast({
          title: '请填写维修方案',
          icon: 'none'
        });
        return null;
      }
      if (!this.inspectionForm.images.length && !this.inspectionForm.video) {
        uni.showToast({
          title: '请至少上传一份检查凭证',
          icon: 'none'
        });
        return null;
      }
      return {
        inspectionDiagnosis,
        repairPlan,
        serviceFee,
        materialFee,
        images: this.inspectionForm.images.slice(),
        video: this.inspectionForm.video ? { ...this.inspectionForm.video } : null
      };
    },
    buildFeeUpdatePayloadDraft() {
      const serviceFee = normalizeAmountInput(this.inspectionForm.serviceFee);
      const materialFee = normalizeAmountInput(this.inspectionForm.materialFee);
      if (serviceFee == null || materialFee == null) {
        uni.showToast({
          title: '费用格式不正确',
          icon: 'none'
        });
        return null;
      }
      return {
        serviceFee,
        materialFee
      };
    },
    uploadSingleInspectionMedia(filePath, mediaType) {
      return uploadWorkerInspectionMedia(filePath, mediaType).then((result) => {
        if (!result || result.code !== 200 || !result.data) {
          throw new Error((result && result.message) || '检查凭证上传失败');
        }
        return result.data;
      });
    },
    uploadPendingInspectionMedia(draft) {
      let imageChain = Promise.resolve([]);
      (draft.images || []).forEach((item, index) => {
        imageChain = imageChain.then((images) => {
          if (!item || !item.localPath) {
            images.push({
              url: item.url,
              name: item.name,
              fileSize: item.fileSize,
              mimeType: item.mimeType,
              width: item.width,
              height: item.height
            });
            return images;
          }
          return this.uploadSingleInspectionMedia(item.localPath, 'image').then((uploaded) => {
            images.push({
              url: uploaded.url || '',
              name: uploaded.name || item.name || `inspection-image-${index + 1}.jpg`,
              fileSize: uploaded.fileSize || item.fileSize || 0,
              mimeType: uploaded.mimeType || item.mimeType || 'image/jpeg',
              width: uploaded.width || item.width || null,
              height: uploaded.height || item.height || null
            });
            return images;
          });
        });
      });

      return imageChain.then((images) => {
        const draftVideo = draft.video;
        if (!draftVideo) {
          return { images, video: null };
        }
        if (!draftVideo.localPath) {
          return {
            images,
            video: {
              url: draftVideo.url,
              name: draftVideo.name,
              fileSize: draftVideo.fileSize,
              mimeType: draftVideo.mimeType,
              width: draftVideo.width,
              height: draftVideo.height,
              duration: draftVideo.duration,
              thumbnailUrl: draftVideo.thumbnailUrl
            }
          };
        }
        return this.uploadSingleInspectionMedia(draftVideo.localPath, 'video').then((uploaded) => ({
          images,
          video: {
            url: uploaded.url || '',
            name: uploaded.name || draftVideo.name || 'inspection-video.mp4',
            fileSize: uploaded.fileSize || draftVideo.fileSize || 0,
            mimeType: uploaded.mimeType || draftVideo.mimeType || 'video/mp4',
            width: uploaded.width || draftVideo.width || null,
            height: uploaded.height || draftVideo.height || null,
            duration: uploaded.duration || draftVideo.duration || 0,
            thumbnailUrl: uploaded.thumbnailUrl || draftVideo.thumbnailUrl || ''
          }
        }));
      });
    },
    submitInspection() {
      if (this.isFeeEditMode) {
        this.submitInspectionFeeUpdate();
        return;
      }
      if (this.submittingInspection || this.uploadingImage || this.uploadingVideo) return;
      const draft = this.buildInspectionPayloadDraft();
      if (!draft) return;
      this.submittingInspection = true;
      uni.showLoading({
        title: '正在上传凭证...',
        mask: true
      });
      this.uploadPendingInspectionMedia(draft)
        .then(({ images, video }) => submitWorkerInspection(this.orderId, {
          inspectionDiagnosis: draft.inspectionDiagnosis,
          repairPlan: draft.repairPlan,
          serviceFee: draft.serviceFee,
          materialFee: draft.materialFee,
          images,
          video
        }))
        .then((res) => {
          const success = this.applyDetailResponse(res, '检查结果已提交');
          if (success) {
            this.showInspectionPopup = false;
            this.inspectionPopupMode = 'submit';
            this.inspectionForm = this.createInspectionForm();
          }
        })
        .catch((error) => {
          showUploadErrorModal(error, {
            title: '提交检查结果失败',
            fallback: '提交检查结果失败'
          });
        })
        .finally(() => {
          uni.hideLoading();
          this.submittingInspection = false;
        });
    },
    submitInspectionFeeUpdate() {
      if (this.submittingInspection) return;
      const draft = this.buildFeeUpdatePayloadDraft();
      if (!draft) return;
      this.submittingInspection = true;
      updateWorkerInspectionFees(this.orderId, draft)
        .then((res) => {
          const success = this.applyDetailResponse(res, '费用已更新');
          if (success) {
            this.showInspectionPopup = false;
            this.inspectionPopupMode = 'submit';
            this.inspectionForm = this.createInspectionForm();
          }
        })
        .catch((error) => {
          uni.showToast({
            title: (error && error.message) || '修改费用失败',
            icon: 'none'
          });
        })
        .finally(() => {
          this.submittingInspection = false;
        });
    },
    applyDetailResponse(res, successText) {
      if (!res || res.code !== 200 || !res.data) {
        uni.showToast({
          title: (res && res.message) || '操作失败',
          icon: 'none'
        });
        return false;
      }
      this.detail = this.normalizeDetail(res.data);
      if (successText) {
        uni.showToast({
          title: successText,
          icon: 'success'
        });
      }
      return true;
    }
  }
};
</script>

<style scoped>
.worker-order-detail {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.1) 0, rgba(20, 184, 166, 0) 24%),
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.14) 0, rgba(59, 130, 246, 0) 30%),
    linear-gradient(180deg, #eef5ff 0%, #f8fbff 40%, #f5f7fb 100%);
}

.header {
  height: calc(84rpx + var(--status-bar-height));
  padding: var(--status-bar-height) 20rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  background-color: rgba(248, 251, 255, 0.92);
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.88);
  backdrop-filter: blur(16rpx);
}

.header-side {
  width: 72rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
}

.header-side-right {
  justify-content: flex-end;
}

.header-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.page-scroll {
  box-sizing: border-box;
  padding: 18rpx 20rpx 7.125rem;
  background: transparent;
}

.detail-content {
  background: transparent;
}

.page-scroll ::v-deep .uni-scroll-view,
.page-scroll ::v-deep .uni-scroll-view-content,
.page-scroll ::v-deep .uni-scroll-view-content-vertical {
  background: transparent !important;
}

.state-card,
.section-card,
.form-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.88);
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.02);
}

.state-card {
  padding: 28rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.state-title {
  color: #111827;
  font-size: 26rpx;
  font-weight: 600;
}

.state-desc {
  margin-top: 10rpx;
  color: #64748b;
  font-size: 22rpx;
  line-height: 1.5;
}

.state-action {
  margin-top: 18rpx;
  padding: 14rpx 26rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #1677ff 0%, #2dd4bf 100%);
  color: #ffffff;
  font-size: 24rpx;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 24rpx 22rpx;
  background: linear-gradient(135deg, #155eef 0%, #0ea5e9 52%, #14b8a6 100%);
  box-shadow: 0 4rpx 12rpx rgba(21, 94, 239, 0.08);
  border-radius: 24rpx;
}

.hero-card + .section-card {
  margin-top: 12rpx;
  position: static;
  z-index: auto;
}

.hero-card::before,
.hero-card::after {
  content: '';
  position: absolute;
  border-radius: 50%;
}

.hero-card::before {
  width: 220rpx;
  height: 220rpx;
  top: -78rpx;
  right: -58rpx;
  background: rgba(255, 255, 255, 0.12);
}

.hero-card::after {
  width: 160rpx;
  height: 160rpx;
  right: 86rpx;
  bottom: -68rpx;
  background: rgba(255, 255, 255, 0.08);
}

.hero-head,
.info-row,
.progress-head,
.form-head,
.video-meta,
.video-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.hero-head,
.hero-title,
.hero-order-row,
.hero-hint,
.hero-metrics {
  position: relative;
  z-index: 1;
}

.hero-status,
.hero-mode,
.hero-title,
.hero-order-no,
.hero-hint {
  color: #ffffff;
}

.hero-status-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
  flex: 1;
}

.hero-status-icon {
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-status {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
}

.hero-mode {
  flex-shrink: 0;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background-color: rgba(255, 255, 255, 0.18);
  font-size: 20rpx;
}

.hero-title {
  margin-top: 12rpx;
  font-size: 32rpx;
  font-weight: 600;
  line-height: 1.35;
}

.hero-order-row {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.hero-order-no {
  font-size: 22rpx;
  opacity: 0.92;
  word-break: break-all;
}

.hero-hint {
  margin-top: 12rpx;
  font-size: 22rpx;
  line-height: 1.5;
  opacity: 0.96;
}

.hero-metrics {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.hero-metric-card {
  padding: 16rpx 14rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8rpx);
}

.hero-metric-label {
  display: block;
  color: rgba(255, 255, 255, 0.74);
  font-size: 20rpx;
}

.hero-metric-value {
  display: block;
  margin-top: 8rpx;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.35;
}

.section-card {
  position: relative;
  overflow: hidden;
  margin-top: 14rpx;
  padding: 20rpx;
  
}

.section-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 6rpx;
  background-color: transparent;
}

.section-headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 0;
}

.section-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.section-icon-overview {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.15) 0%, rgba(59, 130, 246, 0.24) 100%);
}

.section-icon-fault {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.14) 0%, rgba(245, 158, 11, 0.24) 100%);
}

.section-icon-inspection {
  background: linear-gradient(135deg, rgba(13, 148, 136, 0.14) 0%, rgba(45, 212, 191, 0.24) 100%);
}

.section-icon-fee {
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.14) 0%, rgba(245, 158, 11, 0.24) 100%);
}

.section-icon-progress {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.14) 0%, rgba(168, 85, 247, 0.24) 100%);
}

.section-texts {
  min-width: 0;
}

.section-title {
  display: block;
  color: #111827;
  font-size: 28rpx;
  font-weight: 600;
}

.section-subtitle {
  display: block;
  margin-top: 4rpx;
  color: #94a3b8;
  font-size: 20rpx;
  line-height: 1.4;
}

.section-tag {
  flex-shrink: 0;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background-color: #f1f5f9;
  color: #475569;
  font-size: 20rpx;
}

.section-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(241, 245, 249, 0.9);
  color: #64748b;
}

.section-toggle-text {
  font-size: 20rpx;
  color: #64748b;
}

.section-card-collapsed {
  padding-bottom: 16rpx;
}

.section-card-collapsed .section-headline {
  margin-bottom: 0;
}

.info-row {
  min-height: 56rpx;
  padding: 10rpx 0;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.68);
}

.info-row + .info-row {
  margin-top: 0;
}

.info-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-label {
  flex-shrink: 0;
  width: 132rpx;
  color: #64748b;
  font-size: 20rpx;
}

.info-value {
  flex: 1;
  color: #334155;
  font-size: 22rpx;
  line-height: 1.5;
  text-align: right;
  white-space: pre-wrap;
  word-break: break-all;
}

.info-value-strong {
  color: #111827;
  font-size: 26rpx;
  font-weight: 600;
}

.fault-card {
  padding: 18rpx 16rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fbff 100%);
  border: 1rpx solid rgba(191, 219, 254, 0.68);
}

.fault-card + .fault-card {
  margin-top: 14rpx;
}

.fault-card-head {
  margin-bottom: 8rpx;
}

.fault-chip {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(249, 115, 22, 0.1);
}

.fault-chip-text {
  color: #c2410c;
  font-size: 20rpx;
  font-weight: 600;
}

.media-group {
  margin-top: 16rpx;
}

.media-title-row {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
}

.media-title {
  color: #64748b;
  font-size: 20rpx;
}

.image-list,
.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 12rpx;
}

.media-image,
.upload-image-card,
.upload-add-card {
  width: 176rpx;
  height: 176rpx;
  border-radius: 16rpx;
}

.media-image,
.upload-image {
  background-color: #e5e7eb;
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.4);
}

.upload-image-card {
  position: relative;
  overflow: hidden;
}

.upload-image {
  width: 100%;
  height: 100%;
}

.upload-remove {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background-color: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-add-card,
.upload-video-card {
  border: 2rpx dashed #cbd5e1;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-video-card {
  width: 100%;
  min-height: 200rpx;
  border-radius: 16rpx;
  margin-top: 12rpx;
}

.upload-add-text {
  margin-top: 10rpx;
  color: #64748b;
  font-size: 20rpx;
}

.video-card {
  margin-top: 12rpx;
  padding: 14rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
  border: 1rpx solid rgba(226, 232, 240, 0.84);
}

.video-card + .video-card {
  margin-top: 12rpx;
}

.video-card-popup {
  margin-top: 12rpx;
}

.video-preview {
  position: relative;
  width: 100%;
  height: 280rpx;
  overflow: hidden;
  border-radius: 16rpx;
  background-color: #0f172a;
}

.media-video,
.media-video-poster,
.media-video-fallback {
  width: 100%;
  height: 280rpx;
  border-radius: 16rpx;
}

.media-video {
  background-color: #0f172a;
}

.media-video-poster {
  display: block;
}

.media-video-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92) 0%, rgba(30, 41, 59, 0.96) 100%);
}

.video-play-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.08) 0%, rgba(15, 23, 42, 0.42) 100%);
}

.video-play-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background-color: rgba(15, 23, 42, 0.62);
  border: 1rpx solid rgba(255, 255, 255, 0.16);
}

.video-play-text {
  color: #ffffff;
  font-size: 22rpx;
  line-height: 1;
}

.video-meta {
  margin-top: 10rpx;
  align-items: flex-start;
  justify-content: flex-end;
}

.video-name,
.video-duration,
.progress-desc,
.progress-operator,
.empty-text,
.action-hint-text,
.inspection-subtitle,
.tip-text,
.form-tip {
  color: #64748b;
  font-size: 22rpx;
  line-height: 1.5;
}

.video-name {
  flex: 1;
}

.video-duration {
  flex-shrink: 0;
}

.video-actions {
  margin-top: 8rpx;
  justify-content: flex-end;
}

.video-action-link {
  color: #1677ff;
  font-size: 20rpx;
}

.progress-item {
  position: relative;
  padding-left: 26rpx;
}

.progress-item + .progress-item {
  margin-top: 16rpx;
}

.progress-item::before {
  content: '';
  position: absolute;
  left: 9rpx;
  top: 18rpx;
  bottom: -18rpx;
  width: 2rpx;
  background-color: #dbeafe;
}

.progress-item:last-child::before {
  display: none;
}

.progress-dot {
  position: absolute;
  left: 0;
  top: 8rpx;
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1677ff 0%, #36cfc9 100%);
  box-shadow: 0 0 0 6rpx rgba(22, 119, 255, 0.1);
}

.progress-main {
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
  border: 1rpx solid rgba(226, 232, 240, 0.88);
  border-radius: 18rpx;
  padding: 16rpx 16rpx 14rpx;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.04);
}

.progress-head {
  align-items: flex-start;
}

.progress-status {
  flex: 1;
  color: #111827;
  font-size: 24rpx;
  font-weight: 600;
}

.progress-time {
  flex-shrink: 0;
  color: #94a3b8;
  font-size: 20rpx;
}

.progress-desc,
.progress-operator,
.empty-text {
  display: block;
  margin-top: 6rpx;
}

.progress-desc {
  white-space: pre-wrap;
  word-break: break-word;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  padding: 16rpx 20rpx 24rpx;
  background: rgba(255, 255, 255, 0.92);
  border-top: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 -8rpx 28rpx rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18rpx);
}

.action-hint-block {
  margin-bottom: 12rpx;
}

.action-hint-title {
  display: block;
  color: #111827;
  font-size: 24rpx;
  font-weight: 600;
}

.action-hint-text {
  display: block;
  margin-top: 4rpx;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
}

.action-buttons-single {
  grid-template-columns: minmax(0, 1fr);
}

.inspection-popup {
  padding: 22rpx 20rpx 20rpx;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 20%);
}

.inspection-header {
  margin-bottom: 14rpx;
}

.inspection-title {
  display: block;
  color: #111827;
  font-size: 30rpx;
  font-weight: 600;
}

.inspection-subtitle {
  display: block;
  margin-top: 6rpx;
}

.inspection-scroll {
  max-height: 62vh;
}

.form-card {
  padding: 18rpx 16rpx;
}

.form-card + .form-card {
  margin-top: 12rpx;
}

.form-card-tip {
  background: linear-gradient(135deg, #eff6ff 0%, #f0fdfa 100%);
  border: 1rpx solid rgba(191, 219, 254, 0.8);
}

.form-label {
  display: block;
  color: #111827;
  font-size: 24rpx;
  font-weight: 600;
}

.form-tip {
  flex-shrink: 0;
}

.form-textarea {
  width: 100%;
  min-height: 148rpx;
  margin-top: 12rpx;
  padding: 16rpx 14rpx;
  border-radius: 16rpx;
  background-color: #f8fafc;
  border: 1rpx solid rgba(226, 232, 240, 0.88);
  box-sizing: border-box;
  color: #334155;
  font-size: 22rpx;
  line-height: 1.5;
}

.fee-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 12rpx;
}

.fee-item {
  padding: 14rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
}

.fee-label {
  display: block;
  color: #475569;
  font-size: 20rpx;
}

.fee-input {
  height: 64rpx;
  margin-top: 8rpx;
  color: #111827;
  font-size: 28rpx;
  font-weight: 600;
}

.fee-note {
  margin-top: 12rpx;
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1rpx solid rgba(191, 219, 254, 0.78);
}

.fee-note-text,
.auto-fee-subtitle,
.auto-fee-label {
  color: #64748b;
  font-size: 20rpx;
  line-height: 1.5;
}

.auto-fee-card {
  margin-top: 12rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%);
  border: 1rpx solid rgba(226, 232, 240, 0.9);
}

.auto-fee-head,
.auto-fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.auto-fee-title {
  color: #0f172a;
  font-size: 22rpx;
  font-weight: 600;
}

.auto-fee-row {
  padding-top: 12rpx;
}

.auto-fee-value {
  color: #334155;
  font-size: 22rpx;
  font-weight: 500;
}

.auto-fee-row-strong {
  margin-top: 6rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid rgba(226, 232, 240, 0.88);
}

.auto-fee-label-strong,
.auto-fee-value-strong {
  color: #111827;
  font-size: 24rpx;
  font-weight: 600;
}

.inspection-footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 16rpx;
}

.video-preview-popup {
  width: 680rpx;
  max-width: calc(100vw - 48rpx);
  padding: 20rpx;
  border-radius: 24rpx;
  background-color: #ffffff;
  box-sizing: border-box;
}

.video-preview-player {
  width: 100%;
  height: 420rpx;
  border-radius: 18rpx;
  background-color: #0f172a;
}

.video-preview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 16rpx;
}
</style>
