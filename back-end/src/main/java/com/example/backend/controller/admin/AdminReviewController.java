package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Products;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.review.ReviewReplyRequest;
import com.example.backend.model.review.ReviewStatusUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ProductsService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.TechnicianAccountsService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewsService reviewsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final ProductsService productsService;

    public AdminReviewController(
        ReviewsService reviewsService,
        TechnicianAccountsService technicianAccountsService,
        ProductsService productsService
    ) {
        this.reviewsService = reviewsService;
        this.technicianAccountsService = technicianAccountsService;
        this.productsService = productsService;
    }

    @GetMapping
    public Result<Page<ReviewItemResponse>> pageReviews(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "reviewType", required = false) Integer reviewType,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "rating", required = false) Integer rating,
        @RequestParam(value = "hasReply", required = false) Integer hasReply
    ) {
        LoginUserInfo admin = requireAdmin();
        Set<String> targetIds = buildStoreTargetIds(admin);
        return Result.success(reviewsService.pageAdminReviews(pageNum, pageSize, keyword, reviewType, status, rating, hasReply, targetIds));
    }

    @PostMapping("/{id}/status")
    public Result<ReviewItemResponse> updateStatus(
        @PathVariable("id") String id,
        @RequestBody(required = false) ReviewStatusUpdateRequest request
    ) {
        requireAdmin();
        return Result.success(reviewsService.updateAdminReviewStatus(id, request == null ? null : request.getStatus()));
    }

    @PostMapping("/{id}/reply")
    public Result<ReviewItemResponse> replyReview(
        @PathVariable("id") String id,
        @RequestBody(required = false) ReviewReplyRequest request
    ) {
        requireAdmin();
        return Result.success(reviewsService.replyAdminReview(id, request == null ? null : request.getReplyContent()));
    }

    private Set<String> buildStoreTargetIds(LoginUserInfo admin) {
        if (admin == null || !admin.isStoreAdmin() || !StringUtils.hasText(admin.getStoreId())) {
            return null; // 超管不过滤
        }
        Set<String> ids = new HashSet<>();
        // 门店师傅
        List<TechnicianAccounts> techs = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getStoreId, admin.getStoreId())
                .eq(TechnicianAccounts::getIsDelete, 0)
        );
        ids.addAll(techs.stream().map(TechnicianAccounts::getId).collect(Collectors.toSet()));
        // 门店商品
        List<Products> products = productsService.list(
            new LambdaQueryWrapper<Products>()
                .eq(Products::getStoreId, admin.getStoreId())
                .eq(Products::getIsDelete, 0)
        );
        ids.addAll(products.stream().map(Products::getId).collect(Collectors.toSet()));
        return ids;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问评价管理");
        }
        return user;
    }
}
