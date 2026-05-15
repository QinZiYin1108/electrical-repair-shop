package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.ProductCategories;
import com.example.backend.entity.Products;
import com.example.backend.entity.Images;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminProductCategoryCreateRequest;
import com.example.backend.model.admin.AdminProductCategoryResponse;
import com.example.backend.model.admin.AdminProductCategoryUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminProductCategoryManageService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.ProductCategoriesService;
import com.example.backend.service.ProductsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminProductCategoryManageServiceImpl implements AdminProductCategoryManageService {

    private static final String PRODUCT_CATEGORY_ICON_BUSINESS_TYPE = "PRODUCTCATEGORY";

    private final ProductCategoriesService productCategoriesService;
    private final ProductsService productsService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;

    public AdminProductCategoryManageServiceImpl(
        ProductCategoriesService productCategoriesService,
        ProductsService productsService,
        ImagesService imagesService,
        OssUtil ossUtil
    ) {
        this.productCategoriesService = productCategoriesService;
        this.productsService = productsService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
    }

    @Override
    public List<AdminProductCategoryResponse> listCategories() {
        return buildCategoryTree(listAllCategories());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductCategoryResponse createCategory(AdminProductCategoryCreateRequest request) {
        ProductCategories parent = null;
        String parentId = normalizeBlankToNull(request.getParentId());
        if (StringUtils.hasText(parentId)) {
            parent = requireCategory(parentId);
            if (defaultIfNull(parent.getLevel(), 1) >= 3) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品分类最多支持三级");
            }
        }

        long now = System.currentTimeMillis();
        ProductCategories category = new ProductCategories();
        category.setId(SnowflakeIdUtil.nextProductCategoryId());
        category.setName(request.getName().trim());
        category.setParentId(parentId);
        category.setLevel(parent == null ? 1 : defaultIfNull(parent.getLevel(), 1) + 1);
        category.setDescription(normalizeBlankToNull(request.getDescription()));
        category.setIconUrl(null);
        category.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        category.setIsActive(resolveCategoryActive(request.getIsActive(), parent));
        category.setCreatedTime(now);
        category.setUpdatedTime(now);

        if (!productCategoriesService.save(category)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增商品分类失败");
        }

        Map<String, ProductCategories> categoryMap = new LinkedHashMap<>();
        if (parent != null) {
            categoryMap.put(parent.getId(), parent);
        }
        categoryMap.put(category.getId(), category);
        return toCategoryResponse(category, categoryMap, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProductCategoryResponse updateCategory(String id, AdminProductCategoryUpdateRequest request) {
        ProductCategories current = requireCategory(id);
        List<ProductCategories> allCategories = listAllCategories();
        Map<String, ProductCategories> categoryMap = allCategories.stream()
            .collect(Collectors.toMap(ProductCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        String newParentId = normalizeBlankToNull(request.getParentId());
        if (Objects.equals(id, newParentId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "父级分类不能选择自身");
        }

        ProductCategories newParent = null;
        if (StringUtils.hasText(newParentId)) {
            newParent = categoryMap.get(newParentId);
            if (newParent == null || Objects.equals(newParent.getIsDelete(), 1)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "父级分类不存在");
            }
            if (isDescendantCategory(id, newParentId, categoryMap)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "父级分类不能选择当前分类的子级");
            }
        }

        int newLevel = newParent == null ? 1 : defaultIfNull(newParent.getLevel(), 1) + 1;
        int subtreeDepth = calculateSubtreeDepth(id, categoryMap);
        if (newLevel + subtreeDepth - 1 > 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品分类最多支持三级");
        }

        current.setName(request.getName().trim());
        current.setParentId(newParentId);
        current.setLevel(newLevel);
        current.setDescription(normalizeBlankToNull(request.getDescription()));
        current.setIconUrl(null);
        current.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        current.setIsActive(resolveCategoryActive(request.getIsActive(), newParent));
        current.setUpdatedTime(System.currentTimeMillis());

        if (!productCategoriesService.updateById(current)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新商品分类失败");
        }

        int oldLevel = defaultIfNull(categoryMap.get(id).getLevel(), 1);
        int levelDelta = newLevel - oldLevel;
        boolean cascadeDisable = Objects.equals(current.getIsActive(), 0);
        if (levelDelta != 0 || cascadeDisable) {
            List<ProductCategories> descendants = collectDescendants(id, categoryMap);
            List<ProductCategories> toUpdate = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (ProductCategories descendant : descendants) {
                boolean changed = false;
                if (levelDelta != 0) {
                    descendant.setLevel(defaultIfNull(descendant.getLevel(), 1) + levelDelta);
                    changed = true;
                }
                if (cascadeDisable && !Objects.equals(descendant.getIsActive(), 0)) {
                    descendant.setIsActive(0);
                    changed = true;
                }
                if (changed) {
                    descendant.setUpdatedTime(now);
                    toUpdate.add(descendant);
                }
            }
            if (!toUpdate.isEmpty()) {
                productCategoriesService.updateBatchById(toUpdate);
            }
        }

        Map<String, ProductCategories> latestCategoryMap = listAllCategories().stream()
            .collect(Collectors.toMap(ProductCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        return toCategoryResponse(current, latestCategoryMap, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        ProductCategories current = productCategoriesService.getById(id);
        if (current == null) {
            return;
        }

        long childCount = productCategoriesService.count(
            new LambdaQueryWrapper<ProductCategories>()
                .eq(ProductCategories::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该分类下存在子分类，无法删除");
        }

        long productCount = productsService.count(
            new LambdaQueryWrapper<Products>()
                .eq(Products::getCategoryId, id)
        );
        if (productCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该分类下存在商品，无法删除");
        }

        productCategoriesService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadCategoryIcon(String id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的分类图标");
        }
        UploadLimitUtil.validateImageSize(file);

        ProductCategories category = requireCategory(id);
        if (!Objects.equals(category.getLevel(), 3)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有第三级分类才能上传图标");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception ignored) {
        }

        String origin = Optional.ofNullable(file.getOriginalFilename()).orElse("product-category-icon");
        String ext = "";
        int dot = origin.lastIndexOf('.');
        if (dot >= 0 && dot < origin.length() - 1) {
            ext = origin.substring(dot);
        }

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = "product-category-icons/" + id + "/" + date + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        String url = ossUtil.upload(objectName, new ByteArrayInputStream(bytes));

        LoginUserInfo user = AuthUserContext.get();
        long now = System.currentTimeMillis();
        Images image = new Images();
        image.setId(SnowflakeIdUtil.nextImageId());
        image.setOriginalName(origin);
        image.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        image.setFilePath(objectName);
        image.setFileUrl(url);
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        image.setWidth(width);
        image.setHeight(height);
        image.setUploaderId(user == null ? null : user.getAccountId());
        image.setUploaderType(3);
        image.setBusinessType(PRODUCT_CATEGORY_ICON_BUSINESS_TYPE);
        image.setBusinessId(id);
        image.setCreatedTime(now);

        if (!imagesService.save(image)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存分类图标失败");
        }

        return url;
    }

    private ProductCategories requireCategory(String id) {
        ProductCategories category = productCategoriesService.getById(id);
        if (category == null || Objects.equals(category.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品分类不存在");
        }
        return category;
    }

    private List<ProductCategories> listAllCategories() {
        return productCategoriesService.list(
            new LambdaQueryWrapper<ProductCategories>()
                .orderByAsc(ProductCategories::getSortOrder)
                .orderByDesc(ProductCategories::getCreatedTime)
        );
    }

    private List<AdminProductCategoryResponse> buildCategoryTree(List<ProductCategories> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> iconMap = loadLatestCategoryIconUrlMap(
            categories.stream()
                .filter(item -> Objects.equals(item.getLevel(), 3))
                .map(ProductCategories::getId)
                .collect(Collectors.toList())
        );
        Map<String, ProductCategories> sourceMap = categories.stream()
            .collect(Collectors.toMap(ProductCategories::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<String, AdminProductCategoryResponse> nodeMap = new LinkedHashMap<>();
        for (ProductCategories category : categories) {
            nodeMap.put(category.getId(), toCategoryResponse(category, sourceMap, iconMap.get(category.getId())));
        }

        List<AdminProductCategoryResponse> roots = new ArrayList<>();
        for (AdminProductCategoryResponse node : nodeMap.values()) {
            if (StringUtils.hasText(node.getParentId()) && nodeMap.containsKey(node.getParentId())) {
                nodeMap.get(node.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<AdminProductCategoryResponse> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator
            .comparing((AdminProductCategoryResponse item) -> defaultIfNull(item.getSortOrder(), 0))
            .thenComparing(item -> defaultIfNull(item.getCreatedTime(), 0L), Comparator.reverseOrder()));
        for (AdminProductCategoryResponse node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private AdminProductCategoryResponse toCategoryResponse(
        ProductCategories category,
        Map<String, ProductCategories> categoryMap,
        String iconUrl
    ) {
        AdminProductCategoryResponse response = new AdminProductCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        if (StringUtils.hasText(category.getParentId()) && categoryMap != null) {
            ProductCategories parent = categoryMap.get(category.getParentId());
            response.setParentName(parent == null ? null : parent.getName());
        }
        response.setLevel(category.getLevel());
        response.setDescription(category.getDescription());
        response.setIconUrl(Objects.equals(category.getLevel(), 3) ? iconUrl : null);
        response.setSortOrder(category.getSortOrder());
        response.setIsActive(category.getIsActive());
        response.setCreatedTime(category.getCreatedTime());
        response.setUpdatedTime(category.getUpdatedTime());
        return response;
    }

    private Map<String, String> loadLatestCategoryIconUrlMap(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, PRODUCT_CATEGORY_ICON_BUSINESS_TYPE)
                .in(Images::getBusinessId, categoryIds)
                .orderByDesc(Images::getCreatedTime)
        );
        Map<String, String> result = new HashMap<>();
        for (Images image : images) {
            if (!result.containsKey(image.getBusinessId())) {
                result.put(image.getBusinessId(), image.getFileUrl());
            }
        }
        return result;
    }

    private boolean isDescendantCategory(String currentId, String targetId, Map<String, ProductCategories> categoryMap) {
        Set<String> visited = new LinkedHashSet<>();
        String parentId = normalizeBlankToNull(targetId);
        while (StringUtils.hasText(parentId) && visited.add(parentId)) {
            if (Objects.equals(parentId, currentId)) {
                return true;
            }
            ProductCategories category = categoryMap.get(parentId);
            if (category == null) {
                return false;
            }
            parentId = normalizeBlankToNull(category.getParentId());
        }
        return false;
    }

    private int calculateSubtreeDepth(String rootId, Map<String, ProductCategories> categoryMap) {
        return calculateSubtreeDepth(rootId, categoryMap, 1);
    }

    private int calculateSubtreeDepth(String rootId, Map<String, ProductCategories> categoryMap, int currentDepth) {
        int maxDepth = currentDepth;
        for (ProductCategories category : categoryMap.values()) {
            if (Objects.equals(normalizeBlankToNull(category.getParentId()), rootId)) {
                maxDepth = Math.max(maxDepth, calculateSubtreeDepth(category.getId(), categoryMap, currentDepth + 1));
            }
        }
        return maxDepth;
    }

    private List<ProductCategories> collectDescendants(String rootId, Map<String, ProductCategories> categoryMap) {
        List<ProductCategories> descendants = new ArrayList<>();
        collectDescendants(rootId, categoryMap, descendants);
        return descendants;
    }

    private void collectDescendants(String rootId, Map<String, ProductCategories> categoryMap, List<ProductCategories> descendants) {
        for (ProductCategories category : categoryMap.values()) {
            if (Objects.equals(normalizeBlankToNull(category.getParentId()), rootId)) {
                descendants.add(category);
                collectDescendants(category.getId(), categoryMap, descendants);
            }
        }
    }

    private Integer resolveCategoryActive(Integer requestActive, ProductCategories parent) {
        int active = defaultIfNull(requestActive, 1);
        if (parent != null && Objects.equals(parent.getIsActive(), 0)) {
            return 0;
        }
        return active;
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
