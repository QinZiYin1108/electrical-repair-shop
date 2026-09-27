package com.example.backend.service;

/** 商品库存的原子操作。用于替代“读取库存后再写回”的并发不安全写法。 */
public interface InventoryService {

    /**
     * 原子扣减库存：仅当剩余库存充足时扣减。
     *
     * @return 是否扣减成功；库存不足返回 false
     */
    boolean deductStock(String productId, int quantity, long now);

    /** 原子回补库存（取消/超时释放预占）。 */
    void restoreStock(String productId, int quantity, long now);

    /** 原子累加销量（支付成功后确认）。 */
    void increaseSales(String productId, int quantity, long now);
}
