/**
 * 시드 금액의 통화별 허용 범위와 검증.
 *
 * 컴포넌트 파일에 두면 Vite 가 그 파일의 Fast Refresh 를 포기한다(한 파일이 컴포넌트와
 * 비컴포넌트를 함께 내보내면 부분 갱신을 못 한다). 화면과 무관한 규칙이므로 따로 둔다.
 */
const SEED_LIMITS: Record<string, { min: number; max: number }> = {
  KRW: { min: 1_000_000, max: 50_000_000 },
  USDT: { min: 1_000, max: 30_000 },
};

// 0원은 "이 거래소엔 시드를 넣지 않음"이라 유효하다. 넣는 경우에만 통화별 한도를 검사한다.
export function seedAmountError(baseCurrency: string, amount: number): string | null {
  if (amount === 0) return null;
  const limit = SEED_LIMITS[baseCurrency];
  if (amount >= limit.min && amount <= limit.max) return null;
  return baseCurrency === "USDT"
    ? "1,000 ~ 30,000 USDT 범위로 입력해주세요"
    : "100만원 ~ 5,000만원 범위로 입력해주세요";
}
