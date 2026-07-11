# 카카오 로그인 리뷰

## 1차 차단 이슈

- [ ] **[api/src/main/java/ksh/tryptobackend/user/application/service/KakaoLoginService.java:45-63] 최초 로그인 레이스 처리가 애플리케이션 서비스에 노출됨 (JPA 예외 직접 의존 + private 메서드)** (출처: oop, 컨벤션)
  - **설명:** `registerAndIssueSession`이 `org.springframework.dao.DataIntegrityViolationException`(JPA 인프라 타입)을 직접 import·catch 해서 unique 위반 시 재조회하는 동시성 처리를 서비스 계층에 구현했다. application 계층이 포트 구현체가 JPA라는 사실에 의존하는 DIP 위반이고, 애플리케이션 서비스가 순수 오케스트레이션만 하고 private 메서드를 두지 않는다는 api 컨벤션에도 어긋난다. 같은 저장소의 `trading/adapter/out/persistence/JpaPositionCommandAdapter.getOrCreate()` 는 동일한 "동시 생성 레이스"를 어댑터 안에 완전히 캡슐화한 선례가 있다.
  - **수정 제안:** `saveAndFlush` → `catch (DataIntegrityViolationException)` → 재조회 흐름을 `UserCommandAdapter`(포트 구현체) 안으로 옮겨 `JpaPositionCommandAdapter` 패턴을 따른다. `KakaoLoginService` 는 포트 메서드 한 번만 호출하도록 오케스트레이션만 남기고, JPA 예외 타입 의존과 private 재시도 메서드를 제거한다.

- [ ] **[api/src/main/java/ksh/tryptobackend/user/application/service/KakaoLoginService.java:50-55] 닉네임 유니크 위반을 소셜 신원 위반으로 오인해 정상 로그인을 실패 처리** (출처: 동시성)
  - **설명:** 신규 회원 저장 시 `nickname` 유니크 제약과 `(provider, provider_id)` 유니크 제약 두 개가 걸려 있는데, catch 블록은 원인을 구분하지 않고 무조건 소셜 신원 재조회를 한다. 서로 무관한 두 사용자가 동시에 가입하다 닉네임 후보가 우연히 겹치면(check-then-act, 16만 조합), 뒤 요청은 닉네임 충돌로 `DataIntegrityViolationException` 이 나는데 소셜 신원으로 재조회하면 없으므로 `SOCIAL_LOGIN_FAILED(401)` 로 잘못 응답되어 정상 인증이 무산된다. 런칭 초기·이벤트 등 동시 가입이 몰리는 상황에서 관측될 수 있다.
  - **수정 제안:** 제약 위반 원인을 구분한다(Hibernate `ConstraintViolationException.getConstraintName()` 등). 닉네임 충돌이면 새 닉네임으로 저장을 소수 회 재시도하고, 소셜 신원 충돌일 때만 재조회 후 기존 회원으로 로그인한다. (1번 이슈로 레이스 처리를 어댑터로 옮길 때 함께 반영한다.)

- [ ] **[api/src/main/java/ksh/tryptobackend/user/adapter/out/acl/UserAclSocialIdentityQueryAdapter.java] 카카오 외부 HTTP 호출에 connect/read 타임아웃 미설정** (출처: 성능)
  - **설명:** 토큰 교환(`requestToken`)·사용자 정보 조회(`requestMember`) 두 번의 외부 호출이 Tomcat 워커 스레드에서 동기 실행되는데 `RestClient` 에 커넥션/읽기 타임아웃이 없다. 카카오가 느려지면 워커 스레드를 무기한 점유해, 로그인 이외 엔드포인트까지 포함한 공용 스레드 풀이 고갈될 수 있다.
  - **수정 제안:** `RestClient.Builder` 생성 시 `ClientHttpRequestFactorySettings`(또는 `JdkClientHttpRequestFactory`)로 connect timeout(예: 2~3초), read timeout(예: 3~5초)을 설정한다. 가능하면 `application.yml` 의 `spring.http.client.connect-timeout`/`read-timeout` 으로 앱 전역 기본값을 둔다. 타임아웃 초과 시 기존 `RestClientException` catch(`SOCIAL_SERVER_ERROR`)가 그대로 동작하는지 확인한다.

- [ ] **[api/src/main/java/ksh/tryptobackend/user/adapter/out/persistence/entity/UserJpaEntity.java:39-43] provider / provider_id 컬럼에 nullable = false 제약 누락** (출처: 컨벤션)
  - **설명:** plan.md 는 email 제거 근거로 "provider/provider_id 는 null 이 없다(소셜 로그인 사용자는 항상 값 존재)"를 명시했는데, 대체된 두 컬럼에 `nullable = false` 가 빠졌다(직전 email 컬럼은 명시했었음). unique 제약만으로는 대부분 DB 에서 null 중복을 막지 못한다.
  - **수정 제안:** `@Column(name = "provider", nullable = false, length = 20)`, `@Column(name = "provider_id", nullable = false)` 로 제약을 명시한다.

- [ ] **[api/src/main/java/ksh/tryptobackend/user/adapter/out/SessionCommandAdapter.java] Redis 접근 어댑터가 persistence 하위 패키지·기술 접두어 명명 규칙 미준수** (출처: 컨벤션)
  - **설명:** api 컨벤션은 Redis 등 모든 저장소 접근 어댑터를 `adapter/out/persistence/` 하위에 두고 `{사용기술}{애그리거트}{Query|Command}Adapter`(예: `RedisLivePriceQueryAdapter`)로 명명하도록 한다. 이번에 새로 만든 `SessionCommandAdapter` 는 `StringRedisTemplate` 으로 Redis 에 직접 접근하면서 `adapter/out/` 루트에 있고 기술 접두어도 없다.
  - **수정 제안:** `adapter/out/persistence/RedisSessionCommandAdapter.java` 로 옮기고 클래스명을 `RedisSessionCommandAdapter` 로 변경한다. (기존 `UserCommandAdapter` 등은 이번 범위 밖이므로 이번에 새로 만든 파일만 정정.)

## 1차 참고 이슈 (수정 안 함, 보고용)

- [api/.../user/application/port/out/SocialIdentityQueryPort.java:5-7] 카카오 인증(도메인 로직 연동)을 CRUD성 `...QueryPort` 로 표현·명명 — `domain/service` 의 도메인 의도 이름(예: `SocialAuthenticator`)으로 두는 편이 컨벤션에 맞음 (ddd)
- [api/.../user/domain/model/User.java:38-52] `User.create` 가 SocialIdentity=null 허용해 1:1 불변식이 DB 제약에만 의존 (ddd)
- [api/.../user/domain/vo/SocialIdentity.java:3-7] `of` 팩토리에 null·blank 유효성 검증 부재 (ddd)
- [api/.../marketdata/application/service/SyncMarketMetaService.java:79-102] 리팩토링으로 도입된 boolean 플래그 인자(`putBaseSymbolNames(..., boolean domestic)`) — 의미 드러나는 두 메서드로 분리 권장 (oop)
- [api/.../user/application/service/KakaoLoginService.java:41-59] `issueSession(User, boolean newUser)` 플래그 인자 + 바로 위 `User newUser` 와 동명이의 식별자 혼동 (oop)
- [api/.../user/domain/model/User.java:25-52] `registerWith` 와 `create` 의 builder 호출부 중복(DRY) — `registerWith` 가 `create` 위임 권장 (oop)
- [api/.../user/adapter/out/acl/UserAclSocialIdentityQueryAdapter.java:55-92] `requestToken`/`requestMember` 간 에러 처리(onStatus 4xx/5xx, catch) 중복 (oop)
- [api/.../user/adapter/out/acl/UserAclSocialIdentityQueryAdapter.java:86-87] `requestMember`(사용자 정보 조회) 4xx 를 `SOCIAL_LOGIN_FAILED(401)` 로 매핑 — spec 표는 "사용자 정보 조회 실패"를 `SOCIAL_SERVER_ERROR(502)` 로 분류 (oop)
- [api/.../user/domain/model/User.java:19] `socialIdentity` nullable 이 외부(영속성 매퍼)에 null 체크를 강제 — `Optional` 노출 또는 주석 권장 (oop)
- [api/.../user/adapter/out/service/UniqueNicknameGeneratorImpl.java:29-38] 닉네임 생성기 TOCTOU 창 — DB 유니크 제약 실패를 자체 감지·재시도 못 함(1번 차단과 연계 해결) (동시성)
- [api/.../user/adapter/out/service/UniqueNicknameGeneratorImpl.java] 신규 가입 1건당 후보마다 DB 왕복(최악 20회) — 표본 공간 확대·배치 조회 고려 (성능)
