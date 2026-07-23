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

# 진행 중 캔들 (소비자 즉석 집계)

집계 Task 는 구간이 닫힌 뒤에만 상위봉(1시간 이상)을 기록하므로, 진행 중인 현재 구간의 봉은 이 measurement 에 존재하지 않는다. `api` 는 최신 조회 시 현재 구간의 봉을 즉석 집계해 덧붙인다.

- 완성된 하위 구간은 저장된 상위봉(`candle_1d`·`candle_1h`·`candle_1m`)으로 채운다.
- 진행 중인 마지막 분은 `ticker_raw`(원본 틱, 필드 `price`)로 채운다.
- 집계 방식은 Task 와 동일하다: 시가 `first`, 고가 `max`, 저가 `min`, 종가 `last`.

즉 이 measurement 는 **닫힌 봉의 단일 소스**이고, 진행 중 봉은 소비자가 하위 measurement 와 `ticker_raw` 로 합성한다.
