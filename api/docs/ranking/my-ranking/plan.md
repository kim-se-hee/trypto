## API 명세

`GET /api/rankings/me`

### Request Parameters (Query String)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 유저 ID (인증 구현 전 임시) |
| period | String | O | `DAILY` \| `WEEKLY` \| `MONTHLY` |

### Request

```
GET /api/rankings/me?userId=1&period=DAILY
```

### Response (랭킹 참여 중)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "내 랭킹을 조회했습니다.",
  "data": {
    "rank": 26,
    "nickname": "포지션마스터",
    "profitRate": 27.95,
    "tradeCount": 77
  }
}
```

### Response (랭킹 미참여)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "내 랭킹을 조회했습니다.",
  "data": null
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_RANKING_PERIOD | 400 | 유효하지 않은 기간 값 |
