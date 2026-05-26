package com.example.backend.controller.worker;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.review.ReviewReplyRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/worker/reviews")
public class WorkerReviewController {

    private final ReviewsService reviewsService;

    public WorkerReviewController(ReviewsService reviewsService) {
        this.reviewsService = reviewsService;
    }

    @GetMapping
    public Result<List<ReviewItemResponse>> listReviews() {
        LoginUserInfo worker = requireWorker();
        return Result.success(reviewsService.listWorkerReviews(worker.getAccountId()));
    }

    @PostMapping("/{id}/reply")
    public Result<ReviewItemResponse> replyReview(
        @PathVariable("id") String id,
        @RequestBody(required = false) ReviewReplyRequest request
    ) {
        LoginUserInfo worker = requireWorker();
        String replyContent = request == null ? null : request.getReplyContent();
        return Result.success(reviewsService.replyWorkerReview(id, worker.getAccountId(), replyContent));
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅评价");
        }
        return user;
    }
}
