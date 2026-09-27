package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.mapper.ProductsMapper;
import org.junit.jupiter.api.Test;

class InventoryServiceImplTests {

    @Test
    void deductStockFalseWhenInsufficient() {
        ProductsMapper mapper = mock(ProductsMapper.class);
        when(mapper.deductStock("P1", 3, 100L)).thenReturn(0);
        InventoryServiceImpl service = new InventoryServiceImpl(mapper);

        assertFalse(service.deductStock("P1", 3, 100L));
    }

    @Test
    void deductStockTrueWhenOneRowUpdated() {
        ProductsMapper mapper = mock(ProductsMapper.class);
        when(mapper.deductStock("P1", 2, 100L)).thenReturn(1);
        InventoryServiceImpl service = new InventoryServiceImpl(mapper);

        assertTrue(service.deductStock("P1", 2, 100L));
    }

    @Test
    void nonPositiveQuantityIsNoOp() {
        ProductsMapper mapper = mock(ProductsMapper.class);
        InventoryServiceImpl service = new InventoryServiceImpl(mapper);

        assertFalse(service.deductStock("P1", 0, 100L));
        service.restoreStock("P1", 0, 100L);
        service.increaseSales("P1", -1, 100L);

        verifyNoInteractions(mapper);
    }

    @Test
    void restoreAndSalesDelegateToMapper() {
        ProductsMapper mapper = mock(ProductsMapper.class);
        InventoryServiceImpl service = new InventoryServiceImpl(mapper);

        service.restoreStock("P1", 2, 100L);
        service.increaseSales("P1", 2, 100L);

        verify(mapper).restoreStock("P1", 2, 100L);
        verify(mapper).increaseSales("P1", 2, 100L);
    }
}
