package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserMallCategoryResponse;
import com.example.backend.model.user.UserMallFavoriteProductListItemResponse;
import com.example.backend.model.user.UserMallProductDetailResponse;
import com.example.backend.model.user.UserMallProductFavoriteRequest;
import com.example.backend.model.user.UserMallProductFavoriteResponse;
import com.example.backend.model.user.UserMallProductListItemResponse;
import com.example.backend.service.UserMallService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/user/mall")
public class UserMallController {

    private final UserMallService userMallService;

    public UserMallController(UserMallService userMallService) {
        this.userMallService = userMallService;
    }

    @GetMapping("/categories")
    public Result<List<UserMallCategoryResponse>> listCategories(
        @RequestParam(value = "productType", required = false) Integer productType
    ) {
        return Result.success(userMallService.listCategories(productType));
    }

    @GetMapping("/products")
    public Result<List<UserMallProductListItemResponse>> listProducts(
        @RequestParam(value = "productType", required = false) Integer productType,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "categoryId", required = false) String categoryId,
        @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
        @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
        @RequestParam(value = "onlyInStock", required = false) Boolean onlyInStock,
        @RequestParam(value = "onlyFreeShipping", required = false) Boolean onlyFreeShipping,
        @RequestParam(value = "marketingTag", required = false) String marketingTag,
        @RequestParam(value = "sortBy", required = false) String sortBy
    ) {
        return Result.success(userMallService.listProducts(
            productType,
            keyword,
            categoryId,
            minPrice,
            maxPrice,
            onlyInStock,
            onlyFreeShipping,
            marketingTag,
            sortBy
        ));
    }

    @GetMapping("/favorites")
    public Result<List<UserMallFavoriteProductListItemResponse>> listFavoriteProducts() {
        return Result.success(userMallService.listFavoriteProducts());
    }

    @GetMapping("/products/{id}")
    public Result<UserMallProductDetailResponse> getProductDetail(@PathVariable("id") String id) {
        return Result.success(userMallService.getProductDetail(id));
    }

    @PostMapping("/products/{id}/favorite")
    public Result<UserMallProductFavoriteResponse> toggleProductFavorite(
        @PathVariable("id") String id,
        @RequestBody(required = false) UserMallProductFavoriteRequest request
    ) {
        return Result.success(userMallService.toggleProductFavorite(
            id,
            request == null ? null : request.getFavorite()
        ));
    }
}
