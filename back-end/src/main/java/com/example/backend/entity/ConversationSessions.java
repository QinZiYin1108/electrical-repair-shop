package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 站内会话表（用户与师傅）
 * @TableName conversation_sessions
 */
@TableName(value ="conversation_sessions")
@Data
public class ConversationSessions {
    /**
     * 主键，CS+雪花ID
     */
    @TableId
    private String id;

    /**
     * 用户账号ID，对应user_accounts.id
     */
    private String userAccountId;

    /**
     * 师傅账号ID，对应technician_accounts.id
     */
    private String technicianAccountId;

    /**
     * 关联维修订单ID，对应repair_orders.id
     */
    private String repairOrderId;

    /**
     * 最后一条消息ID
     */
    private String lastMessageId;

    /**
     * 最后一条消息内容摘要
     */
    private String lastMessageContent;

    /**
     * 最后一条消息时间戳
     */
    private Long lastMessageTime;

    /**
     * 用户未读消息数
     */
    private Integer userUnreadCount;

    /**
     * 师傅未读消息数
     */
    private Integer technicianUnreadCount;

    /**
     * 会话状态：1-正常，2-已关闭
     */
    private Integer status;

    /**
     * 创建时间戳
     */
    private Long createdTime;

    /**
     * 更新时间戳
     */
    private Long updatedTime;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDelete;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ConversationSessions other = (ConversationSessions) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserAccountId() == null ? other.getUserAccountId() == null : this.getUserAccountId().equals(other.getUserAccountId()))
            && (this.getTechnicianAccountId() == null ? other.getTechnicianAccountId() == null : this.getTechnicianAccountId().equals(other.getTechnicianAccountId()))
            && (this.getRepairOrderId() == null ? other.getRepairOrderId() == null : this.getRepairOrderId().equals(other.getRepairOrderId()))
            && (this.getLastMessageId() == null ? other.getLastMessageId() == null : this.getLastMessageId().equals(other.getLastMessageId()))
            && (this.getLastMessageContent() == null ? other.getLastMessageContent() == null : this.getLastMessageContent().equals(other.getLastMessageContent()))
            && (this.getLastMessageTime() == null ? other.getLastMessageTime() == null : this.getLastMessageTime().equals(other.getLastMessageTime()))
            && (this.getUserUnreadCount() == null ? other.getUserUnreadCount() == null : this.getUserUnreadCount().equals(other.getUserUnreadCount()))
            && (this.getTechnicianUnreadCount() == null ? other.getTechnicianUnreadCount() == null : this.getTechnicianUnreadCount().equals(other.getTechnicianUnreadCount()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getUpdatedTime() == null ? other.getUpdatedTime() == null : this.getUpdatedTime().equals(other.getUpdatedTime()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserAccountId() == null) ? 0 : getUserAccountId().hashCode());
        result = prime * result + ((getTechnicianAccountId() == null) ? 0 : getTechnicianAccountId().hashCode());
        result = prime * result + ((getRepairOrderId() == null) ? 0 : getRepairOrderId().hashCode());
        result = prime * result + ((getLastMessageId() == null) ? 0 : getLastMessageId().hashCode());
        result = prime * result + ((getLastMessageContent() == null) ? 0 : getLastMessageContent().hashCode());
        result = prime * result + ((getLastMessageTime() == null) ? 0 : getLastMessageTime().hashCode());
        result = prime * result + ((getUserUnreadCount() == null) ? 0 : getUserUnreadCount().hashCode());
        result = prime * result + ((getTechnicianUnreadCount() == null) ? 0 : getTechnicianUnreadCount().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getUpdatedTime() == null) ? 0 : getUpdatedTime().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userAccountId=").append(userAccountId);
        sb.append(", technicianAccountId=").append(technicianAccountId);
        sb.append(", repairOrderId=").append(repairOrderId);
        sb.append(", lastMessageId=").append(lastMessageId);
        sb.append(", lastMessageContent=").append(lastMessageContent);
        sb.append(", lastMessageTime=").append(lastMessageTime);
        sb.append(", userUnreadCount=").append(userUnreadCount);
        sb.append(", technicianUnreadCount=").append(technicianUnreadCount);
        sb.append(", status=").append(status);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}