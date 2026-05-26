<template>
  <div class="placeholder-page">
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const title = computed(() => route.meta.title || '功能模块');
const description = computed(() => route.meta.description || '该模块的具体页面稍后实现。');
</script>

<style scoped>
.placeholder-page {
  padding: 16px;
}
</style>

