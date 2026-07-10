## API 명세

`PUT /api/users/{userId}/nickname`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 유저 ID |

### Request Body

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| nickname | String | O | `@NotBlank` | 새 닉네임 |

### Request

```
PUT /api/users/1/nickname
```

```json
{
  "nickname": "새닉네임"
}
```

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "닉네임이 변경되었습니다.",
  "data": {
    "userId": 1,
    "nickname": "새닉네임"
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| USER_NOT_FOUND | 404 | 존재하지 않는 사용자 |
| NICKNAME_SAME_AS_CURRENT | 400 | 현재 닉네임과 동일 |
| INVALID_NICKNAME_LENGTH | 400 | 닉네임 길이 2~20자 위반 |
| NICKNAME_ALREADY_EXISTS | 409 | 이미 사용 중인 닉네임 |
