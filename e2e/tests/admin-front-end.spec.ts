import { test, expect } from '@playwright/test';

// 后台烟测：登录页可达并渲染。
// 需先设置 E2E_ADMIN_URL 并手动启动 admin-front-end（`npm run serve`），否则跳过。
const ADMIN_URL = process.env.E2E_ADMIN_URL;

test.describe('后台 smoke', () => {
  test.skip(!ADMIN_URL, '未设置 E2E_ADMIN_URL，跳过后台 E2E');

  test('登录页可加载并渲染', async ({ page }) => {
    const response = await page.goto('/');
    expect(response?.ok()).toBeTruthy();
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('input').first()).toBeVisible();
  });
});
