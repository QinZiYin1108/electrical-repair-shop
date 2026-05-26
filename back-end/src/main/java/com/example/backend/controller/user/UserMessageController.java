package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.ConversationMessages;
import com.example.backend.entity.ConversationSessions;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.SystemMessages;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ConversationMessagesService;
import com.example.backend.service.ConversationSessionsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/messages")
public class UserMessageController {

    private static final int ROLE_USER = 1;
    private static final int ROLE_WORKER = 2;
    private static final int CONTENT_TYPE_TEXT = 1;
    private static final int CONTENT_TYPE_IMAGE = 2;
    private static final int CONTENT_TYPE_SYSTEM = 4;
    private static final int CONTENT_TYPE_VIDEO = 5;
    private static final int SESSION_STATUS_ACTIVE = 1;
    private static final int SESSION_STATUS_CLOSED = 2;
    private static final int MESSAGE_STATUS_NORMAL = 1;
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConversationSessionsService conversationSessionsService;
    private final SystemMessagesService systemMessagesService;
    private final ConversationMessagesService conversationMessagesService;
    private final RepairOrdersService repairOrdersService;
    private final TechnicianAccountsService technicianAccountsService;
    private final OssUtil ossUtil;

    public UserMessageController(
        ConversationSessionsService conversationSessionsService,
        SystemMessagesService systemMessagesService,
        ConversationMessagesService conversationMessagesService,
        RepairOrdersService repairOrdersService,
        TechnicianAccountsService technicianAccountsService,
        OssUtil ossUtil
    ) {
        this.conversationSessionsService = conversationSessionsService;
        this.systemMessagesService = systemMessagesService;
        this.conversationMessagesService = conversationMessagesService;
        this.repairOrdersService = repairOrdersService;
        this.technicianAccountsService = technicianAccountsService;
        this.ossUtil = ossUtil;
    }

    @GetMapping("/system")
    public Result<List<Map<String, Object>>> listSystemMessages() {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        List<SystemMessages> list = systemMessagesService.list(
            new LambdaQueryWrapper<SystemMessages>()
                .eq(SystemMessages::getReceiverId, accountId)
                .eq(SystemMessages::getReceiverType, ROLE_USER)
                .eq(SystemMessages::getIsDelete, 0)
                .orderByDesc(SystemMessages::getCreatedTime)
        );
        List<Map<String, Object>> items = new ArrayList<>();
        if (list != null) {
            for (SystemMessages msg : list) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", msg.getId());
                item.put("title", msg.getTitle());
                item.put("content", msg.getContent());
                item.put("messageType", msg.getMessageType());
                item.put("businessType", msg.getBusinessType());
                item.put("businessId", msg.getBusinessId());
                item.put("priority", msg.getPriority());
                item.put("isRead", msg.getIsRead());
                item.put("readTime", msg.getReadTime());
                item.put("createdTime", msg.getCreatedTime());
                items.add(item);
            }
        }
        return Result.success(items);
    }

    @GetMapping("/system/mark-all-read")
    public Result<Void> markAllSystemRead() {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        long now = System.currentTimeMillis();
        SystemMessages update = new SystemMessages();
        update.setIsRead(1);
        update.setReadTime(now);
        update.setUpdatedTime(now);
        systemMessagesService.update(
            update,
            new LambdaQueryWrapper<SystemMessages>()
                .eq(SystemMessages::getReceiverId, accountId)
                .eq(SystemMessages::getReceiverType, ROLE_USER)
                .eq(SystemMessages::getIsRead, 0)
                .eq(SystemMessages::getIsDelete, 0)
        );
        return Result.success();
    }

    @GetMapping("/chat")
    public Result<Map<String, Object>> listChatMessages(@RequestParam("sessionId") String sessionId) {
        LoginUserInfo user = requireCurrentUser();
        ConversationSessions session = requireOwnedSession(sessionId, user.getAccountId());
        RepairOrders order = getRepairOrder(session.getRepairOrderId());
        TechnicianAccounts technician = getTechnician(session.getTechnicianAccountId());

        List<ConversationMessages> list = conversationMessagesService.list(
            new LambdaQueryWrapper<ConversationMessages>()
                .eq(ConversationMessages::getSessionId, sessionId)
                .eq(ConversationMessages::getIsDelete, 0)
                .eq(ConversationMessages::getStatus, MESSAGE_STATUS_NORMAL)
                .orderByAsc(ConversationMessages::getSendTime)
        );
        List<Map<String, Object>> items = new ArrayList<>();
        if (list != null) {
            for (ConversationMessages msg : list) {
                items.add(buildMessageItem(msg));
            }
        }

        long now = System.currentTimeMillis();
        ConversationMessages updateMsg = new ConversationMessages();
        updateMsg.setReadTime(now);
        conversationMessagesService.update(
            updateMsg,
            new LambdaQueryWrapper<ConversationMessages>()
                .eq(ConversationMessages::getSessionId, sessionId)
                .eq(ConversationMessages::getReceiverId, user.getAccountId())
                .eq(ConversationMessages::getReceiverType, ROLE_USER)
                .isNull(ConversationMessages::getReadTime)
                .eq(ConversationMessages::getIsDelete, 0)
        );
        ConversationSessions updateSession = new ConversationSessions();
        updateSession.setId(sessionId);
        updateSession.setUserUnreadCount(0);
        updateSession.setUpdatedTime(now);
        conversationSessionsService.updateById(updateSession);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getId());
        data.put("title", buildSessionTitle(order, technician));
        data.put("peerName", technician == null ? "维修师傅" : safe(technician.getUsername()));
        data.put("orderId", order == null ? "" : safe(order.getId()));
        data.put("orderNo", order == null ? "" : safe(order.getOrderNo()));
        data.put("status", safeInt(session.getStatus()));
        data.put("canSend", safeInt(session.getStatus()) == SESSION_STATUS_ACTIVE);
        data.put("messages", items);
        return Result.success(data);
    }

    @PostMapping("/chat/send")
    public Result<Map<String, Object>> sendChatMessage(@RequestBody(required = false) UserSendChatMessageRequest request) {
        LoginUserInfo user = requireCurrentUser();
        String sessionId = request == null ? null : trimToNull(request.getSessionId());
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "会话ID不能为空");
        }
        ConversationSessions session = requireOwnedSession(sessionId, user.getAccountId());
        if (safeInt(session.getStatus()) != SESSION_STATUS_ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前会话已结束");
        }

        int contentType = normalizeContentType(request == null ? null : request.getContentType());
        String content = normalizeMessageContent(contentType, request == null ? null : request.getContent());
        Map<String, Object> extraData = normalizeExtraData(contentType, request == null ? null : request.getExtraData());
        long now = System.currentTimeMillis();

        ConversationMessages message = new ConversationMessages();
        message.setId(SnowflakeIdUtil.nextConversationMessageId());
        message.setSessionId(sessionId);
        message.setSenderId(user.getAccountId());
        message.setSenderType(ROLE_USER);
        message.setReceiverId(session.getTechnicianAccountId());
        message.setReceiverType(ROLE_WORKER);
        message.setContentType(contentType);
        message.setContent(content);
        message.setExtraData(writeJson(extraData));
        message.setSendTime(now);
        message.setStatus(MESSAGE_STATUS_NORMAL);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(0);
        message.setIsDelete(0);
        if (!conversationMessagesService.save(message)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        ConversationSessions updateSession = new ConversationSessions();
        updateSession.setId(sessionId);
        updateSession.setLastMessageId(message.getId());
        updateSession.setLastMessageContent(buildLastMessagePreview(contentType, content));
        updateSession.setLastMessageTime(now);
        updateSession.setTechnicianUnreadCount(safeInt(session.getTechnicianUnreadCount()) + 1);
        updateSession.setUpdatedTime(now);
        if (!conversationSessionsService.updateById(updateSession)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "会话状态更新失败");
        }
        return Result.success(buildMessageItem(message));
    }

    @PostMapping(value = "/chat/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadChatMedia(
        @RequestParam(value = "mediaType", required = false) String mediaType,
        @RequestPart("file") MultipartFile file
    ) {
        LoginUserInfo user = requireCurrentUser();
        return Result.success(uploadChatMediaInternal("user", user.getAccountId(), mediaType, file));
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> listSessions() {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        List<ConversationSessions> sessions = conversationSessionsService.list(
            new LambdaQueryWrapper<ConversationSessions>()
                .eq(ConversationSessions::getUserAccountId, accountId)
                .eq(ConversationSessions::getStatus, SESSION_STATUS_ACTIVE)
                .eq(ConversationSessions::getIsDelete, 0)
                .orderByDesc(ConversationSessions::getLastMessageTime)
                .orderByDesc(ConversationSessions::getUpdatedTime)
        );
        if (sessions == null || sessions.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, RepairOrders> orderMap = listRepairOrderMap(sessions.stream()
            .map(ConversationSessions::getRepairOrderId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet()));
        Map<String, TechnicianAccounts> technicianMap = listTechnicianMap(sessions.stream()
            .map(ConversationSessions::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet()));

        List<Map<String, Object>> items = new ArrayList<>();
        for (ConversationSessions session : sessions) {
            RepairOrders order = orderMap.get(session.getRepairOrderId());
            TechnicianAccounts technician = technicianMap.get(session.getTechnicianAccountId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", session.getId());
            item.put("type", "chat");
            item.put("title", buildSessionTitle(order, technician));
            item.put("subtitle", buildSessionSubtitle(session));
            item.put("avatarUrl", "");
            item.put("unreadCount", safeInt(session.getUserUnreadCount()));
            item.put("orderId", order == null ? "" : safe(order.getId()));
            item.put("orderNo", order == null ? "" : safe(order.getOrderNo()));
            item.put("peerName", technician == null ? "维修师傅" : safe(technician.getUsername()));
            item.put("status", safeInt(session.getStatus()));
            item.put("lastMessageTime", session.getLastMessageTime());
            items.add(item);
        }
        return Result.success(items);
    }

    @GetMapping("/unread-flag")
    public Result<Map<String, Object>> unreadFlag() {
        LoginUserInfo user = requireCurrentUser();
        String accountId = user.getAccountId();
        long chatUnreadCount = conversationSessionsService.count(
            new LambdaQueryWrapper<ConversationSessions>()
                .eq(ConversationSessions::getUserAccountId, accountId)
                .eq(ConversationSessions::getStatus, SESSION_STATUS_ACTIVE)
                .eq(ConversationSessions::getIsDelete, 0)
                .gt(ConversationSessions::getUserUnreadCount, 0)
        );
        long systemUnreadCount = systemMessagesService.count(
            new LambdaQueryWrapper<SystemMessages>()
                .eq(SystemMessages::getReceiverId, accountId)
                .eq(SystemMessages::getReceiverType, ROLE_USER)
                .eq(SystemMessages::getIsRead, 0)
                .eq(SystemMessages::getIsDelete, 0)
        );
        int chatCount = (int) chatUnreadCount;
        int systemCount = (int) systemUnreadCount;
        Map<String, Object> data = new HashMap<>();
        data.put("chatUnreadCount", chatCount);
        data.put("systemUnreadCount", systemCount);
        data.put("totalUnreadCount", chatCount + systemCount);
        return Result.success(data);
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该接口");
        }
        return user;
    }

    private ConversationSessions requireOwnedSession(String sessionId, String accountId) {
        ConversationSessions session = conversationSessionsService.getById(sessionId);
        if (session == null
            || safeInt(session.getIsDelete()) != 0
            || !accountId.equals(session.getUserAccountId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "会话不存在或无权访问");
        }
        return session;
    }

    private Map<String, Object> buildMessageItem(ConversationMessages message) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", message.getId());
        item.put("sessionId", message.getSessionId());
        item.put("senderId", message.getSenderId());
        item.put("senderType", message.getSenderType());
        item.put("receiverId", message.getReceiverId());
        item.put("receiverType", message.getReceiverType());
        item.put("contentType", message.getContentType());
        item.put("content", safe(message.getContent()));
        item.put("extraData", parseJsonMap(message.getExtraData()));
        item.put("sendTime", message.getSendTime());
        item.put("readTime", message.getReadTime());
        return item;
    }

    private String buildSessionTitle(RepairOrders order, TechnicianAccounts technician) {
        String technicianName = technician == null ? "维修师傅" : safe(technician.getUsername());
        return technicianName;
    }

    private String buildSessionSubtitle(ConversationSessions session) {
        String lastContent = safe(session.getLastMessageContent());
        return StringUtils.hasText(lastContent) ? lastContent : "暂无消息";
    }

    private Map<String, RepairOrders> listRepairOrderMap(Set<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .in(RepairOrders::getId, orderIds)
                .eq(RepairOrders::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(RepairOrders::getId, item -> item, (left, right) -> left));
    }

    private Map<String, TechnicianAccounts> listTechnicianMap(Set<String> technicianIds) {
        if (technicianIds == null || technicianIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, technicianIds)
                .eq(TechnicianAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(TechnicianAccounts::getId, item -> item, (left, right) -> left));
    }

    private RepairOrders getRepairOrder(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        return repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private TechnicianAccounts getTechnician(String technicianId) {
        if (!StringUtils.hasText(technicianId)) {
            return null;
        }
        return technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, technicianId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
    }

    private int normalizeContentType(Integer contentType) {
        int value = contentType == null ? CONTENT_TYPE_TEXT : contentType;
        if (value != CONTENT_TYPE_TEXT && value != CONTENT_TYPE_IMAGE && value != CONTENT_TYPE_VIDEO) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持文本、图片和视频消息");
        }
        return value;
    }

    private String normalizeMessageContent(int contentType, String content) {
        String normalized = trimToNull(content);
        if (contentType == CONTENT_TYPE_TEXT) {
            if (!StringUtils.hasText(normalized)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入消息内容");
            }
            if (normalized.length() > MAX_TEXT_LENGTH) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能超过1000字");
            }
            return normalized;
        }
        return StringUtils.hasText(normalized)
            ? normalized
            : (contentType == CONTENT_TYPE_IMAGE ? "[图片]" : "[视频]");
    }

    private Map<String, Object> normalizeExtraData(int contentType, Map<String, Object> extraData) {
        Map<String, Object> normalized = extraData == null ? new HashMap<>() : new HashMap<>(extraData);
        if (contentType == CONTENT_TYPE_TEXT) {
            return normalized;
        }
        String mediaUrl = trimToNull(stringValue(normalized.get("url")));
        if (!StringUtils.hasText(mediaUrl)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "媒体地址不能为空");
        }
        normalized.put("url", mediaUrl);
        normalized.put("name", trimToNull(stringValue(normalized.get("name"))));
        normalized.put("thumbnailUrl", trimToNull(stringValue(normalized.get("thumbnailUrl"))));
        normalized.put("mimeType", trimToNull(stringValue(normalized.get("mimeType"))));
        return normalized;
    }

    private Map<String, Object> uploadChatMediaInternal(String roleFolder, String accountId, String mediaType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        String mediaKind = resolveUploadMediaType(mediaType, file.getContentType(), file.getOriginalFilename());
        UploadLimitUtil.validateMediaSize(mediaKind, file);
        String originalFilename = trimToNull(file.getOriginalFilename());
        String extension = resolveUploadExtension(originalFilename, file.getContentType(), mediaKind);
        String objectName = "conversation/" + roleFolder + "/" + accountId + "/" + mediaKind + "/" + UUID.randomUUID() + extension;

        String uploadUrl;
        try (InputStream in = file.getInputStream()) {
            uploadUrl = ossUtil.upload(objectName, in);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传媒体失败");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("url", uploadUrl);
        data.put("name", StringUtils.hasText(originalFilename) ? originalFilename : objectName);
        data.put("fileSize", file.getSize());
        data.put("mimeType", resolveUploadMimeType(file.getContentType(), mediaKind));
        if ("image".equals(mediaKind)) {
            try (InputStream in = file.getInputStream()) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    data.put("width", image.getWidth());
                    data.put("height", image.getHeight());
                }
            } catch (Exception ignored) {
            }
        }
        return data;
    }

    private String resolveUploadMediaType(String mediaType, String mimeType, String originalFilename) {
        String normalizedType = trimToNull(mediaType);
        if (StringUtils.hasText(normalizedType)) {
            String lower = normalizedType.toLowerCase();
            if ("image".equals(lower) || "video".equals(lower)) {
                return lower;
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "mediaType 仅支持 image 或 video");
        }
        String normalizedMime = trimToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if (lowerMime.startsWith("image/")) {
                return "image";
            }
            if (lowerMime.startsWith("video/")) {
                return "video";
            }
        }
        String fileName = trimToNull(originalFilename);
        if (StringUtils.hasText(fileName)) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp")) {
                return "image";
            }
            if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") || lowerName.endsWith(".m4v")
                || lowerName.endsWith(".avi") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm")) {
                return "video";
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别上传文件类型");
    }

    private String resolveUploadExtension(String originalFilename, String mimeType, String mediaType) {
        String filename = trimToNull(originalFilename);
        if (StringUtils.hasText(filename)) {
            int index = filename.lastIndexOf('.');
            if (index >= 0 && index < filename.length() - 1) {
                String extension = filename.substring(index);
                if (extension.length() <= 10) {
                    return extension;
                }
            }
        }
        String normalizedMime = trimToNull(mimeType);
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
        String normalized = trimToNull(mimeType);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "video".equals(mediaType) ? "video/mp4" : "image/jpeg";
    }

    private String buildLastMessagePreview(int contentType, String content) {
        if (contentType == CONTENT_TYPE_IMAGE) {
            return "[图片]";
        }
        if (contentType == CONTENT_TYPE_VIDEO) {
            return "[视频]";
        }
        if (contentType == CONTENT_TYPE_SYSTEM) {
            return "[系统消息]";
        }
        String normalized = safe(content);
        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private Map<String, Object> parseJsonMap(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String writeJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息附件数据格式错误");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Data
    private static final class UserSendChatMessageRequest {

        private String sessionId;
        private Integer contentType;
        private String content;
        private Map<String, Object> extraData = new HashMap<>();
    }
}
