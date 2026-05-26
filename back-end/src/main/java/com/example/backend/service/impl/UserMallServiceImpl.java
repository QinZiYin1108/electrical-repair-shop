package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.ProductFavorites;
import com.example.backend.entity.ProductCategories;
import com.example.backend.entity.Products;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ProductFavoritesMapper;
import com.example.backend.model.review.ReviewItemResponse;
import com.example.backend.model.user.UserMallCategoryResponse;
import com.example.backend.model.user.UserMallFavoriteProductListItemResponse;
import com.example.backend.model.user.UserMallProductDetailResponse;
import com.example.backend.model.user.UserMallProductFavoriteResponse;
import com.example.backend.model.user.UserMallProductListItemResponse;
import com.example.backend.model.user.UserMallProductSpecItem;
import com.example.backend.service.ProductFavoritesService;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ProductCategoriesService;
import com.example.backend.service.ProductsService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.UserMallService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserMallServiceImpl implements UserMallService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<UserMallProductSpecItem>> SPEC_LIST_TYPE = new TypeReference<>() {};

    private final ProductsService productsService;
    private final ProductCategoriesService productCategoriesService;
    private final ProductFavoritesService productFavoritesService;
    private final ProductFavoritesMapper productFavoritesMapper;
    private final ReviewsService reviewsService;

    public UserMallServiceImpl(
        ProductsService productsService,
        ProductCategoriesService productCategoriesService,
        ProductFavoritesService productFavoritesService,
        ProductFavoritesMapper productFavoritesMapper,
        ReviewsService reviewsService
    ) {
        this.productsService = productsService;
        this.productCategoriesService = productCategoriesService;
        this.productFavoritesService = productFavoritesService;
        this.productFavoritesMapper = productFavoritesMapper;
        this.reviewsService = reviewsService;
    }

    @Override
    public List<UserMallCategoryResponse> listCategories(Integer productType) {
        int normalizedProductType = normalizeProductType(productType);

        List<Products> products = listActiveProducts(normalizedProductType);
        if (products.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, ProductCategories> categoryMap = loadCategoryMap();
        Map<String, Long> productCountMap = products.stream()
            .filter(item -> StringUtils.hasText(item.getCategoryId()))
            .collect(Collectors.groupingBy(Products::getCategoryId, Collectors.counting()));

        List<UserMallCategoryResponse> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : productCountMap.entrySet()) {
            ProductCategories category = categoryMap.get(entry.getKey());
            if (category == null || !Objects.equals(category.getIsActive(), 1)) {
                continue;
            }
            UserMallCategoryResponse item = new UserMallCategoryResponse();
            item.setId(category.getId());
            item.setName(category.getName());
            item.setPathText(buildCategoryPath(category.getId(), categoryMap));
            item.setIconUrl(category.getIconUrl());
            item.setProductCount(entry.getValue().intValue());
            result.add(item);
        }

        result.sort(Comparator
            .comparing((UserMallCategoryResponse item) -> categorySortOrder(item.getId(), categoryMap))
            .thenComparing(item -> defaultText(item.getPathText(), item.getName()))
        );
        return result;
    }

    @Override
    public List<UserMallProductListItemResponse> listProducts(
        Integer productType,
        String keyword,
        String categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean onlyInStock,
        Boolean onlyFreeShipping,
        String marketingTag,
        String sortBy
    ) {
        int normalizedProductType = normalizeProductType(productType);
        Map<String, ProductCategories> categoryMap = loadCategoryMap();
        BigDecimal normalizedMinPrice = normalizePrice(minPrice, "最低价格不能小于0");
        BigDecimal normalizedMaxPrice = normalizePrice(maxPrice, "最高价格不能小于0");
        if (normalizedMinPrice != null && normalizedMaxPrice != null
            && normalizedMinPrice.compareTo(normalizedMaxPrice) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最低价格不能高于最高价格");
        }
        String normalizedMarketingTag = normalizeMarketingTag(marketingTag);
        String normalizedSortBy = normalizeSortBy(sortBy);

        LambdaQueryWrapper<Products> wrapper = baseProductWrapper(normalizedProductType);

        String normalizedKeyword = trimToNull(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(query -> query
                .like(Products::getName, normalizedKeyword)
                .or()
                .like(Products::getBrand, normalizedKeyword)
                .or()
                .like(Products::getModel, normalizedKeyword)
                .or()
                .like(Products::getProductNo, normalizedKeyword)
            );
        }

        String normalizedCategoryId = trimToNull(categoryId);
        if (StringUtils.hasText(normalizedCategoryId)) {
            ProductCategories category = categoryMap.get(normalizedCategoryId);
            if (category == null || !Objects.equals(category.getIsActive(), 1)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "商品分类不存在");
            }
            Set<String> categoryIds = collectCategoryIds(normalizedCategoryId, categoryMap);
            wrapper.in(Products::getCategoryId, categoryIds);
        }

        if (normalizedMinPrice != null) {
            wrapper.ge(Products::getSellingPrice, normalizedMinPrice);
        }
        if (normalizedMaxPrice != null) {
            wrapper.le(Products::getSellingPrice, normalizedMaxPrice);
        }
        if (Boolean.TRUE.equals(onlyInStock)) {
            wrapper.gt(Products::getStockQuantity, 0);
        }
        if (Boolean.TRUE.equals(onlyFreeShipping)) {
            wrapper.eq(Products::getIsFreeShipping, 1);
        }

        applyMarketingTag(wrapper, normalizedMarketingTag);
        applySort(wrapper, normalizedSortBy);

        List<Products> products = productsService.list(wrapper);

        List<UserMallProductListItemResponse> result = new ArrayList<>();
        for (Products product : products) {
            result.add(toListItem(product, categoryMap));
        }
        return result;
    }

    @Override
    public List<UserMallFavoriteProductListItemResponse> listFavoriteProducts() {
        LoginUserInfo user = requireCurrentUser();
        List<ProductFavorites> favorites = productFavoritesMapper.selectActiveByAccountId(user.getAccountId());
        if (favorites == null || favorites.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> productIds = favorites.stream()
            .map(ProductFavorites::getProductId)
            .map(UserMallServiceImpl::trimToNull)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Products> products = productsService.list(
            new LambdaQueryWrapper<Products>()
                .in(Products::getId, productIds)
                .eq(Products::getStatus, 1)
        );
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Products> productMap = products.stream()
            .collect(Collectors.toMap(Products::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<String, ProductCategories> categoryMap = loadCategoryMap();

        List<UserMallFavoriteProductListItemResponse> result = new ArrayList<>();
        for (ProductFavorites favorite : favorites) {
            if (favorite == null) {
                continue;
            }
            Products product = productMap.get(favorite.getProductId());
            if (product == null) {
                continue;
            }
            result.add(toFavoriteListItem(product, categoryMap, favorite.getCreatedTime()));
        }
        return result;
    }

    @Override
    public UserMallProductDetailResponse getProductDetail(String id) {
        LoginUserInfo user = currentUserOrNull();
        String productId = trimToNull(id);
        if (!StringUtils.hasText(productId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品ID不能为空");
        }

        Products product = requireProduct(productId);
        Map<String, ProductCategories> categoryMap = loadCategoryMap();
        return toDetailItem(product, categoryMap, user == null ? null : user.getAccountId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMallProductFavoriteResponse toggleProductFavorite(String id, Boolean favorite) {
        LoginUserInfo user = requireCurrentUser();
        String productId = trimToNull(id);
        if (!StringUtils.hasText(productId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品ID不能为空");
        }
        requireProduct(productId);

        boolean shouldFavorite = !Boolean.FALSE.equals(favorite);
        ProductFavorites existing = productFavoritesMapper.selectAnyByAccountIdAndProductId(user.getAccountId(), productId);
        long now = System.currentTimeMillis();

        if (shouldFavorite) {
            if (existing == null) {
                ProductFavorites record = new ProductFavorites();
                record.setId(SnowflakeIdUtil.nextProductFavoriteId());
                record.setAccountId(user.getAccountId());
                record.setProductId(productId);
                record.setCreatedTime(now);
                record.setVersion(0);
                record.setIsDelete(0);
                if (!productFavoritesService.save(record)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏失败");
                }
            } else if (!Objects.equals(existing.getIsDelete(), 0)) {
                productFavoritesMapper.restoreById(existing.getId(), now);
            }
        } else if (existing != null && Objects.equals(existing.getIsDelete(), 0)) {
            productFavoritesMapper.softDeleteById(existing.getId());
        }

        int favoriteCount = countFavorite(productId);
        Products update = new Products();
        update.setId(productId);
        update.setFavoriteCount(favoriteCount);
        productsService.updateById(update);

        UserMallProductFavoriteResponse response = new UserMallProductFavoriteResponse();
        response.setIsFavorite(shouldFavorite && isFavorite(user.getAccountId(), productId));
        response.setFavoriteCount(favoriteCount);
        return response;
    }

    private List<Products> listActiveProducts(int productType) {
        return productsService.list(
            baseProductWrapper(productType)
                .orderByAsc(Products::getSortOrder)
                .orderByDesc(Products::getUpdatedTime)
        );
    }

    private LambdaQueryWrapper<Products> baseProductWrapper(int productType) {
        LambdaQueryWrapper<Products> wrapper = new LambdaQueryWrapper<>();
        if (productType == 1) {
            wrapper.and(query -> query.eq(Products::getProductType, 1).or().isNull(Products::getProductType));
        } else {
            wrapper.eq(Products::getProductType, productType);
        }
        wrapper.eq(Products::getStatus, 1);
        return wrapper;
    }

    private Map<String, ProductCategories> loadCategoryMap() {
        List<ProductCategories> categories = productCategoriesService.list(
            new LambdaQueryWrapper<ProductCategories>()
                .eq(ProductCategories::getIsActive, 1)
                .orderByAsc(ProductCategories::getSortOrder)
                .orderByDesc(ProductCategories::getCreatedTime)
        );
        return categories.stream()
            .collect(Collectors.toMap(ProductCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Set<String> collectCategoryIds(String rootId, Map<String, ProductCategories> categoryMap) {
        Set<String> result = new LinkedHashSet<>();
        collectCategoryIds(rootId, categoryMap, result);
        return result;
    }

    private void collectCategoryIds(String rootId, Map<String, ProductCategories> categoryMap, Set<String> result) {
        if (!StringUtils.hasText(rootId) || !result.add(rootId)) {
            return;
        }
        for (ProductCategories category : categoryMap.values()) {
            if (Objects.equals(trimToNull(category.getParentId()), rootId)) {
                collectCategoryIds(category.getId(), categoryMap, result);
            }
        }
    }

    private UserMallProductListItemResponse toListItem(Products product, Map<String, ProductCategories> categoryMap) {
        UserMallProductListItemResponse item = new UserMallProductListItemResponse();
        Integer normalizedProductType = defaultIfNull(product.getProductType(), 1);
        ProductCategories category = categoryMap.get(product.getCategoryId());

        item.setId(product.getId());
        item.setProductType(normalizedProductType);
        item.setProductTypeText(productTypeText(normalizedProductType));
        item.setName(product.getName());
        item.setCategoryId(product.getCategoryId());
        item.setCategoryName(category == null ? null : category.getName());
        item.setCategoryPath(buildCategoryPath(product.getCategoryId(), categoryMap));
        item.setBrand(product.getBrand());
        item.setModel(product.getModel());
        item.setMainImageUrl(product.getMainImageUrl());
        item.setSellingPrice(product.getSellingPrice());
        item.setOriginalPrice(product.getOriginalPrice());
        item.setStockQuantity(defaultIfNull(product.getStockQuantity(), 0));
        item.setSalesCount(defaultIfNull(product.getSalesCount(), 0));
        item.setIsFreeShipping(defaultIfNull(product.getIsFreeShipping(), 0));
        item.setIsHot(defaultIfNull(product.getIsHot(), 0));
        item.setIsNew(defaultIfNull(product.getIsNew(), 0));
        item.setIsRecommended(defaultIfNull(product.getIsRecommended(), 0));
        return item;
    }

    private UserMallFavoriteProductListItemResponse toFavoriteListItem(Products product, Map<String, ProductCategories> categoryMap, Long favoriteTime) {
        UserMallFavoriteProductListItemResponse item = new UserMallFavoriteProductListItemResponse();
        Integer normalizedProductType = defaultIfNull(product.getProductType(), 1);
        ProductCategories category = categoryMap.get(product.getCategoryId());

        item.setId(product.getId());
        item.setProductType(normalizedProductType);
        item.setProductTypeText(productTypeText(normalizedProductType));
        item.setName(product.getName());
        item.setCategoryId(product.getCategoryId());
        item.setCategoryName(category == null ? null : category.getName());
        item.setCategoryPath(buildCategoryPath(product.getCategoryId(), categoryMap));
        item.setBrand(product.getBrand());
        item.setModel(product.getModel());
        item.setMainImageUrl(product.getMainImageUrl());
        item.setSellingPrice(product.getSellingPrice());
        item.setOriginalPrice(product.getOriginalPrice());
        item.setStockQuantity(defaultIfNull(product.getStockQuantity(), 0));
        item.setSalesCount(defaultIfNull(product.getSalesCount(), 0));
        item.setIsFreeShipping(defaultIfNull(product.getIsFreeShipping(), 0));
        item.setIsHot(defaultIfNull(product.getIsHot(), 0));
        item.setIsNew(defaultIfNull(product.getIsNew(), 0));
        item.setIsRecommended(defaultIfNull(product.getIsRecommended(), 0));
        item.setFavoriteTime(defaultIfNull(favoriteTime, 0L));
        return item;
    }

    private UserMallProductDetailResponse toDetailItem(Products product, Map<String, ProductCategories> categoryMap, String accountId) {
        UserMallProductDetailResponse item = new UserMallProductDetailResponse();
        Integer normalizedProductType = defaultIfNull(product.getProductType(), 1);
        ProductCategories category = categoryMap.get(product.getCategoryId());
        List<ReviewItemResponse> reviews = reviewsService.listPublicProductReviews(product.getId());

        item.setId(product.getId());
        item.setProductType(normalizedProductType);
        item.setProductTypeText(productTypeText(normalizedProductType));
        item.setName(product.getName());
        item.setCategoryId(product.getCategoryId());
        item.setCategoryName(category == null ? null : category.getName());
        item.setCategoryPath(buildCategoryPath(product.getCategoryId(), categoryMap));
        item.setBrand(product.getBrand());
        item.setModel(product.getModel());
        item.setDescription(product.getDescription());
        item.setMainImageUrl(product.getMainImageUrl());
        item.setImageUrls(readStringList(product.getImageUrls()));
        item.setVideoUrls(readStringList(product.getVideoUrls()));
        item.setSpecifications(readSpecifications(product.getSpecifications()));
        item.setSellingPrice(product.getSellingPrice());
        item.setOriginalPrice(product.getOriginalPrice());
        item.setStockQuantity(defaultIfNull(product.getStockQuantity(), 0));
        item.setWarrantyPeriod(defaultIfNull(product.getWarrantyPeriod(), 0));
        item.setIsHot(defaultIfNull(product.getIsHot(), 0));
        item.setIsNew(defaultIfNull(product.getIsNew(), 0));
        item.setIsRecommended(defaultIfNull(product.getIsRecommended(), 0));
        item.setGalleryUrls(buildGalleryUrls(product.getMainImageUrl(), item.getImageUrls()));
        item.setFavoriteCount(countFavorite(product.getId()));
        item.setIsFavorite(isFavorite(accountId, product.getId()));
        item.setReviewCount(reviews.size());
        item.setReviewRating(calculateReviewRating(reviews));
        item.setReviews(reviews);
        return item;
    }

    private Products requireProduct(String productId) {
        Products product = productsService.getOne(
            new LambdaQueryWrapper<Products>()
                .eq(Products::getId, productId)
                .eq(Products::getStatus, 1)
                .last("limit 1"),
            false
        );
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }
        return product;
    }

    private int countFavorite(String productId) {
        Long count = productFavoritesMapper.countActiveByProductId(productId);
        return count == null ? 0 : count.intValue();
    }

    private boolean isFavorite(String accountId, String productId) {
        if (!StringUtils.hasText(accountId) || !StringUtils.hasText(productId)) {
            return false;
        }
        ProductFavorites favorite = productFavoritesMapper.selectAnyByAccountIdAndProductId(accountId, productId);
        return favorite != null && Objects.equals(favorite.getIsDelete(), 0);
    }

    private BigDecimal calculateReviewRating(List<ReviewItemResponse> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ReviewItemResponse review : reviews) {
            if (review == null || review.getRating() == null) {
                continue;
            }
            total = total.add(BigDecimal.valueOf(review.getRating()));
            count++;
        }
        if (count <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    private List<String> buildGalleryUrls(String mainImageUrl, List<String> imageUrls) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (StringUtils.hasText(mainImageUrl)) {
            urls.add(mainImageUrl.trim());
        }
        for (String imageUrl : imageUrls == null ? List.<String>of() : imageUrls) {
            if (StringUtils.hasText(imageUrl)) {
                urls.add(imageUrl.trim());
            }
        }
        return new ArrayList<>(urls);
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
            return normalizeStringList(list);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<UserMallProductSpecItem> readSpecifications(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<UserMallProductSpecItem> list = OBJECT_MAPPER.readValue(json, SPEC_LIST_TYPE);
            if (list == null) {
                return new ArrayList<>();
            }
            return list.stream()
                .map(this::normalizeSpecItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> normalizeStringList(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
            .map(UserMallServiceImpl::trimToNull)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private UserMallProductSpecItem normalizeSpecItem(UserMallProductSpecItem item) {
        if (item == null) {
            return null;
        }
        String key = trimToNull(item.getKey());
        String value = trimToNull(item.getValue());
        if (!StringUtils.hasText(key) && !StringUtils.hasText(value)) {
            return null;
        }
        UserMallProductSpecItem result = new UserMallProductSpecItem();
        result.setKey(defaultText(key, ""));
        result.setValue(defaultText(value, ""));
        return result;
    }

    private String buildCategoryPath(String categoryId, Map<String, ProductCategories> categoryMap) {
        if (!StringUtils.hasText(categoryId) || categoryMap == null || categoryMap.isEmpty()) {
            return "";
        }
        List<String> pathItems = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        String currentId = categoryId;
        while (StringUtils.hasText(currentId) && visited.add(currentId)) {
            ProductCategories current = categoryMap.get(currentId);
            if (current == null) {
                break;
            }
            if (StringUtils.hasText(current.getName())) {
                pathItems.add(0, current.getName());
            }
            currentId = trimToNull(current.getParentId());
        }
        return String.join(" / ", pathItems);
    }

    private int normalizeProductType(Integer productType) {
        if (productType == null || productType == 1) {
            return 1;
        }
        if (productType == 2) {
            return 2;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "商品类型不支持");
    }

    private String productTypeText(Integer productType) {
        return Objects.equals(productType, 2) ? "二手商品" : "普通商品";
    }

    private Integer categorySortOrder(String categoryId, Map<String, ProductCategories> categoryMap) {
        ProductCategories category = categoryMap.get(categoryId);
        return category == null ? Integer.MAX_VALUE : defaultIfNull(category.getSortOrder(), Integer.MAX_VALUE);
    }

    private void applyMarketingTag(LambdaQueryWrapper<Products> wrapper, String marketingTag) {
        if (!StringUtils.hasText(marketingTag)) {
            return;
        }
        if ("recommended".equals(marketingTag)) {
            wrapper.eq(Products::getIsRecommended, 1);
            return;
        }
        if ("hot".equals(marketingTag)) {
            wrapper.eq(Products::getIsHot, 1);
            return;
        }
        wrapper.eq(Products::getIsNew, 1);
    }

    private void applySort(LambdaQueryWrapper<Products> wrapper, String sortBy) {
        if ("priceAsc".equals(sortBy)) {
            wrapper.orderByAsc(Products::getSellingPrice)
                .orderByDesc(Products::getIsRecommended)
                .orderByAsc(Products::getSortOrder)
                .orderByDesc(Products::getUpdatedTime)
                .orderByDesc(Products::getCreatedTime);
            return;
        }
        if ("priceDesc".equals(sortBy)) {
            wrapper.orderByDesc(Products::getSellingPrice)
                .orderByDesc(Products::getIsRecommended)
                .orderByAsc(Products::getSortOrder)
                .orderByDesc(Products::getUpdatedTime)
                .orderByDesc(Products::getCreatedTime);
            return;
        }
        if ("salesDesc".equals(sortBy)) {
            wrapper.orderByDesc(Products::getSalesCount)
                .orderByDesc(Products::getIsHot)
                .orderByDesc(Products::getIsRecommended)
                .orderByAsc(Products::getSortOrder)
                .orderByDesc(Products::getUpdatedTime);
            return;
        }
        if ("latest".equals(sortBy)) {
            wrapper.orderByDesc(Products::getCreatedTime)
                .orderByDesc(Products::getUpdatedTime)
                .orderByDesc(Products::getIsNew)
                .orderByAsc(Products::getSortOrder);
            return;
        }
        wrapper.orderByDesc(Products::getIsRecommended)
            .orderByDesc(Products::getIsHot)
            .orderByDesc(Products::getIsNew)
            .orderByAsc(Products::getSortOrder)
            .orderByDesc(Products::getUpdatedTime)
            .orderByDesc(Products::getCreatedTime);
    }

    private BigDecimal normalizePrice(BigDecimal price, String message) {
        if (price == null) {
            return null;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return price;
    }

    private String normalizeMarketingTag(String marketingTag) {
        String normalized = trimToNull(marketingTag);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ("recommended".equals(normalized) || "hot".equals(normalized) || "new".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "商品标签筛选不支持");
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = trimToNull(sortBy);
        if (!StringUtils.hasText(normalized) || "default".equals(normalized)) {
            return "default";
        }
        if ("priceAsc".equals(normalized)
            || "priceDesc".equals(normalized)
            || "salesDesc".equals(normalized)
            || "latest".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "商品排序方式不支持");
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问商城数据");
        }
        return user;
    }

    private LoginUserInfo currentUserOrNull() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getAccountId()) ? user : null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()
            || "undefined".equalsIgnoreCase(trimmed)
            || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
