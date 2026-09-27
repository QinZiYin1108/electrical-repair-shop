import { defineConfig, devices } from '@playwright/test';

/**
 * 默认目标：官网（Astro preview，端口 4321）。
 * 后台：设置环境变量 E2E_ADMIN_URL（例如 http://localhost:8080）后运行 `npm run test:admin`，
 *       且需先手动启动 admin-front-end（`npm run serve`）。
 * 覆盖：E2E_OFFICIAL_URL / E2E_ADMIN_URL 可覆盖默认地址；设置后不再自动启动官网 webServer。
 */
const OFFICIAL_URL = process.env.E2E_OFFICIAL_URL ?? 'http://localhost:4321';
const ADMIN_URL = process.env.E2E_ADMIN_URL ?? 'http://localhost:8080';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'official-website',
      testMatch: /official-website\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: OFFICIAL_URL },
    },
    {
      name: 'admin-front-end',
      testMatch: /admin-front-end\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: ADMIN_URL },
    },
  ],
  // 仅当未外部指定官网地址时，自动拉起官网预览服务器
  webServer: process.env.E2E_OFFICIAL_URL
    ? undefined
    : {
        command: 'npm run preview',
        cwd: '../official-website',
        url: OFFICIAL_URL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
