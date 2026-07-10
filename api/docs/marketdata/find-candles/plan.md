## InfluxDB 조회 구조

measurement는 주기별로 분리되어 있다 (`candle_1m`, `candle_1h`, `candle_4h`, `candle_1d`, `candle_1w`, `candle_1M`).
interval 파라미터를 measurement 이름으로 매핑하여 쿼리한다.

| interval | measurement |
|----------|-------------|
| `1m` | `candle_1m` |
| `1h` | `candle_1h` |
| `4h` | `candle_4h` |
| `1d` | `candle_1d` |
| `1w` | `candle_1w` |
| `1M` | `candle_1M` |


## 커시 기반 페이징 적용

차트를 과거 방향으로 넘기면 과거 캔들을 조회해 이어 붙여야 한다. 이때 캔들을 오프셋 대신 커서 기반으로 페이징 조회한다.

## API 명세

`GET /api/candles?exchange={exchange}&coin={coin}&interval={interval}&limit={limit}&cursor={cursor}`

### Query Parameters

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| exchange | String | O | | 거래소 코드 (`UPBIT`, `BITHUMB`, `BINANCE`) |
| coin | String | O | | 코인 심볼 (`BTC`, `ETH` 등) |
| interval | String | O | | 캔들 주기 (`1m`, `1h`, `4h`, `1d`, `1w`, `1M`) |
| limit | Integer | X | 60 | 조회할 캔들 개수 (1~200) |
| cursor | String | X | | ISO 8601 타임스탬프. 이 시각 이전의 캔들을 조회한다 (과거 스크롤용) |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "캔들 데이터를 조회했습니다.",
  "data": [
    {
      "time": "2026-03-10T00:00:00Z",
      "open": 68500000.0,
      "high": 69200000.0,
      "low": 67800000.0,
      "close": 68900000.0
    },
    {
      "time": "2026-03-11T00:00:00Z",
      "open": 68900000.0,
      "high": 70100000.0,
      "low": 68400000.0,
      "close": 69750000.0
    }
  ]
}
```

### 응답 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| time | String | 캔들 시작 시각 (ISO 8601) |
| open | Double | 시가 |
| high | Double | 고가 |
| low | Double | 저가 |
| close | Double | 종가 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_CANDLE_INTERVAL | 400 | 지원하지 않는 캔들 주기 |
| INVALID_CANDLE_LIMIT | 400 | limit 범위 초과 (1~200) |
