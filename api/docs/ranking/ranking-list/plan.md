# 커서 기반 페이징 조회

랭킹 순위 데이터는 커서 기반 페이징으로 조회한다.

## API 명세

### 참고사항

- 이 API는 읽기 전용 조회이다. 랭킹 데이터는 배치가 미리 집계해 둔 결과를 조회한다
- `referenceDate`를 생략하면 최신 집계 결과를 반환한다
- 닉네임은 `User` 테이블에서 조인하여 반환한다. 클라이언트가 별도 API를 호출할 필요 없다
- 커서 기반 페이징을 사용한다. `nextCursor` 값을 다음 요청의 `cursorRank`로 전달하면 다음 페이지를 조회할 수 있다

`GET /api/rankings`

### Request Parameters (Query String)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| period | String | O | `DAILY` \| `WEEKLY` \| `MONTHLY` |
| referenceDate | String (yyyy-MM-dd) | X | 기준 날짜 (없으면 최신) |
| cursorRank | Integer | X | 이전 페이지 마지막 rank (첫 페이지는 생략) |
| size | int | X | 페이지 크기 (기본값 20, 최대 50) |

### Request

```
GET /api/rankings?period=DAILY&size=20
GET /api/rankings?period=DAILY&cursorRank=20&size=20
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "랭킹을 조회했습니다.",
  "data": {
    "content": [
      {
        "rank": 1,
        "userId": 42,
        "nickname": "코인마스터",
        "profitRate": 15.23,
        "tradeCount": 12
      },
      {
        "rank": 2,
        "userId": 17,
        "nickname": "홀드러",
        "profitRate": 12.87,
        "tradeCount": 5
      },
      {
        "rank": 3,
        "userId": 88,
        "nickname": "스윙트레이더",
        "profitRate": 12.87,
        "tradeCount": 8
      }
    ],
    "nextCursor": 3,
    "hasNext": false
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_RANKING_PERIOD | 400 | 유효하지 않은 기간 값 |
| RANKING_NOT_FOUND | 404 | 해당 기간의 랭킹 데이터가 없음 (배치 미실행) |
