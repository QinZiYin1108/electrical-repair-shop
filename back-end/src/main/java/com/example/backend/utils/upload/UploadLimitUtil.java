package com.example.backend.utils.upload;

import com.example.backend.common.ErrorCode;
import com.example.backend.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

public final class UploadLimitUtil {

    public static final long IMAGE_MAX_SIZE_BYTES = 5L * 1024 * 1024;
    public static final long VIDEO_MAX_SIZE_BYTES = 30L * 1024 * 1024;
    public static final String IMAGE_MAX_SIZE_TEXT = "5MB";
    public static final String VIDEO_MAX_SIZE_TEXT = "30MB";

    private UploadLimitUtil() {
    }

    public static void validateImageSize(MultipartFile file) {
        validateSize(file == null ? 0L : file.getSize(), IMAGE_MAX_SIZE_BYTES, "图片", IMAGE_MAX_SIZE_TEXT);
    }

    public static void validateVideoSize(MultipartFile file) {
        validateSize(file == null ? 0L : file.getSize(), VIDEO_MAX_SIZE_BYTES, "视频", VIDEO_MAX_SIZE_TEXT);
    }

    public static void validateMediaSize(String mediaType, MultipartFile file) {
        if ("video".equalsIgnoreCase(mediaType)) {
            validateVideoSize(file);
            return;
        }
        if ("image".equalsIgnoreCase(mediaType)) {
            validateImageSize(file);
            return;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "mediaType 仅支持 image 或 video");
    }

    private static void validateSize(long fileSize, long maxSize, String label, String maxSizeText) {
        if (fileSize > maxSize) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "大小不能超过" + maxSizeText);
        }
    }
}
