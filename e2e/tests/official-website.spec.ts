import { test, expect } from '@playwright/test';

// 官网关键路径烟测：首页可达、渲染、含可见内容。
// 运行前提：official-website 已 `npm run build`（webServer 会执行 `npm run preview`）。
test.describe('官网 smoke', () => {
  test('首页可加载并渲染', async ({ page }) => {
    const response = await page.goto('/');
    expect(response?.ok()).toBeTruthy();
    await expect(page.locator('body')).toBeVisible();
    // 首页应至少渲染一个可点击链接或按钮
    const interactive = page.locator('a, button').first();
    await expect(interactive).toBeVisible();
  });
});
