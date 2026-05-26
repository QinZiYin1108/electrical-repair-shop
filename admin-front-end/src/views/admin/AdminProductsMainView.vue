<template>
  <AdminProductManagePanel
    :product-type="1"
    title="商品信息管理"
    description="管理电器商城中的普通商品，包括基础信息、价格、库存、图文与规格参数。"
  />
</template>

<script setup>
import AdminProductManagePanel from './components/AdminProductManagePanel.vue';
</script>
