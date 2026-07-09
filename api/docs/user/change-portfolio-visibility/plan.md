## API 명세

`PUT /api/users/{userId}/portfolio-visibility`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 유저 ID |

### Request Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| portfolioPublic | Boolean | O | `@NotNull` | 포트폴리오 공개 여부 |

### Request

```
PUT /api/users/1/portfolio-visibility
```

```json
{
  "portfolioPublic": false
}
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "포트폴리오 공개 설정이 변경되었습니다.",
  "data": {
    "userId": 1,
    "portfolioPublic": false
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| USER_NOT_FOUND | 404 | 존재하지 않는 사용자 |
