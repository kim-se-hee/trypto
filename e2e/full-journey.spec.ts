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

// ── 1단계: 마켓 탐색 ──

test.describe("마켓 탐색", () => {
  test("업비트 마켓 페이지 로드", async () => {
    await page.goto(`${BASE_URL}/market`);
    await expect(page.getByText("코인 시세", { exact: true })).toBeVisible();
    await expect(page.getByText("업비트 기준", { exact: false })).toBeVisible();
  });

  test("빗썸 거래소 탭 전환", async () => {
    await page.locator("button", { hasText: "빗썸" }).first().click();
    await expect(page.getByText("빗썸 기준", { exact: false })).toBeVisible();
  });

  test("바이낸스 거래소 탭 전환", async () => {
    await page.locator("button", { hasText: "바이낸스" }).first().click();
    await expect(page.getByText("바이낸스 기준", { exact: false })).toBeVisible();
    await expect(page.getByText("USDT 마켓", { exact: false })).toBeVisible();
  });

  test("업비트로 복귀", async () => {
    await page.locator("button", { hasText: "업비트" }).first().click();
    await expect(page.getByText("업비트 기준", { exact: false })).toBeVisible();
  });
});

// ── 2단계: 포트폴리오 확인 ──

test.describe("포트폴리오 확인", () => {
  test("포트폴리오 페이지 로드", async () => {
    await page.click('a[href="/portfolio"]');
    await expect(page.getByText("투자내역", { exact: true })).toBeVisible();
  });

  test("거래소 탭과 보유 자산 표시", async () => {
    await expect(page.getByText("보유 KRW", { exact: true }).first()).toBeVisible();
    await expect(page.getByText("총 보유자산")).toBeVisible();
  });
});

// ── 3단계: 시장가 매수 주문 ──

test.describe("시장가 매수 주문", () => {
  test("시장가 매수 실행", async () => {
    await page.goto(`${BASE_URL}/market`);
    await page.waitForLoadState("networkidle");

    await page.evaluate(() => window.scrollTo(0, 800));
    await page.locator("button", { hasText: "시장가" }).first().click();
    await page.fill('input[placeholder="0"]', "10000");
    await page.locator("button", { hasText: "매수" }).last().click();

    await page.waitForTimeout(1000);
    await expect(page.locator('input[placeholder="0"]')).toHaveValue("");
  });

  test("체결 내역에 매수 주문 표시", async () => {
    await page.locator("button", { hasText: "거래내역" }).click();
    await expect(page.getByRole("button", { name: "체결", exact: true })).toBeVisible();
    await expect(page.locator("span:text('매수')").first()).toBeVisible();
  });
});

// ── 4단계: 시장가 매도 주문 ──

test.describe("시장가 매도 주문", () => {
  test("시장가 매도 실행", async () => {
    await page.locator("button", { hasText: "매도" }).first().click();
    await expect(page.getByText("주문 가능", { exact: true })).toBeVisible();

    await page.locator("button", { hasText: "10%" }).click();
    await page.locator("button", { hasText: "매도" }).last().click();

    await page.waitForTimeout(1000);
  });
});

// ── 5단계: 지정가 매수 주문 ──

test.describe("지정가 매수 주문", () => {
  test("지정가 매수 주문 등록", async () => {
    await page.locator("button", { hasText: "매수" }).first().click();
    await page.locator("button", { hasText: "지정가" }).click();
    await expect(page.getByText("주문 가능", { exact: true })).toBeVisible();

    const priceInput = page.locator("input").nth(1);
    await priceInput.fill("5");
    await page.fill('input[placeholder="0"]', "10000");
    // 매수 submit 버튼 JS 클릭 (sticky 레이아웃으로 viewport 밖 이슈)
    await page.evaluate(() => {
      const buttons = document.querySelectorAll("button");
      const submitBtn = Array.from(buttons).filter(
        (b) => b.textContent?.trim() === "매수" && b.className.includes("inline-flex")
      );
      submitBtn[submitBtn.length - 1]?.click();
    });
    await page.waitForTimeout(1000);
  });

  test("미체결 주문 확인", async () => {
    await page.locator("button", { hasText: "거래내역" }).click();
    await page.getByRole("button", { name: "미체결" }).click();

    await expect(page.getByText("대기").first()).toBeVisible();
    await expect(page.locator("span:text('지정가')").first()).toBeVisible();
  });
});

// ── 6단계: 입출금 ──

test.describe("입출금", () => {
  test("입출금 페이지 로드 및 잔고 표시", async () => {
    await page.goto(`${BASE_URL}/wallet`);
    await expect(page.getByText("보유 자산")).toBeVisible();
    await expect(page.getByText("KRW").first()).toBeVisible();
  });

  test("코인 클릭 시 상세 패널 표시", async () => {
    await page.getByText("원화").click();
    await expect(page.getByText("잔고 상세").first()).toBeVisible();
  });
});

// ── 7단계: 긴급 자금 충전 ──

test.describe("긴급 자금 충전", () => {
  test("긴급 자금 충전 성공", async () => {
    await page.goto(`${BASE_URL}/market`);
    await page.waitForLoadState("networkidle");
    await page.evaluate(() => window.scrollTo(0, 400));

    await page.locator("button", { hasText: "긴급 자금 투입하기" }).click();

    await expect(page.getByRole("heading", { name: "긴급 자금 투입" })).toBeVisible();
    await expect(page.getByText("1회 상한은")).toBeVisible();

    await page.fill('input[placeholder="금액 입력"]', "100000");
    await page.locator("button", { hasText: "투입 확정" }).click();
    await page.waitForTimeout(1000);

    await expect(page.getByText("남은 횟수").first()).toBeVisible();
  });
});

// ── 8단계: 랭킹 조회 ──

test.describe("랭킹 조회", () => {
  test("랭킹 페이지 로드", async () => {
    await page.click('a[href="/ranking"]');
    await expect(page.getByRole("heading", { name: "랭킹" })).toBeVisible();
  });

  test("내 랭킹 표시", async () => {
    await expect(page.getByText("내 랭킹", { exact: true })).toBeVisible();
  });

  test("일간 통계 표시", async () => {
    await expect(page.getByText("일간 통계")).toBeVisible();
    await expect(page.getByText("참여자").first()).toBeVisible();
    await expect(page.getByText("최고 수익률")).toBeVisible();
    await expect(page.getByText("평균 수익률")).toBeVisible();
  });

  test("랭킹 목록에 Top 3 표시", async () => {
    const cards = page.locator("text=/%/");
    await expect(cards.first()).toBeVisible();
  });
});

// ── 9단계: 투자 복기 ──

test.describe("투자 복기", () => {
  test("투자 복기 페이지 로드", async () => {
    await page.click('a[href="/regret"]');
    await expect(page.getByText("투자 복기", { exact: true }).first()).toBeVisible();
    await expect(page.getByText("규칙만 지켰으면 얼마를 벌었을까?")).toBeVisible();
  });

  test("복기 그래프 또는 빈 상태 표시", async () => {
    const hasData = await page.getByText("놓친 수익").isVisible().catch(() => false);
    if (hasData) {
      await expect(page.getByText("실제").first()).toBeVisible();
    }
    await expect(page.getByText("투자 복기").first()).toBeVisible();
  });

  test("복기 하단 섹션 표시", async () => {
    await page.evaluate(() => window.scrollTo(0, 600));
    const hasViolation = await page.getByText("규칙 위반 거래").isVisible().catch(() => false);
    if (hasViolation) {
      await expect(page.getByText("나 vs 나")).toBeVisible();
    }
  });
});

// ── 10단계: 프로필 설정 ──

test.describe("프로필 설정", () => {
  test("마이페이지 로드", async () => {
    await page.click('a[href="/mypage"]');
    await expect(page.getByText("마이페이지")).toBeVisible();
    await expect(page.getByText("프로필", { exact: true })).toBeVisible();
  });

  test("닉네임 변경", async () => {
    await page.getByText("수정").click();

    const nicknameInput = page.locator("input").first();
    await nicknameInput.clear();
    await nicknameInput.fill("E2E테스트유저");

    await page.getByRole("button", { name: "저장" }).click();
    await page.waitForTimeout(500);
    await expect(page.getByText("E2E테스트유저").first()).toBeVisible();

    // 복원
    await page.getByText("수정").click();
    const restoreInput = page.locator("input").first();
    await restoreInput.clear();
    await restoreInput.fill("코인러너");
    await page.getByRole("button", { name: "저장" }).click();
    await page.waitForTimeout(500);
  });

  test("현재 라운드 정보 표시", async () => {
    await expect(page.getByText("현재 라운드")).toBeVisible();
    await expect(page.getByText("진행중")).toBeVisible();
  });

  test("투자 원칙 표시", async () => {
    await expect(page.getByText("투자 원칙", { exact: true })).toBeVisible();
    await expect(page.getByText("손절").first()).toBeVisible();
  });
});

// ── 11단계: 라운드 종료 ──

test.describe("라운드 종료", () => {
  test("라운드 종료", async () => {
    await page.evaluate(() => window.scrollTo(0, 600));
    await page.getByRole("button", { name: "라운드 종료" }).click();

    await expect(page.getByText("라운드를 종료하시겠습니까?")).toBeVisible();

    await page.getByRole("button", { name: "종료", exact: true }).click();
    await page.waitForTimeout(1000);

    await expect(page.getByText("투자 라운드 시작")).toBeVisible();
  });
});

// ── 12단계: 라운드 시작 ──

test.describe("라운드 시작", () => {
  test("새 라운드 시작", async () => {
    await page.getByRole("button", { name: "500만" }).first().click();
    await page.getByRole("button", { name: "50만" }).click();
    await page.locator('button[role="switch"]').first().click();

    await page.evaluate(() => window.scrollTo(0, 2000));
    await page.locator("button", { hasText: "라운드 시작하기" }).click();
    await page.waitForTimeout(1000);

    await expect(page.getByText("코인 시세", { exact: true })).toBeVisible();
    await expect(page.getByText("긴급 자금 투입").first()).toBeVisible();
    await expect(page.getByText("3회").first()).toBeVisible();
  });
});
