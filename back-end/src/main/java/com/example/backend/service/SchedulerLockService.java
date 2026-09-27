package com.example.backend.service;

import com.example.backend.mapper.SchedulerLocksMapper;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 轻量定时任务分布式锁，基于 MySQL 单表实现多实例互斥。
 *
 * <p>约定：任务自身仍需幂等，锁只用于避免多实例重复执行同一批次。
 */
@Service
public class SchedulerLockService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerLockService.class);

    private final SchedulerLocksMapper schedulerLocksMapper;
    private final String owner = resolveOwner();

    public SchedulerLockService(SchedulerLocksMapper schedulerLocksMapper) {
        this.schedulerLocksMapper = schedulerLocksMapper;
    }

    private static String resolveOwner() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            host = "unknown";
        }
        return host + ":" + ProcessHandle.current().pid();
    }

    /**
     * 尝试获取名为 name 的锁，最长持有 atMostForMillis。
     *
     * @return 是否获取成功
     */
    public boolean tryLock(String name, long atMostForMillis, long now) {
        long until = now + Math.max(atMostForMillis, 1L);
        try {
            if (schedulerLocksMapper.insertLock(name, until, owner, now) == 1) {
                return true;
            }
        } catch (DuplicateKeyException ex) {
            // 锁已存在，尝试抢占已过期的锁
        }
        return schedulerLocksMapper.acquireExpired(name, until, owner, now) == 1;
    }

    /** 释放锁。 */
    public void release(String name, long now) {
        try {
            schedulerLocksMapper.release(name, now);
        } catch (RuntimeException ex) {
            log.warn("释放调度锁失败: name={}", name, ex);
        }
    }

    /** 在锁保护下运行任务；未获取到锁则直接跳过。 */
    public void runLocked(String name, long atMostForMillis, Runnable task) {
        long now = System.currentTimeMillis();
        if (!tryLock(name, atMostForMillis, now)) {
            log.debug("调度锁被占用，跳过本次执行: name={}", name);
            return;
        }
        try {
            task.run();
        } finally {
            release(name, System.currentTimeMillis());
        }
    }
}
