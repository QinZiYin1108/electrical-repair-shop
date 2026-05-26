package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 师傅技能表
 * @TableName technician_skills
 */
@TableName(value ="technician_skills")
@Data
public class TechnicianSkills {
    /**
     * 主键，TS+雪花ID
     */
    @TableId
    private String id;

    /**
     * 师傅账号ID
     */
    private String technicianAccountId;

    /**
     * 服务类型ID
     */
    private String serviceTypeId;

    /**
     * 技能等级：1-初级，2-中级，3-高级，4-专家
     */
    private Integer skillLevel;

    /**
     * 认证证书URL
     */
    private String certificationUrl;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer isActive;

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
        TechnicianSkills other = (TechnicianSkills) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getTechnicianAccountId() == null ? other.getTechnicianAccountId() == null : this.getTechnicianAccountId().equals(other.getTechnicianAccountId()))
            && (this.getServiceTypeId() == null ? other.getServiceTypeId() == null : this.getServiceTypeId().equals(other.getServiceTypeId()))
            && (this.getSkillLevel() == null ? other.getSkillLevel() == null : this.getSkillLevel().equals(other.getSkillLevel()))
            && (this.getCertificationUrl() == null ? other.getCertificationUrl() == null : this.getCertificationUrl().equals(other.getCertificationUrl()))
            && (this.getIsActive() == null ? other.getIsActive() == null : this.getIsActive().equals(other.getIsActive()))
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
        result = prime * result + ((getTechnicianAccountId() == null) ? 0 : getTechnicianAccountId().hashCode());
        result = prime * result + ((getServiceTypeId() == null) ? 0 : getServiceTypeId().hashCode());
        result = prime * result + ((getSkillLevel() == null) ? 0 : getSkillLevel().hashCode());
        result = prime * result + ((getCertificationUrl() == null) ? 0 : getCertificationUrl().hashCode());
        result = prime * result + ((getIsActive() == null) ? 0 : getIsActive().hashCode());
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
        sb.append(", technicianAccountId=").append(technicianAccountId);
        sb.append(", serviceTypeId=").append(serviceTypeId);
        sb.append(", skillLevel=").append(skillLevel);
        sb.append(", certificationUrl=").append(certificationUrl);
        sb.append(", isActive=").append(isActive);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", version=").append(version);
        sb.append(", isDelete=").append(isDelete);
        sb.append("]");
        return sb.toString();
    }
}