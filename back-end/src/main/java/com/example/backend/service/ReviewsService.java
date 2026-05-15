package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.Reviews;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.review.ReviewSubmitRequest;
import com.example.backend.model.review.ReviewUploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author Administrator
* @description 针对表【reviews(评价表)】的数据库操作Service
* @createDate 2026-03-03 11:26:16
*/
public interface ReviewsService extends IService<Reviews> {

    Reviews getUserOrderReviewEntity(String orderId, String accountId);

    Reviews getUserProductOrderReviewEntity(String orderId, String accountId);

    ReviewItemResponse submitUserReview(String accountId, ReviewSubmitRequest request);

    ReviewItemResponse submitUserProductReview(String accountId, ReviewSubmitRequest request);

    ReviewItemResponse getUserOrderReview(String orderId, String accountId);

    ReviewItemResponse getUserProductOrderReview(String orderId, String accountId);

    List<ReviewItemResponse> listPublicTechnicianReviews(String technicianId);

    List<ReviewItemResponse> listPublicProductReviews(String productId);

    List<ReviewItemResponse> listWorkerReviews(String technicianId);

    ReviewItemResponse replyWorkerReview(String reviewId, String technicianId, String replyContent);

    ReviewItemResponse replyAdminReview(String reviewId, String replyContent);

    Page<ReviewItemResponse> pageAdminReviews(
        long pageNum,
        long pageSize,
        String keyword,
        Integer reviewType,
        Integer status,
        Integer rating,
        Integer hasReply
    );

    ReviewItemResponse updateAdminReviewStatus(String reviewId, Integer status);

    ReviewUploadImageResponse uploadReviewImage(MultipartFile file, String uploaderId, Integer uploaderType);

    void refreshTechnicianRating(String technicianId);
}
