# 개요

로그인 유저의 프로필 정보를 조회한다. 마이페이지 진입 시 사용자 기본 정보를 표시하기 위해 사용한다.

# 처리 로직

1. Request Parameter에서 `userId`를 받는다
2. `UserQueryPort.findById(userId)`로 사용자를 조회한다
3. 사용자가 없으면 `USER_NOT_FOUND` 에러를 반환한다
4. 사용자 프로필 정보를 반환한다

## 설계 포인트

- 인증 미구현 상태이므로 `userId`를 Request Parameter로 받는다 (인증 구현 후 SecurityContext로 전환)
- user 테이블 단독 조회로 완결되며 다른 컨텍스트 의존이 없다

# API 명세

`GET /api/users/{userId}`

## Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 유저 ID |

## Request

```
GET /api/users/1
```

## Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "사용자 프로필을 조회했습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "포지션마스터",
    "portfolioPublic": true,
    "createdAt": "2026-02-27T14:30:00"
  }
}
```

## 에러 응답

| code | status | 설명 |
|------|--------|------|
| USER_NOT_FOUND | 404 | 존재하지 않는 사용자 |

# 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant Controller as UserController
    participant Service as GetUserProfileService
    participant UserAdapter as UserJpaPersistenceAdapter
    participant MySQL

    Client->>Controller: GET /api/users/{userId}
    Controller->>Service: getUserProfile(query)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 01 사용자 조회
    end
    Service->>UserAdapter: findById(userId)
    UserAdapter->>MySQL: SELECT * FROM user WHERE user_id = ?
    UserAdapter-->>Service: Optional<User>

    alt 사용자 존재
        Service-->>Controller: User
        Controller-->>Client: 200 OK (data: { userId, email, nickname, ... })
    else 사용자 없음
        Service-->>Controller: USER_NOT_FOUND
        Controller-->>Client: 404 Not Found
    end
```
