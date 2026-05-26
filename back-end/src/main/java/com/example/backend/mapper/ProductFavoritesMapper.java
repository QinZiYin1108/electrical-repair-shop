package com.example.backend.mapper;

import com.example.backend.entity.ProductFavorites;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
* @author Administrator
* @description 针对表【product_favorites(商品收藏表)】的数据库操作Mapper
* @createDate 2026-03-03 11:26:16
* @Entity com.example.backend.entity.ProductFavorites
*/
public interface ProductFavoritesMapper extends BaseMapper<ProductFavorites> {

    @Select("select id, account_id, product_id, created_time, version, is_delete from product_favorites where account_id = #{accountId} and product_id = #{productId} limit 1")
    ProductFavorites selectAnyByAccountIdAndProductId(@Param("accountId") String accountId, @Param("productId") String productId);

    @Select("select id, account_id, product_id, created_time, version, is_delete from product_favorites where account_id = #{accountId} and is_delete = 0 order by created_time desc, id desc")
    List<ProductFavorites> selectActiveByAccountId(@Param("accountId") String accountId);

    @Select("select count(1) from product_favorites where product_id = #{productId} and is_delete = 0")
    Long countActiveByProductId(@Param("productId") String productId);

    @Update("update product_favorites set is_delete = 0, created_time = #{createdTime}, version = ifnull(version, 0) + 1 where id = #{id}")
    int restoreById(@Param("id") String id, @Param("createdTime") Long createdTime);

    @Update("update product_favorites set is_delete = 1, version = ifnull(version, 0) + 1 where id = #{id} and is_delete = 0")
    int softDeleteById(@Param("id") String id);
}




