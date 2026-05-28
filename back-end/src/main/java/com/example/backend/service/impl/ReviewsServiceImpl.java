package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderItems;
import com.example.backend.entity.ProductOrders;
import com.example.backend.entity.Products;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.SystemMessages;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ReviewsMapper;
import com.example.backend.model.review.ReviewImageItemResponse;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.review.ReviewSubmitImageItem;
import com.example.backend.model.review.ReviewSubmitRequest;
import com.example.backend.model.review.ReviewUploadImageResponse;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderItemsService;
import com.example.backend.service.ProductOrdersService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewsServiceImpl extends ServiceImpl<ReviewsMapper, Reviews> implements ReviewsService {

    private static final String REVIEW_BUSINESS_TYPE = "REVIEW";
    private static final int ORDER_TYPE_REPAIR = 1;
    private static final int ORDER_TYPE_PRODUCT = 2;
    private static final int TARGET_TYPE_TECHNICIAN = 1;
    private static final int TARGET_TYPE_PRODUCT = 2;
    private static final int REPAIR_ORDER_STATUS_COMPLETED = 6;
    private static final int PRODUCT_ORDER_STATUS_PENDING_REVIEW = 4;
    private static final int PRODUCT_ORDER_STATUS_COMPLETED = 5;
    private static final int REVIEW_STATUS_NORMAL = 1;
    private static final int REVIEW_STATUS_HIDDEN = 2;
    private static final int USER_UPLOADER_TYPE = 1;
    private static final int MAX_IMAGE_COUNT = 5;
    private static final int SYSTEM_MESSAGE_FOR_WORKER = 2;
    private static final int SYSTEM_MESSAGE_TYPE_ORDER = 2;
    private static final int SYSTEM_MESSAGE_PRIORITY_HIGH = 1;
    private static final String WORKER_REVIEW_MESSAGE_TYPE = "USER_REVIEW_NOTIFY_WORKER";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

    private final RepairOrdersService repairOrdersService;
    private final ProductOrdersService productOrdersService;
    private final OrderItemsService orderItemsService;
    private final ProductsService productsService;
    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final ServiceTypesService serviceTypesService;
    private final ImagesService imagesService;
    private final SystemMessagesService systemMessagesService;
    private final OssUtil ossUtil;

    public ReviewsServiceImpl(
        RepairOrdersService repairOrdersService,
        ProductOrdersService productOrdersService,
        OrderItemsService orderItemsService,
        ProductsService productsService,
        UserAccountsService userAccountsService,
        TechnicianAccountsService technicianAccountsService,
        ServiceTypesService serviceTypesService,
        ImagesService imagesService,
        SystemMessagesService systemMessagesService,
        OssUtil ossUtil
    ) {
        this.repairOrdersService = repairOrdersService;
        this.productOrdersService = productOrdersService;
        this.orderItemsService = orderItemsService;
        this.productsService = productsService;
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.serviceTypesService = serviceTypesService;
        this.imagesService = imagesService;
        this.systemMessagesService = systemMessagesService;
        this.ossUtil = ossUtil;
    }

    @Override
    public Reviews getUserOrderReviewEntity(String orderId, String accountId) {
        String normalizedOrderId = trimToNull(orderId);
        String normalizedAccountId = trimToNull(accountId);
        if (!StringUtils.hasText(normalizedOrderId) || !StringUtils.hasText(normalizedAccountId)) {
            return null;
        }
        return getOne(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getOrderId, normalizedOrderId)
                .eq(Reviews::getAccountId, normalizedAccountId)
                .eq(Reviews::getOrderType, ORDER_TYPE_REPAIR)
                .eq(Reviews::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    @Override
    public Reviews getUserProductOrderReviewEntity(String orderId, String accountId) {
        String normalizedOrderId = trimToNull(orderId);
        String normalizedAccountId = trimToNull(accountId);
        if (!StringUtils.hasText(normalizedOrderId) || !StringUtils.hasText(normalizedAccountId)) {
            return null;
        }
        return getOne(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getOrderId, normalizedOrderId)
                .eq(Reviews::getAccountId, normalizedAccountId)
                .eq(Reviews::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(Reviews::getTargetType, TARGET_TYPE_PRODUCT)
                .eq(Reviews::getIsDelete, 0)
                .orderByAsc(Reviews::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewItemResponse submitUserReview(String accountId, ReviewSubmitRequest request) {
        String normalizedAccountId = trimToNull(accountId);
        String orderId = trimToNull(request == null ? null : request.getOrderId());
        Integer rating = request == null ? null : request.getRating();
        String content = trimToNull(request == null ? null : request.getContent());
        List<ReviewSubmitImageItem> images = normalizeImages(request == null ? null : request.getImages());
        int anonymous = normalizeAnonymousFlag(request == null ? null : request.getIsAnonymous());

        if (!StringUtils.hasText(normalizedAccountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分范围应为1到5星");
        }
        if (!StringUtils.hasText(content) && images.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写评价内容或上传图片");
        }

        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getAccountId, normalizedAccountId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (safeInt(order.getStatus()) != REPAIR_ORDER_STATUS_COMPLETED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已完成订单可评价");
        }
        if (!StringUtils.hasText(order.getTechnicianAccountId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单暂无可评价师傅");
        }
        if (getUserOrderReviewEntity(orderId, normalizedAccountId) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单已评价");
        }

        long now = System.currentTimeMillis();
        Reviews review = new Reviews();
        review.setId(SnowflakeIdUtil.nextReviewId());
        review.setAccountId(normalizedAccountId);
        review.setOrderId(orderId);
        review.setOrderType(ORDER_TYPE_REPAIR);
        review.setTargetId(order.getTechnicianAccountId());
        review.setTargetType(TARGET_TYPE_TECHNICIAN);
        review.setRating(rating);
        review.setContent(StringUtils.hasText(content) ? content : "");
        review.setIsAnonymous(anonymous);
        review.setStatus(REVIEW_STATUS_NORMAL);
        review.setReplyContent(null);
        review.setReplyTime(null);
        review.setCreatedTime(now);
        review.setUpdatedTime(now);
        review.setVersion(0);
        review.setIsDelete(0);
        if (!save(review)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交评价失败");
        }

        saveReviewImages(review.getId(), normalizedAccountId, USER_UPLOADER_TYPE, images, now);
        notifyWorkerReviewSubmitted(order, review, serviceTypesService.getById(order.getServiceTypeId()), now);
        refreshTechnicianRating(order.getTechnicianAccountId());
        return buildReviewResponse(requireReview(review.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewItemResponse submitUserProductReview(String accountId, ReviewSubmitRequest request) {
        String normalizedAccountId = trimToNull(accountId);
        String orderId = trimToNull(request == null ? null : request.getOrderId());
        Integer rating = request == null ? null : request.getRating();
        String content = trimToNull(request == null ? null : request.getContent());
        List<ReviewSubmitImageItem> images = normalizeImages(request == null ? null : request.getImages());
        int anonymous = normalizeAnonymousFlag(request == null ? null : request.getIsAnonymous());

        if (!StringUtils.hasText(normalizedAccountId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分范围应为1到5星");
        }
        if (!StringUtils.hasText(content) && images.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写评价内容或上传图片");
        }

        ProductOrders order = productOrdersService.getOne(
            new LambdaQueryWrapper<ProductOrders>()
                .eq(ProductOrders::getId, orderId)
                .eq(ProductOrders::getAccountId, normalizedAccountId)
                .eq(ProductOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品订单不存在");
        }
        int orderStatus = safeInt(order.getOrderStatus());
        if (orderStatus != PRODUCT_ORDER_STATUS_PENDING_REVIEW && orderStatus != PRODUCT_ORDER_STATUS_COMPLETED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前商品订单暂不可评价");
        }
        if (getUserProductOrderReviewEntity(orderId, normalizedAccountId) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单已评价");
        }

        List<OrderItems> orderItems = orderItemsService.list(
            new LambdaQueryWrapper<OrderItems>()
                .eq(OrderItems::getOrderId, orderId)
                .eq(OrderItems::getIsDelete, 0)
                .orderByAsc(OrderItems::getCreatedTime)
        );
        List<OrderItems> reviewItems = orderItems.stream()
            .filter(item -> item != null && StringUtils.hasText(trimToNull(item.getProductId())))
            .collect(Collectors.toList());
        if (reviewItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单暂无可评价商品");
        }

        long now = System.currentTimeMillis();
        String firstReviewId = null;
        Set<String> productIds = new LinkedHashSet<>();
        for (OrderItems item : reviewItems) {
            String productId = trimToNull(item.getProductId());
            if (!productIds.add(productId)) {
                continue;
            }
            Reviews review = new Reviews();
            review.setId(SnowflakeIdUtil.nextReviewId());
            review.setAccountId(normalizedAccountId);
            review.setOrderId(orderId);
            review.setOrderType(ORDER_TYPE_PRODUCT);
            review.setTargetId(productId);
            review.setTargetType(TARGET_TYPE_PRODUCT);
            review.setRating(rating);
            review.setContent(StringUtils.hasText(content) ? content : "");
            review.setIsAnonymous(anonymous);
            review.setStatus(REVIEW_STATUS_NORMAL);
            review.setReplyContent(null);
            review.setReplyTime(null);
            review.setCreatedTime(now);
            review.setUpdatedTime(now);
            review.setVersion(0);
            review.setIsDelete(0);
            if (!save(review)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交商品评价失败");
            }
            saveReviewImages(review.getId(), normalizedAccountId, USER_UPLOADER_TYPE, images, now);
            if (firstReviewId == null) {
                firstReviewId = review.getId();
            }
        }

        if (!StringUtils.hasText(firstReviewId)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交商品评价失败");
        }

        if (orderStatus == PRODUCT_ORDER_STATUS_PENDING_REVIEW) {
            order.setOrderStatus(PRODUCT_ORDER_STATUS_COMPLETED);
            order.setUpdatedTime(now);
            if (!productOrdersService.updateById(order)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新商品订单状态失败");
            }
        }
        return buildReviewResponse(requireReview(firstReviewId));
    }

    @Override
    public ReviewItemResponse getUserOrderReview(String orderId, String accountId) {
        Reviews review = getUserOrderReviewEntity(orderId, accountId);
        return review == null ? null : buildReviewResponse(review);
    }

    @Override
    public ReviewItemResponse getUserProductOrderReview(String orderId, String accountId) {
        Reviews review = getUserProductOrderReviewEntity(orderId, accountId);
        return review == null ? null : buildReviewResponse(review);
    }

    @Override
    public List<ReviewItemResponse> listPublicTechnicianReviews(String technicianId) {
        String normalizedTechnicianId = trimToNull(technicianId);
        if (!StringUtils.hasText(normalizedTechnicianId)) {
            return Collections.emptyList();
        }
        List<Reviews> list = list(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getTargetId, normalizedTechnicianId)
                .eq(Reviews::getTargetType, TARGET_TYPE_TECHNICIAN)
                .eq(Reviews::getStatus, REVIEW_STATUS_NORMAL)
                .eq(Reviews::getIsDelete, 0)
                .orderByDesc(Reviews::getCreatedTime)
        );
        return buildReviewResponses(list);
    }

    @Override
    public List<ReviewItemResponse> listPublicProductReviews(String productId) {
        String normalizedProductId = trimToNull(productId);
        if (!StringUtils.hasText(normalizedProductId)) {
            return Collections.emptyList();
        }
        List<Reviews> list = list(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getTargetId, normalizedProductId)
                .eq(Reviews::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(Reviews::getTargetType, TARGET_TYPE_PRODUCT)
                .eq(Reviews::getStatus, REVIEW_STATUS_NORMAL)
                .eq(Reviews::getIsDelete, 0)
                .orderByDesc(Reviews::getCreatedTime)
        );
        return buildReviewResponses(list);
    }

    @Override
    public List<ReviewItemResponse> listWorkerReviews(String technicianId) {
        String normalizedTechnicianId = trimToNull(technicianId);
        if (!StringUtils.hasText(normalizedTechnicianId)) {
            return Collections.emptyList();
        }
        List<Reviews> list = list(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getTargetId, normalizedTechnicianId)
                .eq(Reviews::getTargetType, TARGET_TYPE_TECHNICIAN)
                .eq(Reviews::getIsDelete, 0)
                .orderByDesc(Reviews::getCreatedTime)
        );
        return buildReviewResponses(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewItemResponse replyWorkerReview(String reviewId, String technicianId, String replyContent) {
        String normalizedReviewId = trimToNull(reviewId);
        String normalizedTechnicianId = trimToNull(technicianId);
        String normalizedReplyContent = trimToNull(replyContent);
        if (!StringUtils.hasText(normalizedReviewId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价ID不能为空");
        }
        if (!StringUtils.hasText(normalizedTechnicianId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权回复当前评价");
        }
        if (!StringUtils.hasText(normalizedReplyContent)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }

        Reviews review = requireReview(normalizedReviewId);
        if (safeInt(review.getTargetType()) != TARGET_TYPE_TECHNICIAN
            || !normalizedTechnicianId.equals(trimToNull(review.getTargetId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权回复当前评价");
        }
        if (StringUtils.hasText(trimToNull(review.getReplyContent()))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前评价已回复");
        }

        long now = System.currentTimeMillis();
        review.setReplyContent(normalizedReplyContent);
        review.setReplyTime(now);
        review.setUpdatedTime(now);
        if (!updateById(review)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "回复评价失败");
        }
        return buildReviewResponse(requireReview(normalizedReviewId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewItemResponse replyAdminReview(String reviewId, String replyContent) {
        String normalizedReviewId = trimToNull(reviewId);
        String normalizedReplyContent = trimToNull(replyContent);
        if (!StringUtils.hasText(normalizedReviewId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价ID不能为空");
        }
        if (!StringUtils.hasText(normalizedReplyContent)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }

        Reviews review = requireReview(normalizedReviewId);
        if (StringUtils.hasText(trimToNull(review.getReplyContent()))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前评价已回复");
        }

        long now = System.currentTimeMillis();
        review.setReplyContent(normalizedReplyContent);
        review.setReplyTime(now);
        review.setUpdatedTime(now);
        if (!updateById(review)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "回复评价失败");
        }
        return buildReviewResponse(requireReview(normalizedReviewId));
    }

    @Override
    public Page<ReviewItemResponse> pageAdminReviews(
        long pageNum,
        long pageSize,
        String keyword,
        Integer reviewType,
        Integer status,
        Integer rating,
        Integer hasReply,
        java.util.Set<String> targetIds
    ) {
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<Reviews> wrapper = new LambdaQueryWrapper<Reviews>()
            .eq(Reviews::getIsDelete, 0);
        int normalizedReviewType = safeInt(reviewType);
        if (normalizedReviewType == ORDER_TYPE_REPAIR) {
            wrapper.eq(Reviews::getOrderType, ORDER_TYPE_REPAIR)
                .eq(Reviews::getTargetType, TARGET_TYPE_TECHNICIAN);
        } else if (normalizedReviewType == ORDER_TYPE_PRODUCT) {
            wrapper.eq(Reviews::getOrderType, ORDER_TYPE_PRODUCT)
                .eq(Reviews::getTargetType, TARGET_TYPE_PRODUCT);
        }
        if (status != null) {
            wrapper.eq(Reviews::getStatus, status);
        }
        if (rating != null) {
            wrapper.eq(Reviews::getRating, rating);
        }
        if (hasReply != null) {
            if (hasReply == 1) {
                wrapper.and(q -> q.isNotNull(Reviews::getReplyContent).ne(Reviews::getReplyContent, ""));
            } else if (hasReply == 0) {
                wrapper.and(q -> q.isNull(Reviews::getReplyContent).or().eq(Reviews::getReplyContent, ""));
            }
        }
        if (targetIds != null && !targetIds.isEmpty()) {
            wrapper.in(Reviews::getTargetId, targetIds);
        }
        applyKeywordFilter(wrapper, keyword);
        wrapper.orderByDesc(Reviews::getCreatedTime);

        Page<Reviews> page = page(new Page<>(currentPage, currentSize), wrapper);
        Page<ReviewItemResponse> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(buildReviewResponses(page.getRecords()));
        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewItemResponse updateAdminReviewStatus(String reviewId, Integer status) {
        String normalizedReviewId = trimToNull(reviewId);
        if (!StringUtils.hasText(normalizedReviewId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价ID不能为空");
        }
        int normalizedStatus = safeInt(status);
        if (normalizedStatus != REVIEW_STATUS_NORMAL && normalizedStatus != REVIEW_STATUS_HIDDEN) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价状态不正确");
        }

        Reviews review = requireReview(normalizedReviewId);
        if (safeInt(review.getStatus()) != normalizedStatus) {
            review.setStatus(normalizedStatus);
            review.setUpdatedTime(System.currentTimeMillis());
            if (!updateById(review)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新评价状态失败");
            }
            if (safeInt(review.getTargetType()) == TARGET_TYPE_TECHNICIAN) {
                refreshTechnicianRating(review.getTargetId());
            }
        }
        return buildReviewResponse(requireReview(normalizedReviewId));
    }

    @Override
    public ReviewUploadImageResponse uploadReviewImage(MultipartFile file, String uploaderId, Integer uploaderType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传图片不能为空");
        }
        UploadLimitUtil.validateImageSize(file);
        String originalFilename = resolveMediaName(file.getOriginalFilename(), "review-image.jpg");
        String mimeType = trimToNull(file.getContentType());
        if (!isImageFile(originalFilename, mimeType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持上传图片文件");
        }

        String extension = resolveImageExtension(originalFilename, mimeType);
        String objectName = "reviews/images/" + UUID.randomUUID() + extension;
        String fileUrl;
        try (InputStream inputStream = file.getInputStream()) {
            fileUrl = ossUtil.upload(objectName, inputStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传评价图片失败");
        }

        ReviewUploadImageResponse response = new ReviewUploadImageResponse();
        response.setUrl(fileUrl);
        response.setName(originalFilename);
        response.setFileSize(file.getSize());
        response.setMimeType(StringUtils.hasText(mimeType) ? mimeType : "image/jpeg");
        fillImageSize(file, response);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshTechnicianRating(String technicianId) {
        String normalizedTechnicianId = trimToNull(technicianId);
        if (!StringUtils.hasText(normalizedTechnicianId)) {
            return;
        }
        TechnicianAccounts technician = technicianAccountsService.getById(normalizedTechnicianId);
        if (technician == null || safeInt(technician.getIsDelete()) == 1) {
            return;
        }

        List<Reviews> reviews = list(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getTargetId, normalizedTechnicianId)
                .eq(Reviews::getTargetType, TARGET_TYPE_TECHNICIAN)
                .eq(Reviews::getStatus, REVIEW_STATUS_NORMAL)
                .eq(Reviews::getIsDelete, 0)
        );
        BigDecimal rating = ZERO;
        if (!reviews.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (Reviews review : reviews) {
                total = total.add(BigDecimal.valueOf(safeInt(review.getRating())));
            }
            rating = total.divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
        }
        technician.setRating(rating);
        technician.setUpdatedTime(System.currentTimeMillis());
        technicianAccountsService.updateById(technician);
    }

    private void applyKeywordFilter(LambdaQueryWrapper<Reviews> wrapper, String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return;
        }

        Set<String> repairOrderIds = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getIsDelete, 0)
                .and(q -> q.like(RepairOrders::getOrderNo, normalizedKeyword).or().like(RepairOrders::getId, normalizedKeyword))
        ).stream().map(RepairOrders::getId).filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> productOrderIds = productOrdersService.list(
            new LambdaQueryWrapper<ProductOrders>()
                .eq(ProductOrders::getIsDelete, 0)
                .and(q -> q.like(ProductOrders::getOrderNo, normalizedKeyword).or().like(ProductOrders::getId, normalizedKeyword))
        ).stream().map(ProductOrders::getId).filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> userIds = userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .eq(UserAccounts::getIsDelete, 0)
                .and(q -> q.like(UserAccounts::getUsername, normalizedKeyword)
                    .or().like(UserAccounts::getPhone, normalizedKeyword)
                    .or().like(UserAccounts::getId, normalizedKeyword))
        ).stream().map(UserAccounts::getId).filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> technicianIds = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getIsDelete, 0)
                .and(q -> q.like(TechnicianAccounts::getUsername, normalizedKeyword)
                    .or().like(TechnicianAccounts::getPhone, normalizedKeyword)
                    .or().like(TechnicianAccounts::getId, normalizedKeyword))
        ).stream().map(TechnicianAccounts::getId).filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> productIds = productsService.list(
            new LambdaQueryWrapper<Products>()
                .eq(Products::getIsDelete, 0)
                .and(q -> q.like(Products::getName, normalizedKeyword)
                    .or().like(Products::getProductNo, normalizedKeyword)
                    .or().like(Products::getBrand, normalizedKeyword)
                    .or().like(Products::getModel, normalizedKeyword)
                    .or().like(Products::getId, normalizedKeyword))
        ).stream().map(Products::getId).filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));

        wrapper.and(q -> {
            q.like(Reviews::getContent, normalizedKeyword);
            if (!repairOrderIds.isEmpty()) {
                q.or().in(Reviews::getOrderId, repairOrderIds);
            }
            if (!productOrderIds.isEmpty()) {
                q.or().in(Reviews::getOrderId, productOrderIds);
            }
            if (!userIds.isEmpty()) {
                q.or().in(Reviews::getAccountId, userIds);
            }
            if (!technicianIds.isEmpty()) {
                q.or().in(Reviews::getTargetId, technicianIds);
            }
            if (!productIds.isEmpty()) {
                q.or().in(Reviews::getTargetId, productIds);
            }
        });
    }

    private Reviews requireReview(String reviewId) {
        Reviews review = getOne(
            new LambdaQueryWrapper<Reviews>()
                .eq(Reviews::getId, reviewId)
                .eq(Reviews::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }

    private List<ReviewItemResponse> buildReviewResponses(List<Reviews> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> repairOrderIds = reviews.stream()
            .filter(item -> safeInt(item.getOrderType()) == ORDER_TYPE_REPAIR)
            .map(Reviews::getOrderId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> productOrderIds = reviews.stream()
            .filter(item -> safeInt(item.getOrderType()) == ORDER_TYPE_PRODUCT)
            .map(Reviews::getOrderId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> userIds = reviews.stream()
            .map(Reviews::getAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> technicianIds = reviews.stream()
            .filter(item -> safeInt(item.getTargetType()) == TARGET_TYPE_TECHNICIAN)
            .map(Reviews::getTargetId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> productIds = reviews.stream()
            .filter(item -> safeInt(item.getTargetType()) == TARGET_TYPE_PRODUCT)
            .map(Reviews::getTargetId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> reviewIds = reviews.stream()
            .map(Reviews::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, RepairOrders> orderMap = repairOrderIds.isEmpty()
            ? Collections.emptyMap()
            : repairOrdersService.list(
                new LambdaQueryWrapper<RepairOrders>()
                    .in(RepairOrders::getId, repairOrderIds)
                    .eq(RepairOrders::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(RepairOrders::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Map<String, ProductOrders> productOrderMap = productOrderIds.isEmpty()
            ? Collections.emptyMap()
            : productOrdersService.list(
                new LambdaQueryWrapper<ProductOrders>()
                    .in(ProductOrders::getId, productOrderIds)
                    .eq(ProductOrders::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(ProductOrders::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Map<String, UserAccounts> userMap = userIds.isEmpty()
            ? Collections.emptyMap()
            : userAccountsService.list(
                new LambdaQueryWrapper<UserAccounts>()
                    .in(UserAccounts::getId, userIds)
                    .eq(UserAccounts::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(UserAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Map<String, TechnicianAccounts> technicianMap = technicianIds.isEmpty()
            ? Collections.emptyMap()
            : technicianAccountsService.list(
                new LambdaQueryWrapper<TechnicianAccounts>()
                    .in(TechnicianAccounts::getId, technicianIds)
                    .eq(TechnicianAccounts::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(TechnicianAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Map<String, Products> productMap = productIds.isEmpty()
            ? Collections.emptyMap()
            : productsService.list(
                new LambdaQueryWrapper<Products>()
                    .in(Products::getId, productIds)
                    .eq(Products::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(Products::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Set<String> serviceTypeIds = orderMap.values().stream()
            .map(RepairOrders::getServiceTypeId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ServiceTypes> serviceTypeMap = serviceTypeIds.isEmpty()
            ? Collections.emptyMap()
            : serviceTypesService.list(
                new LambdaQueryWrapper<ServiceTypes>()
                    .in(ServiceTypes::getId, serviceTypeIds)
                    .eq(ServiceTypes::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(ServiceTypes::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Map<String, List<ReviewImageItemResponse>> imageMap = listReviewImages(reviewIds);

        List<ReviewItemResponse> responseList = new ArrayList<>();
        for (Reviews review : reviews) {
            RepairOrders order = orderMap.get(review.getOrderId());
            UserAccounts user = userMap.get(review.getAccountId());
            TechnicianAccounts technician = technicianMap.get(review.getTargetId());
            Products product = productMap.get(review.getTargetId());
            ServiceTypes serviceType = order == null ? null : serviceTypeMap.get(order.getServiceTypeId());

            ReviewItemResponse item = new ReviewItemResponse();
            item.setId(review.getId());
            item.setOrderId(review.getOrderId());
            ProductOrders productOrder = productOrderMap.get(review.getOrderId());
            item.setOrderNo(safeInt(review.getOrderType()) == ORDER_TYPE_PRODUCT
                ? (productOrder == null ? "" : safe(productOrder.getOrderNo()))
                : (order == null ? "" : safe(order.getOrderNo())));
            item.setOrderType(review.getOrderType());
            item.setOrderTypeText(getOrderTypeText(review.getOrderType()));
            item.setRating(review.getRating());
            item.setContent(safe(review.getContent()));
            item.setIsAnonymous(review.getIsAnonymous());
            item.setStatus(review.getStatus());
            item.setStatusText(getReviewStatusText(review.getStatus()));
            item.setCreatedTime(review.getCreatedTime());
            item.setUpdatedTime(review.getUpdatedTime());
            item.setUserId(review.getAccountId());
            item.setUserName(user == null ? "" : safe(user.getUsername()));
            item.setUserDisplayName(resolveDisplayUserName(review, user));
            item.setTechnicianId(safeInt(review.getTargetType()) == TARGET_TYPE_TECHNICIAN ? safe(review.getTargetId()) : "");
            item.setTechnicianName(technician == null ? "" : safe(technician.getUsername()));
            item.setServiceTypeName(serviceType == null ? "" : safe(serviceType.getName()));
            item.setProductId(safeInt(review.getTargetType()) == TARGET_TYPE_PRODUCT ? safe(review.getTargetId()) : "");
            item.setProductName(product == null ? "" : safe(product.getName()));
            item.setReplyContent(safe(review.getReplyContent()));
            item.setReplyTime(review.getReplyTime());
            item.setHasReply(StringUtils.hasText(trimToNull(review.getReplyContent())));
            item.setCanReply(!item.getHasReply());
            item.setImages(imageMap.getOrDefault(review.getId(), Collections.emptyList()));
            responseList.add(item);
        }
        return responseList;
    }

    private ReviewItemResponse buildReviewResponse(Reviews review) {
        List<ReviewItemResponse> list = buildReviewResponses(Collections.singletonList(review));
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, List<ReviewImageItemResponse>> listReviewImages(Set<String> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Images> imageList = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, REVIEW_BUSINESS_TYPE)
                .in(Images::getBusinessId, reviewIds)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
        );
        Map<String, List<ReviewImageItemResponse>> imageMap = new LinkedHashMap<>();
        for (Images image : imageList) {
            if (image == null || !StringUtils.hasText(image.getBusinessId()) || !StringUtils.hasText(image.getFileUrl())) {
                continue;
            }
            ReviewImageItemResponse item = new ReviewImageItemResponse();
            item.setId(image.getId());
            item.setUrl(image.getFileUrl());
            item.setThumbnailUrl(image.getFileUrl());
            item.setName(resolveMediaName(image.getOriginalName(), image.getFileName()));
            item.setWidth(image.getWidth());
            item.setHeight(image.getHeight());
            imageMap.computeIfAbsent(image.getBusinessId(), key -> new ArrayList<>()).add(item);
        }
        return imageMap;
    }

    private void saveReviewImages(
        String reviewId,
        String uploaderId,
        Integer uploaderType,
        List<ReviewSubmitImageItem> images,
        long now
    ) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int i = 0; i < images.size(); i++) {
            ReviewSubmitImageItem image = images.get(i);
            String fileUrl = trimToNull(image.getUrl());
            if (!StringUtils.hasText(fileUrl)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "评价图片地址不能为空");
            }
            String fileName = resolveMediaName(image.getName(), "review-image-" + (i + 1) + ".jpg");
            Images entity = new Images();
            entity.setId(SnowflakeIdUtil.nextImageId());
            entity.setOriginalName(fileName);
            entity.setFileName(fileName);
            entity.setFilePath(fileUrl);
            entity.setFileUrl(fileUrl);
            entity.setFileSize(image.getFileSize() == null ? 0L : image.getFileSize());
            entity.setMimeType(resolveImageMimeType(image.getMimeType()));
            entity.setWidth(image.getWidth());
            entity.setHeight(image.getHeight());
            entity.setUploaderId(uploaderId);
            entity.setUploaderType(uploaderType);
            entity.setBusinessType(REVIEW_BUSINESS_TYPE);
            entity.setBusinessId(reviewId);
            entity.setCreatedTime(now);
            entity.setIsDelete(0);
            if (!imagesService.save(entity)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存评价图片失败");
            }
        }
    }

    private void notifyWorkerReviewSubmitted(RepairOrders order, Reviews review, ServiceTypes serviceType, long now) {
        if (order == null || review == null || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return;
        }
        UserAccounts user = getUserAccount(review.getAccountId());
        String userName = safeInt(review.getIsAnonymous()) == 1
            ? "匿名用户"
            : (user == null ? "用户" : safe(user.getUsername()));
        String serviceName = serviceType == null ? "" : safe(serviceType.getName());
        String title = "收到新的服务评价";
        StringBuilder content = new StringBuilder()
            .append(userName)
            .append("已对订单")
            .append(safe(order.getOrderNo()))
            .append("完成评价");
        if (StringUtils.hasText(serviceName)) {
            content.append("，服务项目：").append(serviceName);
        }
        if (review.getRating() != null && review.getRating() > 0) {
            content.append("，评分：").append(review.getRating()).append("星");
        }
        if (StringUtils.hasText(trimToNull(review.getContent()))) {
            String text = trimToNull(review.getContent());
            content.append("，评价：").append(text.length() > 30 ? text.substring(0, 30) + "..." : text);
        }
        saveWorkerSystemMessage(
            order.getTechnicianAccountId(),
            title,
            content.toString(),
            WORKER_REVIEW_MESSAGE_TYPE,
            review.getId(),
            now
        );
    }

    private void saveWorkerSystemMessage(
        String workerId,
        String title,
        String content,
        String businessType,
        String businessId,
        long now
    ) {
        if (!StringUtils.hasText(workerId) || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return;
        }
        SystemMessages message = new SystemMessages();
        message.setId(SnowflakeIdUtil.nextSystemMessageId());
        message.setReceiverId(workerId);
        message.setReceiverType(SYSTEM_MESSAGE_FOR_WORKER);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(SYSTEM_MESSAGE_TYPE_ORDER);
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        message.setPriority(SYSTEM_MESSAGE_PRIORITY_HIGH);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(0);
        message.setIsDelete(0);
        if (!systemMessagesService.save(message)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存评价提醒消息失败");
        }
    }

    private List<ReviewSubmitImageItem> normalizeImages(List<ReviewSubmitImageItem> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewSubmitImageItem> normalized = images.stream().filter(item -> item != null).collect(Collectors.toList());
        if (normalized.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价图片最多上传5张");
        }
        return normalized;
    }

    private int normalizeAnonymousFlag(Integer isAnonymous) {
        return safeInt(isAnonymous) == 1 ? 1 : 0;
    }

    private String getOrderTypeText(Integer orderType) {
        int value = safeInt(orderType);
        if (value == ORDER_TYPE_REPAIR) {
            return "服务评价";
        }
        if (value == ORDER_TYPE_PRODUCT) {
            return "商品评价";
        }
        return "评价";
    }

    private String getReviewStatusText(Integer status) {
        int value = safeInt(status);
        if (value == REVIEW_STATUS_NORMAL) {
            return "正常";
        }
        if (value == REVIEW_STATUS_HIDDEN) {
            return "已隐藏";
        }
        return "未知";
    }

    private String resolveDisplayUserName(Reviews review, UserAccounts user) {
        if (safeInt(review == null ? null : review.getIsAnonymous()) == 1) {
            return "匿名用户";
        }
        return user == null ? "" : safe(user.getUsername());
    }

    private UserAccounts getUserAccount(String accountId) {
        String normalizedAccountId = trimToNull(accountId);
        if (!StringUtils.hasText(normalizedAccountId)) {
            return null;
        }
        UserAccounts user = userAccountsService.getById(normalizedAccountId);
        return user == null || safeInt(user.getIsDelete()) == 1 ? null : user;
    }

    private void fillImageSize(MultipartFile file, ReviewUploadImageResponse response) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                response.setWidth(image.getWidth());
                response.setHeight(image.getHeight());
            }
        } catch (IOException ignored) {
            // ignore
        }
    }

    private boolean isImageFile(String originalFilename, String mimeType) {
        String normalizedMimeType = trimToNull(mimeType);
        if (StringUtils.hasText(normalizedMimeType) && normalizedMimeType.toLowerCase().startsWith("image/")) {
            return true;
        }
        String filename = trimToNull(originalFilename);
        if (!StringUtils.hasText(filename)) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".jpg")
            || lowerName.endsWith(".jpeg")
            || lowerName.endsWith(".png")
            || lowerName.endsWith(".webp")
            || lowerName.endsWith(".gif")
            || lowerName.endsWith(".bmp");
    }

    private String resolveImageExtension(String originalFilename, String mimeType) {
        String filename = trimToNull(originalFilename);
        if (StringUtils.hasText(filename)) {
            int index = filename.lastIndexOf('.');
            if (index >= 0 && index < filename.length() - 1) {
                return filename.substring(index);
            }
        }
        String normalizedMimeType = trimToNull(mimeType);
        if ("image/png".equalsIgnoreCase(normalizedMimeType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(normalizedMimeType)) {
            return ".webp";
        }
        if ("image/gif".equalsIgnoreCase(normalizedMimeType)) {
            return ".gif";
        }
        if ("image/bmp".equalsIgnoreCase(normalizedMimeType)) {
            return ".bmp";
        }
        return ".jpg";
    }

    private String resolveImageMimeType(String mimeType) {
        String normalizedMimeType = trimToNull(mimeType);
        return StringUtils.hasText(normalizedMimeType) ? normalizedMimeType : "image/jpeg";
    }

    private String resolveMediaName(String value, String fallback) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
