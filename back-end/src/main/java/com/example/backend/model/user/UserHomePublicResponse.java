package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户首页Public响应")
@Data
public class UserHomePublicResponse {

    private List<BannerItem> banners = new ArrayList<>();

    private List<NoticeItem> notices = new ArrayList<>();

    private List<HotCategoryItem> hotCategories = new ArrayList<>();

    @Data
    public static class BannerItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "内容类型")
        private Integer contentType;

        @Schema(description = "tag")
        private String tag;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "subtitle")
        private String subtitle;

        @Schema(description = "图片URL")
        private String imageUrl;

        @Schema(description = "emoji")
        private String emoji;
    }

    @Data
    public static class NoticeItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "text")
        private String text;
    }

    @Data
    public static class HotCategoryItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "描述")
        private String desc;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "图标URL")
        private String iconUrl;
    }
}
