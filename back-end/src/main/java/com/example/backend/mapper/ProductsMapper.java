package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Products;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author Administrator
 * @description 针对表【products(商品表)】的数据库操作Mapper
 * @createDate 2026-03-03 11:26:16 @Entity com.example.backend.entity.Products
 */
public interface ProductsMapper extends BaseMapper<Products> {

    @Update(
            "UPDATE products SET stock_quantity = stock_quantity - #{quantity}, updated_time = #{now} "
                    + "WHERE id = #{productId} AND is_delete = 0 AND stock_quantity >= #{quantity}")
    int deductStock(
            @Param("productId") String productId,
            @Param("quantity") int quantity,
            @Param("now") long now);

    @Update(
            "UPDATE products SET stock_quantity = stock_quantity + #{quantity}, updated_time = #{now} "
                    + "WHERE id = #{productId} AND is_delete = 0")
    int restoreStock(
            @Param("productId") String productId,
            @Param("quantity") int quantity,
            @Param("now") long now);

    @Update(
            "UPDATE products SET sales_count = COALESCE(sales_count, 0) + #{quantity}, "
                    + "updated_time = #{now} WHERE id = #{productId} AND is_delete = 0")
    int increaseSales(
            @Param("productId") String productId,
            @Param("quantity") int quantity,
            @Param("now") long now);
}
