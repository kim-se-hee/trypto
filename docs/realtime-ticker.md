# 개요

외부 시세 수집기가 업비트/빗썸/바이낸스 WebSocket에서 수신한 티커를 정규화하여 Redis에 적재하고, RabbitMQ fanout exchange로 발행한다.
우리 서버는 세 가지 방식으로 시세를 소비한다.

| 소비 방식 | 용도 | 메커니즘 |
|----------|------|---------|
| **캐시 조회** | 주문 체결, 포트폴리오 평가 등 서버 내부 로직 | Redis GET/MGET |
| **실시간 스트리밍** | 클라이언트에 시세 push | RabbitMQ fanout → STOMP WebSocket |
| **미체결 주문 매칭** | 지정가 주문 체결 | RabbitMQ fanout → 로컬 캐시 매칭 |

# Redis 내 티커 저장 구조

## 키 형식

```
ticker:{exchange}:{base}/{quote}
```

| 예시 | 거래소 | 마켓 |
|------|-------|------|
| `ticker:UPBIT:BTC/KRW` | 업비트 | BTC/KRW |
| `ticker:BITHUMB:ETH/KRW` | 빗썸 | ETH/KRW |
| `ticker:BINANCE:BTC/USDT` | 바이낸스 | BTC/USDT |

- `exchange`: 거래소 이름 (UPBIT, BITHUMB, BINANCE) — DB `EXCHANGE` 테이블의 `name` 값과 일치
- `base`: 거래 대상 코인 심볼
- `quote`: 기축통화 심볼

## 값: 정규화 티커 (NormalizedTicker JSON)

```json
{
  "exchange": "UPBIT",
  "base": "BTC",
  "quote": "KRW",
  "display_name": "비트코인",
  "last_price": 143250000.0,
  "change_rate": 0.0123,
  "quote_turnover": 892400000000.0,
  "ts_ms": 1709913600000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `exchange` | String | 거래소 식별자 |
| `base` | String | 거래 대상 코인 심볼 |
| `quote` | String | 기축통화 심볼 |
| `display_name` | String | 사용자 표시용 코인 이름 |
| `last_price` | Number | 최신 체결가 |
| `change_rate` | Number | 등락률 (비율). +1.23%면 `0.0123`, -4%면 `-0.04` |
| `quote_turnover` | Number | 24시간 누적 거래대금 (기축통화 단위) |
| `ts_ms` | Long | 수집기가 티커를 수신한 시각 (epoch ms) |

**`change_rate` 기준 차이:**
- 업비트/빗썸: 전일 종가 대비
- 바이낸스: 최근 24시간 대비

## 현재가 단순 조회

서버 내부 로직(주문, 포트폴리오 평가 등)에서 현재가가 필요할 때 Redis에서 직접 조회한다.

### 조회 흐름

```
Service
  → GetLivePriceUseCase / GetLivePricesUseCase (Input Port)
    → GetLivePriceService / GetLivePricesService
      → LivePriceQueryPort (Output Port)
        → LivePriceQueryAdapter
          → Redis GET/MGET → NormalizedTicker JSON에서 last_price 추출
```

### Redis 키 매핑

`LivePriceQueryAdapter`는 `exchangeCoinId`로부터 Redis 키를 조립한다.

```
exchangeCoinId
  → EXCHANGE_COIN 테이블 → exchangeId, coinId
  → EXCHANGE 테이블 → name(거래소명), baseCurrencyCoinId(기축통화)
  → COIN 테이블 → symbol(base), symbol(quote)
  → "ticker:{exchange}:{base}/{quote}"
```

매핑 결과는 `ConcurrentHashMap`에 캐싱되어 이후 DB 조회 없이 재사용된다.

### UseCase

| UseCase | 메서드 | 설명 |
|---------|-------|------|
| `GetLivePriceUseCase` | `getCurrentPrice(Long exchangeCoinId) → BigDecimal` | 단건 조회 (Redis GET) |
| `GetLivePricesUseCase` | `getCurrentPrices(Set<Long> exchangeCoinIds) → Map<Long, BigDecimal>` | 일괄 조회 (Redis MGET) |


## 실시간 스트리밍 — 클라이언트에 시세 push

시세 수집기가 RabbitMQ fanout exchange(`ticker.exchange`)로 발행한 `TickerMessage`를 marketdata 리스너가 소비하여 STOMP WebSocket으로 클라이언트에 전달한다. 상세는 [live-ticker-streaming.md](marketdata/live-ticker-streaming.md)를 참조한다.

### 메시지 흐름

```
시세 수집기 → RabbitMQ Fanout Exchange (ticker.exchange)
                  │
            ticker.marketdata.{uuid} 큐
                  │
            LiveTickerEventListener
                  │
            ResolveLiveTickerUseCase.resolve() → LiveTickerResult
                  │
            LivePriceResponse 변환 + SimpMessagingTemplate.convertAndSend()
                  │
            SimpleBroker → /topic/prices.{exchangeId}
                  │
            WebSocket 클라이언트
```

### STOMP 토픽

| STOMP 토픽 | 전파 수단 |
|-----------|----------|
| `/topic/prices.{exchangeId}` | RabbitMQ fanout |

클라이언트는 현재 보고 있는 거래소 토픽 1개만 구독하고, 거래소 탭 전환 시 기존 구독 해제 + 새 거래소 구독한다.

### 메시지 포맷 (LivePriceResponse)

```json
{
  "coinId": 1,
  "symbol": "BTC",
  "price": 143250000,
  "changeRate": 0.0234,
  "quoteTurnover": 892400000000,
  "timestamp": 1709913600000
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `coinId` | Long | 코인 ID |
| `symbol` | String | 코인 심볼 |
| `price` | BigDecimal | 현재가 (거래소 기축통화 단위) |
| `changeRate` | BigDecimal | 등락률 (비율) — 업비트/빗썸: 전일종가 대비, 바이낸스: 24h 대비. +1.23%면 0.0123 |
| `quoteTurnover` | BigDecimal | 24시간 누적 거래대금 (기축통화 단위) |
| `timestamp` | Long | 시세 수신 시각 (epoch ms) |
