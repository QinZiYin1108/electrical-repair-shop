package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户端门店详情响应")
@Data
public class UserStoreDetailResponse {

    @Schema(description = "门店ID")
    private String id;

    @Schema(description = "门店名称")
    private String name;

    @Schema(description = "门店Logo URL")
    private String logoUrl;

    @Schema(description = "评分（有效评价<3单时为null，前端展示'暂无评分'）")
    private BigDecimal rating;

    @Schema(description = "评价订单数")
    private Integer ratingCount;

    @Schema(description = "是否营业中：0-打烊，1-营业中")
    private Integer isOnline;

    @Schema(description = "门店简介")
    private String description;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "门店地址")
    private String address;

    @Schema(description = "营业时间列表（按周一至周日排序）")
    private List<StoreHoursItem> businessHours = new ArrayList<>();

    @Data
    public static class StoreHoursItem {

        @Schema(description = "周几：1-周一，7-周日")
        private Integer dayOfWeek;

        @Schema(description = "日期标签：周一...周日")
        private String dayLabel;

        @Schema(description = "开始时间，格式HH:mm")
        private String startTime;

        @Schema(description = "结束时间，格式HH:mm")
        private String endTime;

        @Schema(description = "是否可用：0-休息，1-营业")
        private Integer isAvailable;
    }
}
