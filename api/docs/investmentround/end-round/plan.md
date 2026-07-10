## API 명세

`POST /api/rounds/{roundId}/end`

### 참고사항

- 현행 패턴에 맞춰 `userId`를 요청 바디로 받는다.
- 재요청(이미 종료된 라운드)은 200 성공으로 처리하고 기존 종료 정보를 반환한다.

### Path Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roundId | Long | O | 라운드 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 사용자 ID |

### Request

```json
{
  "userId": 1
}
```

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "라운드를 종료했습니다.",
  "data": {
    "roundId": 1,
    "status": "ENDED",
    "endedAt": "2026-03-01T11:40:00"
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ROUND_NOT_FOUND | 404 | 라운드를 찾을 수 없음 |
| ROUND_ACCESS_DENIED | 403 | 본인 라운드가 아님 |
| ROUND_NOT_ACTIVE | 404 | 종료 대상 상태가 아님(`BANKRUPT`) |
| CONCURRENT_MODIFICATION | 409 | 동시 종료 요청 충돌(`@Version` 낙관적 잠금) |

> `ROUND_NOT_FOUND`, `ROUND_ACCESS_DENIED`를 `ErrorCode`와 `messages.properties`에 반영한다.
