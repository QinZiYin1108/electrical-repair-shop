package com.example.backend.service.impl;

import com.example.backend.mapper.ProductsMapper;
import com.example.backend.service.InventoryService;
import org.springframework.stereotype.Service;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final ProductsMapper productsMapper;

    public InventoryServiceImpl(ProductsMapper productsMapper) {
        this.productsMapper = productsMapper;
    }

    @Override
    public boolean deductStock(String productId, int quantity, long now) {
        if (productId == null || quantity <= 0) {
            return false;
        }
        return productsMapper.deductStock(productId, quantity, now) == 1;
    }

    @Override
    public void restoreStock(String productId, int quantity, long now) {
        if (productId == null || quantity <= 0) {
            return;
        }
        productsMapper.restoreStock(productId, quantity, now);
    }

    @Override
    public void increaseSales(String productId, int quantity, long now) {
        if (productId == null || quantity <= 0) {
            return;
        }
        productsMapper.increaseSales(productId, quantity, now);
    }
}
