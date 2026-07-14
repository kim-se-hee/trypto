거래소 체결가 기반 OHLC 캔들을 InfluxDB에 시 지키는 계약.

# 개요

| 항목 | 값 |
|------|------|
| 저장소 | InfluxDB 2.x |
| 발행자 | `collector` — 거래소 WebSocket 체결가로 1분봉 write |
| 소비자 | `api` — `CandleQueryAdapter` |

# Measurement

| measurement | 주기 | 생성 방식 |
|-------------|------|----------|
| `candle_1m` | 1분 | 수집기가 직접 write |
| `candle_1h` | 1시간 | Continuous Query |
| `candle_4h` | 4시간 | Continuous Query |
| `candle_1d` | 1일 | Continuous Query |
| `candle_1w` | 1주 | Continuous Query |
| `candle_1M` | 1개월 | Continuous Query |

# 스키마

| 구분 | 이름 | 타입 | 설명 |
|------|------|------|------|
| tag | `exchange` | String | 거래소 식별자 (UPBIT, BITHUMB, BINANCE) |
| tag | `symbol` | String | 마켓 심볼, `BASE/QUOTE` 형식 (BTC/KRW, ETH/USDT 등) |
| field | `open` | Float | 시가 |
| field | `high` | Float | 고가 |
| field | `low` | Float | 저가 |
| field | `close` | Float | 종가 |
| timestamp | | Time | 해당 주기의 시작 시각 |
