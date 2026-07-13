## API 명세

### 참고사항

- 랭킹 화면에서 특정 유저를 클릭하여 진입하는 흐름이다

`GET /api/rankings/{userId}/portfolio`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 열람 대상 유저 ID |

### Request Parameters (Query String)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| period | String | O | `DAILY` \| `WEEKLY` \| `MONTHLY` |

### Request

```
GET /api/rankings/42/portfolio?period=DAILY
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "포트폴리오를 조회했습니다.",
  "data": {
    "userId": 42,
    "nickname": "코인마스터",
    "rank": 1,
    "profitRate": 15.23,
    "holdings": [
      {
        "coinSymbol": "BTC",
        "exchangeName": "업비트",
        "assetRatio": 45.2,
        "profitRate": 12.5
      },
      {
        "coinSymbol": "ETH",
        "exchangeName": "바이낸스",
        "assetRatio": 30.1,
        "profitRate": 8.3
      },
      {
        "coinSymbol": "KRW",
        "exchangeName": "업비트",
        "assetRatio": 24.7,
        "profitRate": 0.0
      }
    ]
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_RANKING_PERIOD | 400 | 유효하지 않은 기간 값 |
| USER_NOT_FOUND | 404 | 유저를 찾을 수 없음 |
| PORTFOLIO_VIEW_NOT_ALLOWED | 403 | 100위 이내가 아닌 유저의 포트폴리오 열람 시도 |
| ROUND_NOT_ACTIVE | 404 | 진행 중인 라운드가 없음 |
