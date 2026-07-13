## 처리 로직

1. Request Parameter에서 `userId`를 받는다
2. `UserQueryPort.findById(userId)`로 사용자를 조회한다
3. 사용자가 없으면 `USER_NOT_FOUND` 에러를 반환한다
4. 사용자 프로필 정보를 반환한다

### 설계 포인트

- 인증 미구현 상태이므로 `userId`를 Request Parameter로 받는다 (인증 구현 후 SecurityContext로 전환)
- user 테이블 단독 조회로 완결되며 다른 컨텍스트 의존이 없다

## API 명세

`GET /api/users/{userId}`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 유저 ID |

### Request

```
GET /api/users/1
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "사용자 프로필을 조회했습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "포지션마스터",
    "createdAt": "2026-02-27T14:30:00"
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| USER_NOT_FOUND | 404 | 존재하지 않는 사용자 |
