const { defineConfig } = require('@vue/cli-service');

const devProxyTarget = process.env.VUE_APP_DEV_PROXY_TARGET || 'https://leonyin.cn';

module.exports = defineConfig({
  transpileDependencies: true,
  parallel: false,
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: devProxyTarget,
        changeOrigin: true
      }
    }
  }
});
