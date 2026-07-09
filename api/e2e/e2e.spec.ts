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

// ── 2단계: 캔들 차트 ──

test.describe("캔들 차트", () => {
  test("마켓 페이지에서 차트 영역 확인", async () => {
    await page.goto(`${BASE_URL}/market`);
    await expect(page.getByText("코인 시세", { exact: true })).toBeVisible();

    await page.evaluate(() => window.scrollTo(0, 500));
    await page.waitForTimeout(500);

    await expect(page.getByRole("button", { name: "1분" })).toBeVisible();
    await expect(page.getByRole("button", { name: "1시간" })).toBeVisible();
    await expect(page.getByRole("button", { name: "4시간" })).toBeVisible();
  });

  test("일간 차트 기본 표시", async () => {
    const dayButton = page.getByRole("button", { name: "일", exact: true });
    await expect(dayButton).toBeVisible();

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

// ── 3단계: 포트폴리오 확인 ──

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

// ── 4단계: 시장가 매수 주문 ──

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

// ── 5단계: 시장가 매도 주문 ──

test.describe("시장가 매도 주문", () => {
  test("시장가 매도 실행", async () => {
    await page.locator("button", { hasText: "매도" }).first().click();
    await expect(page.getByText("주문 가능", { exact: true })).toBeVisible();

    await page.locator("button", { hasText: "10%" }).click();
    await page.locator("button", { hasText: "매도" }).last().click();

    await page.waitForTimeout(1000);
  });
});

// ── 6단계: 지정가 매수 주문 ──

test.describe("지정가 매수 주문", () => {
  test("지정가 매수 주문 등록", async () => {
    await page.locator("button", { hasText: "매수" }).first().click();
    await page.locator("button", { hasText: "지정가" }).click();
    await expect(page.getByText("주문 가능", { exact: true })).toBeVisible();

    const priceInput = page.locator("input").nth(1);
    await priceInput.fill("5");
    await page.fill('input[placeholder="0"]', "10000");
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

// ── 7단계: 주문 취소 ──

test.describe("주문 취소", () => {
  test("마켓 페이지 로드", async () => {
    await page.goto(`${BASE_URL}/market`);
    await expect(page.getByText("코인 시세", { exact: true })).toBeVisible();
  });

  test("지정가 매수 주문 등록", async () => {
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(500);

    await page.locator("button", { hasText: "매수" }).first().click();
    await page.locator("button", { hasText: "지정가" }).first().click();
    await expect(page.getByText("주문 가능", { exact: true })).toBeVisible();

    const priceInput = page.locator("input").nth(1);
    await priceInput.fill("1");
    await page.fill('input[placeholder="0"]', "5000");

    await page.evaluate(() => {
      const buttons = document.querySelectorAll("button");
      const submitBtn = Array.from(buttons).filter(
        (b) =>
          b.textContent?.trim() === "매수" && b.className.includes("inline-flex")
      );
      submitBtn[submitBtn.length - 1]?.click();
    });
    await page.waitForTimeout(1000);
  });

  test("미체결 주문 목록에서 주문 확인", async () => {
    await page.locator("button", { hasText: "거래내역" }).click();
    await page.getByRole("button", { name: "미체결" }).click();
    await expect(page.getByText("대기").first()).toBeVisible();
    await expect(page.locator("span:text('지정가')").first()).toBeVisible();
  });

  test("미체결 주문 취소", async () => {
    const beforeCount = await page.locator("button", { hasText: "취소" }).count();
    await page.locator("button", { hasText: "취소" }).first().click();
    await page.waitForTimeout(1500);

    const afterCount = await page.locator("button", { hasText: "취소" }).count();
    expect(afterCount).toBeLessThan(beforeCount);
  });
});

// ── 8단계: 지정가 매도 ──

test.describe("지정가 매도 준비", () => {
  test("시장가 매수로 코인 확보", async () => {
    await page.goto(`${BASE_URL}/market`);
    await page.waitForLoadState("networkidle");
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(500);

    await page.locator("button", { hasText: "매수" }).first().click();
    await page.locator("button", { hasText: "시장가" }).first().click();
    await page.fill('input[placeholder="0"]', "10000");
    await page.locator("button", { hasText: "매수" }).last().click();
    await page.waitForTimeout(1000);
  });
});

test.describe("지정가 매도", () => {
  test("매도 탭으로 전환", async () => {
    await page.locator("button", { hasText: "매도" }).first().click();
    await expect(page.getByText("주문 가능", { exact: true })).toBeVisible();
  });

  test("지정가 매도 주문 등록", async () => {
    await page.locator("button", { hasText: "지정가" }).first().click();

    const priceInput = page.locator("input").nth(1);
    await priceInput.fill("99999999");

    await page.locator("button", { hasText: "10%" }).click();

    await page.evaluate(() => {
      const buttons = document.querySelectorAll("button");
      const submitBtn = Array.from(buttons).filter(
        (b) =>
          b.textContent?.trim() === "매도" && b.className.includes("inline-flex")
      );
      submitBtn[submitBtn.length - 1]?.click();
    });
    await page.waitForTimeout(1000);
  });

  test("미체결 매도 주문 확인", async () => {
    await page.locator("button", { hasText: "거래내역" }).click();
    await page.getByRole("button", { name: "미체결" }).click();
    await expect(page.getByText("대기").first()).toBeVisible();
    await expect(page.locator("span:text('지정가')").first()).toBeVisible();
  });
});

// ── 9단계: 입출금 ──

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

// ── 10단계: 거래소 간 송금 ──

test.describe("거래소 간 송금", () => {
  test("출금 모달 열기", async () => {
    await page.evaluate(() => {
      document
        .querySelectorAll('[data-slot="dialog-close"]')
        .forEach((b) => (b as HTMLElement).click());
    });
    await page.waitForTimeout(300);

    await page.goto(`${BASE_URL}/wallet`);
    await page.waitForLoadState("networkidle");

    await page.evaluate(() => {
      const items = document.querySelectorAll('[class*="cursor-pointer"]');
      const btcItem = Array.from(items).find(
        (e) => e.textContent?.includes("BTC") && e.textContent?.includes("비트코인")
      );
      btcItem?.scrollIntoView();
      (btcItem as HTMLElement)?.click();
    });
    await page.waitForTimeout(500);

    await page.evaluate(() => {
      const buttons = document.querySelectorAll("button");
      const withdrawBtn = Array.from(buttons).find(
        (b) =>
          b.textContent?.includes("출금") && !b.textContent?.includes("입출금")
      );
      (withdrawBtn as HTMLElement)?.click();
    });
    await page.waitForTimeout(500);

    await expect(page.getByText("출금", { exact: false }).first()).toBeVisible();
  });

  test("도착 거래소 선택", async () => {
    await expect(page.getByText("도착 거래소")).toBeVisible();

    await page.evaluate(() => {
      const trigger = document.querySelector('[role="combobox"]');
      (trigger as HTMLElement)?.click();
    });
    await page.waitForTimeout(300);

    const options = page.locator('[role="option"]');
    await expect(options.first()).toBeVisible();

    await options.first().click();
    await page.waitForTimeout(300);
  });

  test("출금 수량 입력 및 가용 잔고 표시", async () => {
    await expect(page.getByText("출금 수량")).toBeVisible();
    await expect(page.getByText("가용:")).toBeVisible();
    await expect(page.locator("button", { hasText: "최대" })).toBeVisible();
    await expect(page.locator("button", { hasText: "출금하기" })).toBeVisible();
  });
});

// ── 12단계: 거래소 간 송금 실행 ──

test.describe("거래소 간 송금 실행", () => {
  test("보유 코인으로 출금 모달 열기", async () => {
    await page.goto(`${BASE_URL}/wallet`);
    await page.waitForLoadState("networkidle");

    // 잔고가 있는 코인 선택 (시장가 매수로 산 코인)
    await page.evaluate(() => {
      const items = document.querySelectorAll('[class*="cursor-pointer"]');
      // 잔고가 0이 아닌 첫 번째 코인 (원화 제외)
      const coinItem = Array.from(items).find((e) => {
        const text = e.textContent ?? "";
        return !text.includes("원화") && !text.includes("KRW") && /[0-9]/.test(text);
      });
      coinItem?.scrollIntoView();
      (coinItem as HTMLElement)?.click();
    });
    await page.waitForTimeout(500);

    // 출금 버튼 클릭
    await page.evaluate(() => {
      const buttons = document.querySelectorAll("button");
      const withdrawBtn = Array.from(buttons).find(
        (b) =>
          b.textContent?.includes("출금") && !b.textContent?.includes("입출금")
      );
      (withdrawBtn as HTMLElement)?.click();
    });
    await page.waitForTimeout(500);

    await expect(page.getByText("출금 수량")).toBeVisible();
  });

  test("도착 거래소 선택 및 수량 입력 후 출금 실행", async () => {
    // 도착 거래소 선택
    await page.evaluate(() => {
      const trigger = document.querySelector('[role="combobox"]');
      (trigger as HTMLElement)?.click();
    });
    await page.waitForTimeout(300);
    await page.locator('[role="option"]').first().click();
    await page.waitForTimeout(300);

    // 최대 버튼 클릭
    await page.locator("button", { hasText: "최대" }).click();
    await page.waitForTimeout(300);

    // 출금하기 클릭
    await page.locator("button", { hasText: "출금하기" }).click();
    await page.waitForTimeout(1500);
  });

  test("입출금 내역에 출금 기록 표시", async () => {
    // 모달이 닫히고 wallet 페이지로 돌아옴
    await expect(page.getByText("보유 자산")).toBeVisible();

    // 출금 내역 확인을 위해 코인 다시 선택
    await page.evaluate(() => {
      const items = document.querySelectorAll('[class*="cursor-pointer"]');
      const coinItem = Array.from(items).find((e) => {
        const text = e.textContent ?? "";
        return !text.includes("원화") && !text.includes("KRW");
      });
      coinItem?.scrollIntoView();
      (coinItem as HTMLElement)?.click();
    });
    await page.waitForTimeout(500);

    // 입출금 내역에서 출금 기록 확인
    await expect(page.getByText("입출금 내역").first()).toBeVisible();
  });
});

// ── 13단계: 입출금 내역 필터 ──

test.describe("입출금 내역 필터", () => {
  test("입출금 내역에서 입금/출금 필터 확인", async () => {
    await page.evaluate(() => {
      document
        .querySelectorAll('[data-slot="dialog-close"]')
        .forEach((b) => (b as HTMLElement).click());
    });
    await page.waitForTimeout(500);

    await page.goto(`${BASE_URL}/wallet`);
    await page.waitForLoadState("networkidle");

    await page.evaluate(() => {
      const items = document.querySelectorAll('[class*="cursor-pointer"]');
      const btcItem = Array.from(items).find(
        (e) => e.textContent?.includes("BTC") && e.textContent?.includes("비트코인")
      );
      btcItem?.scrollIntoView();
      (btcItem as HTMLElement)?.click();
    });
    await page.waitForTimeout(500);

    await expect(page.getByText("입출금 내역").first()).toBeVisible();

    await expect(page.getByRole("button", { name: "전체" }).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "입금" }).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "출금" }).first()).toBeVisible();

    await expect(page.getByRole("button", { name: "진행중" }).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "완료" }).first()).toBeVisible();
  });

  test("입금 필터 클릭", async () => {
    await page.getByRole("button", { name: "입금" }).last().click();
    await page.waitForTimeout(300);

    await expect(page.getByText("입출금 내역").first()).toBeVisible();
  });
});

// ── 13단계: 긴급 자금 충전 ──

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

// ── 14단계: 랭킹 조회 ──

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

// ── 15단계: 고수 포트폴리오 열람 ──

test.describe("고수 포트폴리오 열람", () => {
  test("랭킹 페이지 로드", async () => {
    await page.goto(`${BASE_URL}/ranking`);
    await expect(page.getByRole("heading", { name: "랭킹" })).toBeVisible();
  });

  test("랭킹 목록에 랭커 카드 표시", async () => {
    await expect(page.getByText("29회 거래").first()).toBeVisible();
  });

  test("공개 포트폴리오 랭커 클릭 시 보유 종목 표시", async () => {
    // 랭킹 더보기를 두 번 눌러 44등까지 로드 (1-20 → 21-40 → 41-60)
    for (let i = 0; i < 2; i++) {
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
      await page.waitForTimeout(500);
      await page.locator("button", { hasText: "랭킹 더보기" }).click();
      await page.waitForTimeout(1000);
    }

    // 메인 리스트에서 최리플(44등, 공개) 카드의 button 클릭
    await page.evaluate(() => {
      const buttons = document.querySelectorAll("button");
      const card = Array.from(buttons).find(
        (b) => b.textContent?.includes("최리플") && b.textContent?.includes("거래")
      );
      card?.scrollIntoView();
      card?.click();
    });
    await page.waitForTimeout(1000);

    // 포트폴리오 확장 후 보유 코인이 표시되는지 확인
    await expect(page.getByText("ETH").first()).toBeVisible();
  });
});

// ── 16단계: 투자 복기 ──

test.describe("투자 복기", () => {
  test("투자 복기 페이지 로드", async () => {
    await page.click('a[href="/regret"]');
    await expect(page.getByText("투자 복기", { exact: true }).first()).toBeVisible();
    await expect(page.getByText("규칙만 지켰으면 얼마를 벌었을까?")).toBeVisible();
  });

});

// ── 17단계: 프로필 설정 ──

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

// ── 18단계: 포트폴리오 공개/비공개 ──

test.describe("포트폴리오 공개/비공개", () => {
  test("마이페이지 로드", async () => {
    await page.goto(`${BASE_URL}/mypage`);
    await expect(page.getByText("마이페이지")).toBeVisible();
    await expect(page.getByText("프로필", { exact: true })).toBeVisible();
  });

  test("포트폴리오 공개 설정 확인", async () => {
    await expect(page.getByText("포트폴리오 공개")).toBeVisible();

    const toggleSwitch = page.locator('button[role="switch"]').first();
    await expect(toggleSwitch).toBeVisible();

    const currentState = await toggleSwitch.getAttribute("data-state");
    expect(currentState).toBeTruthy();
  });

  test("포트폴리오 공개 설정 토글", async () => {
    const toggleSwitch = page.locator('button[role="switch"]').first();
    const beforeState = await toggleSwitch.getAttribute("data-state");

    await toggleSwitch.click();
    await page.waitForTimeout(500);

    const afterState = await toggleSwitch.getAttribute("data-state");
    expect(afterState).not.toBe(beforeState);

    if (afterState === "checked") {
      await expect(page.getByText("공개").first()).toBeVisible();
    } else {
      await expect(page.getByText("비공개").first()).toBeVisible();
    }
  });

  test("토글 상태 복원", async () => {
    const toggleSwitch = page.locator('button[role="switch"]').first();

    await toggleSwitch.click();
    await page.waitForTimeout(500);

    await expect(page.getByText("포트폴리오 공개")).toBeVisible();
  });
});

// ── 19단계: 라운드 종료 ──

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

// ── 20단계: 라운드 시작 ──

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
