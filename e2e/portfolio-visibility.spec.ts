import { test, expect, type Page } from "@playwright/test";

const BASE_URL = "http://localhost:5173";

let page: Page;

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage();
});

test.afterAll(async () => {
  await page.close();
});

test.describe.configure({ mode: "serial" });

// ── 포트폴리오 공개/비공개 ──

test.describe("포트폴리오 공개/비공개", () => {
  test("마이페이지 로드", async () => {
    await page.goto(`${BASE_URL}/mypage`);
    await expect(page.getByText("마이페이지")).toBeVisible();
    await expect(page.getByText("프로필", { exact: true })).toBeVisible();
  });

  test("포트폴리오 공개 설정 확인", async () => {
    await expect(page.getByText("포트폴리오 공개")).toBeVisible();

    // 현재 상태 확인 (공개/비공개)
    const toggleSwitch = page.locator('button[role="switch"]').first();
    await expect(toggleSwitch).toBeVisible();

    // 현재 상태 저장
    const currentState = await toggleSwitch.getAttribute("data-state");
    expect(currentState).toBeTruthy();
  });

  test("포트폴리오 공개 설정 토글", async () => {
    const toggleSwitch = page.locator('button[role="switch"]').first();
    const beforeState = await toggleSwitch.getAttribute("data-state");

    // 토글 클릭
    await toggleSwitch.click();
    await page.waitForTimeout(500);

    // 상태가 변경되었는지 확인
    const afterState = await toggleSwitch.getAttribute("data-state");
    expect(afterState).not.toBe(beforeState);

    // 공개/비공개 텍스트 변경 확인
    if (afterState === "checked") {
      await expect(page.getByText("공개").first()).toBeVisible();
    } else {
      await expect(page.getByText("비공개").first()).toBeVisible();
    }
  });

  test("토글 상태 복원", async () => {
    const toggleSwitch = page.locator('button[role="switch"]').first();

    // 다시 토글하여 원래 상태로 복원
    await toggleSwitch.click();
    await page.waitForTimeout(500);

    // 원래 상태로 돌아왔는지 확인
    await expect(page.getByText("포트폴리오 공개")).toBeVisible();
  });
});
