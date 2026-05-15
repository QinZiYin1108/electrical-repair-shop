package com.example.backend.mapper;

import com.example.backend.entity.TechnicianSkills;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
* @author Administrator
* @description 针对表【technician_skills(师傅技能表)】的数据库操作Mapper
* @createDate 2026-03-03 11:26:16
* @Entity com.example.backend.entity.TechnicianSkills
*/
public interface TechnicianSkillsMapper extends BaseMapper<TechnicianSkills> {

    @Select("""
        SELECT *
        FROM technician_skills
        WHERE technician_account_id = #{technicianAccountId}
          AND service_type_id = #{serviceTypeId}
        LIMIT 1
        """)
    TechnicianSkills selectAnyByTechnicianAndServiceType(
        @Param("technicianAccountId") String technicianAccountId,
        @Param("serviceTypeId") String serviceTypeId
    );

    @Update("""
        UPDATE technician_skills
        SET skill_level = 1,
            is_active = 1,
            is_delete = 0,
            updated_time = #{updatedTime}
        WHERE technician_account_id = #{technicianAccountId}
          AND service_type_id = #{serviceTypeId}
        LIMIT 1
        """)
    int restoreByTechnicianAndServiceType(
        @Param("technicianAccountId") String technicianAccountId,
        @Param("serviceTypeId") String serviceTypeId,
        @Param("updatedTime") Long updatedTime
    );

    @Update("""
        UPDATE technician_skills
        SET is_active = 0,
            is_delete = 1,
            updated_time = #{updatedTime}
        WHERE technician_account_id = #{technicianAccountId}
          AND service_type_id = #{serviceTypeId}
          AND is_delete = 0
        """)
    int logicalDeleteByTechnicianAndServiceType(
        @Param("technicianAccountId") String technicianAccountId,
        @Param("serviceTypeId") String serviceTypeId,
        @Param("updatedTime") Long updatedTime
    );
}




