package com.example.backend.controller.user;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.review.ReviewSubmitRequest;
import com.example.backend.model.review.ReviewUploadImageResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ReviewsService;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user/reviews")
public class UserReviewController {

    private final ReviewsService reviewsService;

    public UserReviewController(ReviewsService reviewsService) {
        this.reviewsService = reviewsService;
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ReviewUploadImageResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        LoginUserInfo user = requireUser();
        return Result.success(reviewsService.uploadReviewImage(file, user.getAccountId(), 1));
    }

    @PostMapping("/submit")
    public Result<ReviewItemResponse> submitReview(@RequestBody(required = false) ReviewSubmitRequest request) {
        LoginUserInfo user = requireUser();
        return Result.success(reviewsService.submitUserReview(user.getAccountId(), request));
    }

    @PostMapping("/product-submit")
    public Result<ReviewItemResponse> submitProductReview(@RequestBody(required = false) ReviewSubmitRequest request) {
        LoginUserInfo user = requireUser();
        return Result.success(reviewsService.submitUserProductReview(user.getAccountId(), request));
    }

    @GetMapping("/order")
    public Result<ReviewItemResponse> getOrderReview(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireUser();
        return Result.success(reviewsService.getUserOrderReview(orderId, user.getAccountId()));
    }

    @GetMapping("/product-order")
    public Result<ReviewItemResponse> getProductOrderReview(@RequestParam("orderId") String orderId) {
        LoginUserInfo user = requireUser();
        return Result.success(reviewsService.getUserProductOrderReview(orderId, user.getAccountId()));
    }

    @GetMapping("/technician")
    public Result<List<ReviewItemResponse>> listTechnicianReviews(@RequestParam("technicianId") String technicianId) {
        requireUser();
        return Result.success(reviewsService.listPublicTechnicianReviews(technicianId));
    }

    private LoginUserInfo requireUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问用户评价");
        }
        return user;
    }
}
