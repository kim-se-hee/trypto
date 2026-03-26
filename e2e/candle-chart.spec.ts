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

// ── 캔들 차트 ──

test.describe("캔들 차트", () => {
  test("마켓 페이지에서 차트 영역 확인", async () => {
    await page.goto(`${BASE_URL}/market`);
    await expect(page.getByText("코인 시세", { exact: true })).toBeVisible();

    // 차트 영역으로 스크롤 (WAXP 상세 아래)
    await page.evaluate(() => window.scrollTo(0, 500));
    await page.waitForTimeout(500);

    // 차트 주기 버튼 확인
    await expect(page.getByRole("button", { name: "1분" })).toBeVisible();
    await expect(page.getByRole("button", { name: "1시간" })).toBeVisible();
    await expect(page.getByRole("button", { name: "4시간" })).toBeVisible();
  });

  test("일간 차트 기본 표시", async () => {
    // 기본 선택은 "일"
    const dayButton = page.getByRole("button", { name: "일", exact: true });
    await expect(dayButton).toBeVisible();

    // 차트 캔버스/SVG 존재 확인
    const hasChart = await page
      .locator("canvas, svg")
      .first()
      .isVisible()
      .catch(() => false);
    expect(hasChart).toBe(true);
  });

  test("1분봉 차트로 전환", async () => {
    await page.getByRole("button", { name: "1분" }).click();
    await page.waitForTimeout(500);

    // 차트가 여전히 표시되는지 확인
    const hasChart = await page
      .locator("canvas, svg")
      .first()
      .isVisible()
      .catch(() => false);
    expect(hasChart).toBe(true);
  });

  test("주간 차트로 전환", async () => {
    await page.getByRole("button", { name: "주", exact: true }).click();
    await page.waitForTimeout(500);

    const hasChart = await page
      .locator("canvas, svg")
      .first()
      .isVisible()
      .catch(() => false);
    expect(hasChart).toBe(true);
  });

  test("월간 차트로 전환", async () => {
    await page.getByRole("button", { name: "월", exact: true }).click();
    await page.waitForTimeout(500);

    const hasChart = await page
      .locator("canvas, svg")
      .first()
      .isVisible()
      .catch(() => false);
    expect(hasChart).toBe(true);
  });
});
