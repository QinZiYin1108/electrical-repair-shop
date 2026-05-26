package com.example.backend.model.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserHomePublicResponse {

    private List<BannerItem> banners = new ArrayList<>();

    private List<NoticeItem> notices = new ArrayList<>();

    private List<HotCategoryItem> hotCategories = new ArrayList<>();

    @Data
    public static class BannerItem {
        private String id;
        private Integer contentType;
        private String tag;
        private String title;
        private String subtitle;
        private String imageUrl;
        private String emoji;
    }

    @Data
    public static class NoticeItem {
        private String id;
        private String text;
    }

    @Data
    public static class HotCategoryItem {
        private String id;
        private String name;
        private String desc;
        private String icon;
        private String iconUrl;
    }
}
