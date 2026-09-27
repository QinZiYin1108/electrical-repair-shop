package com.example.backend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 定时任务分布式锁的存取。锁名全局唯一，通过 `locked_until` 实现到期可抢占。 */
public interface SchedulerLocksMapper {

    @Insert(
            "INSERT INTO scheduler_locks(name, locked_until, locked_by, updated_time) "
                    + "VALUES(#{name}, #{until}, #{owner}, #{now})")
    int insertLock(
            @Param("name") String name,
            @Param("until") long until,
            @Param("owner") String owner,
            @Param("now") long now);

    @Update(
            "UPDATE scheduler_locks SET locked_until = #{until}, locked_by = #{owner}, "
                    + "updated_time = #{now} WHERE name = #{name} AND locked_until < #{now}")
    int acquireExpired(
            @Param("name") String name,
            @Param("until") long until,
            @Param("owner") String owner,
            @Param("now") long now);

    @Update(
            "UPDATE scheduler_locks SET locked_until = 0, updated_time = #{now} WHERE name = #{name}")
    int release(@Param("name") String name, @Param("now") long now);
}
