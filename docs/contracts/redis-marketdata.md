collector가 Redis에 적재하고 api가 읽는 마켓 데이터 계약. 정규화 티커(시세)와 마켓 메타데이터 두 종류를 담는다.

# 개요

| 항목 | 값 |
|------|------|
| 종류 | Redis |
| 발행자 | `collector` — 외부 거래소 WebSocket/REST 수신 후 적재 |
| 소비자 | `api` — `LivePriceQueryAdapter` (시세), 기동 시 `coin`/`exchange_coin` 시드 (메타데이터) |
| Content-Type | JSON 문자열 |

# 정규화 티커

## 키

```
ticker:{exchange}:{base}/{quote}
```

| 예시 | 거래소 | 마켓 |
|------|-------|------|
| `ticker:UPBIT:BTC/KRW` | 업비트 | BTC/KRW |
| `ticker:BITHUMB:ETH/KRW` | 빗썸 | ETH/KRW |
| `ticker:BINANCE:BTC/USDT` | 바이낸스 | BTC/USDT |

| 토큰 | 약속 |
|------|------|
| `exchange` | `UPBIT` / `BITHUMB` / `BINANCE` (DB `EXCHANGE` 테이블 `name` 과 일치) |
| `base` | 거래 대상 코인 심볼 |
| `quote` | 기축통화 심볼 |

## 값

NormalizedTicker JSON.

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
| `last_price` | Number | 최신 체결가 (quote 단위) |
| `change_rate` | Number | 등락률(비율). +1.23%면 `0.0123`, -4%면 `-0.04` |
| `quote_turnover` | Number | 24시간 누적 거래대금 (quote 단위) |
| `ts_ms` | Long | 수집기가 티커를 수신한 시각 (epoch ms) |

**`change_rate` 기준 차이:**
- 업비트/빗썸: 전일 종가 대비
- 바이낸스: 최근 24시간 대비

# 마켓 메타데이터

거래소별 거래 가능 마켓 목록. collector 기동 시 거래소 REST로 조회하여 적재하고, api 기동 시 읽어 `coin` / `exchange_coin` 테이블에 시드한다.

## 키

```
market-meta:{exchange}
```

| 예시 | 거래소 |
|------|-------|
| `market-meta:UPBIT` | 업비트 |
| `market-meta:BITHUMB` | 빗썸 |
| `market-meta:BINANCE` | 바이낸스 |

| 토큰 | 약속 |
|------|------|
| `exchange` | `UPBIT` / `BITHUMB` / `BINANCE` |

TTL 없음. collector 재기동 시 덮어쓰기.

## 값

MarketInfo JSON 배열.

```json
[
  {"base": "BTC", "quote": "KRW", "pair": "BTC/KRW", "displayName": "비트코인"},
  {"base": "ETH", "quote": "KRW", "pair": "ETH/KRW", "displayName": "이더리움"}
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `base` | String | 거래 대상 코인 심볼 |
| `quote` | String | 기축통화 심볼 |
| `pair` | String | `{base}/{quote}` 조합 |
| `displayName` | String | 사용자 표시용 코인 이름 |
