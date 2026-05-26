<template>
  <div class="protocol-page" v-loading="loading">
    <div class="protocol-shell">
      <div class="protocol-topbar">
        <el-button text @click="goBack">返回</el-button>
        <div class="protocol-meta" v-if="protocol.updatedTime">
          最近更新：{{ formatTime(protocol.updatedTime) }}
        </div>
      </div>

      <div class="protocol-card">
        <h1>{{ protocol.title || '协议内容' }}</h1>
        <div class="protocol-filename" v-if="protocol.fileName">
          文件：{{ protocol.fileName }}
        </div>
        <div class="protocol-content" v-html="protocol.htmlContent"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchPublicProtocol } from '../../api/adminSystemSettings';
import { markdownToHtml } from '../../utils/markdown';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const protocol = reactive({
  title: '',
  fileName: '',
  content: '',
  htmlContent: '',
  updatedTime: null
});

watch(
  () => route.params.type,
  () => {
    loadProtocol();
  }
);

onMounted(() => {
  loadProtocol();
});

async function loadProtocol() {
  const type = String(route.params.type || '');
  if (!type) {
    return;
  }
  loading.value = true;
  try {
    const res = await fetchPublicProtocol(type);
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || '加载协议失败');
      return;
    }
    protocol.title = res.data.title || '';
    protocol.fileName = res.data.fileName || '';
    protocol.content = res.data.content || '';
    protocol.htmlContent = markdownToHtml(protocol.content);
    protocol.updatedTime = res.data.updatedTime || null;
    document.title = res.data.title || '协议内容';
  } catch (error) {
    ElMessage.error('加载协议失败');
  } finally {
    loading.value = false;
  }
}

function formatTime(value) {
  const date = new Date(Number(value || 0));
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return date.toLocaleString();
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push('/login');
}
</script>

<style scoped>
.protocol-page {
  min-height: 100vh;
  padding: 32px 20px;
  background: linear-gradient(180deg, #eef6fb 0%, #f8fbfd 100%);
  box-sizing: border-box;
}

.protocol-shell {
  max-width: 920px;
  margin: 0 auto;
}

.protocol-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.protocol-meta {
  font-size: 13px;
  color: #6a7d8d;
}

.protocol-card {
  background: #fff;
  border-radius: 24px;
  padding: 28px;
  box-shadow: 0 18px 60px rgba(29, 55, 77, 0.08);
}

.protocol-card h1 {
  margin: 0;
  font-size: 28px;
  color: #173247;
}

.protocol-filename {
  margin-top: 10px;
  font-size: 13px;
  color: #7b8d9c;
}

.protocol-content {
  margin-top: 24px;
}

:deep(.protocol-content img) {
  max-width: 100%;
}

@media (max-width: 768px) {
  .protocol-page {
    padding: 16px;
  }

  .protocol-card {
    padding: 20px;
    border-radius: 18px;
  }

  .protocol-topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
