package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.ProductCategories;
import com.example.backend.entity.Products;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminProductCategoryResponse;
import com.example.backend.model.admin.AdminProductResponse;
import com.example.backend.model.admin.AdminProductSaveRequest;
import com.example.backend.model.admin.AdminProductSpecItem;
import com.example.backend.model.admin.AdminProductUploadMediaResponse;
import com.example.backend.service.AdminProductManageService;
import com.example.backend.service.ProductCategoriesService;
import com.example.backend.service.ProductsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminProductManageServiceImpl implements AdminProductManageService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<AdminProductSpecItem>> SPEC_ITEM_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final DateTimeFormatter MEDIA_OBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ProductsService productsService;
    private final ProductCategoriesService productCategoriesService;
    private final OssUtil ossUtil;

    public AdminProductManageServiceImpl(
        ProductsService productsService,
        ProductCategoriesService productCategoriesService,
        OssUtil ossUtil
    ) {
        this.productsService = productsService;
        this.productCategoriesService = productCategoriesService;
        this.ossUtil = ossUtil;
    }

    @Override
    public List<AdminProductCategoryResponse> listProductCategories() {
        List<ProductCategories> categories = listAllCategories();
        return buildCategoryTree(categories);
    }

    @Override
    public List<AdminProductResponse> listProducts(Integer productType, String keyword, String categoryId, Integer status, String storeId) {
        int normalizedProductType = normalizeProductType(productType);
        List<ProductCategories> categories = listAllCategories();
        Map<String, ProductCategories> categoryMap = categories.stream()
            .collect(Collectors.toMap(ProductCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        LambdaQueryWrapper<Products> wrapper = new LambdaQueryWrapper<>();
        applyProductTypeCondition(wrapper, normalizedProductType);

        // 店铺管理员只能看到自己门店的商品
        if (StringUtils.hasText(storeId)) {
            wrapper.eq(Products::getStoreId, storeId);
        }

        String normalizedKeyword = normalizeBlankToNull(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(query -> query
                .like(Products::getName, normalizedKeyword)
                .or()
                .like(Products::getProductNo, normalizedKeyword)
                .or()
                .like(Products::getBrand, normalizedKeyword)
                .or()
                .like(Products::getModel, normalizedKeyword)
            );
        }

        String normalizedCategoryId = normalizeBlankToNull(categoryId);
        if (StringUtils.hasText(normalizedCategoryId)) {
            if (!categoryMap.containsKey(normalizedCategoryId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品分类不存在");
            }
            Set<String> categoryIds = collectCategoryIds(normalizedCategoryId, categoryMap);
            wrapper.in(Products::getCategoryId, categoryIds);
        }

        if (status != null) {
            validateStatus(status);
            wrapper.eq(Products::getStatus, status);
        }

        List<Products> products = productsService.list(
            wrapper.orderByAsc(Products::getSortOrder)
                .orderByDesc(Products::getUpdatedTime)
                .orderByDesc(Products::getCreatedTime)
        );

        List<AdminProductResponse> responses = new ArrayList<>();
        for (Products product : products) {
            responses.add(toProductResponse(product, categoryMap));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductResponse createProduct(Integer productType, AdminProductSaveRequest request, String storeId) {
        int normalizedProductType = normalizeProductType(productType);
        validateSaveRequest(request);
        Map<String, ProductCategories> categoryMap = loadCategoryMap();
        requireCategory(request.getCategoryId(), categoryMap);

        long now = System.currentTimeMillis();
        String productId = SnowflakeIdUtil.nextProductId();

        Products product = new Products();
        product.setId(productId);
        product.setProductNo(generateProductNo(normalizedProductType, productId));
        product.setProductType(normalizedProductType);
        fillProduct(product, request, now);
        product.setSalesCount(0);
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setCreatedTime(now);
        product.setUpdatedTime(now);

        // 店铺管理员创建的商品自动归属其门店
        if (StringUtils.hasText(storeId)) {
            product.setStoreId(storeId);
            product.setAuditStatus(2); // 店铺管理员创建的商品自动审核通过
        }

        if (!productsService.save(product)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增商品失败");
        }
        return toProductResponse(product, categoryMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductResponse updateProduct(Integer productType, String id, AdminProductSaveRequest request, String storeId) {
        int normalizedProductType = normalizeProductType(productType);
        validateSaveRequest(request);

        Products current = requireProduct(id, normalizedProductType);

        // 店铺管理员只能编辑自己门店的商品
        if (StringUtils.hasText(storeId) && !storeId.equals(current.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑其他门店的商品");
        }
        Map<String, ProductCategories> categoryMap = loadCategoryMap();
        requireCategory(request.getCategoryId(), categoryMap);

        long now = System.currentTimeMillis();
        current.setProductType(normalizedProductType);
        fillProduct(current, request, now);
        current.setUpdatedTime(now);

        if (!productsService.updateById(current)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新商品失败");
        }
        return toProductResponse(current, categoryMap);
    }

    @Override
    public AdminProductUploadMediaResponse uploadProductMedia(String mediaType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        String uploadType = resolveUploadMediaType(mediaType, file.getContentType(), file.getOriginalFilename());
        UploadLimitUtil.validateMediaSize(uploadType, file);
        String extension = resolveUploadExtension(file.getOriginalFilename(), file.getContentType(), uploadType);
        String objectName = "products/" + uploadType + "/" + LocalDateTime.now().format(MEDIA_OBJECT_DATE_FORMAT)
            + "_" + UUID.randomUUID().toString().replace("-", "") + extension;

        String fileUrl;
        try (InputStream inputStream = file.getInputStream()) {
            fileUrl = ossUtil.upload(objectName, inputStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传商品素材失败");
        }

        AdminProductUploadMediaResponse response = new AdminProductUploadMediaResponse();
        response.setUrl(fileUrl);
        response.setName(resolveMediaName(file.getOriginalFilename(), "video".equals(uploadType) ? "product-video.mp4" : "product-image.jpg"));
        response.setFileSize(file.getSize());
        response.setMimeType(resolveUploadMimeType(file.getContentType(), uploadType));
        response.setMediaType(uploadType);
        if ("image".equals(uploadType)) {
            fillImageSize(file, response);
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Integer productType, String id, String storeId) {
        int normalizedProductType = normalizeProductType(productType);
        Products current = productsService.getById(id);
        if (current == null) {
            return;
        }
        if (!isProductMatchedType(current, normalizedProductType)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        // 店铺管理员只能删除自己门店的商品
        if (StringUtils.hasText(storeId) && !storeId.equals(current.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除其他门店的商品");
        }
        productsService.removeById(id);
    }

    private void fillProduct(Products product, AdminProductSaveRequest request, long now) {
        product.setName(request.getName().trim());
        product.setCategoryId(request.getCategoryId().trim());
        product.setBrand(request.getBrand().trim());
        product.setModel(request.getModel().trim());
        product.setDescription(normalizeBlankToNull(request.getDescription()));
        product.setSpecifications(writeSpecifications(request.getSpecifications()));
        product.setMainImageUrl(request.getMainImageUrl().trim());
        product.setImageUrls(writeStringList(request.getImageUrls()));
        product.setVideoUrls(writeStringList(request.getVideoUrls()));
        product.setOriginalPrice(normalizeMoney(request.getOriginalPrice()));
        product.setSellingPrice(normalizeMoney(request.getSellingPrice()));
        product.setCostPrice(normalizeMoney(request.getCostPrice()));
        product.setStockQuantity(defaultIfNull(request.getStockQuantity(), 0));
        product.setWarningStock(defaultIfNull(request.getWarningStock(), 0));
        product.setWeight(request.getWeight() == null ? null : normalizeMoney(request.getWeight()));
        product.setDimensions(normalizeBlankToNull(request.getDimensions()));
        product.setWarrantyPeriod(defaultIfNull(request.getWarrantyPeriod(), 0));
        product.setShippingFee(resolveShippingFee(request));
        product.setIsFreeShipping(defaultIfNull(request.getIsFreeShipping(), 0));
        product.setStatus(defaultIfNull(request.getStatus(), 1));
        product.setIsHot(defaultIfNull(request.getIsHot(), 0));
        product.setIsNew(defaultIfNull(request.getIsNew(), 0));
        product.setIsRecommended(defaultIfNull(request.getIsRecommended(), 0));
        product.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        product.setUpdatedTime(now);
    }

    private void fillImageSize(MultipartFile file, AdminProductUploadMediaResponse response) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                response.setWidth(image.getWidth());
                response.setHeight(image.getHeight());
            }
        } catch (IOException ignored) {
        }
    }

    private Products requireProduct(String id, int productType) {
        Products current = productsService.getById(id);
        if (current == null || !isProductMatchedType(current, productType)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return current;
    }

    private boolean isProductMatchedType(Products product, int productType) {
        Integer currentType = product.getProductType();
        if (productType == 1) {
            return currentType == null || currentType == 1;
        }
        return Objects.equals(currentType, productType);
    }

    private void applyProductTypeCondition(LambdaQueryWrapper<Products> wrapper, int productType) {
        if (productType == 1) {
            wrapper.and(query -> query.eq(Products::getProductType, 1).or().isNull(Products::getProductType));
            return;
        }
        wrapper.eq(Products::getProductType, productType);
    }

    private Map<String, ProductCategories> loadCategoryMap() {
        return listAllCategories().stream()
            .collect(Collectors.toMap(ProductCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private List<ProductCategories> listAllCategories() {
        return productCategoriesService.list(
            new LambdaQueryWrapper<ProductCategories>()
                .orderByAsc(ProductCategories::getSortOrder)
                .orderByDesc(ProductCategories::getCreatedTime)
        );
    }

    private ProductCategories requireCategory(String categoryId, Map<String, ProductCategories> categoryMap) {
        String normalizedCategoryId = normalizeBlankToNull(categoryId);
        ProductCategories category = categoryMap.get(normalizedCategoryId);
        if (category == null || Objects.equals(category.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品分类不存在");
        }
        return category;
    }

    private List<AdminProductCategoryResponse> buildCategoryTree(List<ProductCategories> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, AdminProductCategoryResponse> nodeMap = new LinkedHashMap<>();
        for (ProductCategories category : categories) {
            AdminProductCategoryResponse response = new AdminProductCategoryResponse();
            response.setId(category.getId());
            response.setName(category.getName());
            response.setParentId(category.getParentId());
            response.setLevel(category.getLevel());
            response.setDescription(category.getDescription());
            response.setIconUrl(category.getIconUrl());
            response.setSortOrder(category.getSortOrder());
            response.setIsActive(category.getIsActive());
            nodeMap.put(response.getId(), response);
        }

        List<AdminProductCategoryResponse> roots = new ArrayList<>();
        for (AdminProductCategoryResponse node : nodeMap.values()) {
            if (StringUtils.hasText(node.getParentId()) && nodeMap.containsKey(node.getParentId())) {
                nodeMap.get(node.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        sortCategoryTree(roots);
        return roots;
    }

    private void sortCategoryTree(List<AdminProductCategoryResponse> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator
            .comparing((AdminProductCategoryResponse item) -> defaultIfNull(item.getSortOrder(), 0))
            .thenComparing(item -> defaultIfNull(item.getLevel(), 0))
            .thenComparing(item -> defaultIfNull(item.getName(), ""))
        );
        for (AdminProductCategoryResponse node : nodes) {
            sortCategoryTree(node.getChildren());
        }
    }

    private Set<String> collectCategoryIds(String categoryId, Map<String, ProductCategories> categoryMap) {
        Set<String> categoryIds = new LinkedHashSet<>();
        collectCategoryIds(categoryId, categoryMap, categoryIds);
        return categoryIds;
    }

    private void collectCategoryIds(String categoryId, Map<String, ProductCategories> categoryMap, Set<String> container) {
        if (!StringUtils.hasText(categoryId) || !container.add(categoryId)) {
            return;
        }
        for (ProductCategories category : categoryMap.values()) {
            if (Objects.equals(normalizeBlankToNull(category.getParentId()), categoryId)) {
                collectCategoryIds(category.getId(), categoryMap, container);
            }
        }
    }

    private AdminProductResponse toProductResponse(Products product, Map<String, ProductCategories> categoryMap) {
        AdminProductResponse response = new AdminProductResponse();
        Integer normalizedProductType = defaultIfNull(product.getProductType(), 1);
        response.setId(product.getId());
        response.setProductNo(product.getProductNo());
        response.setProductType(normalizedProductType);
        response.setProductTypeText(productTypeText(normalizedProductType));
        response.setName(product.getName());
        response.setCategoryId(product.getCategoryId());

        ProductCategories category = categoryMap == null ? null : categoryMap.get(product.getCategoryId());
        response.setCategoryName(category == null ? null : category.getName());
        response.setCategoryPath(buildCategoryPath(product.getCategoryId(), categoryMap));

        response.setBrand(product.getBrand());
        response.setModel(product.getModel());
        response.setDescription(product.getDescription());
        response.setSpecifications(readSpecifications(product.getSpecifications()));
        response.setMainImageUrl(product.getMainImageUrl());
        response.setImageUrls(readStringList(product.getImageUrls()));
        response.setVideoUrls(readStringList(product.getVideoUrls()));
        response.setOriginalPrice(product.getOriginalPrice());
        response.setSellingPrice(product.getSellingPrice());
        response.setCostPrice(product.getCostPrice());
        response.setStockQuantity(defaultIfNull(product.getStockQuantity(), 0));
        response.setWarningStock(defaultIfNull(product.getWarningStock(), 0));
        response.setSalesCount(defaultIfNull(product.getSalesCount(), 0));
        response.setViewCount(defaultIfNull(product.getViewCount(), 0));
        response.setFavoriteCount(defaultIfNull(product.getFavoriteCount(), 0));
        response.setWeight(product.getWeight());
        response.setDimensions(product.getDimensions());
        response.setWarrantyPeriod(defaultIfNull(product.getWarrantyPeriod(), 0));
        response.setShippingFee(product.getShippingFee());
        response.setIsFreeShipping(defaultIfNull(product.getIsFreeShipping(), 0));
        response.setStatus(defaultIfNull(product.getStatus(), 1));
        response.setStatusText(statusText(defaultIfNull(product.getStatus(), 1)));
        response.setIsHot(defaultIfNull(product.getIsHot(), 0));
        response.setIsNew(defaultIfNull(product.getIsNew(), 0));
        response.setIsRecommended(defaultIfNull(product.getIsRecommended(), 0));
        response.setSortOrder(defaultIfNull(product.getSortOrder(), 0));
        response.setCreatedTime(product.getCreatedTime());
        response.setUpdatedTime(product.getUpdatedTime());
        return response;
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
            currentId = normalizeBlankToNull(current.getParentId());
        }
        return String.join(" / ", pathItems);
    }

    private List<AdminProductSpecItem> readSpecifications(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<AdminProductSpecItem> list = OBJECT_MAPPER.readValue(json, SPEC_ITEM_LIST_TYPE);
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

    private String writeSpecifications(List<AdminProductSpecItem> specifications) {
        List<AdminProductSpecItem> normalizedList = new ArrayList<>();
        for (AdminProductSpecItem item : specifications == null ? List.<AdminProductSpecItem>of() : specifications) {
            AdminProductSpecItem normalizedItem = normalizeSpecItem(item);
            if (normalizedItem != null) {
                normalizedList.add(normalizedItem);
            }
        }
        return writeJson(normalizedList);
    }

    private String writeStringList(List<String> values) {
        return writeJson(normalizeStringList(values));
    }

    private String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商品数据序列化失败");
        }
    }

    private String resolveUploadMediaType(String mediaType, String mimeType, String originalFilename) {
        String normalizedType = normalizeBlankToNull(mediaType);
        if (StringUtils.hasText(normalizedType)) {
            String lower = normalizedType.toLowerCase();
            if ("image".equals(lower) || "video".equals(lower)) {
                return lower;
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "mediaType 仅支持 image 或 video");
        }
        String normalizedMime = normalizeBlankToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if (lowerMime.startsWith("image/")) {
                return "image";
            }
            if (lowerMime.startsWith("video/")) {
                return "video";
            }
        }
        String fileName = normalizeBlankToNull(originalFilename);
        if (StringUtils.hasText(fileName)) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".bmp")) {
                return "image";
            }
            if (lowerName.endsWith(".mp4")
                || lowerName.endsWith(".mov")
                || lowerName.endsWith(".m4v")
                || lowerName.endsWith(".avi")
                || lowerName.endsWith(".mkv")
                || lowerName.endsWith(".webm")) {
                return "video";
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别上传文件类型");
    }

    private String resolveUploadExtension(String originalFilename, String mimeType, String mediaType) {
        String filename = normalizeBlankToNull(originalFilename);
        if (StringUtils.hasText(filename)) {
            int index = filename.lastIndexOf('.');
            if (index >= 0 && index < filename.length() - 1) {
                String extension = filename.substring(index);
                if (extension.length() <= 10) {
                    return extension;
                }
            }
        }
        String normalizedMime = normalizeBlankToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if ("image/png".equals(lowerMime)) {
                return ".png";
            }
            if ("image/webp".equals(lowerMime)) {
                return ".webp";
            }
            if ("image/gif".equals(lowerMime)) {
                return ".gif";
            }
            if ("video/quicktime".equals(lowerMime)) {
                return ".mov";
            }
            if ("video/webm".equals(lowerMime)) {
                return ".webm";
            }
            if (lowerMime.startsWith("video/")) {
                return ".mp4";
            }
        }
        return "video".equals(mediaType) ? ".mp4" : ".jpg";
    }

    private String resolveUploadMimeType(String mimeType, String mediaType) {
        String normalized = normalizeBlankToNull(mimeType);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "video".equals(mediaType) ? "video/mp4" : "image/jpeg";
    }

    private String resolveMediaName(String value, String fallback) {
        String normalized = normalizeBlankToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private List<String> normalizeStringList(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
            .map(AdminProductManageServiceImpl::normalizeBlankToNull)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private AdminProductSpecItem normalizeSpecItem(AdminProductSpecItem item) {
        if (item == null) {
            return null;
        }
        String key = normalizeBlankToNull(item.getKey());
        String value = normalizeBlankToNull(item.getValue());
        if (!StringUtils.hasText(key) && !StringUtils.hasText(value)) {
            return null;
        }
        AdminProductSpecItem normalizedItem = new AdminProductSpecItem();
        normalizedItem.setKey(key == null ? "" : key);
        normalizedItem.setValue(value == null ? "" : value);
        return normalizedItem;
    }

    private BigDecimal resolveShippingFee(AdminProductSaveRequest request) {
        if (defaultIfNull(request.getIsFreeShipping(), 0) == 1) {
            return BigDecimal.ZERO;
        }
        if (request.getShippingFee() == null) {
            return BigDecimal.ZERO;
        }
        return normalizeMoney(request.getShippingFee());
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private void validateSaveRequest(AdminProductSaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        validateNonNegative(request.getOriginalPrice(), "原价");
        validateNonNegative(request.getSellingPrice(), "售价");
        validateNonNegative(request.getCostPrice(), "成本价");
        validateNonNegative(request.getShippingFee(), "运费");
        validateNonNegative(request.getWeight(), "重量");
        validateNonNegative(request.getStockQuantity(), "库存数量");
        validateNonNegative(request.getWarningStock(), "预警库存");
        validateNonNegative(request.getWarrantyPeriod(), "保修期");
        validateNonNegative(request.getSortOrder(), "排序值");

        validateSwitchValue(request.getIsFreeShipping(), "包邮状态");
        validateSwitchValue(request.getIsHot(), "热销状态");
        validateSwitchValue(request.getIsNew(), "新品状态");
        validateSwitchValue(request.getIsRecommended(), "推荐状态");
        validateStatus(request.getStatus());

        if (request.getSellingPrice() != null && request.getOriginalPrice() != null
            && request.getSellingPrice().compareTo(request.getOriginalPrice()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "售价不能高于原价");
        }
    }

    private void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "不能小于0");
        }
    }

    private void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "不能小于0");
        }
    }

    private void validateSwitchValue(Integer value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value != 0 && value != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "仅支持0或1");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (status != 1 && status != 2 && status != 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品状态仅支持上架、下架、缺货");
        }
    }

    private int normalizeProductType(Integer productType) {
        if (productType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品类型不能为空");
        }
        if (productType == 1 || productType == 2) {
            return productType;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "商品类型仅支持普通商品和二手商品");
    }

    private String generateProductNo(int productType, String productId) {
        String prefix = productType == 2 ? "SH" : "SP";
        String dateText = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String suffix = productId == null ? String.valueOf(System.currentTimeMillis()) : productId.replaceAll("[^0-9]", "");
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return prefix + dateText + suffix;
    }

    private String productTypeText(Integer productType) {
        return Objects.equals(productType, 2) ? "二手商品" : "普通商品";
    }

    private String statusText(Integer status) {
        if (Objects.equals(status, 1)) {
            return "上架";
        }
        if (Objects.equals(status, 2)) {
            return "下架";
        }
        if (Objects.equals(status, 3)) {
            return "缺货";
        }
        return "未知状态";
    }

    private static String normalizeBlankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
