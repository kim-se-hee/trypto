/**
 * 통합 숫자 포맷 유틸리티.
 * 모든 페이지에서 이 파일의 함수만 사용한다.
 */

// ── KRW 약어 (억/만원, 단위 포함) ────────────────────────────

export function formatKRW(value: number): string {
  const abs = Math.abs(value);
  const sign = value < 0 ? "-" : "";
  if (abs >= 1_0000_0000) {
    const eok = Math.floor(abs / 1_0000_0000);
    const man = Math.round((abs % 1_0000_0000) / 1_0000);
    if (man > 0) return `${sign}${eok}억 ${man.toLocaleString("ko-KR")}만원`;
    return `${sign}${eok}억원`;
  }
  if (abs >= 1_0000) return `${sign}${Math.round(abs / 1_0000).toLocaleString("ko-KR")}만원`;
  return `${sign}${Math.round(abs).toLocaleString("ko-KR")}원`;
}

// ── KRW 약어 (억/만, 단위 없음 — 테이블용) ──────────────────

export function formatKRWCompact(value: number): string {
  const abs = Math.abs(value);
  const sign = value < 0 ? "-" : "";
  if (abs >= 1_0000_0000) {
    const eok = Math.floor(abs / 1_0000_0000);
    const man = Math.round((abs % 1_0000_0000) / 1_0000);
    if (man > 0) return `${sign}${eok}억 ${man.toLocaleString("ko-KR")}만`;
    return `${sign}${eok}억`;
  }
  if (abs >= 1_0000) return `${sign}${Math.round(abs / 1_0000).toLocaleString("ko-KR")}만`;
  return value.toLocaleString("ko-KR");
}

// ── 통화별 금액 포맷 (단위 포함, 카드/요약용) ─────────────────

export function formatCurrency(value: number, baseCurrency: string): string {
  if (baseCurrency === "USDT") {
    return `$${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  return formatKRW(value);
}

// ── 통화별 금액 포맷 (단위 없음, 테이블용) ────────────────────

export function formatCurrencyCompact(value: number, baseCurrency: string): string {
  if (baseCurrency === "USDT") {
    return `$${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  return formatKRWCompact(value);
}

// ── 법정통화 환산 표시 (≈ 접두사 포함) ──────────────────────

export function formatFiatEstimate(value: number, baseCurrency: string): string {
  return `≈ ${formatCurrency(value, baseCurrency)}`;
}

// ── 코인 수량 포맷 ──────────────────────────────────────────

export function formatQuantity(quantity: number): string {
  if (quantity >= 1_000_000) return quantity.toLocaleString("en-US", { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  if (quantity >= 1_000) return quantity.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  if (quantity >= 1) return quantity.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
  return quantity.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 8 });
}

// ── 가격 포맷 (통화 기호 포함, 정밀도 자동) ──────────────────

export function formatPrice(price: number, baseCurrency: string): string {
  if (baseCurrency === "SOL") {
    if (price >= 1) return price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
    if (price >= 0.0001) return price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 8 });
    return price.toExponential(2);
  }
  if (baseCurrency === "USDT") {
    if (price >= 100) return price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (price >= 1) return price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    return price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
  }
  return price.toLocaleString("ko-KR");
}

// ── 거래대금 포맷 ──────────────────────────────────────────

export function formatVolume(volume: number, baseCurrency: string): string {
  if (baseCurrency === "SOL") {
    return `◎${volume.toLocaleString("en-US")}`;
  }
  if (baseCurrency === "USDT") {
    return `$${volume.toLocaleString("en-US")}`;
  }
  return volume.toLocaleString("ko-KR");
}

// ── 변동률 포맷 ──────────────────────────────────────────

export function formatChangeRate(rate: number): string {
  const percent = rate * 100;
  const sign = percent > 0 ? "+" : "";
  return `${sign}${percent.toFixed(2)}%`;
}

// ── 통화 기호 ──────────────────────────────────────────

export function getCurrencySymbol(baseCurrency: string): string {
  if (baseCurrency === "KRW") return "₩";
  if (baseCurrency === "SOL") return "◎";
  return "";
}

// ── 소액 필터 기준값 ──────────────────────────────────────

export const SMALL_AMOUNT_THRESHOLD: Record<string, number> = {
  KRW: 1_000,
  USDT: 1,
};
