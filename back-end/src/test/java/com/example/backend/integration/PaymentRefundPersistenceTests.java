package com.example.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.backend.common.system.SystemConfigBootstrap;
import com.example.backend.entity.PaymentRefunds;
import com.example.backend.mapper.PaymentRefundsMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 真实 MySQL（Testcontainers）集成测试：覆盖唯一约束与并发写入。 用 Flyway 基线建表；无 Docker 环境（如本机）自动跳过，在有 Docker 的 CI 执行。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PaymentRefundPersistenceTests {

    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @MockBean SystemConfigBootstrap systemConfigBootstrap;

    @Autowired PaymentRefundsMapper paymentRefundsMapper;

    @Test
    void duplicateIdempotencyKeyIsRejected() {
        paymentRefundsMapper.insert(refund("RF-A", "IDEM-DUP"));

        assertThrows(
                DuplicateKeyException.class,
                () -> paymentRefundsMapper.insert(refund("RF-B", "IDEM-DUP")));
    }

    @Test
    void concurrentDuplicateIdempotencyKeyInsertsOnlyOnce() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger inserted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int index = i;
            futures.add(
                    pool.submit(
                            () -> {
                                try {
                                    start.await();
                                    paymentRefundsMapper.insert(
                                            refund("RF-C" + index, "IDEM-CONC"));
                                    inserted.incrementAndGet();
                                } catch (Exception ignored) {
                                    // 唯一约束冲突预期发生
                                }
                            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        assertEquals(1, inserted.get());
    }

    private PaymentRefunds refund(String id, String idempotencyKey) {
        long now = System.currentTimeMillis();
        PaymentRefunds refund = new PaymentRefunds();
        refund.setId(id);
        refund.setRefundNo("REF-" + id);
        refund.setPaymentId("PR1");
        refund.setPaymentNo("PAY1");
        refund.setAccountId("U1");
        refund.setRefundAmount(new BigDecimal("1.00"));
        refund.setCurrency("CNY");
        refund.setRefundStatus(1);
        refund.setProvider(1);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setInitiatedTime(now);
        refund.setCreatedTime(now);
        refund.setUpdatedTime(now);
        refund.setVersion(0);
        refund.setIsDelete(0);
        return refund;
    }
}
