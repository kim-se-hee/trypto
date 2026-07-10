## 설계 포인트

- 인증 불필요 (공개 통계)
- 랭킹은 상위 일부가 아니라 해당 기간 참여자 전원을 적재한다. 현재 트래픽이 많지 않아 전량을 적재해 배치로 집계해도 부담이 없으며, 통계값(참여자 수·최고/평균 수익률)은 DB 집계(COUNT/MAX/AVG)로 산출한다
- `referenceDate`는 내부적으로 최신 날짜를 자동 결정한다

## API 명세

`GET /api/rankings/stats`

### Request Parameters (Query String)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| period | String | O | `DAILY` \| `WEEKLY` \| `MONTHLY` |

### Request

```
GET /api/rankings/stats?period=DAILY
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "랭킹 통계를 조회했습니다.",
  "data": {
    "totalParticipants": 100,
    "maxProfitRate": 45.52,
    "avgProfitRate": 7.31
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_RANKING_PERIOD | 400 | 유효하지 않은 기간 값 |
| RANKING_NOT_FOUND | 404 | 해당 기간의 랭킹 데이터가 없음 |
