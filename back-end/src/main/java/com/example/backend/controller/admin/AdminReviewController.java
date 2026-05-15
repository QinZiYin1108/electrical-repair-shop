package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.review.ReviewReplyRequest;
import com.example.backend.model.review.ReviewStatusUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ReviewsService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewsService reviewsService;

    public AdminReviewController(ReviewsService reviewsService) {
        this.reviewsService = reviewsService;
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
        requireAdmin();
        return Result.success(reviewsService.pageAdminReviews(pageNum, pageSize, keyword, reviewType, status, rating, hasReply));
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
