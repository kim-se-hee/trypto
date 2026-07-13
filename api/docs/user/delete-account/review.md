# 리뷰 사항

## 1차 차단 이슈

- [x] **[api/src/main/java/ksh/tryptobackend/user/application/service/DeleteAccountService.java:35-47] 탈퇴 협력 로직이 응용 서비스에 절차적으로 흩어져 있음** (출처: ddd)
  - **설명:** 탈퇴는 `User` 의 상태 전이(탈퇴 시각·익명 닉네임 교체)와 `SocialAccount` 의 연결 해제가 한 덩어리로 성립해야 하는 하나의 도메인 개념이다. 현재는 두 애그리거트의 협력 순서가 응용 서비스의 호출 순서에만 암묵적으로 존재하여, "탈퇴하면 소셜 연결도 반드시 끊긴다" 는 규칙이 도메인 어디에도 표현되지 않는다. 익명 닉네임 부여 시점도 응용 서비스가 결정하므로 `User.withdraw()` 는 임의의 닉네임을 받는 형태이며, `SocialAccount.disconnect()` 도 탈퇴 맥락과 무관하게 단독 호출 가능한 경로로 열려 있다. (ddd-guideline §3, §8 위반)
  - **수정 제안:** `user/domain/service` 에 상태 없는 협력형 도메인 서비스(예: `AccountWithdrawalService`)를 두고 두 애그리거트 인스턴스를 인자로 받아 `user.withdraw(...)` 와 `socialAccount.disconnect()` 를 그 안에서 수행한다. 응용 서비스는 조회 → 도메인 서비스 호출 → 저장 → 세션 종료만 담당한다.

- [x] **[api/src/main/java/ksh/tryptobackend/user/adapter/out/persistence/RedisSessionCommandAdapter.java:41-48] `deleteAllOf` 의 조회-후-삭제 비원자성으로 탈퇴 후에도 유효한 세션이 남을 수 있음** (출처: 동시성)
  - **설명:** `deleteAllOf` 는 "세트 멤버 조회 → 개별 세션 키 삭제 → 세트 키 삭제" 를 서로 다른 Redis 명령으로 실행하며, 그 사이에 `create`(로그인) 가 끼어드는 것을 막는 장치가 없다. 탈퇴 처리 중 다른 기기에서 새 세션 `S2` 가 생성되어 `SMEMBERS` 시점 이후에 세트에 추가되면, `session:S2` 키는 삭제되지 않은 채 세트 키만 통째로 지워져 추적 정보가 사라진다. 인증 경로(`RedisSessionReader`)는 DB 의 `deleted_at` 을 확인하지 않고 Redis 세션 키 존재 여부만 보며 조회할 때마다 TTL 을 갱신하므로, 이 세션은 만료되지 않고 탈퇴한 계정으로 계속 인증된다. `create` 자체도 `SET` 과 `SADD` 가 원자적이지 않아 같은 원인의 누락이 발생할 수 있다.
  - **수정 제안:** `deleteAllOf` 를 Lua 스크립트(`EVAL`)로 작성해 멤버 조회·세션 키 삭제·세트 키 삭제를 단일 원자 연산으로 처리한다. `create` 의 `SET` + `SADD` + `EXPIRE` 도 함께 원자화하면 두 키의 정합성이 코드로 보장된다.

- [x] **[api/src/main/java/ksh/tryptobackend/common/web/auth/RedisSessionReader.java:14,27] 세션 세트(`user-sessions:`) 의 키 규칙과 TTL 갱신 책임이 두 클래스에 중복됨** (출처: oop)
  - **설명:** `user-sessions:{userId}` 세트의 생성·삭제·일괄 삭제는 `RedisSessionCommandAdapter` 가 책임지는데, 다른 패키지의 `RedisSessionReader` 가 동일한 키 접두사를 문자열 리터럴로 복제해 TTL 을 직접 갱신한다. 한쪽만 키 규칙을 바꾸면 다른 쪽이 조용히 깨지며, 그 결과 `deleteAllOf` 가 아무것도 지우지 못해 탈퇴 회원의 세션이 살아남는다. 이 세트는 "탈퇴 시 모든 세션 즉시 종료" 요구사항을 지탱하는 핵심 자료구조이므로 소유권이 한 곳에 있어야 한다.
  - **수정 제안:** 키 조립과 TTL 갱신 책임을 `SessionCommandPort`/`RedisSessionCommandAdapter` 한 곳으로 모으고(예: `refreshTtl(userId, sessionId)` 추가), `RedisSessionReader` 는 포트를 통해 위임한다.

- [x] **[api/src/main/java/ksh/tryptobackend/user/adapter/out/SocialAccountQueryAdapter.java:21-26] 소셜 계정 조회 실패에 `USER_NOT_FOUND` 를 사용** (출처: 컨벤션, ddd, oop)
  - **설명:** 조회 대상은 `SocialAccount` 인데 존재하지 않을 때 `User` 애그리거트 전용 에러인 `USER_NOT_FOUND` 를 던진다. 같은 컨텍스트의 `SocialAccountCommandAdapter` 는 동일한 상황에 `SOCIAL_LOGIN_FAILED` 를 사용하고 있어 일관성도 없다. 실패 원인이 응답과 로그에서 왜곡되어 추적이 어렵다.
  - **수정 제안:** `SOCIAL_ACCOUNT_NOT_FOUND` 전용 에러 코드를 추가하거나, 기존 관례에 맞춰 통일한다.

- [x] **[api/src/main/java/ksh/tryptobackend/user/domain/model/User.java:17-24] 필드 나열 순서 위반 (final 필드가 가변 필드 사이에 끼임)** (출처: 컨벤션)
  - **설명:** api/docs/conventions.md 는 "상수 → final 필드 → 가변 필드" 순서를 규정한다. 이번 변경으로 `deletedAt`·`updatedAt` 이 가변 필드가 되면서 여전히 final 인 `createdAt` 이 가변 필드 사이에 끼었다.
  - **수정 제안:** `createdAt` 을 다른 final 필드(`userId`, `version`, `socialAccountId`) 뒤로 옮겨 final 필드를 앞에 모은다.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [DeleteAccountService.java:46] `@Transactional` 범위 안에서 Redis 세션 삭제를 수행 — DB 커밋 전 실행되는 비트랜잭션 부수 효과이며 커넥션 점유 시간도 늘어난다. `afterCommit` 훅으로 옮기는 방안 (출처: 동시성, 성능, oop)
- [RedisSessionReader.java:27] 인증 핫패스에 `EXPIRE` 왕복 1회 추가 — 파이프라인 또는 Lua 로 묶어 왕복을 줄이는 방안 (출처: 성능)
- [SocialAccount.java:40-42] `disconnect()` 에 불변식 검증이 없어 이미 해제된 상태에서도 조용히 통과 — `User.withdraw()` 와 방어 강도가 비대칭 (출처: ddd)
- [SessionCommandPort.java:8] `deleteAllOf` 네이밍이 모호 — `deleteAllByUserId` 등이 더 명확 (출처: oop)
- [DeleteAccountStepDefinition.java:16-22] 두 스텝 정의 본문이 동일하며 세션 유무가 클라이언트 내부 상태로 결정됨 (출처: oop)
- [spec.md] "탈퇴 시 미체결 주문 모두 취소" — 리뷰 이후 **요구사항 자체를 철회**했다. 도메인 이벤트 기반 취소를 구현했으나, 라운드 종료 시에도 미체결 주문을 취소하지 않는 기존 관례와 어긋나고 확장 범위 대비 이득이 적어 되돌렸다. 근거는 spec.md 의 "미체결 주문 — 취소하지 않는다" 절에 있다 (출처: ddd, oop)

## 2차 차단 이슈

없음. 리뷰 범위 `7cec87ae..HEAD` (1차 차단 이슈 반영분) 에 대해 ddd·oop·동시성·성능·컨벤션 리뷰어 전원이 차단 0건으로 승인했다.

## 2차 참고 이슈 (수정 안 함, 보고용)

- [AccountClosureService.java:15-18] 협력 불변식 검증 부재 — 넘겨받은 `SocialAccount` 가 해당 `User` 와 실제로 연결된 계정인지 확인하지 않는다. `SocialAccount.isConnectedTo(userId)` 로 검증하면 도메인 서비스가 협력 불변식의 수문장이 된다 (출처: ddd)
- [RedisSessionCommandAdapter.java:16,66-75] `Command` 어댑터가 `SessionReader` 조회 책임까지 겸한다 — 키 소유권 일원화를 위한 의도적 선택이나, 클래스명을 `RedisSessionAdapter` 로 바꾸거나 키·TTL 전용 컴포넌트로 분리하는 방안 (출처: ddd)
- [AccountClosureService.java:11] 이름이 wallet 컨텍스트의 "출금(withdrawal)" 과 혼동될 소지 — `AccountClosureService` 로 개명해 반영함 (출처: ddd)
- [RedisSessionCommandAdapter.java:64-79] 인증 핫패스 `findUserId` 의 `GET` + `EXPIRE` 2회가 여전히 비원자적 3회 왕복 — 다른 명령처럼 Lua 로 묶는 방안 (출처: 동시성, oop)
- [RedisSessionCommandAdapter.java:21-43] Lua 스크립트 자체를 검증하는 통합 테스트 부재 — Testcontainers 로 `create` → `deleteAllOf` 경로 검증 권장 (출처: oop)
- [DeleteAccountService.java:41-45] 1차 참고 이슈(트랜잭션 커밋 전 Redis 세션 삭제)가 그대로 남아 있음 — 커밋 전 세션을 지우면 미커밋 상태를 읽은 재로그인이 새 세션을 만들어 탈퇴 후에도 살아남을 수 있다. `afterCommit` 훅으로 이전 권장 (출처: 동시성)
