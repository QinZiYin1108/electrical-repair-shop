package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ShoppingCarts;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ShoppingCartsMapper extends BaseMapper<ShoppingCarts> {

    @Select("select id, account_id, product_id, quantity, selected, created_time, updated_time, version, is_delete from shopping_carts where account_id = #{accountId} and product_id = #{productId} limit 1")
    ShoppingCarts selectAnyByAccountIdAndProductId(@Param("accountId") String accountId, @Param("productId") String productId);

    @Update("update shopping_carts set quantity = #{quantity}, selected = #{selected}, updated_time = #{updatedTime}, is_delete = 0, version = ifnull(version, 0) + 1 where id = #{id}")
    int restoreById(@Param("id") String id, @Param("quantity") Integer quantity, @Param("selected") Integer selected, @Param("updatedTime") Long updatedTime);
}
