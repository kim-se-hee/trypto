## API 명세

### 참고사항

- 거래소 탭 전환은 클라이언트가 walletId를 바꿔서 호출한다 (walletId는 라운드 응답에 포함)
- 거래소 상장 코인 목록은 별도 API(`GET /api/exchanges/{exchangeId}/coins`)로 캐싱한다
- 현재가는 WebSocket `/topic/tickers.{exchangeId}`로 수신한다
- 프론트엔드가 코인 목록 + 잔고 + 현재가를 coinId로 조합하여 렌더링한다
- 정렬은 서버가 하지 않고 클라이언트가 처리한다

### REST API

`GET /api/users/{userId}/wallets/{walletId}/balances`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 사용자 ID |
| walletId | Long | O | 조회할 지갑 ID |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "잔고를 조회했습니다.",
  "data": {
    "exchangeId": 1,
    "baseCurrencySymbol": "KRW",
    "baseCurrencyAvailable": 2450000,
    "baseCurrencyLocked": 150000,
    "balances": [
      {
        "coinId": 1,
        "available": 0.052341,
        "locked": 0.001
      },
      {
        "coinId": 2,
        "available": 1.245,
        "locked": 0
      }
    ]
  }
}
```

### 필드 설명

**data**

| 필드 | 타입 | 설명 |
|------|------|------|
| exchangeId | Long | 거래소 ID |
| baseCurrencySymbol | String | 기축통화 심볼 (KRW, USDT) |
| baseCurrencyAvailable | BigDecimal | 기축통화 사용 가능 잔고 |
| baseCurrencyLocked | BigDecimal | 기축통화 잠금 잔고 |

**balances[]**

| 필드 | 타입 | 설명 |
|------|------|------|
| coinId | Long | 코인 ID (거래소 코인 목록의 coinId와 매칭) |
| available | BigDecimal | 사용 가능 잔고 |
| locked | BigDecimal | 잠금 잔고 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 지갑을 찾을 수 없음 |
| WALLET_NOT_OWNED | 403 | 지갑 소유자가 아님 |
