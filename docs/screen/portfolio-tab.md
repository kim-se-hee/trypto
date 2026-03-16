# 개요

포트폴리오 탭은 거래소별 보유 자산 현황을 보여준다. 정적 데이터(REST API, 캐싱)와 실시간 시세(WebSocket)를 프론트에서 조합하여 평가금액·수익률을 실시간 갱신한다.

# 화면 구성

| 영역 | 설명 |
|------|------|
| 거래소 탭 | 업비트 / 빗썸 / 바이낸스 — 탭 전환 시 활성 라운드의 walletId로 포트폴리오 API 호출 |
| 보유 기축통화 | "보유 KRW" 또는 "보유 USDT" — 총 잔고(available + frozen) 표시 |
| 자산 요약 카드 | 총 보유자산(보유 기축통화 + 코인 평가금액 합계), 총매수(전체 코인 매수금액 합계), 총평가(전체 코인 평가금액 합계), 평가손익(총평가 − 총매수), 수익률(평가손익 / 총매수 × 100) |
| 보유 코인 목록 | 코인명, 보유수량, 평균매수가, 현재가, 평가금액, 평가손익, 수익률 |
| 자산 구성 | 도넛 차트 — 코인별 평가금액 비중(%), 중앙에 총평가 표시 |


## 잔고 용어 정의

| 용어 | 정의 | 사용 화면            |
|------|------|------------------|
| 총 잔고 (balance) | available + frozen | 포트폴리오 탭 "보유 KRW" |
| 사용 가능 잔고 (available) | 즉시 주문에 사용할 수 있는 금액 | 주문 화면            |
| 동결 잔고 (frozen) | 미체결 지정가 주문 동결 + 동결 송금(FROZEN 상태 이체)에 묶인 금액 | 표시 x             |

- 포트폴리오 탭의 "보유 KRW"는 **총 잔고**다. 동결 금액도 사용자 자산이므로 포함한다.
- 주문 화면에서는 **사용 가능 잔고**를 별도로 표시하여 실제 주문 가능 금액을 보여준다.

# 데이터 소스

| 데이터 | 소스 | 갱신 주기 | 캐싱 |
|--------|------|----------|------|
| 거래소별 walletId | 활성 라운드 API (`wallets: [{walletId, exchangeId}]`) | 라운드 시작 시 1회 | O (라운드 시작 시 무효화) |
| 보유 코인 목록 (coinSymbol, coinName, quantity, avgBuyPrice, currentPrice) | 포트폴리오 API | 탭 진입 시 fresh fetch | X |
| 기축통화 잔고 (baseCurrencyBalance) | 포트폴리오 API | 탭 진입 시 fresh fetch | X |
| 현재가 실시간 갱신 | WebSocket 티커 | 실시간 | X |

## 캐싱 가능 데이터

| API | 용도 | staleTime |
|-----|------|-----------|
| `GET /api/rounds/active` | 거래소별 walletId 조회 | 라운드 시작 시 무효화 |

## 캐싱 불가 데이터

| API | 이유 |
|-----|------|
| `GET /api/users/{userId}/wallets/{walletId}/portfolio` | 주문 체결·송금으로 보유수량/잔고 수시 변동 |

# API 의존

| API | 문서 | 용도 |
|-----|------|------|
| `GET /api/rounds/active?userId=` | [active-round.md](../investmentround/active-round.md) | 거래소별 walletId 획득 |
| `GET /api/users/{userId}/wallets/{walletId}/portfolio` | [my-holdings.md](../portfolio/my-holdings.md) | 보유 코인(coinSymbol, coinName 포함) + 기축통화 잔고 |
| WebSocket `/topic/tickers.{exchangeId}` | [live-ticker-streaming.md](../marketdata/live-ticker-streaming.md) | 실시간 시세 |

# 프론트 조합 흐름

```
1. 앱 초기화 (최초 1회, 캐싱)
   → GET /api/rounds/active → wallets: [{walletId, exchangeId}, ...]

2. 포트폴리오 탭 진입
   → GET /api/users/{userId}/wallets/{walletId}/portfolio → 보유 코인 + 기축통화 잔고
   → SUBSCRIBE /topic/tickers.{exchangeId} → 실시간 시세

3. 프론트 state
   - wallets: [{walletId, exchangeId}, ...]            ← 활성 라운드 API (캐싱)
   - holdings: [{coinId, coinSymbol, coinName, quantity, avgBuyPrice, currentPrice}, ...]  ← 포트폴리오 API (fresh)
   - baseCurrency: {symbol, balance}                 ← 포트폴리오 API (fresh)
   - tickerMap: Map<coinId, {price, ...}>            ← WebSocket

4. 렌더링 (초기 + 실시간 공통)
   초기에는 holdings의 currentPrice로, 실시간에는 tickerMap 수신 시 해당 coinId의 currentPrice를 덮어쓰고 아래 전체를 재계산한다.

   코인별 계산:
   - 평가금액 = quantity × currentPrice
   - 평가손익 = (currentPrice - avgBuyPrice) × quantity
   - 수익률 = (currentPrice - avgBuyPrice) / avgBuyPrice × 100

   자산 요약 카드:
   - 총매수 = Σ(quantity × avgBuyPrice)
   - 총평가 = Σ(quantity × currentPrice)
   - 평가손익 = 총평가 − 총매수
   - 수익률 = 평가손익 / 총매수 × 100
   - 총 보유자산 = baseCurrencyBalance + 총평가

   자산 구성 (도넛 차트):
   - 코인별 비중 = 코인별 평가금액 / 총평가 × 100
   - 중앙에 총평가 표시

5. 거래소 탭 전환
   → 기존 WebSocket 구독 해제
   → wallets에서 해당 거래소의 walletId 조회
   → GET /api/users/{userId}/wallets/{walletId}/portfolio (fresh fetch)
   → 새 거래소 토픽 구독
```

# WebSocket 의존

## 실시간 시세 (브로드캐스트)

STOMP 토픽: `/topic/tickers.{exchangeId}`

- 시세 변동 시 해당 coinId의 currentPrice를 덮어쓰고 코인별 계산 + 자산 요약 카드 + 자산 구성 비중을 재계산한다
- 마켓탭과 동일한 토픽을 공유한다

## 사용자 이벤트 (개인 채널)

STOMP user destination: `/user/queue/events`

- 서버에서 사용자별 비동기 이벤트를 푸시한다
- 포트폴리오 탭에서 수신해야 하는 이벤트:

| eventType | 발생 시점 | 프론트 동작 |
|-----------|----------|-----------|
| `ORDER_FILLED` | 지정가 주문 체결  | 해당 holding만 로컬 갱신 + 자산요약 재계산 (refetch 없음) |

- 메시지 형식: `{eventType, walletId, orderId, coinId, side, quantity, price, fee}`
- 현재 walletId와 일치할 때만 로컬 갱신 트리거
- 매수 체결: holding의 qty 증가 + avgBuyPrice 재계산 + baseCurrencyBalance 차감 (price × qty + fee)
- 매도 체결: holding의 qty 감소 (전량 매도 시 목록에서 제거) + baseCurrencyBalance 증가 (price × qty - fee)

## 송금 후 갱신 (동기)

송금은 사용자가 직접 트리거하는 동기 API 호출이므로 WebSocket 이벤트 없이 API 응답으로 갱신한다.

- 송금 API 응답의 `status`, `fee`를 확인한다
- SUCCESS: `baseCurrencyBalance -= amount + fee` (프론트가 요청 시 보낸 amount + 응답의 fee로 계산)
- FROZEN: `baseCurrencyBalance`는 변경 없음 (동결은 총 잔고에 포함)
- 목적지 거래소 잔고는 탭 전환 시 fresh fetch로 반영된다

# 현재가 실시간 갱신 전략

포트폴리오 API가 반환하는 `currentPrice`는 조회 시점의 스냅샷이다. 이후 시세 변동을 반영하기 위해 WebSocket 티커를 구독한다.

- 포트폴리오 API의 `currentPrice`: 초기 렌더링에 사용
- WebSocket 티커의 `price`: 수신 시 해당 coinId의 currentPrice를 덮어씀
- 평가금액·수익률은 프론트에서 재계산

이 방식으로 포트폴리오 API를 반복 호출하지 않고도 실시간 평가금액 갱신이 가능하다.

# 미결 사항

| 항목 | 상태 | 비고 |
|------|------|------|
| 주문 체결 후 자동 갱신 | 설계 완료 | `/user/queue/events`의 `ORDER_FILLED` 이벤트로 해당 holding만 로컬 갱신 (refetch 없음) |
| coinSymbol/coinName 제공 방식 | 미결정 | **현재**: 포트폴리오 API가 서버에서 FindCoinInfoUseCase를 호출하여 응답에 포함 (자기완결적 API). **대안**: API는 coinId만 반환하고, 프론트가 거래소 코인 목록 캐시(`coinMap`)로 매핑 (크로스 컨텍스트 의존 1개 제거). 성능 차이는 무의미하고, API 자기완결성 vs 크로스 컨텍스트 커플링 감소의 트레이드오프 |
