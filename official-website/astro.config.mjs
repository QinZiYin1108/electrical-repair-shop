import { defineConfig } from 'astro/config';

export default defineConfig({
  site: 'https://www.leonyin.cn',
  output: 'static',
  build: {
    assets: 'assets',
  },
});
