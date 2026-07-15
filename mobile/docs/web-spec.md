# Trypto 모바일 앱 이식 사양서

본 문서는 기존 웹 프론트엔드(React + Vite)와 백엔드(Spring Boot)를 실측하여, Flutter 네이티브 모바일 앱으로 이식하기 위한 단일 기준 사양을 정의한다. REST 계약과 응답 봉투, 세션 쿠키 인증과 소셜 로그인(PKCE), STOMP 실시간 시세, 여섯 개 화면(마켓·포트폴리오·입출금·랭킹·투자 복기·로그인/라운드/마이페이지)의 동작 규칙, 디자인 토큰과 포매터 규칙, 그리고 서버 변경 필요 여부에 대한 판정을 담는다. 서술은 코드 실측에 근거하며, 웹 구현과 서버 계약이 어긋나는 지점은 **서버 계약을 기준으로** 정정하여 기술한다. 이식 착수 전 반드시 「이식 리스크와 결정 사항」을 먼저 확인한다.

---

## 이식 리스크와 결정 사항

이식을 가로막거나 착수 전 결정이 필요한 항목을 심각도 순으로 정리한다.

### R1. (치명) OAuth `redirect_uri` 가 서버 고정값이다 — 커스텀 스킴을 쓰려면 백엔드 수정이 필수다

- **사실**: 백엔드는 토큰 교환 시 `redirect_uri` 를 요청 바디가 아니라 **서버 설정값**에서 읽는다(`KakaoOAuthClient.java:45`, `GoogleOAuthClient.java:45` — `form.add("redirect_uri", properties.getRedirectUri())`). `LoginRequest` 의 필드는 `code`, `codeVerifier` 두 개뿐이다(`LoginRequest.java:7`). OAuth2 규격상 토큰 교환의 `redirect_uri` 는 인가 요청에 쓴 값과 완전히 동일해야 하므로, 앱이 `trypto://auth/kakao/callback` 같은 커스텀 스킴으로 인가하면 서버는 여전히 웹 URL 을 보내 provider 가 `redirect_uri_mismatch` 로 거절한다.
- **권장 해법 — 방안 A(1순위, 백엔드 무수정)**: 웹 콜백 URL(`https://{도메인}/auth/{provider}/callback`)을 그대로 인가 `redirect_uri` 로 사용하고, Android App Links / iOS Universal Links 로 OS 가 그 https URL 을 앱으로 가로챈다. 인가 화면은 `flutter_web_auth_2`(Android: Chrome Custom Tabs, iOS: `ASWebAuthenticationSession`)로 띄운다. 앱 내 `WebView` 는 구글 정책상 차단되므로 쓸 수 없다. 필요한 작업은 `assetlinks.json` / `apple-app-site-association` 정적 파일 호스팅뿐이며(`frontend/nginx.conf:37-39` 의 `try_files` 가 실제 파일을 우선 서빙하므로 파일만 추가하면 된다), 제공자 콘솔 추가 등록도 불필요하다.
- **방안 A 의 제약(착수 전 확정 필요)**: `ASWebAuthenticationSession` 의 https 콜백은 **iOS 17.4 이상**에서만 지원된다. 최소 지원 iOS 를 17.4 로 올릴 수 없다면 방안 B 로 전환한다.
- **방안 B(대안, 백엔드 수정 필수)**: `LoginRequest` 에 `redirectUri`(또는 `clientType`) 추가 → `OAuthClient.getIdentity(...)` 시그니처 확장 → 토큰 교환 폼의 `redirect_uri` 를 그 값으로 치환. **클라이언트가 보낸 문자열을 그대로 신뢰하면 오픈 리다이렉터가 되어 인가 코드가 탈취되므로, 설정에 등록된 허용 목록(`app.oauth.{provider}.allowed-redirect-uris`)과 반드시 대조한다.** 추가로 구글은 Android/iOS 클라이언트 유형이 `client_secret` 을 발급하지 않으므로 `GoogleOAuthClient.java:44` 의 `client_secret` 전송을 클라이언트 유형별로 분기해야 하고, 카카오는 콘솔에 커스텀 스킴 등록이 불가하여 Kakao SDK 인가 경로(`kakao{네이티브앱키}://oauth`)를 써야 할 수 있다. 이 두 항목은 **외부 콘솔 정책이라 코드로 확인할 수 없으므로, 방안 B 를 택한다면 콘솔에서 등록 가능 여부를 먼저 검증한 뒤 백엔드를 수정한다.**

### R2. (높음) 세션 쿠키를 네이티브 앱에서 유지해야 하며, 쿠키 만료와 서버 TTL 이 비대칭이다

- **사실**: 인증 수단은 **HttpOnly 세션 쿠키 하나뿐**이며 `Authorization` 헤더는 어디에도 없다. 브라우저와 달리 네이티브 HTTP 클라이언트는 `Set-Cookie` 를 자동 저장·재전송하지 않는다. 더 중요한 비대칭이 있다. Redis 세션 TTL 은 인증된 요청마다 7일로 갱신되는 **슬라이딩** 방식이지만(`RedisSessionCommandAdapter.java:71-79`), 쿠키의 `Max-Age` 는 **로그인 시점에만 발급되고 갱신되지 않는다**(`SessionCookieFactory.issue()` 는 `AuthController.java:36-40` 에서만 호출). 브라우저 쿠키 저장소를 그대로 흉내 내면 매일 앱을 써도 **로그인 후 정확히 7일째에 강제 로그아웃**된다.
- **권장 해법**: 쿠키 jar 를 그대로 쓰지 말고, **로그인 응답의 `Set-Cookie` 에서 `SESSION` 값만 추출해 `flutter_secure_storage` 에 저장(만료 시각은 저장하지 않는다)하고, 모든 요청에 `Cookie: SESSION=<값>` 헤더를 직접 붙이는 Dio 인터셉터**를 둔다. 서버가 401 `UNAUTHENTICATED` 를 주면 그때 저장값을 폐기한다. 이 방식은 서버의 슬라이딩 TTL 을 그대로 활용하므로 주기적으로 앱을 쓰는 사용자는 재로그인이 없고, 백엔드 계약을 전혀 건드리지 않는다. (대안인 `dio_cookie_manager` + `PersistCookieJar` 는 구현이 간단하지만 위 7일 절대 만료를 그대로 물려받는다.)
- **부수 조건**: 운영은 `SESSION_COOKIE_SECURE=true` 이므로 앱 base URL 은 반드시 `https://` 여야 한다. 로컬 개발에서 `http://10.0.2.2:8080` 에 붙으려면 백엔드 `SESSION_COOKIE_SECURE=false`(기본값)여야 하고, Android 는 클리어텍스트를 기본 차단하므로 개발 빌드에 `android:usesCleartextTraffic="true"`(또는 network security config 예외)가 필요하다. `HttpOnly`·`SameSite=Lax` 는 브라우저 전용 개념이라 네이티브에서는 제약이 아니다.

### R3. (높음) STOMP 사용자 큐(`/user/.../queue/events`)는 현행 서버에서 동작하지 않는다

- **사실**: 서버는 `convertAndSendToUser(userId, "/queue/events", payload)` 로 체결 알림을 발행하지만(`StompOrderFilledNotificationAdapter.java:22,48`), 이 규약이 성립하려면 WebSocket 세션에 `Principal` 이 부착되어 있어야 한다. **이 프로젝트의 WebSocket 세션에는 Principal 이 단 한 번도 부착되지 않는다** — `HandshakeInterceptor`, `HandshakeHandler`, `ChannelInterceptor`, `StompHeaderAccessor.setUser` 사용처가 `api/src/main/java` 전체에 하나도 없다(전수 grep 무매치). 따라서 `SimpUserRegistry` 는 항상 비어 있고 해석 대상 세션이 0개가 되어 **메시지가 폐기된다.** 구독 측 형식도 규약과 어긋난다(웹은 `/user/{userId}/queue/events` 로 리터럴 구독하나, 규약상 구독은 `/user/queue/events` 여야 한다). 페이로드마저 어긋난다 — 웹은 `walletId`/`coinId`/`side`/`price`/`fee` 를 기대하지만 서버 실제 페이로드는 `eventType`/`orderId`/`executedPrice`/`quantity`/`executedAt` 5개뿐이다(`OrderFilledStompPayload.java:7-8`).
- **권장 해법**: **이 토픽에 기대어 UI 를 갱신하는 설계를 하지 않는다.** 체결 반영은 **REST 재조회**로 확정 처리한다(주문 제출·취소 성공 직후 `GET /api/orders/available` 재호출, 지정가 지연 체결은 화면 진입·포그라운드 복귀·당김 새로고침 시 재조회). 수신 계층은 만들어 두되 구독 목적지는 Spring 규약대로 **`/user/queue/events`**, 페이로드 모델은 **서버 실제 필드**에 맞춘다. 이 경로를 살리려면 서버에 (1) 핸드셰이크/CONNECT 에서 `SESSION` 쿠키를 읽어 `userId` 이름의 `Principal` 부착, (2) 페이로드에 `walletId`/`coinId`/`side`/`fee` 추가가 필요하다. **웹과의 기능 동등성만 목표라면 두 변경 모두 불필요하다**(웹에서도 이 경로는 작동하지 않으므로 기능 손실이 없다).
- 참고로 **티커 토픽 구독에는 인증이 필요 없다**. WebSocket 핸드셰이크에 어떠한 인증도 걸려 있지 않으므로(`WebSocketConfig.java:41-43`), 세션 쿠키를 실을 필요가 현재로서는 없다. 서버가 위 (1)을 구현할 때에만 `webSocketConnectHeaders: {'Cookie': 'SESSION=$sessionId'}` 를 실으면 된다. STOMP `CONNECT` 프레임의 `login`/`passcode`·`stompConnectHeaders` 는 서버가 읽는 코드가 없어 무시된다.

### R4. (높음) 웹 프론트와 서버 계약이 12곳에서 어긋나 있다 — Flutter 는 서버 계약을 기준으로 구현한다

- **사실**: `userId` 를 바디·쿼리로 보내지만 서버는 전부 `@LoginUser` 로 세션에서 얻어 무시한다. 송금 내역 커서는 프론트가 `cursor`(문자열), 서버가 `cursorTransferId`(Long) 를 받아 **웹의 커서 페이지네이션이 실제로 동작하지 않는다.** 주문 내역 응답에는 `status` 필드 자체가 없는데 프론트는 요청 필터값으로 덮어쓰고 있다. `UserProfileResponse.email` 은 존재하지 않는 사문(死文)이다. 전체 목록은 §1.9 를 참조한다.
- **권장 해법**: §1.9 의 12개 항목을 이식 착수 시 일괄 정정한다. 특히 `cursorTransferId: int` 전송, `nextCursor: int?` 수신, 주문 내역 모델에서 `status` 제거, 요청에서 `userId` 전면 제거를 지킨다.

### R5. (중간) 실시간 티커는 초당 수백 건의 소형 메시지이며, 배칭 없이는 프레임이 무너진다

- **사실**: collector 는 tick 1건마다 크기 1 배치를 발행하므로(`docs/contracts/ticker-exchange.md:17`) `/topic/tickers.{exchangeId}` 로 **초당 수백 건의 배열 메시지**가 들어온다. 거래소당 상장 코인은 최대 600개를 넘는다. 서버는 아웃바운드 큐가 포화되면 `DiscardOldestPolicy` 로 **오래된 메시지를 버리며**(`WebSocketConfig.java:78`), 전송 지연이 5초를 넘거나 버퍼가 512KB 를 넘으면 세션을 끊는다.
- **권장 해법**: 수신 틱을 `Map<String, Ticker>` 버퍼에 모으고 `SchedulerBinding.scheduleFrameCallback` 또는 16ms `Timer` 로 **한 번만 flush** 한다(웹의 `requestAnimationFrame` 배칭에 대응). 틱마다 `setState` 를 호출하면 안 된다. 목록은 `ListView.builder(itemExtent: 68)` 로 보이는 행만 빌드하고, 가격 플래시는 **행 단위 로컬 상태**(`ValueNotifier`)로 두어 전체 목록 리빌드를 유발하지 않게 한다. 티커 스트림은 **완전성이 보장되지 않는 스냅샷 스트림**이므로 델타 누산을 절대 하지 않고 항상 최신 값으로 덮어쓴다.

### R6. (중간) `BigDecimal` 정밀도와 `LocalDateTime` 타임존

- **사실**: 서버는 모든 `BigDecimal` 을 따옴표 없는 JSON 숫자 리터럴로 내린다(Jackson 기본 직렬화). 코인 수량은 소수점 8자리까지 내려오므로 IEEE754 double 로는 정확히 표현되지 않는다. 시각 필드는 **캔들의 `time` 만 `Instant`(Z 포함, UTC)이고 나머지(`createdAt`·`startedAt`·`occurredAt` 등)는 전부 오프셋 없는 `LocalDateTime`**, 즉 서버 로컬시각(Asia/Seoul)이다.
- **권장 해법**: 연산이 들어가는 값(잔고 차감·수수료 계산)은 `Decimal.parse(value.toString())`(`package:decimal`)으로 승격해 다루고, 화면 표시 전용 값만 `double` 로 둔다. JSON 숫자는 KRW 정수 가격에서 `int` 로 올 수 있으므로 **반드시 `num` 으로 받아 `.toDouble()`** 한다(`as double` 캐스팅은 런타임 오류를 낸다). `LocalDateTime` 계열은 파싱 후 **명시적으로 `Asia/Seoul` 로 해석**(`package:timezone`)하고 기기 타임존으로 변환해 표시한다. 캔들 `time` 은 `.toLocal()` 이 필수다.

### R7. (중간) 거래소 목록 조회 API 가 존재하지 않는다

- **사실**: 거래소 ID 는 `application.yml` 의 `app.exchanges` 삽입 순서에 의존하는 auto-increment 값이며(`seed-data.sql:10-13` — `(1,'UPBIT'), (2,'BITHUMB'), (3,'BINANCE')`), 조회 엔드포인트가 없어 웹은 `coins.ts:49-53` 에 하드코딩한다. 이 ID 는 REST 경로, STOMP 토픽, 지갑 매핑, 긴급 자금 요청에 모두 그대로 쓰인다.
- **권장 해법**: Flutter 도 동일한 상수 테이블을 **한 곳(`ExchangeIds` 상수 클래스)** 에 모은다: `1=업비트(KRW, 수수료 0.0005)`, `2=빗썸(KRW, 0.0025)`, `3=바이낸스(USDT, 0.001)`. 서버에 조회 API 를 신설하면 앱·웹 양쪽의 상수 중복을 제거할 수 있으나 **필수는 아니다.**

### R8. (중간) 두 건의 입력값이 서버에서 400 이 아니라 500 을 유발한다 — 클라이언트가 선제 차단해야 한다

- **사실 1**: `idempotencyKey` 가 UUID 형식이 아니면 JSON 역직렬화 실패가 전용 핸들러에 걸리지 않고 catch-all 로 떨어져 **500 `INTERNAL_SERVER_ERROR`** 가 내려온다.
- **사실 2**: `ErrorCode.DUPLICATE_RULE_TYPE`(`ErrorCode.java:32`)의 메시지 키 `duplicate.rule.type` 이 `messages.properties` 에 **정의되어 있지 않다.** 이 오류가 실제로 발생하면 `MessageSource` 가 `NoSuchMessageException` 을 던지고 catch-all 이 잡아 **400 대신 500** 으로 응답한다.
- **권장 해법**: 송금·긴급충전의 `idempotencyKey` 는 반드시 UUID v4(`Uuid().v4()`)로 생성하고, 라운드 생성 시 동일 `ruleType` 중복 전송을 클라이언트에서 미리 차단한다. 서버 수정(메시지 키 추가) 없이 앱 측 방어만으로 회피 가능하다.

### R9. (중간) 웹 구현의 알려진 결함을 그대로 옮기면 기능이 죽는다

- 입출금 화면의 코인 `currentPrice` 가 **0 으로 고정**되어(`WalletPage.tsx:103`) 총 자산·환산액·소액 제외·수량 정렬이 모두 무의미해진다. → `GET /api/exchanges/{id}/coins` 응답의 `price` 를 사용한다.
- 송금 내역 필터가 `exchangeId: ""` 와 `"upbit"` 를 비교해 **항상 공집합**이 된다. → 내역은 이미 `walletId` 로 조회하므로 거래소 필터 자체를 두지 않는다.
- 프론트가 가정하는 송금 상태 6종(`PENDING`/`PROCESSING`/…)은 서버에 없다. 서버 `TransferStatus` 는 **`SUCCESS` 단일값**이다. → 상태 필터 탭을 제거하고 `완료` 배지만 표시한다.
- 라운드 생성·종료·긴급충전·닉네임 변경 실패 시 웹은 콘솔 로그만 남기고 화면에 아무 표시가 없다. → **스낵바로 실패를 알린다.**
- 긴급 자금 상한(100만원), 닉네임 길이(2~20자)를 웹은 입력 단계에서 검증하지 않아 제출 시점에 실패한다. → 입력 단계에서 강제한다.

### R10. (낮음) 401 전역 처리기가 웹에 없다 — 모바일에서 추가한다

- **사실**: `client.ts` 에는 401 전역 처리가 없어 세션 만료 시 개별 API 가 실패할 뿐 자동 로그아웃·화면 이동이 일어나지 않는다.
- **권장 해법**: Dio 인터셉터에서 **HTTP 401 또는 봉투 `code == "UNAUTHENTICATED"`** 를 감지하면 저장된 세션을 지우고 인증 상태를 `null` 로 만든다. go_router `redirect` 가 자동으로 `/login` 으로 보낸다. 백엔드 수정 없이 클라이언트만으로 구현된다.

### R11. (낮음) iOS 배포를 계획한다면 회원 탈퇴를 반드시 붙여야 한다

- **사실**: `DELETE /api/users/me` 는 서버에 구현되어 있으나(`UserController.java:48-54`) **웹 프론트에는 호출부가 없다.** Apple App Store 심사 지침상 계정 생성이 가능한 앱은 앱 내 계정 삭제 경로를 제공해야 한다.
- **권장 해법**: iOS 배포 시 마이페이지에 탈퇴를 추가한다. 응답은 200 + 세션 쿠키 만료이므로 호출 후 로컬 저장소를 비우고 로그인 화면으로 보낸다. 탈퇴 후 30일 재가입 제한이 있어 재로그인 시 `SIGNUP_RESTRICTED`(403) 가 나올 수 있다(`User.java:14,67-71`).

### R12. (낮음) Flutter Web 타깃을 추가하면 CORS 부재가 즉시 문제가 된다

- **사실**: 서버에 CORS 설정이 **전혀 없다**(`@CrossOrigin`/`addCorsMappings`/`CorsConfigurationSource` 전수 grep 0건). 웹이 동작하는 이유는 nginx·Vite dev proxy 가 **동일 출처**를 만들기 때문이다.
- **권장 해법**: **네이티브(Android/iOS)만 대상으로 한다.** 네이티브 클라이언트는 Origin 헤더를 보내지 않고 프리플라이트도 없으므로 현 상태 그대로 호출 가능하다. Flutter Web 을 함께 빌드하려면 그때는 `addCorsMappings` 추가가 필수다.

### R13. (낮음) 다크 모드는 지원하지 않는다

- **사실**: `.dark` 색 정의 블록, `@custom-variant dark`, `prefers-color-scheme`, 테마 토글 코드가 저장소 전체에 하나도 없다. 일부 컴포넌트의 `dark:` 유틸리티는 shadcn 템플릿 잔재이며 다크 토큰이 정의되지 않아 무효다.
- **권장 해법**: `MaterialApp` 에 `theme`(라이트) 하나만 정의하고 `darkTheme` 을 정의하지 않으며, `themeMode: ThemeMode.light` 를 명시해 OS 설정과 무관하게 라이트로 고정한다.

### 백엔드 변경 필요 여부 — 최종 판정

| 항목 | 판정 | 근거 |
|---|---|---|
| 로그인 API 계약(`code`+`codeVerifier`) | **불필요** | 모바일도 동일 바디로 호출 가능 (`LoginRequest.java:7`) |
| 세션 쿠키 발급·검증 | **불필요** | 네이티브도 표준 쿠키 헤더로 통과 (`AuthInterceptor.java:35-38`) |
| 쿠키 `SameSite=None` 전환 | **불필요** | 네이티브 HTTP 클라이언트는 `SameSite`·`HttpOnly` 를 해석하지 않는다 |
| 세션 → 토큰(JWT) 전환 | **불필요** | 쿠키를 그대로 사용 가능 |
| CORS | **불필요** (네이티브 한정) | Flutter Web 타깃 추가 시에만 필요 |
| WebSocket 인증 | **불필요** | 애초에 인증이 없다 |
| 7일 절대 만료 | **불필요** | R2 의 세션 ID 직접 보관으로 클라이언트에서 해결 |
| 401 자동 로그아웃 | **불필요** | 클라이언트 인터셉터로 해결 |
| 거래소 목록 API | **불필요** | 상수 하드코딩으로 대응(신설 시 중복 제거 이점만 있음) |
| STOMP 사용자 큐 살리기 | **선택** | 웹에서도 미동작이므로 기능 동등성에는 불필요. 실시간 체결 푸시를 원하면 필요 |
| **OAuth `redirect_uri`** | **방안 A: 불필요 / 방안 B: 필수** | `KakaoOAuthClient.java:45`, `GoogleOAuthClient.java:45` — 서버 고정값 사용 |

**결론**: 인증 이식의 성패는 오직 `redirect_uri` 한 가지에 달려 있다. **방안 A(App Links/Universal Links)를 채택하면 백엔드를 한 줄도 고치지 않고** 카카오·구글 로그인, 세션 유지, 로그아웃, 인증 복구, 라우트 가드, 전 화면 기능을 이식할 수 있다. iOS 최소 버전(17.4) 또는 앱 링크 검증이 문제가 되면 방안 B 로 전환하되, 백엔드 변경은 "redirect URI 를 플랫폼별로 다중화하고 허용 목록으로 검증한다" 범위를 넘지 않는다.

---
## 1. 백엔드 계약 (REST)

### 1.1 컨트롤러 전수 목록

`api/src/main/java` 기준 `@RestController` 는 13개이며(`@Controller` 는 없음), 이외에 `@RestControllerAdvice` 1개(`GlobalControllerAdvice`)가 존재한다.

| 컨트롤러 | 클래스 베이스 경로 | 파일 |
|---|---|---|
| `AuthController` | `/api/auth` | `api/src/main/java/ksh/tryptobackend/user/adapter/in/web/AuthController.java` |
| `UserController` | `/api/users` | `api/src/main/java/ksh/tryptobackend/user/adapter/in/web/UserController.java` |
| `FeedbackController` | `/api/feedbacks` | `api/src/main/java/ksh/tryptobackend/user/adapter/in/web/FeedbackController.java` |
| `RoundController` | `/api/rounds` | `api/src/main/java/ksh/tryptobackend/investmentround/adapter/in/web/RoundController.java` |
| `RegretController` | `/api/rounds/{roundId}/regret` | `api/src/main/java/ksh/tryptobackend/regretanalysis/adapter/in/web/RegretController.java` |
| `OrderController` | `/api/orders` | `api/src/main/java/ksh/tryptobackend/trading/adapter/in/web/OrderController.java` |
| `TransferController` | `/api/transfers` | `api/src/main/java/ksh/tryptobackend/wallet/adapter/in/web/TransferController.java` |
| `WalletBalanceController` | `/api/wallets/{walletId}/balances` | `api/src/main/java/ksh/tryptobackend/wallet/adapter/in/web/WalletBalanceController.java` |
| `TransferHistoryController` | `/api/wallets/{walletId}/transfers` | `api/src/main/java/ksh/tryptobackend/wallet/adapter/in/web/TransferHistoryController.java` |
| `PortfolioController` | `/api/wallets/{walletId}/portfolio` | `api/src/main/java/ksh/tryptobackend/portfolio/adapter/in/web/PortfolioController.java` |
| `RankingController` | `/api/rankings` | `api/src/main/java/ksh/tryptobackend/ranking/adapter/in/web/RankingController.java` |
| `CandleController` | `/api/candles` | `api/src/main/java/ksh/tryptobackend/marketdata/adapter/in/web/CandleController.java` |
| `ExchangeCoinController` | `/api/exchanges` | `api/src/main/java/ksh/tryptobackend/marketdata/adapter/in/web/ExchangeCoinController.java` |

---

### 1.2 공통 응답 봉투(ApiResponseDto)

모든 REST 응답은 예외 없이 아래 봉투로 감싸여 내려온다. 서버 정의는 `api/src/main/java/ksh/tryptobackend/common/dto/response/ApiResponseDto.java:3`, 프론트 타입은 `frontend/src/lib/api/types.ts:1-6` 이다.

```json
{ "status": 200, "code": "SUCCESS", "message": "조회 성공", "data": { } }
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `status` | int | HTTP 상태 코드와 동일한 값(200 / 201 / 4xx / 5xx) |
| `code` | string | 성공은 `SUCCESS` 또는 `CREATED`, 실패는 `ErrorCode` enum 이름 또는 `VALIDATION_ERROR` / `TYPE_MISMATCH` |
| `message` | string | 사용자 노출용 한국어 메시지(`api/src/main/resources/messages.properties`) |
| `data` | T \| null | 실제 페이로드. 실패 시 항상 `null` |

#### 성공/실패 판별 규칙 (`frontend/src/lib/api/client.ts:9,42-86`)

프론트 클라이언트는 다음 순서로 판정한다. Flutter 에서 동일하게 재현한다.

1. 응답 본문을 텍스트로 읽고, 비어 있으면 `null`, JSON 파싱 실패면 `null` 로 둔다 (`client.ts:31-40`).
2. `response.ok` 가 거짓이면 → 봉투가 객체이면 `message` / `status` / `code` / `data` 를 담아 `ApiClientError` 던짐. 봉투가 없으면 `("Request failed", HTTP status, "UNKNOWN_ERROR")` (`client.ts:59-70`).
3. `response.ok` 이지만 봉투가 객체가 아니면 → `ApiClientError("Invalid API response", status, "INVALID_RESPONSE")` (`client.ts:72-74`).
4. `code` 가 `SUCCESS` 또는 `CREATED` 집합에 없으면 → `ApiClientError` 던짐 (`client.ts:9,76-83`).
5. 위를 모두 통과하면 `envelope.data` 만 반환한다. **호출부는 봉투를 절대 보지 않는다.**

세 가지 팩토리가 존재하며 조합이 다르다는 점에 주의한다 (`ApiResponseDto.java:14-24`).

| 팩토리 | HTTP | `status` | `code` | 사용처 |
|---|---|---|---|---|
| `success` | 200 | 200 | `SUCCESS` | 대부분의 조회·갱신 |
| `created` | 201 | 201 | `CREATED` | `POST /api/rounds`, `POST /api/orders`, `POST /api/transfers` |
| `createdSuccess` | 201 | 201 | `SUCCESS` | `POST /api/feedbacks` |

즉 201 응답에서 `code` 가 `CREATED` 인 경우와 `SUCCESS` 인 경우가 공존하므로, 성공 판정 집합은 반드시 `{SUCCESS, CREATED}` 두 개여야 한다.

커서 페이지 응답은 `data` 안에 `CursorPageResponseDto` 형태로 담긴다.

```json
{ "content": [ ], "nextCursor": 123, "hasNext": true }
```

`nextCursor` 는 **`Long | null`** 이다(`common/dto/response/CursorPageResponseDto.java:6`). `hasNext` 가 false 이면 `nextCursor` 는 null 이다. 커서 파라미터 이름이 엔드포인트마다 다르다는 점에 주의한다.

| 엔드포인트 | 커서 파라미터 | 커서 값의 의미 |
|---|---|---|
| `GET /api/orders` | `cursorOrderId` | 마지막 `orderId` |
| `GET /api/rankings` | `cursorRank` | 마지막 `rank` |
| `GET /api/wallets/{walletId}/transfers` | `cursorTransferId` | 마지막 `transferId` |
| `GET /api/candles` | `cursor` | ISO-8601 시각 |

---

### 1.3 에러 코드 체계

전역 예외 처리는 `api/src/main/java/ksh/tryptobackend/common/exception/GlobalControllerAdvice.java` 에 6개 핸들러만 등록되어 있다.

| 예외 | HTTP | `code` | 비고 |
|---|---|---|---|
| `CustomException` | `ErrorCode.status` | `ErrorCode.name()` | 도메인 규칙 위반 전반 (`GlobalControllerAdvice.java:25-34`) |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | 메시지는 `"필드명: 사유"` 를 `, ` 로 이어붙인 문자열 (`:36-46`) |
| `ObjectOptimisticLockingFailureException` | 409 | `CONCURRENT_MODIFICATION` | (`:48-60`) |
| `DataIntegrityViolationException` | 409 | `DUPLICATE_REQUEST` | (`:62-73`) |
| `MethodArgumentTypeMismatchException` | 400 | `TYPE_MISMATCH` | 쿼리 파라미터 타입 불일치. 메시지는 `"'{파라미터}' 파라미터의 값이 유효하지 않습니다: {값}"` (`:75-83`) |
| `Exception` (catch-all) | 500 | `INTERNAL_SERVER_ERROR` | (`:85-96`) |

**주의:** JSON 역직렬화 실패(예: `idempotencyKey` 가 UUID 형식이 아닌 경우)는 위 5개 전용 핸들러 중 어디에도 걸리지 않아 catch-all 로 떨어져 **400 이 아니라 500 `INTERNAL_SERVER_ERROR`** 봉투가 내려온다. Flutter 클라이언트는 UUID 형식을 로컬에서 강제해야 한다.

프론트 전용(서버가 내려주지 않는) 합성 코드는 `UNKNOWN_ERROR`, `INVALID_RESPONSE` 두 개뿐이다.

#### ErrorCode 전량 (`common/exception/ErrorCode.java:8-75`, 메시지는 `messages.properties` 기준)

응답 봉투의 `code` 필드에 enum 이름이 그대로 실린다.

| HTTP | ErrorCode | 사용자 메시지 | 발생 지점 |
|---|---|---|---|
| 400 | `INSUFFICIENT_BALANCE` | 잔고가 부족합니다. | 주문·송금 |
| 400 | `BELOW_MIN_ORDER_AMOUNT` | 최소 주문 금액 미달입니다. | KRW 5,000 / USDT 5 미만 (`trading/domain/vo/OrderAmountPolicy.java:11-13`) |
| 400 | `ABOVE_MAX_ORDER_AMOUNT` | 최대 주문 금액을 초과했습니다. | KRW 1,000,000,000 초과 (USDT 상한 없음) |
| 400 | `VOLUME_REQUIRED` | 주문 수량을 입력해야 합니다. | `trading/domain/vo/OrderInput.java` |
| 400 | `PRICE_REQUIRED` | 주문 가격을 입력해야 합니다. | 〃 |
| 400 | `VOLUME_NOT_ALLOWED` | 이 주문에는 수량을 지정할 수 없습니다. | 시장가 매수에 `volume` 전송 |
| 400 | `PRICE_NOT_ALLOWED` | 이 주문에는 가격을 지정할 수 없습니다. | 시장가 매도에 `price` 전송 |
| 400 | `UNSUPPORTED_BASE_CURRENCY` | 지원하지 않는 기준 통화입니다. | |
| 400 | `ORDER_NOT_CANCELLABLE` | 취소할 수 없는 주문입니다. | 체결·취소 완료 주문 취소 시도 |
| 400 | `ORDER_NOT_FILLABLE` | 체결할 수 없는 주문 상태입니다. | |
| 400 | `INVALID_FILL_PRICE` | 지정가 조건을 만족하지 않는 체결가입니다. | |
| 400 | `BELOW_MIN_WITHDRAWAL` | 최소 출금 수량 미달입니다. | |
| 400 | `SAME_WALLET_TRANSFER` | 같은 지갑으로는 송금할 수 없습니다. | 송금 |
| 400 | `DIFFERENT_ROUND_TRANSFER` | 같은 투자 라운드의 지갑 간에만 송금할 수 있습니다. | 송금 |
| 400 | `INVALID_SEED_AMOUNT` | 거래소별 시드머니 범위를 확인해주세요. | `SeedAmountPolicy.java:6-8,24-29` |
| 400 | `DUPLICATE_EXCHANGE` | 중복된 거래소입니다. | 라운드 생성 `seeds` |
| 400 | `INVALID_EMERGENCY_FUNDING_LIMIT` | 긴급 자금 상한은 0원 이상 100만원 이하여야 합니다. | `EmergencyFundingAllowance.java:10,14-15` |
| 400 | `INVALID_RULE_THRESHOLD` | 투자 원칙 기준값이 유효하지 않습니다. | `Rule.java:45-55` |
| 400 | `DUPLICATE_RULE_TYPE` | **(messages.properties 에 키 누락 → 실제로는 500 으로 응답됨. R8 참조)** | 라운드 생성 `rules` |
| 400 | `EMERGENCY_FUNDING_DISABLED` | 긴급 자금 충전이 비활성화된 상태입니다. | 상한이 0 |
| 400 | `EMERGENCY_FUNDING_CHANCE_EXHAUSTED` | 긴급 자금 충전 가능 횟수를 모두 사용했습니다. | 남은 횟수 0 |
| 400 | `INVALID_EMERGENCY_FUNDING_AMOUNT` | 긴급 자금 금액은 0 초과, 1회 한도 이하여야 합니다. | `EmergencyFundingAllowance.java:24-33` |
| 400 | `INVALID_RANKING_PERIOD` | 유효하지 않은 랭킹 기간입니다. | |
| 400 | `NICKNAME_SAME_AS_CURRENT` | 현재 닉네임과 동일합니다. | |
| 400 | `INVALID_NICKNAME_LENGTH` | 닉네임은 2자 이상 20자 이하여야 합니다. | `user/domain/vo/Nickname.java:8-12` |
| 400 | `INVALID_FEEDBACK_LENGTH` | 피드백 본문은 20자 이상 1000자 이하여야 합니다. | `user/domain/vo/FeedbackContent.java:8-13` |
| 400 | `INVALID_PROVIDER` | 지원하지 않는 소셜 제공자입니다. | `Provider.from()` |
| 400 | `INVALID_CANDLE_INTERVAL` | 지원하지 않는 캔들 주기입니다. | 캔들 조회 |
| 400 | `INVALID_EXCHANGE_NAME` | 유효하지 않은 거래소 이름입니다. | 캔들 조회 |
| 400 | `INVALID_COIN_SYMBOL` | 유효하지 않은 코인 심볼입니다. | 캔들 조회 |
| 401 | `UNAUTHENTICATED` | 로그인이 필요합니다. | 세션 쿠키 부재·만료 |
| 401 | `SOCIAL_LOGIN_FAILED` | 소셜 로그인에 실패했습니다. | 토큰 교환 실패 |
| 403 | `PORTFOLIO_VIEW_NOT_ALLOWED` | 100위 이내 유저의 포트폴리오만 열람할 수 있습니다. | `ranking/domain/vo/RankingSummary.java:9-12` |
| 403 | `WALLET_NOT_OWNED` | 해당 지갑의 소유자가 아닙니다. | |
| 403 | `WALLET_ACCESS_DENIED` | 해당 지갑에 대한 접근 권한이 없습니다. | |
| 403 | `ROUND_ACCESS_DENIED` | 해당 라운드에 접근할 수 없습니다. | |
| 403 | `SIGNUP_RESTRICTED` | 탈퇴 후 재가입 제한 기간이 지나지 않아 가입할 수 없습니다. | 탈퇴 후 30일 이내 (`User.java:14,67-71`) |
| 404 | `WALLET_NOT_FOUND` | 지갑을 찾을 수 없습니다. | |
| 404 | `WALLET_BALANCE_NOT_FOUND` | 지갑 잔고를 찾을 수 없습니다. | |
| 404 | `EXCHANGE_COIN_NOT_FOUND` | 거래소-코인을 찾을 수 없습니다. | |
| 404 | `EXCHANGE_NOT_FOUND` | 거래소를 찾을 수 없습니다. | |
| 404 | `COIN_NOT_FOUND` | 코인을 찾을 수 없습니다. | |
| 404 | `ORDER_NOT_FOUND` | 주문을 찾을 수 없습니다. | |
| 404 | `RANKING_NOT_FOUND` | 해당 기간의 랭킹 데이터가 없습니다. | 집계 전 포트폴리오 조회 |
| 404 | `ROUND_NOT_FOUND` | 투자 라운드를 찾을 수 없습니다. | |
| 404 | `USER_NOT_FOUND` | 유저를 찾을 수 없습니다. | |
| 404 | `SOCIAL_ACCOUNT_NOT_FOUND` | 소셜 계정을 찾을 수 없습니다. | |
| 409 | `CONCURRENT_MODIFICATION` | 다른 요청과 동시에 처리되어 충돌이 발생했습니다. 다시 시도해주세요. | 낙관적 락 |
| 409 | `DUPLICATE_REQUEST` | 이미 처리된 요청입니다. 잠시 후 다시 시도해주세요. | 멱등키 중복. **주문·송금·긴급충전에서는 서버가 자체 복구하므로 정상 응답으로 바뀌는 경우가 많다** |
| 409 | `ACTIVE_ROUND_EXISTS` | 이미 진행 중인 라운드가 존재합니다. | 라운드 생성 |
| 409 | `ROUND_NOT_ACTIVE` | 진행 중인 라운드가 없습니다. | `GET /api/rounds/active` (`GetActiveRoundService.java:32`) |
| 409 | `NICKNAME_ALREADY_EXISTS` | 이미 사용 중인 닉네임입니다. | |
| 409 | `USER_ALREADY_DELETED` | 이미 탈퇴한 회원입니다. | |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류가 발생했습니다. | |
| 500 | `PRICE_NOT_AVAILABLE` | 현재가 조회에 실패했습니다. | 시세 미수집 코인 |
| 502 | `SOCIAL_SERVER_ERROR` | 소셜 서버 오류로 로그인에 실패했습니다. | |

---

### 1.4 인증과 세션

- 인증 수단은 **HttpOnly 세션 쿠키 하나뿐**이다. 토큰 헤더(Authorization)는 어디에도 없다. `AuthInterceptor.java:36` 이 `WebUtils.getCookie(request, "SESSION")` 만 읽는다.
- 인증 강제는 Spring MVC `HandlerInterceptor` 가 `/api/**` 전체에 걸고, 아래 패턴만 예외로 둔다 (`common/config/WebConfig.java:18-20`).

```java
private static final String[] PUBLIC_PATTERNS = {
    "/api/auth/**", "/api/candles", "/api/exchanges/*/coins", "/api/rankings", "/api/rankings/stats",
};
```

| 공개(비인증) 경로 |
|---|
| `/api/auth/**` |
| `/api/candles` |
| `/api/exchanges/*/coins` |
| `/api/rankings` (정확히 이 경로만) |
| `/api/rankings/stats` |

`/api/rankings` 는 정확 일치 패턴이므로 `/api/rankings/me` 와 `/api/rankings/{userId}/portfolio` 는 **인증이 필요하다.** 그 외 `/api/**` 는 전부 인증 필수이며, 쿠키가 없거나 세션이 만료되면 **401 `UNAUTHENTICATED`** 봉투가 내려온다 (`AuthInterceptor.java:23-33`).

#### 세션 쿠키 사양 (단일 정의 — 이후 절은 이 표를 참조한다)

`user/adapter/in/web/SessionCookieFactory.java:11-38`, `common/config/SessionProperties.java`

| 속성 | 값 |
|---|---|
| 이름 | `SESSION` |
| 값 | `UUID.randomUUID().toString()` (`RedisSessionCommandAdapter.java:50`) |
| `HttpOnly` | 항상 `true` |
| `Secure` | `app.auth.session.secure`(`SESSION_COOKIE_SECURE`) — 로컬 `false`, **운영 `true`** (`docker-compose.prod.yml:160`) |
| `SameSite` | `Lax` (하드코딩 상수, 설정으로 변경 불가) |
| `Path` | `/` |
| `Max-Age` | `app.auth.session.ttl` = 기본 **7일**(604800초) (`SessionProperties.java:16`) |

로그아웃·탈퇴 시에는 같은 이름·경로로 빈 값 + `Max-Age=0` 쿠키를 내려 만료시킨다 (`SessionCookieFactory.java:24-26`).

#### 세션 저장소 (Redis)

`user/adapter/out/persistence/RedisSessionCommandAdapter.java`

- `session:{sessionId}` → `userId` 문자열, `EX = ttl`.
- `user-sessions:{userId}` → 세션 ID 집합(SET), 같은 TTL. 탈퇴 시 전 기기 세션을 한 번에 지우는 데 쓴다 (`DELETE_ALL_SCRIPT`, `DeleteAccountService.java:41`).
- **슬라이딩 만료**: 인증된 요청이 들어올 때마다 `findUserId()` 가 두 키의 TTL 을 다시 7일로 늘린다 (`RedisSessionCommandAdapter.java:71-79`).

> **중요한 비대칭 (모바일 설계에 직결)**: Redis TTL 은 슬라이딩이지만 **쿠키의 `Max-Age` 는 갱신되지 않는다.** `SessionCookieFactory.issue()` 는 로그인 시점에만 호출된다(`AuthController.java:36-40`). 브라우저 쿠키 저장소를 그대로 흉내 내면 **로그인 후 정확히 7일 뒤 클라이언트가 쿠키를 버린다.** 대응책은 R2 및 §2.5 를 참조한다.

부수 사항: 세션 ID 가 Redis 에만 있으므로 Redis 재시작 시 전 사용자가 로그아웃된다(앱은 401 을 받고 재로그인). `DELETE /api/users/me` 는 `deleteAllOf(userId)` 로 해당 사용자의 모든 세션을 지우므로 웹과 앱을 동시에 로그인해 둔 경우 양쪽이 함께 끊긴다.

#### 그 외

- 인증된 요청의 `userId` 는 `@LoginUser` 아규먼트 리졸버가 세션에서 주입한다 (`common/web/auth/LoginUserArgumentResolver.java`). **클라이언트가 보내는 `userId` 는 서버가 신뢰하지도, 사용하지도 않는다.** (§1.9 참조)
- **서버에 CORS 설정이 전혀 없다.** 웹은 Vite dev proxy / nginx 로 동일 출처를 만들어 우회한다 (`frontend/vite.config.ts:17-25`, `frontend/nginx.conf:17-23`). Flutter 네이티브 앱은 CORS 무관하지만, **브라우저와 달리 쿠키가 자동 저장·전송되지 않으므로 쿠키 저장소를 직접 붙여야 한다.**
- 세션 복원: 앱 부팅 시 `GET /api/users/me` 를 호출해 성공하면 로그인 상태, 실패하면 비로그인으로 간주한다 (`frontend/src/contexts/AuthProvider.tsx:13-29`).

---

### 1.5 엔드포인트 인덱스

인증 필요 여부는 §1.4 의 `PUBLIC_PATTERNS` 로 판정하였다. 상세 계약은 §1.6 에 이어진다.

| # | 메서드 | 경로 | 요청 DTO | 응답 DTO (`data`) | 인증 | 웹 사용 |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/auth/{provider}/login` | `LoginRequest{code, codeVerifier}` (body) | `LoginResponse{userId, nickname, newUser}` + `Set-Cookie: SESSION` | 불필요 | O |
| 2 | POST | `/api/auth/logout` | 없음 (쿠키 `SESSION`) | `null` + `Set-Cookie: SESSION=;Max-Age=0` | 불필요 | O |
| 3 | GET | `/api/users/me` | 없음 | `UserProfileResponse{userId, nickname, createdAt}` | 필요 | O |
| 4 | PUT | `/api/users/me/nickname` | `ChangeNicknameRequest{nickname}` (body) | `ChangeNicknameResponse{userId, nickname}` | 필요 | O |
| 5 | DELETE | `/api/users/me` | 없음 | `null` + 세션 쿠키 만료 | 필요 | **X (미사용)** |
| 6 | POST | `/api/feedbacks` | `SendFeedbackRequest{content}` (body) | `null` (201, `code: SUCCESS`) | 필요 | O |
| 7 | POST | `/api/rounds` | `StartRoundRequest{seeds[], emergencyFundingLimit, rules[]}` (body) | `StartRoundResponse` (201, `code: CREATED`) | 필요 | O |
| 8 | POST | `/api/rounds/{roundId}/end` | 없음 | `EndRoundResponse{roundId, status, endedAt}` | 필요 | O |
| 9 | GET | `/api/rounds/active` | 없음 | `GetActiveRoundResponse` | 필요 | O |
| 10 | GET | `/api/rounds/summary` | 없음 | `RoundSummaryResponse{totalRoundCount}` | 필요 | O |
| 11 | POST | `/api/rounds/{roundId}/emergency-funding` | `ChargeEmergencyFundingRequest{exchangeId, amount, idempotencyKey}` (body) | `ChargeEmergencyFundingResponse{roundId, exchangeId, chargedAmount, remainingChargeCount}` | 필요 | O |
| 12 | GET | `/api/rounds/{roundId}/regret` | `GetRegretReportRequest{exchangeId}` (query) | `RegretReportResponse` | 필요 | O |
| 13 | GET | `/api/rounds/{roundId}/regret/chart` | `GetRegretChartRequest{exchangeId}` (query) | `RegretChartResponse` | 필요 | O |
| 14 | POST | `/api/orders` | `PlaceOrderRequest{clientOrderId, walletId, exchangeCoinId, side, orderType, volume, price}` (body) | `PlaceOrderResponse` (201, `code: CREATED`) | 필요 | O |
| 15 | GET | `/api/orders/available` | `GetOrderAvailabilityRequest{walletId, exchangeCoinId, side}` (query) | `OrderAvailabilityResponse{available, currentPrice}` | 필요 | O |
| 16 | GET | `/api/orders` | `FindOrderHistoryRequest{walletId, exchangeCoinId?, side?, status?, cursorOrderId?, size=20}` (query) | `CursorPageResponseDto<OrderHistoryResponse>` | 필요 | O |
| 17 | POST | `/api/orders/{orderId}/cancel` | `CancelOrderRequest{walletId}` (body) | `CancelOrderResponse{orderId, status}` | 필요 | O |
| 18 | POST | `/api/transfers` | `TransferCoinRequest{idempotencyKey, fromWalletId, toWalletId, coinId, amount}` (body) | `TransferCoinResponse{transferId, status}` (201, `code: CREATED`) | 필요 | O |
| 19 | GET | `/api/wallets/{walletId}/balances` | 없음 | `WalletBalancesResponse` | 필요 | O |
| 20 | GET | `/api/wallets/{walletId}/transfers` | `FindTransferHistoryRequest{type?, cursorTransferId?, size?}` (query) | `CursorPageResponseDto<TransferHistoryResponse>` | 필요 | O |
| 21 | GET | `/api/wallets/{walletId}/portfolio` | 없음 | `MyHoldingsResponse` | 필요 | O |
| 22 | GET | `/api/rankings` | `GetRankingsRequest{period, referenceDate?, cursorRank?, size=20}` (query) | `CursorPageResponseDto<RankingItemResponse>` | 불필요 | O |
| 23 | GET | `/api/rankings/me` | `GetMyRankingRequest{period}` (query) | `MyRankingResponse` (랭킹 없으면 `data: null`) | 필요 | O |
| 24 | GET | `/api/rankings/stats` | `GetRankingStatsRequest{period}` (query) | `RankingStatsResponse{totalParticipants, maxProfitRate, avgProfitRate}` | 불필요 | O |
| 25 | GET | `/api/rankings/{userId}/portfolio` | `GetRankerPortfolioRequest{period}` (query) | `RankerPortfolioResponse` | 필요 | O |
| 26 | GET | `/api/candles` | `FindCandlesRequest{exchange, coin, interval, limit(1~200)?, cursor?}` (query) | `List<CandleResponse{time, open, high, low, close}>` | 불필요 | O |
| 27 | GET | `/api/exchanges/{exchangeId}/coins` | 없음 | `List<ExchangeCoinResponse{exchangeCoinId, coinId, coinSymbol, coinName, price, changeRate, volume}>` | 불필요 | O |

**웹이 호출하지 않는 엔드포인트는 `DELETE /api/users/me` 하나뿐이다.** 서버에는 구현되어 있으며(`UserController.java:48-54`) 회원 탈퇴 + 전 세션 삭제 + 쿠키 만료를 수행한다. iOS 배포 시에는 반드시 붙여야 한다(R11).

**모바일에서 필요하지만 서버에 없는 것**: 거래소 목록 조회 엔드포인트(R7).

---

### 1.6 엔드포인트 상세

베이스 URL 은 `VITE_API_BASE_URL`(기본 빈 문자열 = 동일 출처)이다 (`client.ts:8`). 쿼리 문자열 직렬화 시 `undefined` / `null` / `""` 인 값은 **키 자체를 생략한다** (`client.ts:11-24`).

#### 1.6.1 인증 (AuthController)

| 메서드 | 경로 | 인증 | 파라미터 | 요청 바디 | 응답 `data` |
|---|---|---|---|---|---|
| POST | `/api/auth/{provider}/login` | 불필요 | path `provider`: `kakao` \| `google` (대소문자 무시, 그 외 400 `INVALID_PROVIDER`) | `code: string` (필수), `codeVerifier: string` (필수) | `{ userId: number, nickname: string, newUser: boolean }` |
| POST | `/api/auth/logout` | 불필요(쿠키 있으면 사용) | — | 없음 | `null` |

로그인 성공 시 `Set-Cookie: SESSION=...` 이 함께 내려온다 (`AuthController.java:32-41`). 로그아웃은 세션이 없어도 성공하며 만료 쿠키를 내린다 (`:43-52`). 프론트 호출부는 `frontend/src/lib/api/auth-api.ts:14-29`.

#### 1.6.2 회원 (UserController)

| 메서드 | 경로 | 인증 | 요청 바디 | 응답 `data` |
|---|---|---|---|---|
| GET | `/api/users/me` | 필요 | — | `{ userId: number, nickname: string, createdAt: LocalDateTime }` |
| PUT | `/api/users/me/nickname` | 필요 | `{ nickname: string }` (공백 불가, 2~20자) | `{ userId: number, nickname: string }` |
| DELETE | `/api/users/me` | 필요 | — | `null` (만료 쿠키 동봉) |

서버 DTO 는 `user/adapter/in/dto/response/UserProfileResponse.java` 로 **`email` 필드가 없다.** 프론트 타입(`user-api.ts:5`)에만 선언되어 있고 실제로는 항상 `undefined` 이며 화면 어디서도 쓰이지 않는다. 수집 개인정보는 제공자 회원번호뿐이며 이메일은 수집하지 않는다. **Flutter 모델에서는 제거한다.**

#### 1.6.3 피드백 (FeedbackController)

| 메서드 | 경로 | 인증 | 요청 바디 | 응답 |
|---|---|---|---|---|
| POST | `/api/feedbacks` | 필요 | `{ content: string }` (20~1000자, 위반 시 400 `INVALID_FEEDBACK_LENGTH`) | HTTP 201 / `code: "SUCCESS"` / `data: null` |

#### 1.6.4 투자 라운드 (RoundController)

| 메서드 | 경로 | 인증 | 파라미터 | 요청 바디 | 응답 `data` |
|---|---|---|---|---|---|
| POST | `/api/rounds` | 필요 | — | `StartRoundRequest` (아래) | HTTP 201 / `code: "CREATED"` / `StartRoundResponse` |
| POST | `/api/rounds/{roundId}/end` | 필요 | path `roundId: long` | **없음(서버가 바디를 읽지 않음)** | `{ roundId, status, endedAt }` |
| GET | `/api/rounds/active` | 필요 | — | — | `GetActiveRoundResponse` / 없으면 409 `ROUND_NOT_ACTIVE` |
| GET | `/api/rounds/summary` | 필요 | — | — | `{ totalRoundCount: long }` |
| POST | `/api/rounds/{roundId}/emergency-funding` | 필요 | path `roundId: long` | `{ exchangeId: long, amount: BigDecimal(>0), idempotencyKey: UUID }` | `{ roundId, exchangeId, chargedAmount, remainingChargeCount }` |

**StartRoundRequest** (`investmentround/adapter/in/dto/request/StartRoundRequest.java`)

| 필드 | 타입 | 제약 |
|---|---|---|
| `seeds` | `[{ exchangeId: long, amount: BigDecimal }]` | `@NotEmpty`. `amount >= 0`(`@DecimalMin("0")`, 상한 없음). 거래소 중복 금지(400 `DUPLICATE_EXCHANGE`) |
| `emergencyFundingLimit` | BigDecimal | `0 <= x <= 1,000,000` (`EmergencyFundingAllowance.java:10,14-15`) |
| `rules` | `[{ ruleType: RuleType, thresholdValue: BigDecimal }]` | null 허용(빈 목록으로 처리). 타입 중복 금지(400 `DUPLICATE_RULE_TYPE`) |

시드 금액 정책(`investmentround/domain/vo/SeedAmountPolicy.java:6-8,24-29`): 0 은 "배정 안 함"으로 허용되고, 0 초과일 때 국내(UPBIT·BITHUMB)는 1,000,000 ~ 50,000,000, 해외(BINANCE)는 100 ~ 50,000 범위여야 한다. 위반 시 400 `INVALID_SEED_AMOUNT`.

원칙 기준값(`investmentround/domain/model/Rule.java:45-55`): 비율형(`LOSS_CUT`, `PROFIT_TAKE`, `CHASE_BUY_BAN`)은 0 초과 실수, 횟수형(`AVERAGING_DOWN_LIMIT`, `OVERTRADING_LIMIT`)은 1 이상 **정수**여야 한다(소수부 존재 시 400 `INVALID_RULE_THRESHOLD`).

긴급 자금 충전 횟수는 라운드 생성 시 3회로 고정 부여된다(`EmergencyFundingAllowance.java:11`). 1회 충전액은 `0 < amount <= emergencyFundingLimit`.

**StartRoundResponse**: `roundId, roundNumber, status, initialSeed, emergencyFundingLimit, emergencyChargeCount, rules[{ruleId, ruleType, thresholdValue}], wallets[{walletId, exchangeId}], startedAt` — **`userId` 와 `endedAt` 이 없다.**

**GetActiveRoundResponse**: `roundId, userId, roundNumber, status, initialSeed, emergencyFundingLimit, emergencyChargeCount, startedAt, endedAt, rules[], wallets[]` — 이쪽에는 `userId`, `endedAt` 이 있다. 두 응답의 모양이 다르므로 Flutter 에서는 별도 모델 두 개로 두거나, 공통 모델의 `userId`/`endedAt` 을 nullable 로 선언한다.

**`walletId` 는 이 응답에서만 얻는다.** 다른 모든 지갑 API(`/api/wallets/{walletId}/...`)의 경로 변수는 여기서 나온 값이다.

`status`: `ACTIVE` \| `BANKRUPT` \| `ENDED` (`investmentround/domain/vo/RoundStatus.java`).

#### 1.6.5 주문 (OrderController)

| 메서드 | 경로 | 인증 | 파라미터 | 요청 바디 | 응답 `data` |
|---|---|---|---|---|---|
| POST | `/api/orders` | 필요 | — | `PlaceOrderRequest` | HTTP 201 / `code: "CREATED"` / `PlaceOrderResponse` |
| GET | `/api/orders/available` | 필요 | `walletId: long`(필수), `exchangeCoinId: long`(필수), `side: BUY\|SELL`(필수) | — | `{ available: BigDecimal, currentPrice: BigDecimal }` |
| GET | `/api/orders` | 필요 | `walletId`(필수), `exchangeCoinId?`, `side?`, `status?`, `cursorOrderId?: long`, `size?: 1~50 (기본 20)` | — | `CursorPageResponseDto<OrderHistoryResponse>` |
| POST | `/api/orders/{orderId}/cancel` | 필요 | path `orderId: long` | `{ walletId: long }` | `{ orderId, status }` |

**PlaceOrderRequest** (`trading/adapter/in/dto/request/PlaceOrderRequest.java`)

| 필드 | 타입 | 제약 |
|---|---|---|
| `clientOrderId` | string | 필수·공백 불가. 멱등키. 프론트는 `crypto.randomUUID()` 사용 (`OrderPanel.tsx:66-72`). UUID 형식 강제는 아님 |
| `walletId` | long | 필수 |
| `exchangeCoinId` | long | 필수 |
| `side` | `BUY` \| `SELL` | 필수 |
| `orderType` | `MARKET` \| `LIMIT` | 필수 |
| `volume` | BigDecimal | 선택. 양수여야 함(`@Positive`) |
| `price` | BigDecimal | 선택. 양수여야 함(`@Positive`) |

**`volume` / `price` 의 의미는 주문 모드마다 다르다** (`trading/domain/vo/OrderMode.java:7-115`). 이 규칙을 어기면 400 이다.

| 모드 | `volume` | `price` | 위반 시 |
|---|---|---|---|
| MARKET + BUY | **보내면 안 됨** | **필수 — 총 주문 금액(견적 통화)** | `VOLUME_NOT_ALLOWED` / `PRICE_REQUIRED` |
| MARKET + SELL | **필수 — 매도 수량** | **보내면 안 됨** | `VOLUME_REQUIRED` / `PRICE_NOT_ALLOWED` |
| LIMIT + BUY/SELL | 필수 — 수량 | 필수 — 지정가 | `VOLUME_REQUIRED` / `PRICE_REQUIRED` |

프론트 구현도 동일하다 (`OrderPanel.tsx:350-351`): `volume: isMarketBuy ? undefined : parsedQuantity`, `price: 지정가면 지정가, 시장가매수면 금액, 그 외 undefined`.

**PlaceOrderResponse**: `orderId: long, side, orderType, orderAmount: BigDecimal|null, quantity: BigDecimal, price: BigDecimal|null(지정가), filledPrice: BigDecimal|null, fee: BigDecimal|null, status: FILLED|PENDING|CANCELED|FAILED, createdAt, filledAt|null`.

시장가 주문은 즉시 체결되어 `FILLED`, 지정가는 `PENDING` 으로 등록된다(메시지가 각각 "주문이 체결되었습니다." / "주문이 등록되었습니다."). `clientOrderId` 중복 시 서버는 기존 주문을 다시 조회해 정상 201 로 응답한다 (`OrderController.java:47-57`).

**OrderHistoryResponse**: `orderId, exchangeCoinId, side, orderType, filledPrice|null, price|null, quantity, orderAmount, fee, createdAt, filledAt|null` — **`status` 필드가 없다.** (§1.9 참조)

`available` 의 의미(`trading/application/service/GetOrderAvailabilityService.java:28-31`): `BUY` 이면 견적 통화(KRW/USDT) 사용 가능 잔고, `SELL` 이면 해당 코인의 사용 가능 수량이다(잠금분 제외).

수수료율은 서버 설정 고정값이다(`api/src/main/resources/application.yml`): UPBIT 0.0005, BITHUMB 0.0025, BINANCE 0.001.

#### 1.6.6 지갑·송금 (WalletBalanceController / PortfolioController / TransferController / TransferHistoryController)

| 메서드 | 경로 | 인증 | 파라미터 | 요청 바디 | 응답 `data` |
|---|---|---|---|---|---|
| GET | `/api/wallets/{walletId}/balances` | 필요 | path `walletId` | — | `{ exchangeId, baseCurrencySymbol, baseCurrencyAvailable, baseCurrencyLocked, balances: [{ coinId, available, locked }] }` |
| GET | `/api/wallets/{walletId}/portfolio` | 필요 | path `walletId` | — | `{ exchangeId, baseCurrencyBalance, baseCurrencySymbol, holdings: [{ coinId, coinSymbol, coinName, quantity, avgBuyPrice, currentPrice }] }` |
| GET | `/api/wallets/{walletId}/transfers` | 필요 | path `walletId`; query `type?: ALL\|DEPOSIT\|WITHDRAW`, `cursorTransferId?: long`, `size?: 1~50 (기본 20)` | — | `CursorPageResponseDto<TransferHistoryResponse>` |
| POST | `/api/transfers` | 필요 | — | `{ idempotencyKey: UUID, fromWalletId: long, toWalletId: long, coinId: long, amount: BigDecimal(>0) }` | HTTP 201 / `code: "CREATED"` / `{ transferId: long, status: "SUCCESS" }` |

`TransferHistoryResponse`: `transferId, type: DEPOSIT|WITHDRAW, coinId, coinSymbol, amount, status: SUCCESS, createdAt, completedAt|null`.

`TransferStatus` enum 은 값이 `SUCCESS` 하나뿐이다(`wallet/domain/vo/TransferStatus.java`). 송금은 **동기·즉시 완료** 처리되며 `completedAt = createdAt` 이다. 대기/처리중 같은 중간 상태는 존재하지 않는다. `TransferType` 은 `ALL`, `DEPOSIT`, `WITHDRAW` 세 값이며 조회 필터 미지정 시 서버가 `ALL` 로 채운다(`FindTransferHistoryQuery.java:8-11`).

`type` 은 **조회자 지갑 기준**으로 서버가 계산한다: `fromWalletId == 조회 walletId` 이면 `WITHDRAW`, 아니면 `DEPOSIT`(`Transfer.java:40-42`). 응답에는 상대 지갑/거래소 정보가 없다.

송금 제약: 같은 지갑 간 금지(`SAME_WALLET_TRANSFER`), 서로 다른 라운드 지갑 간 금지(`DIFFERENT_ROUND_TRANSFER`), 출발 지갑 소유자 검증(`WALLET_NOT_OWNED`/`WALLET_ACCESS_DENIED`), 가용 잔고 부족 시 `INSUFFICIENT_BALANCE`. 멱등키가 중복이면 서버가 기존 `transferId` 를 `status: SUCCESS` 로 되돌려 준다(`TransferController.java:36-42`).

#### 1.6.7 랭킹 (RankingController)

| 메서드 | 경로 | 인증 | 파라미터 | 응답 `data` |
|---|---|---|---|---|
| GET | `/api/rankings` | **불필요** | `period: DAILY\|WEEKLY\|MONTHLY`(필수), `referenceDate?: yyyy-MM-dd`, `cursorRank?: int(>=1)`, `size?: 1~50 (기본 20)` | `CursorPageResponseDto<{ rank: int, userId: long, nickname: string, profitRate: BigDecimal, tradeCount: int }>` |
| GET | `/api/rankings/me` | 필요 | `period`(필수) | `{ rank, nickname, profitRate, tradeCount }` 또는 **`null`** |
| GET | `/api/rankings/stats` | **불필요** | `period`(필수) | `{ totalParticipants: long, maxProfitRate: BigDecimal, avgProfitRate: BigDecimal }` |
| GET | `/api/rankings/{userId}/portfolio` | 필요 | path `userId: long`; query `period`(필수) | `{ userId, nickname, rank, profitRate, holdings: [{ coinSymbol, exchangeName, assetRatio, profitRate }] }` |

`/api/rankings/me` 는 **성공(`code: SUCCESS`)이면서 `data` 가 `null`** 일 수 있다(랭킹 미집계 사용자). Flutter 모델은 nullable 로 받아야 한다 (`RankingController.java:51-57`, `ranking-api.ts:69-84`).

`/api/rankings/{userId}/portfolio` 는 랭킹 집계 자체가 없으면 404 `RANKING_NOT_FOUND`, 101위 이하이면 403 `PORTFOLIO_VIEW_NOT_ALLOWED` 를 던진다.

정렬은 서버가 `rank` 오름차순으로 확정해 내려주며, 클라이언트가 지정할 정렬 파라미터는 존재하지 않는다. `period` 는 집계 주기가 아니라 **수익률 산출 구간(window)** 이다 — DAILY=1일 전 대비, WEEKLY=7일 전 대비, MONTHLY=30일 전 대비이며 세 가지 모두 매일 갱신된다(`api/docs/ranking/business-rules.md:5-26`).

`referenceDate` 쿼리 파라미터는 `@DateTimeFormat(pattern = "yyyy-MM-dd")` 이므로 **반드시 `yyyy-MM-dd` 문자열**로 보낸다(`GetRankingsRequest.java`).

#### 1.6.8 투자 복기 (RegretController)

| 메서드 | 경로 | 인증 | 파라미터 | 응답 `data` |
|---|---|---|---|---|
| GET | `/api/rounds/{roundId}/regret` | 필요 | path `roundId`; query `exchangeId: long`(필수) | `RegretReportResponse` |
| GET | `/api/rounds/{roundId}/regret/chart` | 필요 | path `roundId`; query `exchangeId: long`(필수) | `RegretChartResponse` |

두 요청 모두 세션 인증이 필요하고, 라운드 소유자가 아니면 서버가 거부한다(`round.validateOwnedBy(userId)`). 해당 라운드에 그 거래소 지갑이 없으면 `WALLET_NOT_FOUND`(`GetRegretReportService.java:34-36`).

**RegretReportResponse** (`regretanalysis/adapter/in/dto/response/RegretReportResponse.java:9-88`)

| 필드 | 타입 | 웹 프론트 사용 여부 |
|---|---|---|
| `reportId`, `roundId`, `exchangeId`, `exchangeName`, `currency`, `analysisStart`, `analysisEnd` | long / string / LocalDate | 미사용 |
| `totalViolations` | int | 사용 |
| `missedProfit`, `actualProfitRate`, `ruleFollowedProfitRate` | BigDecimal | 사용 |
| `ruleImpacts[]` | `{ ruleImpactId, ruleId, ruleType: string, thresholdValue, thresholdUnit: string, violationCount: int, totalLossAmount, impactGap }` | `ruleType`, `thresholdValue`, `violationCount` 만 사용 |
| `violationDetails[]` | `{ violationDetailId, orderId, coinSymbol, violatedRules: string[], profitLoss, occurredAt: LocalDateTime }` | 전부 사용 |

`ruleType` 과 `violatedRules` 는 **enum 이름 문자열**(`LOSS_CUT` 등)로 직렬화된다. `exchangeName`·`currency`·`analysisStart/End`·`totalLossAmount`·`impactGap` 은 웹이 쓰지 않으므로, Flutter 에서는 이를 노출해 정보량을 늘릴 수 있다(§6 참조).

**RegretChartResponse** (`RegretChartResponse.java:8-31`): `roundId, exchangeId, exchangeName, currency, totalDays: int, assetHistory: [{ snapshotDate: yyyy-MM-dd, actualAsset, ruleFollowedAsset, btcHoldAsset }], violationMarkers: [{ snapshotDate, assetValue }]`. 프론트는 서버가 주는 `totalDays` 를 무시하고 `assetHistory.length` 로 다시 계산한다(`regret-api.ts:142`).

리포트는 **야간 배치 산출물**이다. 배치 전에는 서버가 0으로 채운 빈 리포트를 200 으로 반환한다(`GetRegretReportService.java:40-42`, `RegretReport.empty(...)`).

#### 1.6.9 시세 메타·캔들 (ExchangeCoinController / CandleController)

| 메서드 | 경로 | 인증 | 파라미터 | 응답 `data` |
|---|---|---|---|---|
| GET | `/api/exchanges/{exchangeId}/coins` | **불필요** | path `exchangeId: long` | `[{ exchangeCoinId, coinId, coinSymbol, coinName, price, changeRate, volume }]` |
| GET | `/api/candles` | **불필요** | `exchange`(필수), `coin`(필수), `interval`(필수), `limit?: 1~200 (기본 60)`, `cursor?` | `[{ time: Instant, open, high, low, close }]` |

- `exchange` 는 거래소 **이름 문자열**이며 `UPBIT` / `BITHUMB` / `BINANCE` 다(`id` 가 아니다). 프론트는 `resolveCandleExchangeCode()` 로 `upbit → UPBIT` 매핑한다 (`candle-api.ts:71-79`).
- `coin` 은 **심볼만**(`BTC`) 넘긴다. 서버가 거래소의 기준 통화를 붙여 `BTC/KRW`, `BTC/USDT` 로 만든다 (`marketdata/domain/model/CandleFilter.java:16-24`).
- `exchange` / `coin` 은 `[A-Za-z0-9_-]+` 패턴만 허용된다. 위반 시 400 `INVALID_EXCHANGE_NAME` / `INVALID_COIN_SYMBOL`.
- `interval` 허용값은 `1m`, `1h`, `4h`, `1d`, `1w`, `1M` 6종이다(대소문자 구분, `1M` 은 월봉 — `marketdata/domain/model/CandleInterval.java:7-18`). 그 외 400 `INVALID_CANDLE_INTERVAL`.
- `cursor` 는 ISO-8601 `Instant` 문자열(`Instant.parse`)이다.
- 서버 응답 필드명은 `time` 이다. 프론트는 `item.time ?? item.timestamp` 로 방어 코딩되어 있으나(`candle-api.ts:98`) 실제 서버는 `time` 만 내린다.

---

### 1.7 열거형 값

| 열거형 | 값 | 정의 위치 |
|---|---|---|
| `Side` | `BUY`, `SELL` | `trading/domain/vo/Side.java` |
| `OrderType` | `MARKET`, `LIMIT` | `trading/domain/vo/OrderType.java` |
| `OrderStatus` | `FILLED`, `PENDING`, `CANCELED`, `FAILED` | `trading/domain/vo/OrderStatus.java` |
| `TransferType` | `ALL`, `DEPOSIT`, `WITHDRAW` | `wallet/domain/vo/TransferType.java` |
| `TransferStatus` | `SUCCESS` (유일) | `wallet/domain/vo/TransferStatus.java` |
| `RoundStatus` | `ACTIVE`, `BANKRUPT`, `ENDED` | `investmentround/domain/vo/RoundStatus.java` |
| `RuleType` | `LOSS_CUT`, `PROFIT_TAKE`, `CHASE_BUY_BAN`, `AVERAGING_DOWN_LIMIT`, `OVERTRADING_LIMIT` | `common/domain/vo/RuleType.java` |
| `RankingPeriod` | `DAILY`(1일), `WEEKLY`(7일), `MONTHLY`(30일) | `ranking/domain/vo/RankingPeriod.java` |
| `Provider` | `KAKAO`, `GOOGLE` (경로 변수는 대소문자 무관, `Provider.from` 이 `toUpperCase`) | `user/domain/vo/Provider.java` |
| 캔들 `interval` | `1m`, `1h`, `4h`, `1d`, `1w`, `1M` | `marketdata/domain/model/CandleInterval.java` |

`RoundStatus.BANKRUPT` 는 백엔드 운영 코드에 전이 경로가 없다(시드 데이터에만 존재: `common/seed/InvestmentRoundDataSeeder.java:118`). **표시만 지원하면 된다.**

멱등키 규약은 두 종류로 다르다. 주문은 `clientOrderId`(임의 문자열, `@NotBlank`)이고, 송금·긴급자금은 `idempotencyKey`(`UUID` 타입, `@NotNull`)이다. 중복 요청 시 서버는 예외를 던지지 않고 기존 리소스를 조회해 정상 응답을 돌려준다(`OrderController.java:48-55`, `TransferController.java:36-42`, `RoundController.java:88-93`).

---

### 1.8 숫자·날짜 타입 주의점

**서버가 금액을 문자열로 내리는 곳은 없다.** `application.yml` 에 `spring.jackson` 설정 블록이 없으므로 Jackson 기본 직렬화가 적용되며, 모든 `BigDecimal` 은 따옴표 없는 **JSON 숫자 리터럴**로 내려간다(예: `"price": 95432100.50`). 프론트가 곳곳에서 `Number(...)` 로 감싸는 것은 방어 코딩일 뿐 실제 필요 때문이 아니다.

그럼에도 Flutter 에서는 다음을 지켜야 한다.

1. **`BigDecimal` → Dart `double` 직행은 위험하다.** `jsonDecode` 는 JSON 숫자를 `int` 또는 `double` 로만 매핑한다. 코인 수량은 소수점 8자리까지 내려오므로(`0.00012345`) IEEE754 double 로는 정확히 표현되지 않는다. 잔고 차감·수수료 계산 등 **연산이 들어가는 값은 `package:decimal` 의 `Decimal.parse(value.toString())` 으로 승격**해서 다루고, 화면 표시 전용 값만 `double` 로 둔다.
2. **`int` 로 올 수 있음에 주의한다.** KRW 정수 가격은 JSON `int` 로 도착하므로 `as double` 캐스팅은 런타임 오류를 낸다. **반드시 `(json['price'] as num).toDouble()`** 로 받는다.
3. **금액 상한 확인.** KRW 최대 주문 금액이 1,000,000,000 이고 시드는 최대 50,000,000 이다. 정수부는 `2^53` 한참 아래이므로 정수 금액에서 정밀도 손실은 발생하지 않는다. 위험 구간은 오직 소수 수량이다.
4. **`Long` ID 는 Dart `int`(64bit)로 안전하다.** 단 Flutter Web 으로도 빌드한다면 `int` 가 JS number 로 컴파일되어 `2^53` 제한을 받는다.
5. **`profitRate` / `changeRate` / `assetRatio` 의 단위가 서로 다르다.** 서버는 `BigDecimal` 원시값을 내리며 백분율 변환을 하지 않는다. 화면별 규칙은 각 화면 절에서 확인한다(`changeRate` 는 비율(0.0123=1.23%), 랭킹 `profitRate` 는 퍼센트 값 그 자체(12.34=+12.34%)).

**날짜·시각 직렬화**

| Java 타입 | 와이어 형식 | 등장 위치 | Dart 파싱 |
|---|---|---|---|
| `LocalDateTime` | `2026-07-15T10:23:45.123456` (**오프셋·Z 없음**) | `createdAt`, `filledAt`, `startedAt`, `endedAt`, `completedAt`, `occurredAt`, `executedAt` | `DateTime.parse()` → `isUtc == false` 인 "로컬 취급" 값. **실제로는 서버 로컬시각(Asia/Seoul)이다** |
| `LocalDate` | `2026-07-15` | `snapshotDate`, `referenceDate`, `analysisStart`, `analysisEnd` | `DateTime.parse()` |
| `Instant` | `2026-07-15T01:23:45Z` (**Z 포함**) | 캔들 `time` | `DateTime.parse()` → `isUtc == true`. 표시 전 `.toLocal()` 필수 |

즉 **캔들만 UTC 이고 나머지는 오프셋 없는 서버 로컬시각**이다. DB 연결이 `serverTimezone=Asia/Seoul` 로 고정되어 있다(`application.yml`). 기기 타임존이 KST 가 아닌 사용자에게는 `LocalDateTime` 계열이 어긋나 보일 수 있으므로, Flutter 에서는 `LocalDateTime` 계열을 파싱한 뒤 **명시적으로 `Asia/Seoul` 로 해석**(`package:timezone`)하고 기기 타임존으로 변환해서 표시한다. 웹 프론트는 이 처리를 하지 않고 브라우저 로컬 타임존으로 그냥 해석한다.

---

### 1.9 프론트-서버 계약 불일치 목록 (이식 시 정리해야 할 항목)

아래는 웹 프론트 코드와 실제 서버 계약이 어긋난 지점이다. Flutter 에서는 **서버 계약을 기준으로** 구현한다.

| # | 항목 | 웹 프론트 | 서버 실제 | 조치 |
|---|---|---|---|---|
| 1 | `UserProfileResponse.email` | `email: string` 선언 (`user-api.ts:5`) | 필드 없음 | Flutter 모델에서 제거 |
| 2 | `POST /api/rounds` 요청 | 바디에 `userId` 포함 (`round-api.ts:41-46,106`) | `StartRoundRequest` 에 `userId` 필드 없음 → 무시됨 | 보내지 않는다 |
| 3 | `POST /api/rounds` 응답 | `mapRound` 가 `userId`, `endedAt` 을 읽음 (`round-api.ts:81-101`) | `StartRoundResponse` 에 둘 다 없음 → `undefined` | 응답 모델을 분리하고 두 필드를 nullable 로 |
| 4 | `POST /api/rounds/{id}/end` | 바디 `{userId}` 전송 (`round-api.ts:161`) | 컨트롤러에 `@RequestBody` 없음 → 바디 자체를 읽지 않음 | 빈 바디로 보낸다 |
| 5 | 긴급 충전 응답 | `chargedAt: string` 선언 (`round-api.ts:70`) | `ChargeEmergencyFundingResponse` 에 `chargedAt` 없음 | 제거. 시각이 필요하면 클라이언트가 기록 |
| 6 | 긴급 충전 요청 | `userId` 포함 (`round-api.ts:59-63`) | 요청 DTO 에 없음 → 무시 | 보내지 않는다 |
| 7 | `userId` 쿼리 파라미터 | `/api/rounds/active`, `/api/rounds/summary`, `/api/rankings/me`, `/api/wallets/{id}/transfers`, `/api/rounds/{id}/regret`, `/regret/chart` 에 `userId` 를 붙여 보냄 | 전부 `@LoginUser` 로 세션에서 얻음 → 쿼리는 무시 | 보내지 않는다 |
| 8 | 송금 내역 커서 | `cursor: string` 파라미터로 전송 (`transfer-api.ts:42,51`) | 서버는 `cursorTransferId: Long` 을 받음 → **웹의 커서 페이지네이션이 실제로 동작하지 않는다.** 또한 서버 `nextCursor` 는 `Long` 인데 프론트는 `string \| null` 로 선언 | `cursorTransferId: int` 로 보내고 `nextCursor` 를 `int?` 로 받는다 |
| 9 | 주문 내역 `status` | 응답 항목에 `status` 가 있다고 가정하고, 요청 필터값으로 덮어씀 (`order-api.ts:98`) | `OrderHistoryResponse` 에 `status` 필드 자체가 없음 | 서버 응답 모델에서 `status` 제거. 화면에 상태 표시가 필요하면 필터 탭 값을 그대로 사용 |
| 10 | 캔들 시각 필드 | `time ?? timestamp` 방어 (`candle-api.ts:98`) | `time` 만 존재 | `time` 만 파싱 |
| 11 | `DELETE /api/users/me` | 호출부 없음 | 존재함(회원 탈퇴) | iOS 배포 시 마이페이지에 추가 (R11) |
| 12 | 라운드 시드 배분 | 항상 업비트에 전액, 나머지 0 (`round-api.ts:107-111`) | `seeds` 배열로 거래소별 배분 지원 | 웹과 동일 동작을 원하면 그대로. 거래소별 배분 UI 를 넣으려면 서버는 이미 지원함 |
| 13 | STOMP 사용자 큐 | `/user/{userId}/queue/events` 구독 + `walletId`/`coinId`/`side`/`price`/`fee` 기대 | Principal 미부착으로 **메시지 폐기**. 실제 페이로드는 `eventType`/`orderId`/`executedPrice`/`quantity`/`executedAt` | R3 참조. REST 재조회로 대체 |
| 14 | 지갑 화면 코인 시세 | `currentPrice: 0` 하드코딩 (`WalletPage.tsx:103`) | `GET /api/exchanges/{id}/coins` 응답에 `price` 존재 | 그 값을 `currentPrice` 로 사용 |
| 15 | 송금 상태 | `PENDING/PROCESSING/COMPLETED/FAILED/RETURNED/DELAYED` 6종 가정 | `SUCCESS` 단일값 | 단일값으로 모델링, 상태 필터 제거 |

---

### 1.10 프론트 매퍼 변환 규칙

#### 1.10.1 열거형 이름 변환 (`frontend/src/lib/api/mappers.ts`)

프론트 도메인 용어와 서버 enum 이름이 다르다. Flutter 에서 서버 이름을 그대로 쓸지, 프론트와 같은 별칭 계층을 둘지 선택해야 한다(**서버 이름을 그대로 쓰는 편을 권장한다** — 별칭은 순수한 프론트 관례이며 이득이 없다).

| 프론트 `RuleType` | 서버 `RuleType` (와이어 값) | 기준값 단위(프론트 표시) |
|---|---|---|
| `STOP_LOSS` | `LOSS_CUT` | `%` |
| `TAKE_PROFIT` | `PROFIT_TAKE` | `%` |
| `NO_CHASE_BUY` | `CHASE_BUY_BAN` | `%` |
| `AVERAGING_LIMIT` | `AVERAGING_DOWN_LIMIT` | `회` |
| `OVERTRADE_LIMIT` | `OVERTRADING_LIMIT` | `회` |

| 프론트 `RankingPeriod` | 서버 `RankingPeriod` |
|---|---|
| `daily` | `DAILY` |
| `weekly` | `WEEKLY` |
| `monthly` | `MONTHLY` |

단위 표(`%` / `회`)는 프론트가 로컬 상수로 들고 있으며(`regret-api.ts:71-77`), 서버가 내려주는 `thresholdUnit` 을 쓰지 않는다.

#### 1.10.2 주문 대상 ID 해석 절차 (`frontend/src/lib/api/id-mapping.ts`)

주문·잔고 조회에 필요한 세 개의 ID(`exchangeId`, `walletId`, `exchangeCoinId`)는 화면의 (거래소 키, 코인 심볼) 쌍으로부터 아래 순서로 유도된다. `resolveOrderTargetIds(exchangeKey, coinSymbol, getWalletId)` (`id-mapping.ts:46-73`).

1. **exchangeId**: 하드코딩 맵으로 변환한다 (`id-mapping.ts:15-19`). `upbit → 1`, `bithumb → 2`, `binance → 3`. 이 값은 서버 시드 데이터와 일치한다(`api/src/main/resources/db/seed-data.sql:10-13`). 매핑 실패 시 `LOOKUP_FAILED`.
2. **walletId**: 활성 라운드의 `wallets` 배열에서 `exchangeId` 가 일치하는 항목의 `walletId` 를 찾는다 (`RoundProvider.tsx:71-76`). 즉 **`GET /api/rounds/active` 가 선행되어야 하며, 라운드가 없으면 주문 자체가 불가능하다.** 실패 시 `NO_ROUND`.
3. **exchangeCoinId**: `GET /api/exchanges/{exchangeId}/coins` 응답을 **거래소 단위로 메모리 캐시**(`Map<number, ExchangeCoinResponse[]>`, `id-mapping.ts:22,33-40`)한 뒤, `coinSymbol` 을 **대소문자 무시 비교**해서 매칭되는 항목의 `exchangeCoinId` 를 취한다 (`id-mapping.ts:59`). 목록에 없으면 `COIN_UNLISTED`, 조회 자체가 실패하면 `LOOKUP_FAILED`.

실패 사유는 `NO_ROUND` / `COIN_UNLISTED` / `LOOKUP_FAILED` 3종이며, 캐시는 `clearExchangeCoinsCache()` 로만 비워진다(TTL 없음).

**Flutter 구현 지침**: 거래소 코인 목록 캐시는 앱 생명주기 동안 유지하되, 라운드 생성/종료 시점과 로그아웃 시점에 비운다. `exchangeId` 하드코딩 맵은 서버 시드에 종속된 값이므로 상수 클래스 한 곳(`ExchangeIds`)에 모은다. **주문 API 는 `coinId` 가 아니라 `exchangeCoinId` 를 받고, 송금 API 는 `coinId` 를 받으므로 두 ID 를 혼동하지 않도록 주의한다.**

#### 1.10.3 응답 후처리 규칙

| 위치 | 규칙 |
|---|---|
| `order-api.ts:96-104` | 주문 내역의 `status` 를 **요청 필터값으로 덮어쓴다** (`params.status ?? "FILLED"`). 서버 응답에 `status` 가 없기 때문이다 |
| `order-api.ts:77-80, 99-103, 114-119` | 모든 금액·수량 필드를 `Number(...)` 로 감싼다 (서버가 문자열을 줄 가능성에 대한 방어) |
| `round-api.ts:81-102` | `initialSeed`, `emergencyFundingLimit`, `thresholdValue` 를 `Number(...)` 변환. `rules` / `wallets` 가 없으면 빈 배열 |
| `round-api.ts:127-137` | `/api/rounds/active` 가 `ROUND_NOT_ACTIVE` 코드로 실패하면 **에러가 아니라 `null` 로 변환**한다. **Flutter 에서도 이 분기를 반드시 재현한다** |
| `round-api.ts:104-118` | 라운드 생성 시 시드를 항상 `[{1, initialSeed}, {2, 0}, {3, 0}]` 로 만든다 — 전액 업비트 배정, 나머지 0 |
| `regret-api.ts:81-85` | 차트 X축 라벨: `totalDays <= 180` 이면 `M/D`, 아니면 `YYYY.MM` |
| `regret-api.ts:120-126` | 위반 거래 날짜 라벨은 `M/D` 고정 |
| `regret-api.ts:153-157` | 모든 위반 마커의 `type` 을 `"loss"` 로 고정한다 |
| `candle-api.ts:30-66, 96-112` | 캔들 시각을 인터벌 경계로 정규화(`1m`→초 0, `1h`→분·초 0, `4h`→4시간 내림, `1d`→자정, `1w`→그 주 월요일, `1M`→1일)한 뒤 ISO 문자열로 되돌리고, 숫자 필드가 유한하지 않은 항목을 걸러낸 다음 시간 오름차순 정렬한다 |
| `ranking-api.ts:59-115` | `profitRate`, `maxProfitRate`, `avgProfitRate`, `assetRatio` 를 `Number(...)` 변환 |

---

### 1.11 Flutter 클라이언트 구현 시 확정 사항

1. **봉투 언랩 계층.** HTTP 인터셉터 한 곳에서 `ApiResponseDto` 를 벗기고 `data` 만 상위로 넘긴다. 성공 조건은 `2xx && code ∈ {SUCCESS, CREATED}` 이다. 실패는 `status`, `code`, `message`, `data` 를 담은 `ApiException` 으로 통일한다(`ApiClientError` 와 1:1 대응).
2. **세션 유지.** R2 의 방식(세션 ID 를 `flutter_secure_storage` 에 보관 + `Cookie` 헤더 직접 부착)을 채택한다. 앱 재시작 후 `GET /api/users/me` 로 세션이 살아 있는지 확인하는 부팅 플로우(`AuthProvider.tsx:13-29`)를 그대로 옮긴다.
3. **401 전역 처리.** `code == "UNAUTHENTICATED"` 또는 HTTP 401 을 인터셉터에서 잡아 세션을 비우고 로그인 화면으로 보낸다(웹에 없는 개선).
4. **에러 메시지 표시.** 서버 `message` 는 이미 한국어 완성문이다. 별도 코드→문구 매핑 테이블을 만들 필요는 없고, `message` 를 그대로 쓰되 `message` 가 비면 `code` 로 대체한다.
5. **멱등키.** 주문은 `clientOrderId`(문자열), 송금·긴급충전은 `idempotencyKey`(**UUID 형식 필수**). 네트워크 재시도 시 **같은 키를 재사용**해야 한다. 재시도마다 키를 새로 만들면 서버의 중복 보호가 무력화된다.
6. **쿼리 파라미터 생략 규칙.** `null` / 빈 문자열 파라미터는 키 자체를 보내지 않는다(`client.ts:16-18`). Dio 의 기본 동작과 일치하지 않을 수 있으므로 명시적으로 걸러낸다.
7. **`Content-Type` 헤더는 바디가 있을 때만 붙인다** (`client.ts:50`). 바디 없는 `POST /api/auth/logout` 에는 붙지 않는다.
8. **부팅 순서.** 앱 부팅 시 활성 라운드를 먼저 조회해 `exchangeId → walletId` 맵을 캐시한다. 주문·잔고·포트폴리오·송금·복기 화면은 모두 **`GET /api/rounds/active` 로 얻은 `wallets[]` 없이는 아무 요청도 보낼 수 없다.** 라운드 상태를 앱 전역 상태(`RoundProvider` 상당)로 올려두고, 활성 라운드가 없으면(409 `ROUND_NOT_ACTIVE` → `null`) 라운드 생성 화면으로 유도한다.
9. **Base URL.** 단일 오리진. REST 는 `{base}/api/...`, STOMP 는 `wss://{도메인}/ws`. 빌드 설정(`--dart-define`)으로 주입한다.

---
## 2. 인증과 소셜 로그인

### 2.1 전체 구조 요약

| 항목 | 값 | 근거 |
|---|---|---|
| 인증 방식 | 서버 세션 + 쿠키 (Spring Security 미사용) | `common/web/auth/AuthInterceptor.java` |
| 세션 저장소 | Redis (`session:{uuid}` → `userId`) | `user/adapter/out/persistence/RedisSessionCommandAdapter.java:18-58` |
| 로그인 엔드포인트 | `POST /api/auth/{provider}/login` | `user/adapter/in/web/AuthController.java:32-41` |
| 로그아웃 엔드포인트 | `POST /api/auth/logout` | `AuthController.java:43-52` |
| 인증 복구 | `GET /api/users/me` | `frontend/src/contexts/AuthProvider.tsx:13-30` |
| 지원 제공자 | `KAKAO`, `GOOGLE` (대소문자 무시) | `user/domain/vo/Provider.java:11-17` |
| CORS 설정 | **존재하지 않음** (nginx 동일 출처 프록시로 해결) | 전역 grep 무매치, `frontend/nginx.conf:17-23` |

세션 쿠키·Redis 저장소의 상세 사양은 §1.4 를 참조한다.

인가(authorization) 시작은 **클라이언트**가, 토큰 교환은 **백엔드**가 담당한다. `client_secret` 은 백엔드에만 있고, 인가 코드가 클라이언트를 거치므로 PKCE 로 보호한다 (`api/docs/user/login/plan.md:57-72`).

---

### 2.2 OAuth2 PKCE 흐름 (웹 현행)

#### 2.2.1 PKCE 값 생성 규격 (`frontend/src/lib/auth/pkce.ts`)

| 값 | 생성 규칙 | 코드 |
|---|---|---|
| `code_verifier` | 무작위 32바이트 → base64url (패딩 제거) = 43자 | `pkce.ts:22-24` |
| `code_challenge` | `SHA-256(code_verifier)` → base64url | `pkce.ts:27-31` |
| `state` | 무작위 16바이트 → base64url = 22자 | `pkce.ts:34-36` |
| base64url 변환 | `+`→`-`, `/`→`_`, 끝의 `=` 제거 | `pkce.ts:12` |

`code_challenge_method` 는 항상 `S256` 이다 (`social.ts:109`).

#### 2.2.2 저장 위치 (`frontend/src/lib/auth/social.ts:16-20`)

| 키 | 값 | 저장소 |
|---|---|---|
| `oauth_code_verifier` | code_verifier | `sessionStorage` (주 창) |
| `oauth_state` | state | `sessionStorage` (주 창) |
| `oauth_popup` | `"1"` (팝업 판별 표식) | `sessionStorage` (팝업 열기 직전에 심고 즉시 제거 — 팝업만 복사본을 갖는다) |

#### 2.2.3 인가 URL 조립 (`social.ts:89-114`)

```
{authUrl}?client_id={clientId}
         &redirect_uri={redirectUri}
         &response_type=code
         &state={state}
         &code_challenge={challenge}
         &code_challenge_method=S256
         [&scope=openid]   // 구글만 (social.ts:53)
```

| 제공자 | 기본 authUrl | scope |
|---|---|---|
| 카카오 | `https://kauth.kakao.com/oauth/authorize` (`social.ts:44`) | 없음 |
| 구글 | `https://accounts.google.com/o/oauth2/v2/auth` (`social.ts:52`) | `openid` |

#### 2.2.4 단계별 시퀀스 (팝업 경로 — 웹 기본값)

1. 사용자가 로그인 버튼 클릭 → `useSocialLogin.start()` (`useSocialLogin.ts:92-115`).
2. **클릭 핸들러 안에서 즉시** 빈 팝업(`about:blank`, 480×640)을 연다 (`social.ts:122-137`). PKCE 해시 계산(비동기)을 기다린 뒤 창을 열면 브라우저가 팝업 차단을 하기 때문이다.
3. 주 창이 verifier/state 를 생성해 자신의 `sessionStorage` 에 저장하고, 팝업의 `location.href` 를 인가 URL 로 바꾼다 (`social.ts:140-142`).
4. 제공자가 인증·동의 후 `redirect_uri`(= `/auth/{provider}/callback`)로 `?code=&state=` 를 붙여 되돌린다.
5. 콜백 페이지(`SocialCallbackPage.tsx:25-42`)가 자신이 팝업인지 판별한다(`sessionStorage[oauth_popup] === "1"` 우선, 없으면 `window.opener` — 제공자의 COOP 헤더로 `opener` 가 끊길 수 있어 표식을 먼저 본다).
6. 팝업이면 `BroadcastChannel("trypto-social-callback")` 으로 `{provider, code, state, error}` 를 주 창에 보내고 스스로 닫는다 (`social.ts:156-160`, `SocialCallbackPage.tsx:57-71`). **팝업은 검증도 교환도 하지 않는다.**
7. 주 창이 메시지를 받아 `verifySocialCallback()` 으로 검증한다 (`social.ts:191-212`).
8. 검증 성공 즉시 state·verifier 를 지우고(`clearSocialSecrets`, 성공·실패 무관), `POST /api/auth/{provider}/login` 으로 `{code, codeVerifier}` 를 보낸다 (`useSocialLogin.ts:48-61`, `auth-api.ts:14-20`).
9. 백엔드가 `Set-Cookie: SESSION=...` 과 `{userId, nickname, newUser}` 를 반환한다.
10. `loginWithSocial()` 로 인증 상태를 채운다. 화면 이동은 하지 않는다 — `PublicRoute` 가 `isAuthenticated` 를 보고 `/market` 으로 보낸다.

**검증 실패 메시지 (그대로 사용할 것, `social.ts:191-212`)**

| 조건 | 메시지 |
|---|---|
| 제공자가 `error` 파라미터 반환 | `{카카오\|구글} 로그인이 취소되었거나 실패했습니다.` |
| `code` 또는 `state` 누락 | `인가 정보가 올바르지 않습니다.` |
| 저장된 state 없음 / 불일치 | `보안 검증(state)에 실패했습니다. 다시 시도해주세요.` |
| verifier 없음 | `로그인 검증값이 없습니다. 다시 시도해주세요.` |
| 백엔드 교환 실패(비 ApiClientError) | `로그인 처리 중 오류가 발생했습니다.` |
| 인가 URL 조립 실패(환경변수 누락) | `{카카오\|구글} 로그인 설정이 완료되지 않았습니다.` |

#### 2.2.5 폴백 경로 (팝업 차단)

`window.open` 이 `null` 이면 `beginSocialLogin()` 이 주 창 자체를 `location.replace()` 로 제공자에게 보낸다 (`useSocialLogin.ts:99-106`, `social.ts:145-147`). 이때 콜백 페이지는 주 창에서 열리므로 `MainWindowExchange` 가 직접 검증·교환하고 `navigate("/market", {replace: true})` 한다 (`SocialCallbackPage.tsx:74-111`). 인가 코드는 일회용이므로 `startedRef` 로 1회만 교환한다.

부수 처리: 사용자가 팝업을 그냥 닫으면 500ms 폴링으로 감지해 버튼을 되돌린다 (`useSocialLogin.ts:18,73-87`). 폴백 경로에서 뒤로 가기(bfcache)로 로그인 화면이 되살아나면 `pageshow.persisted` 를 감지해 `location.reload()` 한다 (`LoginPage.tsx:17-24`).

**이 절의 팝업·BroadcastChannel·bfcache 처리는 모바일에 옮기지 않는다** (§2.4 참조).

#### 2.2.6 백엔드 처리 (`user/application/service/LoginService.java:39-63`)

1. `SocialAuthenticator.authenticate(provider, code, codeVerifier)` → 제공자별 `OAuthClient` 로 위임 (`SocialAuthenticatorImpl.java:26-32`).
2. 토큰 교환 (POST, `application/x-www-form-urlencoded`): `grant_type=authorization_code`, `client_id`, `client_secret`, **`redirect_uri`(서버 설정값)**, `code`, `code_verifier` (`KakaoOAuthClient.java:40-48`, `GoogleOAuthClient.java:40-48`).
3. 회원번호 조회: 카카오 `GET https://kapi.kakao.com/v2/user/me` 의 `id`, 구글 `GET https://openidconnect.googleapis.com/v1/userinfo` 의 `sub` (`application.yml:84-91`).
4. `social_account` 조회 → 없으면 등록. 연결된 회원이 있으면 로그인(`newUser=false`), 없으면 신규 회원 생성(`newUser=true`).
5. **재가입 제한**: 같은 소셜 신원으로 탈퇴한 회원이 있고 탈퇴 후 30일이 지나지 않았으면 `SIGNUP_RESTRICTED`(403) 로 거부 (`User.java:14,67-71`).
6. Redis 세션 생성 후 세션 ID 반환.

#### 2.2.7 로그인 API 계약

요청
```
POST /api/auth/{provider}/login          // provider ∈ {kakao, google}
Content-Type: application/json
{ "code": "...", "codeVerifier": "..." }  // 둘 다 @NotBlank (LoginRequest.java:7)
```

응답 (200)
```
Set-Cookie: SESSION=<uuid>; Path=/; Max-Age=604800; HttpOnly; SameSite=Lax[; Secure]
{ "status": 200, "code": "SUCCESS", "message": "로그인되었습니다.",
  "data": { "userId": 1, "nickname": "...", "newUser": true } }
```

실패 코드: `INVALID_PROVIDER`(400), `SOCIAL_LOGIN_FAILED`(401), `SIGNUP_RESTRICTED`(403), `SOCIAL_SERVER_ERROR`(502), `VALIDATION_ERROR`(400). 상세는 §1.3.

---

### 2.3 로그아웃 · 인증 복구 · 회원 탈퇴

#### 2.3.1 로그아웃

1. 클라이언트: `POST /api/auth/logout` (바디 없음) — `auth-api.ts:27-29`.
2. 서버: 쿠키에서 `SESSION` 을 읽어(없으면 그냥 통과) `session:{id}` 를 지우고 `user-sessions:{userId}` 집합에서 제거한 뒤, **항상 만료 쿠키를 내려보내고 200 을 반환한다** — 세션이 이미 없어도 성공(멱등) (`AuthController.java:43-52`).
3. 클라이언트: **API 실패 여부와 무관하게** 로컬 인증 상태를 비운다 (`AuthProvider.tsx:36-43`).
4. 화면 이동은 명시적으로 하지 않는다. `user = null` → `ProtectedRoute` 가 `/login` 으로 보낸다.

#### 2.3.2 인증 복구 (앱 부팅 / 새로고침) — `AuthProvider.tsx:8-30`

```
isAuthLoading = true (초기값)
  → GET /api/users/me
      성공 → user = { userId, nickname },  isAuthLoading = false
      실패 → user = null,                  isAuthLoading = false
```

- 성공/실패 판정만 하며 401 을 따로 구분하지 않는다. 어떤 오류든 미인증으로 간주한다.
- `isAuthLoading` 이 `true` 인 동안 모든 가드는 `null` 을 렌더한다(= 아무것도 그리지 않는다). Flutter 에서는 **스플래시/로딩 화면**에 대응한다.

#### 2.3.3 세션 만료의 사후 처리 (현행 웹의 한계)

`client.ts` 에는 **401 전역 처리기가 없다.** 앱 사용 중 세션이 만료되면 개별 API 호출이 `ApiClientError(401, "UNAUTHENTICATED")` 로 실패할 뿐, 자동 로그아웃이나 `/login` 이동은 일어나지 않는다. Flutter 에서는 이 지점을 보완한다(R10). 백엔드 수정은 필요 없다.

#### 2.3.4 회원 탈퇴

`DELETE /api/users/me` 는 회원을 탈퇴시키고 **해당 회원의 모든 세션을 삭제**한 뒤 만료 쿠키를 내린다 (`UserController.java:48-54`, `DeleteAccountService.java:33-42`). **웹 프론트에는 이 API 를 호출하는 코드가 없다.** 기능 동등성 관점에서는 구현 대상이 아니나, iOS 배포 시에는 필수다(R11).

---

### 2.4 PKCE·state 의 모바일 대응

웹의 `sessionStorage` 는 존재하지 않는다. 대신 **로그인 시작~콜백 수신까지 살아 있는 메모리 상태**로 충분하다. `flutter_web_auth_2` 는 인가 결과 URL 을 `await` 로 되돌려주므로, 팝업↔주 창 통신(BroadcastChannel)·팝업 판별·팝업 차단 폴백이 **모두 불필요**하다. 이식 시 다음이 통째로 삭제된다: `openSocialPopup`, `sendSocialPopup`, `publishSocialCallback`, `subscribeSocialCallback`, `OAUTH_POPUP_KEY`, bfcache 처리(`LoginPage.tsx:17-24`), 팝업 닫힘 폴링(`useSocialLogin.ts:73-87`). **`SocialCallbackPage` 에 해당하는 화면은 만들지 않는다** — 콜백은 화면이 아니라 인증 세션의 반환값이 된다.

**PKCE·state 검증 로직(§2.2.1, §2.2.3)은 규격 그대로 유지한다.**

```dart
String _b64url(List<int> b) => base64Url.encode(b).replaceAll('=', '');
final verifier  = _b64url(List.generate(32, (_) => Random.secure().nextInt(256)));
final challenge = _b64url(sha256.convert(utf8.encode(verifier)).bytes);   // crypto 패키지
final state     = _b64url(List.generate(16, (_) => Random.secure().nextInt(256)));

final result = await FlutterWebAuth2.authenticate(
  url: authUrl, callbackUrlScheme: 'trypto');   // 방안 B 기준
final uri = Uri.parse(result);
if (uri.queryParameters['state'] != state) throw ...;   // state 검증은 반드시 클라이언트가
await api.post('/api/auth/$provider/login',
  data: {'code': uri.queryParameters['code'], 'codeVerifier': verifier});
```

프로세스 강제 종료로 메모리가 날아갈 가능성에 대비하려면 verifier/state 를 `flutter_secure_storage` 에 저장하고, 성공·실패 무관하게 즉시 삭제한다(`clearSocialSecrets` 와 동일 원칙).

**카카오 네이티브 SDK(`kakao_flutter_sdk_user`)는 사용하지 않는다.** 백엔드가 **인가 코드 + code_verifier** 를 요구하므로 SDK 의 액세스 토큰 방식과 맞지 않는다. 웹 인가 코드 흐름을 유지한다.

**환경변수 미설정 처리**: 웹은 `clientId` 와 `redirectUri` 두 값이 모두 채워졌는지만 보고(`social.ts:78-81`) 버튼을 비활성화하며, 안내문은 개발 모드에서만 노출한다. 모바일도 빌드 타임 `--dart-define` 값이 비면 버튼을 비활성화하고 디버그 빌드에서만 사유를 표시하는 동일 규칙을 유지한다.

---

### 2.5 세션 쿠키를 모바일에서 유지하는 방법

**기본안 — 세션 ID 직접 보관 (백엔드 무수정, 권장)**

§1.4 에서 확인했듯 쿠키의 `Max-Age` 는 갱신되지 않는 반면 Redis TTL 은 요청마다 7일로 늘어난다. 쿠키 저장소를 그대로 쓰면 매일 앱을 써도 7일째에 강제 로그아웃된다. 앱에서는 다음으로 해결한다.

1. 로그인 응답의 `Set-Cookie` 에서 `SESSION` 값만 추출해 `flutter_secure_storage` 에 저장한다(만료 시각은 저장하지 않는다).
2. 이후 모든 요청에 `Cookie: SESSION=<값>` 헤더를 직접 붙이는 Dio 인터셉터를 둔다.
3. 서버가 401 `UNAUTHENTICATED` 를 주면 그때 저장값을 폐기한다.

이 방식은 서버의 슬라이딩 TTL 정책을 그대로 활용하므로 **주기적으로 앱을 쓰는 사용자는 재로그인이 없다.** 백엔드 계약을 전혀 건드리지 않으며, 쿠키 파싱 라이브러리의 `Max-Age` 해석에 좌우되지 않는다.

**대안 — cookie jar 영속화**

```
dio + dio_cookie_manager + cookie_jar(PersistCookieJar)
저장 경로: (await getApplicationDocumentsDirectory()).path + '/.cookies/'
```

구현이 간단하나 위 7일 절대 만료를 그대로 물려받는다. 메모리 쿠키 저장소를 쓰면 매 실행마다 재로그인이 필요해지므로 최소한 `PersistCookieJar` 를 써야 한다.

**공통 사실 관계**

- 네이티브 앱에는 동일 출처 개념이 없으므로 `credentials: include` 에 해당하는 설정이 필요 없다.
- `HttpOnly` 는 브라우저 JS 접근 차단 속성이라 네이티브에서는 무의미하다. `SameSite=Lax` 도 네이티브 HTTP 클라이언트에는 적용 개념이 없다. **CORS·SameSite 는 Flutter 네이티브 앱에서 문제가 되지 않는다.**
- `Secure` 는 문제가 된다. 운영은 `SESSION_COOKIE_SECURE=true` 이므로 앱의 base URL 은 **반드시 `https://`** 여야 하고, 로컬 개발 서버에 `http://10.0.2.2:8080` 로 붙을 때는 백엔드 `SESSION_COOKIE_SECURE=false`(기본값)여야 쿠키가 저장·전송된다. Android 는 클리어텍스트 HTTP 를 기본 차단하므로 개발 빌드에 `android:usesCleartextTraffic="true"`(또는 network security config 도메인 예외)가 필요하다.

---

### 2.6 redirect URI 방안별 상세 (R1 보충)

#### 방안 A — App Links / Universal Links (백엔드 수정 불필요)

기존 웹 콜백 URL 을 그대로 redirect URI 로 쓰고, OS 가 그 https URL 을 앱으로 가로채게 한다.

- 인가 화면은 `flutter_web_auth_2` 로 띄운다 (Android: Chrome Custom Tabs, iOS: `ASWebAuthenticationSession`). 앱 내 `WebView` 는 구글이 정책상 차단하므로 쓸 수 없다.
- Android: `AndroidManifest.xml` 에 `android:autoVerify="true"` 인 `intent-filter` (`https`, host = 서비스 도메인, pathPrefix = `/auth/`) 를 등록하고, 서버가 `https://{도메인}/.well-known/assetlinks.json` 을 서빙해야 한다. 현재 `frontend/nginx.conf:37-39` 의 `try_files $uri $uri/ /index.html` 는 `/.well-known/` 실제 파일이 있으면 그대로 내려주므로 **정적 파일만 추가하면 된다**(코드 변경 없음).
- iOS: Associated Domains(`applinks:{도메인}`) + `https://{도메인}/.well-known/apple-app-site-association`. `ASWebAuthenticationSession` 의 https 콜백은 **iOS 17.4 이상**에서만 지원되므로, 최소 지원 iOS 버전을 17.4 로 올리거나 방안 B 를 택해야 한다. **착수 전 확정할 것.**
- 앱 링크 검증이 실패하거나 앱이 미설치면 브라우저가 웹 콜백 페이지를 열게 되는데, 이 페이지는 팝업이 아닌 주 창으로 판단해 스스로 로그인을 완료해버린다(`SocialCallbackPage.tsx:74-111`). 이는 자연스러운 폴백이기도 하지만, 모바일에서는 이 경로가 열리지 않도록 앱 링크 검증을 반드시 확인해야 한다.
- 로컬 개발용 `http://localhost:5173/...` 는 딥링크로 잡을 수 없으므로 개발 환경에서는 별도 처리가 필요하다.
- 제공자 콘솔: **추가 등록 불필요** (웹과 같은 URI 를 쓰므로).

#### 방안 B — 커스텀 스킴 + 백엔드 수정

`trypto://auth/kakao/callback` 같은 커스텀 스킴은 App Links 검증·도메인 파일 호스팅이 필요 없어 모바일에서 가장 견고하다. 다만 다음 변경이 따른다.

**백엔드 변경 (필수)** — 제공자별 redirect URI 를 플랫폼별로 다중화한다. 최소 변경안:

1. `user/adapter/in/dto/request/LoginRequest.java:7` 에 `redirectUri`(또는 `clientType` = `WEB` | `MOBILE`) 필드 추가.
2. `LoginCommand`, `SocialAuthenticator`, `OAuthClient.getIdentity(authorizationCode, codeVerifier)` 시그니처에 그 값을 전달.
3. `KakaoOAuthClient.java:45` / `GoogleOAuthClient.java:45` 의 `form.add("redirect_uri", properties.getRedirectUri())` 를 전달받은 값으로 교체.
4. **허용 목록 검증을 반드시 추가한다.** `app.oauth.{provider}.allowed-redirect-uris`(웹 URL + `trypto://auth/{provider}/callback`)에 없으면 `INVALID_PROVIDER` 계열 오류로 거절한다. 검증 없이 통과시키면 오픈 리다이렉터가 되어 인가 코드가 탈취될 수 있다.
5. 백엔드 env 추가: `KAKAO_MOBILE_REDIRECT_URI`, `GOOGLE_MOBILE_REDIRECT_URI`, `GOOGLE_MOBILE_CLIENT_ID`.

**제공자 콘솔 제약 (착수 전 반드시 확인)**

- 구글: "웹 애플리케이션" 클라이언트는 `https` 리디렉션만 허용하므로 커스텀 스킴을 쓰려면 **Android/iOS 클라이언트 ID 를 별도 발급**해야 한다. 이 클라이언트 유형은 `client_secret` 이 없으므로 백엔드의 토큰 교환 폼에서 `client_secret` 을 빼야 한다(현재 `GoogleOAuthClient.java:44` 는 항상 붙인다) — 백엔드 분기 추가가 더 필요하다. SHA-1 지문·번들 ID 등록도 필요하다.
- 카카오: 콘솔의 Redirect URI 는 `http`/`https` 스킴만 등록 가능한 것으로 알려져 있다. 커스텀 스킴을 쓰려면 Kakao SDK 의 인가 코드 발급 경로를 써야 하고, 그 경우 redirect URI 는 SDK 가 정한 `kakao{네이티브앱키}://oauth` 가 되며 백엔드도 그 값으로 교환해야 한다. 네이티브 앱키·키 해시(Android)·번들 ID(iOS) 등록이 필요하다.
- 위 두 항목은 **외부 콘솔 정책**이라 코드로 확인할 수 없다.

#### 외부 콘솔 등록 체크리스트

| 항목 | 방안 A | 방안 B |
|---|---|---|
| 카카오 Redirect URI | 기존 `https://{도메인}/auth/kakao/callback` 재사용 (추가 등록 없음) | 커스텀 스킴 등록 가능 여부 확인 → 불가 시 Kakao SDK 경로 + 네이티브 앱키 등록, 키 해시·번들 ID 등록 |
| 구글 Redirect URI | 기존 `https://{도메인}/auth/google/callback` 재사용 | **Android/iOS 클라이언트 ID 신규 발급**, SHA-1 지문·번들 ID 등록, 백엔드에서 `client_secret` 제외 분기 |
| 도메인 파일 | `assetlinks.json`, `apple-app-site-association` 서빙 필요 | 불필요 |
| 백엔드 env | 변경 없음 | `KAKAO_MOBILE_REDIRECT_URI`, `GOOGLE_MOBILE_REDIRECT_URI`, `GOOGLE_MOBILE_CLIENT_ID` 추가 |

---

### 2.7 라우트 가드 3종

React Router 의 `<Outlet/>` 기반 3종 가드다. Flutter 에서는 go_router 의 `redirect` 콜백 하나로 통합 구현한다.

#### 2.7.1 판정 입력값

| 상태 | 출처 | 의미 |
|---|---|---|
| `isAuthLoading` | `AuthProvider` | `/api/users/me` 재조회 진행 중 |
| `isAuthenticated` | `user !== null` | 로그인 여부 |
| `isRoundLoading` | `RoundProvider` | 라운드 상태 조회 중 (`RoundProvider.tsx:18-49`) |
| `hasActiveRound` | `activeRound !== null` | 진행 중 라운드 존재 |
| `hasEverStartedRound` | `totalRoundCount > 0` | 라운드를 한 번이라도 시작한 적 있음 |

`RoundProvider` 는 `isAuthLoading` 이 끝나고 `user` 가 있을 때만 `fetchActiveRound()` 와 `fetchTotalRoundCount()` 를 병렬 조회한다. 미인증이면 즉시 `isRoundLoading=false`, 라운드 없음으로 확정한다.

핵심 분기: **"라운드를 한 번이라도 시작한 적이 있는가(`hasEverStartedRound`)"** 와 **"지금 활성 라운드가 있는가(`hasActiveRound`)"** 는 별개다. 신규 사용자는 라운드 생성을 건너뛸 수 없고, 라운드를 끝낸 기존 사용자는 라운드 없이도 앱을 자유롭게 둘러볼 수 있다.

#### 2.7.2 가드별 규칙 (순서 그대로 지켜야 한다)

**ProtectedRoute** (`ProtectedRoute.tsx:5-15`) — `/market`, `/portfolio`, `/wallet`, `/ranking`, `/regret`, `/mypage`
```
if (isAuthLoading)                            → 렌더 보류 (빈 화면)
if (!isAuthenticated)                         → /login       (replace)
if (isRoundLoading)                           → 렌더 보류
if (!hasActiveRound && !hasEverStartedRound)  → /round/new   (replace)
else                                          → 화면 표시
```

**PublicRoute** (`PublicRoute.tsx:4-11`) — `/login`
```
if (isAuthLoading)     → 렌더 보류
if (isAuthenticated)   → /market  (replace)
else                   → 화면 표시
```

**RoundGuard** (`RoundGuard.tsx:5-15`) — `/round/new`
```
if (isAuthLoading)     → 렌더 보류
if (!isAuthenticated)  → /login   (replace)
if (isRoundLoading)    → 렌더 보류
if (hasActiveRound)    → /market  (replace)   // 라운드 중복 생성 차단
else                   → 화면 표시
```

**콜백 라우트** `/auth/:provider/callback` 은 **어떤 가드에도 속하지 않는다** (`App.tsx:24`). **그 외 모든 경로** (`*`) → `/login` (replace).

#### 2.7.3 Flutter go_router 로의 변환

```dart
// 전역 redirect. 모든 라우트 이동에서 평가된다.
String? redirect(BuildContext ctx, GoRouterState st) {
  final loc = st.matchedLocation;
  if (loc.startsWith('/auth/')) return null;            // 콜백은 가드 예외
  if (auth.isLoading) return '/splash';                 // 웹의 "렌더 보류" 대체
  if (!auth.isAuthenticated) return loc == '/login' ? null : '/login';
  if (loc == '/login') return '/market';               // PublicRoute
  if (round.isLoading) return '/splash';
  if (loc == '/round/new') {
    return round.hasActiveRound ? '/market' : null;     // RoundGuard
  }
  if (!round.hasActiveRound && !round.hasEverStarted) return '/round/new';
  return null;
}
```

웹의 "`null` 렌더(빈 화면)"는 Flutter 에서 라우트 이동이 아니라 **`/splash` 대기 화면**으로 표현한다(흰 화면 깜빡임 방지). 웹의 `replace: true` 는 go_router 의 `go()`(스택 교체)에 해당하므로 `push()` 를 쓰지 않는다.

---

## 3. 실시간 (STOMP over WebSocket)

### 3.1 연결 규약

#### 3.1.1 프로토콜 확정

**raw WebSocket + STOMP 1.2 이며, SockJS 가 아니다.** 근거는 다음 세 가지다.

| 근거 | 위치 | 내용 |
|---|---|---|
| 서버 엔드포인트 등록 | `common/config/WebSocketConfig.java:41-43` | `registry.addEndpoint(endpoint).setAllowedOriginPatterns("*")` — `.withSockJS()` 호출이 없다 |
| 웹 클라이언트 | `frontend/src/lib/api/websocket.ts:90-96` | `@stomp/stompjs` 의 `Client({ brokerURL: ... })` 를 사용한다. SockJS 라면 `webSocketFactory` 를 지정해야 하나 지정하지 않는다 |
| 부하 테스트 하네스 | `loadtest/k6/lib/stomp.js:16`, `scenarios/ticker_websocket.js:44` | `ws://{host}/ws` 에 raw WebSocket 으로 붙어 `accept-version:1.2` STOMP 프레임을 직접 만들어 보낸다 |

따라서 Flutter 에서는 SockJS 폴백 계층을 구현하지 않는다.

#### 3.1.2 접속 URL

| 항목 | 값 | 근거 |
|---|---|---|
| 경로 | `/ws` | `application.yml:68` (`app.websocket.endpoint`) |
| 웹 URL 결정 | `VITE_WS_BASE_URL` 이 있으면 그 값, 없으면 `ws(s)://{현재 host}/ws` | `websocket.ts:23-30` |
| 리버스 프록시 | nginx `location /ws` → `proxy_pass http://backend:8080` | `frontend/nginx.conf:26-34` |

nginx 는 `Upgrade`/`Connection: upgrade` 헤더를 전달하고 `proxy_read_timeout` / `proxy_send_timeout` 을 3600초로 둔다. Flutter 앱은 브라우저가 아니므로 `window.location` 이 없다. WS 기본 URL 을 빌드 설정(`--dart-define=WS_BASE_URL=...`)으로 주입하고, HTTP API 기본 URL 과 동일 호스트를 쓰도록 구성한다.

#### 3.1.3 하트비트

| 방향 | 값 | 근거 |
|---|---|---|
| 서버 → 클라이언트 | 10,000 ms | `application.yml:69`, `WebSocketConfig.java:34-36` |
| 클라이언트 → 서버 | 10,000 ms | `application.yml:70` |
| 웹 클라이언트 설정 | `heartbeatIncoming: 10000`, `heartbeatOutgoing: 10000` | `websocket.ts:94-95` |

STOMP `CONNECT` 프레임의 `heart-beat` 헤더로 협상한다. 클라이언트→서버 하트비트를 약속 주기 안에 보내지 않으면 서버가 세션을 끊는다. 부하 테스트 하네스는 이벤트 루프 지연으로 인한 drift 를 감안해 협상값보다 2초 짧은 8초 주기로 송신한다(`loadtest/k6/lib/stomp.js:5-7,14`). **Flutter 도 동일한 마진 전략을 적용한다.** 모바일은 OS 의 타이머 유예로 주기가 밀리기 쉬우므로, 송신 주기를 10초가 아니라 **8초**로 설정한다.

또한 서버가 보내는 단독 `LF`(`\n`) 한 글자는 STOMP 프레임이 아니라 server→client 하트비트다. `stomp_dart_client` 는 이를 내부 처리하므로 직접 파싱할 필요는 없으나, 원시 소켓을 다룰 경우 프레임으로 오인하면 안 된다.

#### 3.1.4 연결 타임아웃과 재연결 백오프

```
websocket.ts:90-116
  reconnectDelay: 1000        // 초기값
  connectionTimeout: 5000     // CONNECTED 프레임 미수신 시 실패 처리

  onWebSocketClose:
    reconnectAttempts++
    reconnectDelay = min(1000 * 2^reconnectAttempts, 30000)

  onConnect:
    reconnectAttempts = 0
    reconnectDelay = 1000     // 원복
    reactivateAllSubscribers()
```

실제 대기 시간은 다음과 같다. 최초 종료 시점에 이미 `reconnectAttempts` 가 1 로 증가한 뒤 지연을 계산하므로 **1초가 아니라 2초부터 시작한다.**

| 재시도 회차 | 대기 시간 |
|---|---|
| 1 | 2,000 ms |
| 2 | 4,000 ms |
| 3 | 8,000 ms |
| 4 | 16,000 ms |
| 5회 이상 | 30,000 ms (상한) |

`api/docs/websocket.md:53-60` 의 표는 1초/2초/4초/8초/30초로 기술되어 있으나 이는 설계 문서상의 의도이며, 실제 구현은 위 표와 같다. **Flutter 이식 시에는 실제 구현(2초 시작)을 기준으로 한다.**

재연결에 성공하면 이전 구독을 전부 복원한다(`websocket.ts:110-116`). 연결이 끊기면 보관 중인 모든 구독 핸들을 무효화하여(`invalidateSubscriptions`, `:51-55`) 재연결 후 중복 구독이 생기지 않게 한다.

#### 3.1.5 브로커 목적지 접두사

| 접두사 | 용도 | 근거 |
|---|---|---|
| `/topic`, `/queue` | SimpleBroker 가 처리하는 목적지 | `WebSocketConfig.java:34` |
| `/app` | 클라이언트→서버 애플리케이션 목적지 | `WebSocketConfig.java:32` |
| `/user` | 사용자별 목적지 접두사 | `WebSocketConfig.java:33` |

**클라이언트가 서버로 STOMP `SEND` 를 보내는 경로는 존재하지 않는다.** `@MessageMapping` 을 선언한 컨트롤러가 api 전체에 없으며, 웹 클라이언트도 `SUBSCRIBE` 만 수행한다. 모든 쓰기 동작(주문·송금 등)은 REST 로 처리한다. Flutter 도 WebSocket 을 **수신 전용**으로 설계한다.

#### 3.1.6 인증

**WebSocket 핸드셰이크에는 어떠한 인증도 걸려 있지 않다.**

- `WebSocketConfig.java:41-43` 의 엔드포인트 등록에 `addInterceptors(HandshakeInterceptor)` 나 `setHandshakeHandler(...)` 가 없다.
- `api/src/main/java` 전체에 `HandshakeHandler`, `ChannelInterceptor`, `Principal`, `UserDestinationResolver`, `StompHeaderAccessor` 를 사용하는 코드가 **하나도 없다**(전수 grep 무매치).
- REST 인증은 Spring MVC `HandlerInterceptor` 인 `AuthInterceptor` 가 `SESSION` 쿠키를 읽어 처리하며, 이는 HTTP 요청 처리 파이프라인 전용이라 WebSocket 핸드셰이크에는 적용되지 않는다.

따라서 티커 구독은 인증 없이 즉시 가능하다. 세션 쿠키를 핸드셰이크에 실을 필요가 **현재로서는 없다**(§3.2.2, §3.6.3 참조).

#### 3.1.7 서버 측 백프레셔 정책 (모바일에서 중요)

| 설정 | 값 | 근거 |
|---|---|---|
| 아웃바운드 전송 시간 제한 | 5,000 ms | `WebSocketConfig.java:57` (`setSendTimeLimit`) |
| 아웃바운드 버퍼 크기 제한 | 512 KB | `WebSocketConfig.java:58` (`setSendBufferSizeLimit`) |
| 아웃바운드 스레드풀 큐 포화 정책 | `DiscardOldestPolicy` | `WebSocketConfig.java:78` |

전송이 느린 클라이언트는 제한 초과 시 서버가 세션을 종료한다. 또한 큐가 포화되면 **오래된 메시지를 버린다**. 즉 티커 스트림은 **완전성이 보장되지 않는 스냅샷 스트림**이다. Flutter 클라이언트는 틱 유실을 정상 동작으로 간주해야 하며, 누적 계산(예: 델타 누산)을 절대 하지 않고 항상 최신 값으로 덮어쓴다.

---

### 3.2 구독 토픽과 페이로드

#### 3.2.1 `/topic/tickers.{exchangeId}` — 실시간 시세 (공개)

`exchangeId` 는 정수이며 웹의 고정 목록은 다음과 같다(`frontend/src/lib/types/coins.ts:49-53`).

| exchangeId | key | 이름 | 기준통화 | 수수료율 |
|---|---|---|---|---|
| 1 | `upbit` | 업비트 | KRW | 0.0005 |
| 2 | `bithumb` | 빗썸 | KRW | 0.0025 |
| 3 | `binance` | 바이낸스 | USDT | 0.001 |

**메시지 본문은 JSON 배열이다** (단일 객체가 아니다). 발행 코드는 `LiveTickerEventListener.broadcast`(`marketdata/adapter/in/LiveTickerEventListener.java:56-61`)이며, 요소 타입은 `TickerResponse`(`marketdata/adapter/in/dto/response/TickerResponse.java:6-7`)다.

```json
[
  {
    "coinId": 1,
    "symbol": "BTC",
    "price": 152340000,
    "changeRate": 0.0123,
    "quoteTurnover": 8423199301.55,
    "timestamp": 1735689600123
  }
]
```

| 필드 | Java 타입 | JSON 타입 | 의미 |
|---|---|---|---|
| `coinId` | `Long` | number | 내부 코인 식별자 |
| `symbol` | `String` | string | **코인 심볼**(`BTC`). 거래쌍(`BTC/KRW`)이 **아니다** |
| `price` | `BigDecimal` | number | 현재가 (거래소 기준통화 단위) |
| `changeRate` | `BigDecimal` | number | 24시간 변동률. 소수(1% = `0.01`) |
| `quoteTurnover` | `BigDecimal` | number | 24시간 거래대금 (기준통화 단위) |
| `timestamp` | `Long` | number | 거래소 원본 tick 시각. epoch **밀리초** |

`symbol` 이 거래쌍이 아닌 코인 심볼인 근거: `LiveTickerResult.of` 가 `mapping.coinSymbol()` 을 넣는다. RabbitMQ 입력 메시지의 `symbol` 은 `BTC/KRW` 형태지만(`docs/contracts/ticker-exchange.md:48`), api 가 Redis 매핑 캐시로 변환한 뒤 브로드캐스트한다. 이 덕분에 클라이언트는 REST 로 받은 `coinSymbol` 과 WS 의 `symbol` 을 문자열 그대로 매칭할 수 있다.

부가 사항:

- **매핑에 없는 심볼은 서버가 버린다.** `ResolveLiveTickerService.resolve`(`:20-25`)가 해석 실패 항목을 제거하고, 결과가 전부 비면 아예 발행하지 않는다. 즉 클라이언트는 상장 목록에 없는 심볼을 받지 않는다.
- 메시지에 `publishedAtMs` 라는 STOMP 헤더가 실린다(`LiveTickerEventListener.java:23,59`). 값은 배치 내 최소 `timestamp` 이며 레이턴시 계측용이다. **웹 클라이언트는 이 헤더를 읽지 않는다. Flutter 도 무시한다.**
- 배치 크기는 보장되지 않는다. collector 는 tick 1건마다 크기 1 배치를 발행한다(`docs/contracts/ticker-exchange.md:17`). 즉 실제로는 **초당 수백 건의 소형 메시지**가 들어온다. 배치 크기를 가정하지 말고 배열로 처리한다.

웹의 파싱 규칙(`websocket.ts:144-153`): JSON 파싱 실패 시 조용히 무시하고, 배열이 아니거나 길이 0 이면 콜백을 호출하지 않는다.

#### 3.2.2 `/user/{userId}/queue/events` — 체결 이벤트 (개인)

이 토픽은 **현재 코드 상태에서 동작하지 않는다.** 상세 근거는 R3 을 참조한다. 요약하면 다음과 같다.

- 웹은 `/user/${userId}/queue/events` 로 리터럴 구독한다(`websocket.ts:165-187`).
- 서버는 `messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/events", payload)` 로 발행한다(`StompOrderFilledNotificationAdapter.java:22,48`).
- `convertAndSendToUser` 는 `DefaultUserDestinationResolver` 가 `SimpUserRegistry` 에서 이름이 `{user}` 인 Principal 의 세션을 찾아 치환하는 규약이다. **이 프로젝트의 WebSocket 세션에는 Principal 이 단 한 번도 부착되지 않으므로**(§3.1.6) `SimpUserRegistry` 는 항상 비어 있고 **메시지는 폐기된다.**
- 구독 형식도 규약과 어긋난다. Spring 의 사용자 목적지 구독은 `/user/queue/events` 형태여야 한다(서버가 세션별 목적지로 치환한다). 백엔드 설계 문서도 `/user/queue/events` 로 명시한다(`api/docs/screen/portfolio-tab.md:109`, `transfer-tab.md:61`).

**페이로드 계약도 어긋난다.**

| | 필드 |
|---|---|
| 웹이 기대하는 타입 (`websocket.ts:12-21`) | `eventType`, `walletId`, `orderId`, `coinId`, `side`, `quantity`, `price`, `fee` |
| 설계 문서 (`api/docs/screen/portfolio-tab.md:118`) | 위와 동일 |
| **서버가 실제로 보내는 것** (`notification/dto/OrderFilledStompPayload.java:7-8`) | `eventType`, `orderId`, `executedPrice`, `quantity`, `executedAt` |

```json
{
  "eventType": "ORDER_FILLED",
  "orderId": 123,
  "executedPrice": 152340000,
  "quantity": 0.01,
  "executedAt": "2026-07-15T10:23:45.123"
}
```

| 필드 | Java 타입 | JSON 표현 |
|---|---|---|
| `eventType` | `String` | 항상 `"ORDER_FILLED"` |
| `orderId` | `Long` | number |
| `executedPrice` | `BigDecimal` | number |
| `quantity` | `BigDecimal` | number |
| `executedAt` | `LocalDateTime` | ISO-8601 문자열 (타임존 없음) |

즉 `walletId`, `coinId`, `side`, `price`, `fee` 는 **존재하지 않는다.**

**Flutter 이식 지침**

1. 이 토픽에 기대어 UI 를 갱신하는 설계를 하지 않는다. 체결 반영은 **REST 재조회**로 처리한다(§3.4).
2. 서버가 수정될 것을 대비해 수신 계층은 만들어 두되, 구독 목적지는 Spring 규약대로 **`/user/queue/events`** 를 사용하고, 페이로드 모델은 **서버 실제 필드**에 맞춘다. 알 수 없는 필드는 무시하고, 없는 필드는 `null` 허용으로 둔다.
3. 이 경로를 살리려면 서버에 다음 두 가지가 필요하다. 프론트만으로는 해결되지 않는다.
   - 핸드셰이크 또는 `CONNECT` 프레임에서 `SESSION` 쿠키/헤더를 읽어 `userId` 를 이름으로 갖는 `Principal` 을 세션에 부착 (`HandshakeInterceptor` + `DefaultHandshakeHandler.determineUser` 또는 `ChannelInterceptor` 에서 `StompHeaderAccessor.setUser`)
   - 페이로드에 `walletId`/`coinId`/`side`/`fee` 추가 (UI 가 로컬 갱신을 하려면 필수)

---

### 3.3 티커 병합과 가격 플래시

#### 3.3.1 초기 정적 목록

`GET /api/exchanges/{exchangeId}/coins` 로 상장 코인 목록을 받는다(`exchange-api.ts:13-15`). 응답 항목은 **조회 시점의 시세 스냅샷을 포함한다.**

이를 화면 모델 `CoinData` 로 매핑한다(`useExchangeCoins.ts:26-32`, `lib/types/coins.ts:1-9`).

| CoinData 필드 | 출처 |
|---|---|
| `symbol` | `coinSymbol` |
| `name` | `coinName` |
| `currentPrice` | `price` |
| `changeRate` | `changeRate` |
| `volume` | `volume` |
| `tickedAt` | 없음 (WS 로 갱신된 코인만 값을 가짐) |

조회 실패 시 빈 목록으로 대체한다. 로딩 중에는 **매번 새 배열을 만들지 않고 동일한 상수 빈 배열**을 반환하여 하위 계산의 불필요한 재실행을 막는다(`useExchangeCoins.ts:5-7`).

#### 3.3.2 병합 규칙 (`frontend/src/hooks/useTickers.ts`)

1. **키는 `symbol` 문자열이다.** 수신한 틱을 `Map<symbol, Ticker>` 에 누적한다(`:41`).
2. **초기 목록이 기준이다.** 렌더 결과는 항상 `initialCoins` 를 순회하며 만든다. WS 로만 들어오고 목록에 없는 심볼은 **화면에 추가되지 않는다**(`:59-69`).
3. **틱이 하나도 없으면 초기 목록을 그대로 반환한다**(`:57`).
4. 매칭되는 틱이 있으면 다음 4개 필드만 덮어쓴다. `symbol`·`name` 은 유지한다.

   | 덮어쓸 필드 | 틱의 필드 |
   |---|---|
   | `currentPrice` | `price` |
   | `changeRate` | `changeRate` |
   | `volume` | **`quoteTurnover`** |
   | `tickedAt` | `timestamp` |

5. **거래소를 바꾸면 누적 틱 맵을 비운다**(`:52`, cleanup). 새 거래소의 초기 스냅샷이 이전 거래소의 잔여 틱으로 오염되지 않게 한다.
6. **틱 적용은 프레임 단위로 합친다.** 도착한 틱을 `pending` 맵에 모으고 `requestAnimationFrame` 한 번에 상태를 갱신한다(`:23-45`). 같은 프레임 안에 같은 심볼이 여러 번 오면 마지막 값만 남는다.

**Flutter 이식**: `requestAnimationFrame` 은 존재하지 않는다. 동일 효과를 내려면 수신 틱을 `Map<String, Ticker>` 버퍼에 모으고, `SchedulerBinding.instance.scheduleFrameCallback` 또는 16 ms 주기 `Timer` 로 한 번만 flush 한다. **틱마다 `setState` 를 호출하면 안 된다** — 거래소당 코인이 최대 600개를 넘고 초당 수백 건의 메시지가 들어오므로 프레임이 무너진다.

목록은 `ListView.builder` 로 보이는 행만 빌드한다(웹의 `@tanstack/react-virtual` 가상화에 대응). 웹은 행 높이를 68px 고정, 8행 높이의 자체 스크롤 상자를 쓴다(`CoinTable.tsx:28-30`). 모바일에서는 화면 전체 스크롤로 대체하되 행 높이는 고정한다(`itemExtent: 68`).

#### 3.3.3 가격 플래시 규칙 (`frontend/src/hooks/usePriceFlash.ts`)

| 항목 | 규칙 | 근거 |
|---|---|---|
| 트리거 판정 | **`price` 가 아니라 `tickedAt` 변화로 판정한다.** 같은 가격에 체결된 경우를 놓치지 않기 위함이다 | `usePriceFlash.ts:19,27` |
| 방향 | `price > 직전 price` → `up`, `price < 직전 price` → `down`, 같으면 `same` | `:34-37` |
| 첫 틱 | 직전 `tickedAt` 또는 현재 `tickedAt` 이 `undefined` 이면 방향은 `null` (깜빡이지 않음) | `:28,32-33` |
| 지속 시간 | **100 ms 후 즉시 해제.** 페이드아웃하지 않는다 (서서히 사라지면 잔상이 다음 체결과 겹쳐 오히려 흐려진다) | `:5-7,41-48` |
| 재렌더 시 | 행이 다시 그려질 때는 그 시점 시세로 초기화되므로, **스크롤만으로는 깜빡이지 않는다** | `:25-26` 주석 |

시각 표현은 **숫자의 글자색이나 배경을 바꾸지 않고 테두리만 잠깐 두른다**(`CoinTable.tsx:81-92`). 읽어야 할 숫자가 흔들리는 것을 피하기 위한 의도적 선택이다.

| 방향 | 테두리 색 |
|---|---|
| `up` | 상승색 (`border-positive`, `#2ECC87`) |
| `down` | 하락색 (`border-negative`, `#E85D75`) |
| `same` | 중립 회색 (`border-muted-foreground/40`, `#7C7C8A` 40%) |

테두리 박스 형상: `absolute left-0 -right-2 -inset-y-2.5 rounded-md border` → 숫자 기준 위/아래 10px, 오른쪽 8px 바깥으로 확장된 반경 8px 사각 테두리.

**Flutter 이식**: 행 위젯에 `AnimatedContainer` 를 쓰지 않는다(페이드가 아니라 즉시 on/off 이므로). 100 ms `Timer` 로 상태를 되돌리는 `StatefulWidget` 또는 `ValueNotifier<FlashDirection?>` 을 행마다 두고, `Stack` 의 `Positioned(left: 0, right: -8, top: -10, bottom: -10)` 에 `DecoratedBox(border: Border.all(color: flashColor, width: 1), borderRadius: 8)` 를 얹는다. 플래시 상태는 **행 단위 로컬 상태**로 두어 전체 목록 리빌드를 유발하지 않게 한다.

---

### 3.4 체결 이벤트가 UI 를 바꾸는 방식 (현행 웹)

구독은 `MarketPage` 에서만 이뤄진다(`MarketPage.tsx:35`).

- `userId` 가 `null`(미로그인)이면 구독하지 않는다(`useUserEvents.ts:23`).
- `eventType === "ORDER_FILLED"` 인 경우에만 콜백을 호출한다(`:30`).
- 콜백이 바뀔 때마다 구독을 다시 맺지 않도록 콜백을 `ref` 에 보관하고, 갱신은 렌더가 끝난 뒤에 한다(`:15-20`).

수신한 이벤트는 `orderFilledEvent` 상태로 올라가 `OrderPanel` 에 전달되고, `OrderPanel` 은 이를 받아 **주문 가능 수량을 로컬로 증분한다**(`OrderPanel.tsx:201-210`).

```
매수 체결(side === "BUY") → 매도 가능 수량 += quantity
매도 체결(그 외)          → 매수 가능 금액 += price * quantity - fee
```

**단, §3.2.2 에서 확인했듯 이 경로는 실제로 작동하지 않는다.** 이벤트가 전달되지 않을뿐더러, 설령 전달되더라도 서버 페이로드에 `side`·`price`·`fee` 가 없어 `undefined` 가 되고 위 산술은 `NaN` 을 만든다.

**Flutter 이식 지침**

체결 후 잔고·주문가능 금액 갱신은 **REST 재조회로 확정 처리한다.** 이는 웹에서도 이미 신뢰 가능한 경로로 존재한다.

- 주문 제출 성공 직후: 주문 가능 조회 재호출 + (거래내역 탭이 열려 있으면) 내역 재조회 (`OrderPanel.tsx:358-361`)
- 주문 취소 성공 직후: 목록에서 해당 주문 제거 + 주문 가능 조회 재호출 (`:305-306`)

지정가 주문이 나중에 체결되는 경우를 반영하려면, WS 이벤트 대신 **화면 진입/포그라운드 복귀/사용자 당김 새로고침 시점의 재조회**로 처리한다. 실시간 푸시가 서버에서 정식 지원되면 그때 §3.2.2 의 지침대로 갈아끼운다.

또한 참고로, **포트폴리오 화면은 실시간 티커를 구독하지 않는다.** `api/docs/screen/portfolio-tab.md:100-104` 는 구독을 명시하지만, 웹 구현에서 `useTickers` 를 호출하는 곳은 `MarketPage` 뿐이다(전수 grep 확인). Flutter 도 **마켓 화면에서만 티커를 구독**하여 웹과 기능 동일성을 맞춘다.

---

### 3.5 백그라운드 복귀 시 좀비 연결 처리

#### 3.5.1 웹 구현 (`websocket.ts:68-85`)

`handleVisibilityChange` 가 `document.visibilitychange` 에 붙는다(리스너는 중복 부착 방지 플래그로 1회만 등록).

```
visibilityState === "hidden"
  → hiddenAt = Date.now();  종료

visibilityState === "visible"
  → elapsed = hiddenAt ? Date.now() - hiddenAt : 0
  → hiddenAt = null
  → if (elapsed > 20000 || !client.connected):
        client.active ? forceReconnect() : client.activate()
```

**임계값 20,000 ms 는 하트비트 간격(10초)의 2배다.** `client.connected` 만 보면 ERROR 프레임 처리 전이라 좀비 연결을 잡지 못하므로, **시간 기반으로 강제 재연결**한다.

`forceReconnect`(`:61-66`)는 백오프가 길게 누적되었을 수 있으므로 `reconnectDelay` 를 100 ms 로 낮춘 뒤 `forceDisconnect()` 를 호출한다. 종료 후 자동 재연결 로직이 100 ms 만에 재시도한다.

#### 3.5.2 Flutter lifecycle 이식

`WidgetsBindingObserver.didChangeAppLifecycleState` 로 옮긴다.

| 웹 | Flutter `AppLifecycleState` |
|---|---|
| `visibilitychange` → `hidden` | `paused` (및 iOS 의 `inactive` 후 `hidden`) |
| `visibilitychange` → `visible` | `resumed` |

```dart
@override
void didChangeAppLifecycleState(AppLifecycleState state) {
  if (state == AppLifecycleState.paused || state == AppLifecycleState.hidden) {
    _hiddenAt = DateTime.now();
    return;
  }
  if (state != AppLifecycleState.resumed) return;

  final elapsed = _hiddenAt == null
      ? Duration.zero
      : DateTime.now().difference(_hiddenAt!);
  _hiddenAt = null;

  // 하트비트 10초의 2배를 넘겨 백그라운드였으면 서버가 이미 세션을 끊었다고 본다.
  if (elapsed > const Duration(seconds: 20) || !_isConnected) {
    _forceReconnect(); // 기존 소켓을 닫고 즉시(100ms) 재연결
  }
}
```

모바일 고유의 추가 고려 사항이다. **웹보다 조건이 훨씬 나쁘다는 점을 전제로 설계한다.**

- OS 가 백그라운드에서 소켓과 타이머를 정지시키므로, 짧은 백그라운드 전환에도 좀비 연결이 될 확률이 웹보다 높다. `elapsed` 판정과 별개로 **`resumed` 시점에는 REST 로 최신 스냅샷을 1회 재조회**하여 티커 목록을 동기화한다(누락 틱 보정). 이는 `api/docs/websocket.md:64` 의 설계 지침과도 일치한다.
- **백그라운드에서는 연결을 유지하려 시도하지 않는다.** `paused` 진입 시 소켓을 끊고 `resumed` 에서 새로 맺는 편이 배터리·재연결 신뢰성 모두에 유리하다. 다만 재연결 후 구독을 반드시 복원한다.
- 네트워크 전환(Wi-Fi ↔ 셀룰러)은 브라우저에 없는 이벤트다. `connectivity_plus` 로 연결 변화를 감지해 `_forceReconnect()` 를 태운다. 전환 직후 소켓은 살아 있는 것처럼 보이지만 실제로는 죽어 있는 경우가 흔하다.

---

### 3.6 Flutter 이식 설계

#### 3.6.1 구조

웹은 **모듈 전역 단일 클라이언트**를 쓴다. `client`, `subscribers` 가 모듈 레벨 변수이고(`websocket.ts:38-41`), 여러 훅이 `connect()` 를 호출해도 이미 활성이면 즉시 반환한다(`:88`). 또한 **`disconnect()` 는 앱 어디에서도 호출되지 않는다**(로그아웃 시에도 호출하지 않음 — 전수 grep 확인). 연결은 앱 수명 동안 유지된다.

Flutter 도 동일하게 **앱 전역 싱글톤 서비스** 하나가 연결과 구독 레지스트리를 소유한다. 화면(위젯)은 구독을 요청하고 해제 함수를 받아 `dispose` 에서 호출한다.

```dart
class _Subscriber {
  final String destination;
  final void Function(String body) handler;
  StompUnsubscribe? unsubscribe;   // 연결 끊기면 null 로 무효화
}
```

- `onConnect` 콜백 안에서 **레지스트리의 모든 구독을 다시 등록한다**(웹의 `reactivateAllSubscribers` 대응). `stomp_dart_client` 는 재연결 시 구독을 자동 복원하지 않으므로 이 처리가 없으면 재연결 후 데이터가 오지 않는다.
- 소켓이 닫히면 보관 중인 `unsubscribe` 핸들을 전부 `null` 로 만든다(웹의 `invalidateSubscriptions` 대응).
- 아직 연결되지 않은 상태에서 구독을 요청하면 레지스트리에만 넣어 두고, `onConnect` 시점에 실제 `SUBSCRIBE` 를 보낸다(`websocket.ts:43-49` 대응).

#### 3.6.2 `stomp_dart_client` 설정 시 주의점

```dart
StompClient(
  config: StompConfig(
    url: wsBaseUrl,                                   // 'ws(s)://{host}/ws'
    heartbeatIncoming: const Duration(seconds: 10),   // 서버 send 10s
    heartbeatOutgoing: const Duration(seconds: 8),    // 서버 receive 10s + 마진 2s
    connectionTimeout: const Duration(seconds: 5),    // websocket.ts:96
    reconnectDelay: Duration.zero,                    // 내장 재연결 비활성화 (아래 설명)
    onConnect: _onConnect,
    onWebSocketDone: _onWebSocketDone,
    onStompError: (frame) => log('STOMP error: ${frame.headers['message']}'),
    webSocketConnectHeaders: {},                      // §3.6.3 참조
  ),
)
```

| 함정 | 설명 |
|---|---|
| **SockJS 생성자 금지** | `StompConfig.sockJS(...)` 를 쓰면 안 된다. 서버는 SockJS 엔드포인트를 열지 않았으므로 `/ws/info` 조회에서 실패한다. 반드시 기본 `StompConfig(url: ...)` 를 사용한다 |
| **지수 백오프 직접 구현** | `StompConfig.reconnectDelay` 는 **고정 지연**만 지원한다. 웹의 `min(1000 * 2^n, 30000)` 백오프를 재현하려면 `reconnectDelay: Duration.zero` 로 내장 재연결을 끄고, `onWebSocketDone` 에서 시도 횟수를 증가시켜 직접 `Timer` 로 `activate()` 를 예약한다. `onConnect` 에서 시도 횟수를 0 으로 되돌린다 |
| **하트비트 마진** | outgoing 을 8초로 낮춘다. 협상값 그대로(10초) 두면 모바일 타이머 유예로 서버 판정 기준을 넘길 수 있다 |
| **강제 재연결** | `deactivate()` 는 정상 종료 절차를 밟느라 좀비 소켓에서 오래 걸릴 수 있다. 백그라운드 복귀 시에는 소켓을 즉시 끊고 새 클라이언트를 만드는 편이 확실하다 |
| **BigDecimal 역직렬화** | `price`/`changeRate`/`quoteTurnover` 는 JSON number 로 온다. Dart 에서 `(json['price'] as num).toDouble()` 로 받는다. `int` 로 올 수도 있으므로 `as double` 캐스팅은 KRW 정수 가격에서 런타임 오류를 낸다 |
| **`timestamp`** | epoch **밀리초**다. `DateTime.fromMillisecondsSinceEpoch` 를 쓴다. 단 플래시 판정에는 `int` 원본값 비교로 충분하다 |
| **`executedAt`** | 타임존 없는 ISO-8601 `LocalDateTime` 문자열이다. `DateTime.parse` 는 이를 **로컬 시각**으로 해석한다. 서버 시각대와 다르면 어긋나므로 표시용으로만 쓰고 시간 계산에 쓰지 않는다 |
| **Android cleartext** | `ws://`(비TLS) 는 Android 9+ 에서 기본 차단된다. 개발 환경에서는 `android:usesCleartextTraffic="true"` 를, 운영에서는 `wss://` 를 쓴다 |
| **iOS ATS** | 마찬가지로 `ws://` 는 App Transport Security 에 막힌다. 개발용 예외 설정이 필요하며, 운영은 `wss://` 를 전제로 한다 |

#### 3.6.3 사용자별 큐 인증 — 세션 쿠키를 핸드셰이크에 실어야 하는가

**현재 서버 기준으로는 실을 필요가 없다.** §3.1.6 에서 확인했듯 서버는 WebSocket 핸드셰이크에서 어떤 인증 정보도 읽지 않는다. 티커 토픽은 공개 데이터이며 인증 없이 구독된다. 세션 쿠키를 보내더라도 서버가 무시하므로 사용자별 큐가 살아나지 않는다.

다만 §3.2.2 의 서버 수정이 이뤄진다면(핸드셰이크에서 `SESSION` 쿠키를 읽어 `Principal` 부착), Flutter 는 **브라우저와 달리 쿠키를 자동으로 붙이지 않으므로 직접 실어야 한다.**

```dart
webSocketConnectHeaders: {
  'Cookie': 'SESSION=$sessionId',
},
```

웹은 `fetch` 기본 `credentials: "same-origin"` 에 의존한다(`client.ts:47-54` 에 `credentials` 지정이 없고, nginx 가 API 와 정적 자산을 같은 오리진으로 서빙하므로 쿠키가 자동으로 실린다). `HttpOnly` 는 **브라우저 JavaScript 의 접근만 막는 속성이며 네이티브 HTTP 클라이언트에는 제약이 아니다.**

STOMP `CONNECT` 프레임의 `login`/`passcode` 헤더나 `stompConnectHeaders` 는 이 서버에서 사용하지 않는다. 인증을 붙인다면 **HTTP 업그레이드 요청의 `Cookie` 헤더**(`webSocketConnectHeaders`)가 유일하게 유효한 통로다.

#### 3.6.4 이식 체크리스트

| 웹 동작 | Flutter 대응 | 필수 여부 |
|---|---|---|
| raw WS + STOMP 1.2, `/ws` | `StompConfig(url:)` (SockJS 생성자 금지) | 필수 |
| heartbeat 10s/10s | `heartbeatIncoming: 10s`, `heartbeatOutgoing: 8s` | 필수 |
| `connectionTimeout: 5000` | `connectionTimeout: Duration(seconds: 5)` | 필수 |
| 지수 백오프 2s→30s | 내장 재연결 끄고 직접 구현 | 필수 |
| 재연결 시 구독 복원 | `onConnect` 에서 레지스트리 전체 재구독 | 필수 |
| `/topic/tickers.{1\|2\|3}` 배열 수신 | JSON 배열 파싱, 파싱 실패·빈 배열 무시 | 필수 |
| symbol 기준 병합, 초기 목록이 기준 | `Map<String, Ticker>` 병합, 목록 밖 심볼 무시 | 필수 |
| 거래소 전환 시 틱 맵 초기화 | 동일 | 필수 |
| rAF 배치 flush | 16 ms 타이머/프레임 콜백으로 flush | 필수 (성능) |
| 목록 가상화 | `ListView.builder(itemExtent: 68)` | 필수 (성능) |
| 플래시: tickedAt 변화 + 100 ms 테두리 | 행 로컬 상태 + `Timer(100ms)` | 필수 |
| `visibilitychange` 좀비 재연결 (20초 임계) | `didChangeAppLifecycleState` 동일 로직 | 필수 |
| `/user/{userId}/queue/events` 구독 | **현행 서버에서 미동작.** 체결 반영은 REST 재조회로 대체 | 대체 구현 |
| WS `disconnect()` 미호출 (앱 수명 내내 유지) | 전역 싱글톤 유지 | 필수 |
| 네트워크 전환 감지 | (웹에 없음) `connectivity_plus` 로 강제 재연결 | 모바일 추가 |
| 포그라운드 복귀 시 REST 스냅샷 재조회 | (웹에 없음) 누락 틱 보정 | 모바일 추가 |

---
## 4. 화면: 마켓

마켓 화면은 앱의 중심 화면이며, 실시간 시세 목록·캔들 차트·주문·긴급 자금 투입을 한 화면에 담는다. 웹 구현은 `frontend/src/pages/MarketPage.tsx` 가 상태를 모두 쥐고 하위 컴포넌트에 내려주는 구조다.

### 4.1 화면 구성 요소와 데이터 흐름

#### 4.1.1 구성 요소 트리

| 영역 | 컴포넌트 | 파일 |
|---|---|---|
| 페이지 헤더 | `코인 시세` / `{거래소명} 기준 · {기준통화} 마켓` | `MarketPage.tsx:170-181` |
| 주요 코인 카드 | `MarketOverviewCards` | `components/market/MarketOverviewCards.tsx` |
| 거래소 탭 + 필터 칩 | `ExchangeTabs`, `FilterChips` | `ExchangeTabs.tsx`, `FilterChips.tsx` |
| 캔들 차트 | `CandleChartPanel` | `CandleChartPanel.tsx` |
| 코인 목록(검색창 내장) | `CoinTable` + `CoinSearchInput` | `CoinTable.tsx`, `CoinSearchInput.tsx` |
| 사이드 패널 | `EmergencyFundingCard`, `OrderPanel` | `components/round/EmergencyFundingCard.tsx`, `market/OrderPanel.tsx` |

레이아웃은 `activeRound` 존재 여부로 갈린다. 라운드가 있으면 `lg:grid-cols-[minmax(0,1fr)_360px]` 2열(좌: 차트+목록, 우: 긴급 자금+주문), 없으면 1열이며 사이드 패널 자체가 렌더링되지 않는다 (`MarketPage.tsx:205-255`). 즉 **라운드가 없으면 주문 패널과 긴급 자금 카드는 화면에 없다.** 시세 조회 자체는 계속 되며, `getWalletId` 가 `null` 을 돌려 주문 대상 해석이 `NO_ROUND` 로 실패한다.

#### 4.1.2 거래소 정의 (하드코딩)

`frontend/src/lib/types/coins.ts:49-53`

| id | key | name | baseCurrency |
|---|---|---|---|
| 1 | `upbit` | 업비트 | KRW |
| 2 | `bithumb` | 빗썸 | KRW |
| 3 | `binance` | 바이낸스 | USDT |

`id` 는 REST 경로(`/api/exchanges/{id}/coins`), STOMP 토픽(`/topic/tickers.{id}`), 라운드 지갑 매핑(`wallet.exchangeId`), 긴급 자금 요청의 `exchangeId` 에 모두 그대로 쓰인다. 캔들 API 에는 `key` 를 대문자로 바꾼 코드(`UPBIT`/`BITHUMB`/`BINANCE`)를 쓴다.

웹은 선택 거래소를 URL 쿼리 `?exchange=upbit` 로 보존한다(`MarketPage.tsx:27,37`). Flutter 에서는 라우트 쿼리(go_router `?exchange=`)에 유지해 딥링크·복원이 가능하게 한다.

#### 4.1.3 API 호출 시점

| 시점 | 호출 | 응답 |
|---|---|---|
| 화면 진입 / 거래소 변경 | `GET /api/exchanges/{exchangeId}/coins` | `ExchangeCoinResponse[]` |
| 화면 진입 (상시) | STOMP 구독 `/topic/tickers.{exchangeId}` | `Ticker[]` |
| 로그인 상태 | STOMP 구독 `/user/{userId}/queue/events` | **현행 미동작 (§3.2.2)** |
| 코인 선택 / 간격 변경 | `GET /api/candles?exchange&coin&interval&limit&cursor` | `CandleResponse[]` |
| 주문 대상 확정 시 | `GET /api/orders/available?walletId&exchangeCoinId&side` (BUY/SELL 병렬 2회) | `{available, currentPrice}` |
| 거래내역 탭 진입 / 필터 변경 / 더보기 | `GET /api/orders?walletId&exchangeCoinId&status&cursorOrderId&size=20` | `CursorPageResponseDto<OrderHistoryItem>` |
| 주문 버튼 | `POST /api/orders` | `PlaceOrderResponse` |
| 미체결 취소 | `POST /api/orders/{orderId}/cancel` `{walletId}` | `{orderId, status}` |
| 긴급 자금 확정 | `POST /api/rounds/{roundId}/emergency-funding` | `{roundId, exchangeId, chargedAmount, remainingChargeCount}` |

#### 4.1.4 실시간 시세 병합

§3.3.2 의 규칙을 그대로 따른다. 요점만 다시 짚는다.

- **`changeRate` 는 비율값이다** (0.0123 = +1.23%). 표시할 때만 100을 곱한다(`lib/formatters.ts:99-103`).
- 티커의 `quoteTurnover` 가 화면 모델의 `volume` 에 들어간다.
- 거래소를 바꾸면 티커 맵을 비우고 새 토픽을 구독한다.
- 앱이 백그라운드에 20초 넘게 있다가 복귀하면 강제 재연결한다(§3.5).

#### 4.1.5 거래소 전환 시 초기화

`MarketPage.tsx:152-157` — 거래소를 바꾸면 검색어, 필터(`all`), 선택 코인(`null`)을 모두 초기화한다. **정렬 상태는 초기화하지 않는다.**

---

### 4.2 코인 목록

#### 4.2.1 컬럼 (`CoinTable.tsx:136-141`)

| 컬럼 | 정렬 키 | 내용 | 포맷 |
|---|---|---|---|
| 코인명 | `name` | 상단 `symbol`(13px, semibold) / 하단 `name`(11px, 회색) | — |
| 현재가 | `price` | `{통화기호}{금액}`, 우측 정렬, 등락에 따라 색상 | `formatPrice` |
| 전일대비 | `change` | 알약형 배지 | `formatChangeRate` |
| 거래대금(24H) | `volume` | 우측 정렬, 회색 | `formatVolume` |

`currentPrice <= 0` 이면 현재가·전일대비를 `-` 로 표시하고, `volume <= 0` 이면 거래대금을 `-` 로 표시한다(`CoinTable.tsx:78-113`).

포맷 규칙의 정확한 정의는 §8.5(formatters.ts → Dart 이식 규칙)를 따른다. 요약: `getCurrencySymbol` 은 KRW → `₩`, SOL → `◎`, 그 외(USDT 포함) → 빈 문자열. `formatPrice` 는 **통화 기호를 붙이지 않으므로** 호출부에서 기호를 이어 붙인다 — 따라서 **코인 목록의 USDT 가격에는 `$` 가 붙지 않는다.**

#### 4.2.2 정렬 규칙

비교 함수(`MarketPage.tsx:101-112`):

- `name` → `a.symbol.compareTo(b.symbol)` (**코인명이 아니라 심볼 사전순**)
- `price` → `a.currentPrice - b.currentPrice`
- `change` → `a.changeRate - b.changeRate`
- `volume` → `a.volume - b.volume`
- `dir === "asc"` 면 그대로, `"desc"` 면 부호 반전.

정렬 상태 전이(`hooks/useSort.ts:34-41`): 같은 컬럼을 다시 누르면 `desc ↔ asc` 토글, 다른 컬럼을 누르면 그 컬럼으로 바꾸고 방향을 `desc` 로 되돌린다.

**기본 정렬은 `volume` `desc`** (거래대금 많은 순, `MarketPage.tsx:114-118`).

정렬 아이콘(`components/ui/SortIcon.tsx`): 비활성 컬럼은 양방향 화살표(투명도 30%), 활성 컬럼은 방향에 맞는 단방향 화살표(primary 색).

#### 4.2.3 차트 기본 코인 선택 규칙

`MarketPage.tsx:120-130` — 사용자가 아무 코인도 선택하지 않았으면, **검색·필터를 적용하지 않은 전체 목록**을 현재 정렬 기준으로 세운 뒤 첫 번째 코인을 차트/주문 대상으로 쓴다. 검색으로 좁혀진 목록을 따라가면 글자를 칠 때마다 차트가 바뀌기 때문이다. **이 동작을 Flutter 에서도 그대로 유지한다.**

#### 4.2.4 필터

`FilterChips.tsx:10-14` — 전체(`all`) / 상승(`rising`) / 하락(`falling`). 적용식(`MarketPage.tsx:89-96`):

- `rising` → `changeRate > 0`
- `falling` → `changeRate < 0`
- `changeRate === 0` 인 코인은 상승·하락 어느 쪽에도 잡히지 않는다.

검색 → 필터 순으로 적용한 뒤 정렬한다.

#### 4.2.5 검색 (초성·자모 매칭)

입력 컴포넌트: `CoinSearchInput.tsx`. 플레이스홀더 `코인명/심볼 검색 (초성 가능)`, ESC 키로 비우기, 값이 있으면 우측에 지우기 버튼.

검색 색인(`MarketPage.tsx:56-66`): **정적 코인 목록을 받은 시점에 한 번만** 코인 이름의 초성 문자열과 자모 문자열을 만들어 `symbol → {chosung, jamo}` 맵에 넣는다(둘 다 소문자화). 시세가 갱신돼도 이름은 바뀌지 않으므로 재계산하지 않는다.

매칭 알고리즘(`MarketPage.tsx:68-99`):

```
query = searchQuery.trim().toLowerCase()
if query 가 비어 있지 않으면:
    chosungQuery = isChosungQuery(query)          // ^[ㄱ-ㅎ]+$
    jamoQuery    = toJamo(query)
    코인 통과 조건:
        coin.symbol.toLowerCase().contains(query)                       // 1) 심볼 부분 일치
        또는 (chosungQuery ? index.chosung.startsWith(query)            // 2) 자음만 입력 → 초성 '앞부분' 일치
                          : index.jamo.contains(jamoQuery))             // 3) 그 외 → 자모 부분 일치
```

주의할 점 두 가지다. (가) 초성 검색은 `contains` 가 아니라 **`startsWith`** 다. (나) 자모 경로는 부분 일치(`contains`)다.

한글 변환 로직(`frontend/src/lib/hangul.ts`)의 Dart 이식:

```dart
const int _hangulFirst = 0xAC00; // '가'
const int _hangulLast  = 0xD7A3; // '힣'
const int _jungsungCount = 21;
const int _jongsungCount = 28;

const List<String> _chosung = [
  'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
  'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ',
];
const List<String> _jungsung = [
  'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ',
  'ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ',
];
const List<String> _jongsung = [
  '','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ',
  'ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ',
  'ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ',
];

// 겹자모는 자판에서 두 번에 나눠 눌리므로, 분해도 눌린 순서대로 푼다.
const Map<String, String> _compound = {
  'ㄲ':'ㄱㄱ','ㄳ':'ㄱㅅ','ㄵ':'ㄴㅈ','ㄶ':'ㄴㅎ','ㄸ':'ㄷㄷ',
  'ㄺ':'ㄹㄱ','ㄻ':'ㄹㅁ','ㄼ':'ㄹㅂ','ㄽ':'ㄹㅅ','ㄾ':'ㄹㅌ',
  'ㄿ':'ㄹㅍ','ㅀ':'ㄹㅎ','ㅃ':'ㅂㅂ','ㅄ':'ㅂㅅ','ㅆ':'ㅅㅅ',
  'ㅉ':'ㅈㅈ','ㅘ':'ㅗㅏ','ㅙ':'ㅗㅐ','ㅚ':'ㅗㅣ','ㅝ':'ㅜㅓ',
  'ㅞ':'ㅜㅔ','ㅟ':'ㅜㅣ','ㅢ':'ㅡㅣ',
};

String _split(String jamo) => _compound[jamo] ?? jamo;
bool _isSyllable(int code) => code >= _hangulFirst && code <= _hangulLast;

/// '비트코인' → 'ㅂㅌㅋㅇ'. 한글이 아닌 글자는 그대로 둔다.
String toChosung(String text) {
  final buffer = StringBuffer();
  for (final rune in text.runes) {
    if (_isSyllable(rune)) {
      final offset = rune - _hangulFirst;
      buffer.write(_chosung[offset ~/ (_jungsungCount * _jongsungCount)]);
    } else {
      buffer.writeCharCode(rune);
    }
  }
  return buffer.toString();
}

/// '비트코인' → 'ㅂㅣㅌㅡㅋㅗㅇㅣㄴ'. 조합 중인 글자도 같은 규칙으로 풀린다.
String toJamo(String text) {
  final buffer = StringBuffer();
  for (final rune in text.runes) {
    if (_isSyllable(rune)) {
      final offset = rune - _hangulFirst;
      final cho = _chosung[offset ~/ (_jungsungCount * _jongsungCount)];
      final jung = _jungsung[(offset ~/ _jongsungCount) % _jungsungCount];
      final jong = _jongsung[offset % _jongsungCount];
      buffer..write(_split(cho))..write(_split(jung))..write(_split(jong));
    } else {
      buffer.write(_split(String.fromCharCode(rune)));
    }
  }
  return buffer.toString();
}

final RegExp _consonantsOnly = RegExp(r'^[ㄱ-ㅎ]+$');
bool isChosungQuery(String text) => _consonantsOnly.hasMatch(text);
```

설계 의도(`hangul.ts:30-39, 66-71`): 겹자모를 자판 입력 순서대로 풀기 때문에, 조합 중인 글자(`'빝'` → `ㅂㅣㅌ`)가 완성된 이름(`'비트'` → `ㅂㅣㅌㅡ`)의 앞부분과 그대로 맞아떨어진다. Flutter 의 `TextField.onChanged` 는 한글 조합 중에도 중간 문자열을 전달하므로 이 알고리즘이 그대로 동작한다. 영문 이름은 자모 변환이 항등 함수이므로 `jamo.contains` 경로로 자연히 부분 일치한다.

#### 4.2.6 가상 스크롤

`CoinTable.tsx:25-31`, `hooks/useVirtualList.ts`

- 행 높이 고정 **68px**, 보이는 행 8개 → 목록 상자 높이 최대 **544px**. 코인이 8개 미만이면 `coins.length * 68` 로 줄인다.
- 목록은 페이지가 아니라 **자체 스크롤 상자** 안에서 스크롤한다. 상장 코인이 거래소당 최대 600개를 넘어 전부 그리면 실시간 시세가 들어올 때마다 수천 노드를 다시 훑게 된다(`useVirtualList.ts:10-16`).
- `@tanstack/react-virtual`, overscan 6.
- 목록이 비면 `검색 결과가 없습니다.` 를 높이 192px 영역에 표시한다.

Flutter 에서는 `ListView.builder` + `itemExtent: 68` 로 동일한 효과를 얻는다(별도 가상화 라이브러리 불필요).

#### 4.2.7 가격 갱신 표시(플래시)

§3.3.3 의 규칙을 그대로 따른다.

#### 4.2.8 주요 코인 카드

`MarketOverviewCards.tsx` — 3열 그리드. 표시 대상은 `baseCurrency === "SOL"` 이면 `JUP/BONK/RAY`, 그 외에는 **`BTC/ETH/SOL`** 고정(`:22-23,38`). 현행 거래소 목록에 SOL 마켓이 없으므로 실제로는 항상 `BTC/ETH/SOL` 이다. 목록에 없는 심볼은 건너뛰고, 하나도 없으면 카드 영역을 통째로 렌더링하지 않는다. 각 카드는 심볼·이름·등락률 배지·현재가를 보여준다. 등락률은 `(changeRate*100).toFixed(2)` 에 양수면 `+` 를 붙인다.

---

### 4.3 캔들 차트 (`frontend/src/components/market/CandleChartPanel.tsx`)

#### 4.3.1 데이터 소스 — 두 갈래

차트는 **서버 캔들(REST)** 과 **실시간 체결가(STOMP 티커)** 를 함께 소비한다. 화면에 그려지는 것은 둘을 합친 결과(`mergedCandles`)이며, 합성 규칙은 §4.3.5 에 있다. 정적 캔들만 그리는 구현은 이 화면의 요구를 충족하지 못한다.

**① 서버 캔들 (REST)**

`GET /api/candles?exchange={UPBIT|BITHUMB|BINANCE}&coin={심볼}&interval={간격}&limit={개수}` (`candle-api.ts:81-113`). 서버는 `limit` 을 1~200 으로 제한한다.

클라이언트 후처리(`:96-112`):
1. `time` 필드가 없으면 `timestamp` 필드를 쓴다(**서버는 `time` 만 내리므로 Flutter 는 `time` 만 파싱**).
2. OHLC 를 숫자로 변환하고, 유한하지 않은 값이 하나라도 있는 캔들은 버린다.
3. `time` 을 간격 단위로 **로컬 시간 기준 절삭**한다(`normalizeCandleTime` — §4.3.5.1).
4. 시간 오름차순 정렬한다.

**② 실시간 체결가 (STOMP)**

`/topic/tickers.{exchangeId}` 를 **차트가 열려 있는 동안** 구독한다(`CandleChartPanel.tsx:229-246`). 이를 위해 패널은 거래소 **코드**(`upbit`)와 별개로 거래소 **id**(`1`)를 함께 받는다 — `CandleChartPanel({exchangeKey, exchangeId, baseCurrency, coin})` (`MarketPage.tsx:214-219`).

- 콜백이 받는 배열에서 **자기 심볼 하나만** 고르고 나머지는 버린다.
- `price` 가 유한하지 않거나 `<= 0` 이면 그 틱을 버린다.
- 살아남은 틱은 `normalizeCandleTime(ticker.timestamp, interval)` 로 봉 시각을 구한 뒤 `foldTick` 에 넣는다(§4.3.5.2).

**백엔드 변경은 없다.** 캔들 API·티커 토픽 모두 기존 계약 그대로이며, 합성은 전적으로 클라이언트 책임이다.

차트 헤더의 현재가·등락률은 캔들이 아니라 **실시간 티커의 `coin.currentPrice` / `coin.changeRate`** 를 쓴다(`CandleChartPanel.tsx:559-572`). 즉 헤더는 부모(`MarketPage`)가 내려주는 티커 값을, 봉은 패널이 직접 구독한 티커를 쓴다.

#### 4.3.2 지원 간격과 상수 (`CandleChartPanel.tsx:23-54`)

간격별 값 (`INTERVAL_OPTIONS` `:25-32`, `DEFAULT_VISIBLE_COUNT` `:34-41`)

| 값 | 라벨 | 조회 개수(`candleCount` → `limit`) | 최초 표시 개수(`DEFAULT_VISIBLE_COUNT`) |
|---|---|---|---|
| `1m` | 1분 | 120 | 40 |
| `1h` | 1시간 | 96 | 32 |
| `4h` | 4시간 | 84 | 28 |
| `1d` | 일 | 90 | 32 |
| `1w` | 주 | 72 | 24 |
| `1M` | 월 | 48 | 18 |

전역 상수

| 상수 | 값 | 의미 |
|---|---|---|
| 기본 간격 | `1d` | `useState<CandleInterval>("1d")` (`:169`) |
| `MIN_VISIBLE_COUNT` | 12 | 줌인 하한. 표시 개수는 `[12, 전체 개수]` 로 가둔다 (`:43`) |
| `LIVE_CANDLE_LIMIT` | **4** | 브라우저가 들고 있는 실시간 봉의 최대 개수. 초과분은 **앞에서** 버린다 (`:46`, §4.3.5.2) |
| `RECONCILE_DELAY_MS` | **15,000** | 새 봉이 열린 뒤 서버 캔들을 다시 조회하기까지의 대기 시간 (`:47`, §4.3.6) |
| `limit` 기본값 | 60 | `INTERVAL_OPTIONS` 에서 간격을 못 찾았을 때의 대비값 (`:198`) |
| `CHART_WIDTH` × `CHART_HEIGHT` | 960 × 440 | SVG `viewBox` (`:48-49`) |
| `PADDING` | `{top:20, right:124, bottom:42, left:20}` | 오른쪽 124px 은 y축 가격 라벨 자리 (`:50`) |
| 툴팁 | 220 × 138, 오프셋 `(+10, +10)` | `TOOLTIP_WIDTH`/`TOOLTIP_HEIGHT`/`TOOLTIP_OFFSET_*` (`:51-54`) |

`LIVE_CANDLE_LIMIT` 이 4인 근거(`:44-45` 주석): 봉이 닫혀도 서버 캔들은 InfluxDB 집계 주기(1분 + 오프셋 10초)만큼 늦게 나온다. 그동안 실시간 봉이 자리를 지켜야 하므로, 서버가 따라잡을 때까지 최근 몇 개를 들고 있는다.

코인·거래소·간격이 바뀌면 서버 캔들과 실시간 봉을 **모두** 폐기하고 다시 조회하며, 표시 구간을 기본값으로 되돌리고 호버 상태를 해제한다(§4.3.7).

#### 4.3.3 렌더링 방식

**SVG 직접 렌더링**이다(차트 라이브러리 없음). Flutter 에서는 `CustomPainter` 로 이식한다.

그리는 대상은 서버 캔들이 아니라 **합성 결과 `mergedCandles`**(§4.3.5.3)를 잘라낸 표시 구간이다. 아래 스케일 계산은 그 표시 구간에 대해 매번 다시 수행된다 — 즉 실시간 봉이 갱신되어 고가·저가가 바뀌면 y 스케일도 함께 바뀐다.

좌표계: `viewBox = 0 0 960 440`, 패딩 `{top:20, right:124, bottom:42, left:20}`. 오른쪽 124px 은 y축 가격 라벨 자리다.

스케일 계산(`:292-350`):

```
minPrice   = 보이는 캔들의 low 최솟값
maxPrice   = 보이는 캔들의 high 최댓값
range      = (maxPrice - minPrice) || (maxPrice * 0.02) || 1
paddedMin  = max(0, minPrice - range * 0.08)
paddedMax  = maxPrice + range * 0.08
plotWidth  = 960 - 20 - 124 = 816
plotHeight = 440 - 20 - 42  = 378
slotWidth  = plotWidth / 보이는캔들수
candleWidth= clamp(slotWidth * 0.62, 6, 16)
getX(i)    = 20 + (i + 0.5) * slotWidth
getY(p)    = 20 + (1 - (p - paddedMin) / (paddedMax - paddedMin)) * plotHeight
```

- y축 눈금 4개: `paddedMax` 부터 `(paddedMax-paddedMin)/3` 씩 내려가며, 점선 격자(`4 6` 대시)와 우측 정렬 가격 라벨(13px).
- x축 눈금: `step = max(1, floor(보이는캔들수 / 4))` 마다, 그리고 마지막 캔들에 항상 하나. 라벨 포맷(`:66-96`)은 `1m`→`HH:mm`, `1h`/`4h`→`M/d HH시`, `1M`→`YY. M`, 그 외(`1d`,`1w`)→`M/d` (모두 `ko-KR` 로케일).
- 캔들: 심지는 `high~low` 를 잇는 선(굵기 1.6, 둥근 끝), 몸통은 `min(openY,closeY)` 에서 시작하는 사각형(모서리 반경 2, **최소 높이 2px**). `close >= open` 이면 상승색(`--positive`), 아니면 하락색(`--negative`). 호버 중인 캔들만 불투명도 1, 나머지는 0.9.

#### 4.3.4 상호작용 (웹)

| 조작 | 동작 | 근거 |
|---|---|---|
| 마우스 이동(호버) | 크로스헤어 + 툴팁 | `:411-477, 693-747` |
| 드래그(포인터 다운→이동) | 좌우 패닝 | `:403-426` |
| 휠 | 좌우 패닝 | `:515-518` |
| Ctrl/Cmd + 휠 | 확대/축소 (한 번에 4개씩) | `:506-513` |

- **히트 테스트**(`:439-454`): 커서 x 가 캔들 중심에서 `candleWidth` 이내이고, y 가 `[min(highY,lowY) - 10, max(highY,lowY) + 10]` 범위 안일 때만 그 캔들이 호버 대상이다. 플롯 영역(패딩 안쪽) 밖이면 호버 해제.
- **크로스헤어**: 호버 캔들의 x 에 수직 점선(투명도 0.24), 종가 y 에 수평 점선(투명도 0.16). 둘 다 `4 4` 대시.
- **툴팁**(220×138): 시각(`YY. M. D. HH:mm:ss`, `ko-KR`)과 시가/고가/저가/종가 4행. 커서에서 `(+10, +10)` 오프셋으로 띄우되, 컨테이너 경계를 넘으면 반대편으로 뒤집고 최종적으로 8px 여백 안에 가둔다.
- **패닝**(`:362-372, 421-426`): `movedCandles = round(dragDeltaX / slotWidth)`, `endIndex = 시작endIndex - movedCandles` 를 `[visibleCount, mergedCandles.length]` 로 가둔다. 즉 최신 캔들보다 오른쪽으로는 못 넘어간다. 경계 판정은 서버 캔들이 아니라 **합성 결과의 길이** 기준이다.
- **줌**(`:374-401`): 커서 위치의 캔들을 앵커로 삼아 확대/축소한다. `visibleCount` 를 `[12, mergedCandles.length]` 로 가두고, 앵커가 화면에서 차지하던 비율(`ratio`)을 유지하도록 새 시작 인덱스를 계산한다.

상태 표시: 로딩 중 `캔들 데이터를 불러오는 중...`, 그릴 캔들이 하나도 없으면 `캔들 데이터가 부족합니다`(높이 256px 영역). 캔들 API 실패는 조용히 삼키고 빈 상태를 유지한다(`:200-202`). 단 **서버 캔들이 0개여도 실시간 봉이 쌓이면 차트를 그린다** — 로딩 완료 시 표시 개수를 받아온 개수로 줄이지 않는 이유가 이것이다(`:214-215`).

#### 4.3.5 실시간 갱신 — 합성 규칙 (`CandleChartPanel.tsx:109-160, 227-255`)

서버 캔들 배열(`candles`)과 실시간 봉 배열(`liveCandles`)은 **따로 보관**하고, 그릴 때 합친다. 실시간 봉이 서버 배열을 덮어쓰지 않으므로, 재조정(§4.3.6)으로 서버 값이 갱신되면 합성 결과가 저절로 정정된다.

```
candles     : CandleItem[]   // 서버 캔들 (REST, 시간 오름차순)
liveCandles : CandleItem[]   // 실시간 봉 (최대 LIVE_CANDLE_LIMIT=4개, 시간 오름차순)
mergedCandles = mergeLiveCandles(candles, liveCandles)   // 화면에 그리는 것
```

##### 4.3.5.1 `normalizeCandleTime` — 체결 시각을 어느 봉에 떨어뜨리는가

`candle-api.ts:30-66`. 서버 캔들의 `time` 과 티커의 `timestamp` **양쪽에 같은 함수를 적용**한다. 두 출처의 봉 시각이 문자열로 정확히 일치해야 합성이 성립하므로, 이 함수는 두 경로의 유일한 공통 기준이다.

```
function normalizeCandleTime(time, interval) -> String:
    d = 로컬 시간대의 날짜·시각으로 파싱(time)   # ★ UTC 가 아니라 단말 로컬 기준으로 절삭한다
    if 파싱 실패: return time                    # 웹은 원문을 그대로 돌려준다

    d.millisecond = 0
    switch interval:
        "1m": d.second = 0                                     # 분 단위 (분은 건드리지 않는다)
        "1h": d.minute = 0; d.second = 0
        "4h": d.minute = 0; d.second = 0
              d.hour = floor(d.hour / 4) * 4                   # 0,4,8,12,16,20 시로 내림
        "1d": d.hour = d.minute = d.second = 0                 # 그날 자정
        "1w": d.hour = d.minute = d.second = 0
              diff = (d.weekday == 일요일) ? 6 : d.weekday - 1
              d.day = d.day - diff                             # 그 주 월요일 자정
        "1M": d.hour = d.minute = d.second = 0
              d.day = 1                                        # 그 달 1일 자정
    return d.toIsoUtcString()                                  # 비교·저장은 UTC ISO 문자열
```

절삭은 **로컬 시간 기준**, 결과 표현은 **UTC ISO 문자열**이다. Flutter 이식 시 `DateTime.parse(...).toLocal()` 로 내린 뒤 절삭하고, `toUtc().toIso8601String()` 으로 되돌린다. 티커의 `timestamp` 는 epoch 밀리초이므로 `DateTime.fromMillisecondsSinceEpoch`(로컬)로 만든 뒤 같은 절삭을 적용한다.

##### 4.3.5.2 `foldTick` — 진행 중인 봉 만들기

`CandleChartPanel.tsx:110-130`. 체결 하나를 실시간 봉 배열에 접어 넣는다. 시가·고가·저가·종가가 각각 어떻게 정해지는지가 규칙의 전부다.

```
function foldTick(liveCandles, bucketTime, price) -> CandleItem[]:
    opened = liveCandles.last            # 없으면 null

    # ① 같은 봉 → 갱신. open 은 절대 건드리지 않는다.
    if opened != null and opened.time == bucketTime:
        updated = {
            time:  opened.time,
            open:  opened.open,                       # 그대로
            high:  max(opened.high, price),
            low:   min(opened.low,  price),
            close: price,                             # 마지막 체결가
        }
        return liveCandles[0 .. -2] + [updated]

    # ② 이미 넘어간(닫힌) 봉에 뒤늦게 도착한 체결 → 버린다.
    #    닫힌 봉을 다시 열면 서버 집계와 어긋난다.
    if opened != null and bucketTime < opened.time:
        return liveCandles

    # ③ 새 봉 → 네 값이 모두 이 체결가다. 그리고 최근 4개만 남긴다.
    fresh = { time: bucketTime, open: price, high: price, low: price, close: price }
    return (liveCandles + [fresh]).takeLast(LIVE_CANDLE_LIMIT)   # 초과분은 앞에서 버린다
```

| 필드 | 실시간 봉에서의 정의 |
|---|---|
| `open` | **그 봉에서 클라이언트가 처음 받은 체결가.** 서버가 집계한 진짜 시가가 아니다(구독 시작 전 체결을 못 봤을 수 있다). 서버 봉이 도착하면 §4.3.5.3 이 서버 `open` 으로 되돌린다 |
| `high` | 그 봉에서 본 체결가의 최댓값 |
| `low` | 그 봉에서 본 체결가의 최솟값 |
| `close` | 가장 최근 체결가 |

**틱을 프레임 단위로 솎아내면 안 된다**(`:227-228` 주석). 마켓 목록(`useTickers`)은 `requestAnimationFrame` 으로 프레임당 마지막 값만 반영하지만, 차트는 **들어오는 체결을 전부** `foldTick` 에 넣는다. 한 프레임 안의 체결을 버리면 그 봉의 고가·저가가 실제보다 얕아진다. 화면 갱신 빈도와 접기(fold) 빈도는 **분리해야 한다** — 접기는 매 틱, 그리기는 프레임당 1회(계획서 §5.4 참조).

##### 4.3.5.3 `mergeLiveCandles` — 서버 캔들 위에 실시간 봉을 얹기

`CandleChartPanel.tsx:133-160`. **같은 구간을 서버도 갖고 있으면 서버 값이 기준**이고, 클라이언트는 그 위에 이후 체결만 얹는다.

```
function mergeLiveCandles(candles, liveCandles) -> CandleItem[]:
    if liveCandles.isEmpty: return candles

    merged   = copy(candles)
    lastIdx  = merged.length - 1
    lastTime = (lastIdx < 0) ? -무한대 : merged[lastIdx].time     # 서버가 가진 마지막 봉의 시각

    for live in liveCandles:                # 시간 오름차순
        # ① 서버가 이미 확정한 과거 봉 → 무시한다 (재조정 뒤 남아 있는 낡은 실시간 봉이 여기로 온다)
        if live.time < lastTime: continue

        # ② 서버의 마지막 봉과 같은 구간 → 서버가 기준, 실시간을 덧댄다
        if live.time == lastTime:
            server = merged[lastIdx]
            merged[lastIdx] = {
                time:  server.time,
                open:  server.open,                       # ★ 시가는 서버 값
                high:  max(server.high, live.high),
                low:   min(server.low,  live.low),
                close: live.close,                        # ★ 종가는 최신 체결가
            }
            continue

        # ③ 서버보다 미래의 봉 → 그대로 뒤에 붙인다 (서버가 아직 집계하지 못한 구간)
        merged.append(live)

    return merged
```

- 비교 기준은 루프 내내 **서버의 마지막 봉 `lastTime` 하나**다(루프 안에서 `lastIdx`/`lastTime` 을 갱신하지 않는다). ③으로 붙은 봉끼리는 서로 병합하지 않는다 — `foldTick` 이 봉당 하나만 만들므로 중복이 생기지 않는다.
- 서버 캔들이 **0개**여도 동작한다: `lastTime = -무한대` 이므로 모든 실시간 봉이 ③으로 들어간다.
- 실시간 봉이 없으면 서버 배열을 **그대로**(같은 참조) 돌려준다.

#### 4.3.6 재조정(reconcile) — 닫힌 봉을 서버 값으로 교체 (`CandleChartPanel.tsx:262-281`)

봉이 새로 열렸다는 것은 **직전 봉이 닫혔다**는 뜻이다. 서버는 InfluxDB 집계 주기만큼 늦게 그 봉을 확정하므로, 그 시점을 기다렸다가 다시 조회해 클라이언트가 만들어 둔 봉을 서버 값으로 바꾼다.

```
openedBucket = liveCandles.isEmpty ? null : liveCandles.last.time

# openedBucket 이 바뀔 때마다(= 새 봉이 열릴 때마다) 한 번:
on openedBucket changed:
    이전 타이머가 있으면 취소
    if openedBucket == null: return
    타이머(RECONCILE_DELAY_MS = 15초) 예약:
        data = fetchCandles()                  # 같은 exchange/coin/interval/limit 로 재조회
        if data != null and data.isNotEmpty:
            candles = data                     # 서버 캔들만 교체
        # liveCandles 는 비우지 않는다
        # 뷰포트(visibleCount / anchorEndIndex / followingLatest / 호버)도 건드리지 않는다
```

- 타이머는 **틱마다가 아니라 봉이 바뀔 때만** 다시 걸린다. 같은 봉 안에서 체결이 아무리 많이 들어와도 재조회는 예약되지 않는다.
- 차트를 연 뒤 **첫 틱**도 `openedBucket` 을 `null → 값` 으로 바꾸므로 재조회가 한 번 예약된다.
- `liveCandles` 를 비우지 않는 이유: 재조회 결과가 그 봉을 포함하게 되면 `mergeLiveCandles` 의 규칙 ①(`live.time < lastTime` → 무시) 또는 ②(같은 시각 → 서버가 기준)가 낡은 실시간 봉을 자동으로 무해하게 만든다. 명시적으로 지울 필요가 없고, `LIVE_CANDLE_LIMIT = 4` 가 개수 상한을 준다.
- 조회가 실패하거나 빈 배열이면 **아무것도 하지 않는다.** 실시간 봉이 계속 자리를 지킨다.

#### 4.3.7 요청 키와 상태 폐기 (`CandleChartPanel.tsx:170-185, 206-250`)

`requestKey = "{exchangeKey}:{coin.symbol}:{interval}"`.

서버 캔들과 실시간 봉은 **각각 자기가 어느 요청의 결과인지를 함께 들고 있는다.**

```
loaded : { key: String, candles: CandleItem[] } | null
live   : { key: String, candles: CandleItem[] } | null

# 렌더 시점 판별 (이펙트에서 비우지 않는다)
candles     = (loaded.key == requestKey) ? loaded.candles : EMPTY
liveCandles = (live.key   == requestKey) ? live.candles   : EMPTY
loading     = (loaded.key != requestKey)
```

- 코인·거래소·간격을 바꾼 직후에는 키가 어긋나므로 **이전 코인의 캔들과 실시간 봉이 한 프레임도 화면에 남지 않는다.** 이펙트에서 `setState(null)` 로 비우는 방식은 "비우는 렌더"와 "새 데이터가 오는 렌더" 사이에 이전 값이 노출되는 경합이 있어 쓰지 않는다.
- `foldTick` 도 이 키를 확인한다. 키가 다르면 **빈 배열에서 다시 시작**한다(`:239-242`) — 이전 코인의 봉에 새 코인의 체결이 접히지 않는다.
- 조회가 끝나면 `visibleCount = DEFAULT_VISIBLE_COUNT[interval]`, `anchorEndIndex = candles.length`, `followingLatest = true`, 호버 해제로 되돌린다(`:213-219`).
- 데이터가 없는 구간에서는 **항상 같은 빈 배열 인스턴스**(`EMPTY_CANDLES` `:23`)를 돌려준다. 매번 새 배열을 만들면 합성 결과를 다시 계산하게 된다.

#### 4.3.8 최신 봉 추종 (`CandleChartPanel.tsx:174-176, 257-260, 362-372`)

```
endIndex = followingLatest ? mergedCandles.length
                           : min(anchorEndIndex, mergedCandles.length)
visibleStartIndex = max(0, endIndex - min(visibleCount, mergedCandles.length))
visibleCandles    = mergedCandles[visibleStartIndex .. endIndex)
```

- `followingLatest == true`(기본값)이면 오른쪽 끝은 **항상 마지막 봉**이다. 실시간 봉이 새로 열려 배열이 길어지면 화면이 저절로 따라간다.
- 사용자가 **과거로 패닝하면** `followingLatest = false` 가 되고 `anchorEndIndex` 에 멈춘다. 이후 실시간 봉이 아무리 늘어나도 보고 있는 구간은 움직이지 않는다.
- 패닝·줌은 매번 `followingLatest = (boundedEndIndex >= mergedCandles.length)` 를 다시 계산한다. 따라서 **오른쪽 끝까지 되밀면 추종이 자동으로 재개된다.**
- `min(anchorEndIndex, ...)` 가 있는 이유: 재조정으로 서버 캔들 개수가 줄어들면 합성 결과가 짧아질 수 있다. 그때 앵커가 배열 밖을 가리키지 않도록 잡아 준다.

#### 4.3.9 구독 수명과 중복 구독

- 차트의 티커 구독은 **차트가 화면에 있는 동안만** 유지된다. `exchangeId`·`coin.symbol`·`interval`(=`requestKey`)이 바뀌면 해제 후 재구독한다(`:229-246`).
- 웹은 **같은 토픽에 구독이 2개** 붙는다: 마켓 목록의 `useTickers`(`MarketPage.tsx:51-54`)와 차트 패널이 각각 `subscribeTickers(exchangeId, ...)` 를 호출하고, `websocket.ts:138-163` 은 같은 토픽을 합쳐 주지 않는다. 소켓은 하나지만 STOMP `SUBSCRIBE` 프레임이 둘이라 **같은 페이로드가 두 번 배달된다.**
- 이는 웹의 구현 사정이며 **이식해야 할 동작이 아니다.** Flutter 는 토픽 구독을 하나만 유지하고 그 결과를 목록과 차트에 나눠 준다(계획서 §5.4). 서버 계약상 티커는 스냅샷 스트림이므로 소비자가 몇이든 결과는 같다.
- 소켓이 없으면 `connect()` 를 호출한다(`:230-232`). 목록이 이미 연결해 둔 상태에서는 재사용된다.

#### 4.3.10 거래량 필드가 없다는 사실의 영향

캔들은 `{time, open, high, low, close}` 뿐이다. 서버 `CandleResponse` 에도 거래량이 없다(§1.6.9). 이 부재가 위 설계를 성립시킨다.

1. **실시간 봉을 체결가 하나만으로 완전하게 만들 수 있다.** 거래량 칸이 있었다면 봉 구간의 체결 수량을 누적해야 하는데, 티커의 `quoteTurnover` 는 **24시간 누적 거래대금**이라 봉 단위로 환산할 수 없고, 서버가 폭주 시 메시지를 버리므로(`DiscardOldestPolicy`, §3.1.7) 델타 누산 자체가 불가능하다. 즉 거래량이 있었다면 실시간 봉은 **원리적으로 정확히 만들 수 없었다.**
2. **서버 봉과 실시간 봉이 같은 필드 집합을 가진다.** 두 출처를 형 변환 없이 한 배열에 섞을 수 있고(§4.3.5.3), 재조정으로 통째 교체해도 표시가 달라지지 않는다.
3. **거래량 서브차트가 없다.** 차트 영역은 가격 축 하나만 다루면 된다. y 스케일은 보이는 구간의 `low` 최솟값 / `high` 최댓값만으로 정해진다(§4.3.3).

---

### 4.4 주문 패널 (`frontend/src/components/market/OrderPanel.tsx`)

라운드가 있고 선택 코인이 있을 때만 렌더링된다.

#### 4.4.1 주문 대상 해석 (선행 조건)

`lib/api/id-mapping.ts:46-73`, `MarketPage.tsx:132-150`. 절차는 §1.10.2 와 동일하다.

해석 결과에는 어느 코인의 것인지(`"{exchangeKey}:{symbol}"`)를 함께 담아, 다른 코인으로 옮긴 직후 이전 코인의 주문 대상이 잘못 쓰이는 일을 막는다(`MarketPage.tsx:132-148`).

실패 시 패널 상단 경고(`OrderPanel.tsx:386-408`):

| 사유 | 문구 |
|---|---|
| `NO_ROUND` | `진행 중인 라운드가 없어 주문할 수 없습니다.` + `라운드 시작하기` 링크(`/round/new`) |
| `COIN_UNLISTED` | `이 코인은 아직 주문을 지원하지 않습니다.` |
| `LOOKUP_FAILED` | `주문 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.` |

해석 실패 상태에서는 매수/매도 버튼이 비활성화된다(`:686`).

#### 4.4.2 탭과 주문 유형

- 탭 3개: `매수` / `매도` / `거래내역` (기본 `매수`, `:34-38,105`).
- 주문 유형 2개: `지정가`(LIMIT, 기본) / `시장가`(MARKET) (`:40-43,117`).

입력 필드 노출 규칙(`:128-129`):

| 조합 | 가격 | 수량 | 총액 |
|---|---|---|---|
| 지정가 매수 | 입력 | 입력 | 입력 |
| 지정가 매도 | 입력 | 입력 | 입력 |
| 시장가 매수 | 현재가 고정(비활성) | **숨김** | 입력 |
| 시장가 매도 | 현재가 고정(비활성) | 입력 | **숨김** |

즉 시장가 매수는 "얼마어치"(총액)로, 시장가 매도는 "몇 개"(수량)로 주문한다.

#### 4.4.3 입력 규칙

- 파싱: 콤마를 모두 제거한 뒤 숫자로 바꾸고, 유한수가 아니면 0으로 본다(`:61-64`).
- 표시: 금액·총액은 소수 0자리, 수량은 소수 6자리로 `ko-KR` 천 단위 포맷(`:54-59`).
- **가격 스텝 버튼**: `-` / `+` 각각 **±1,000** (`:597,604`). 기준값은 현재 입력값이며, 비어 있으면 현재가를 기준으로 한다. 0 미만으로 내려가지 않는다.
- **수량↔총액 연동**(지정가에서만, `:224-274`):
  - `displayPrice` = 시장가면 현재가, 지정가면 입력 가격(>0) 또는 현재가.
  - 수량 입력 → `총액 = 수량 × displayPrice` (0자리)
  - 총액 입력 → `수량 = 총액 ÷ displayPrice` (6자리)
  - 가격 변경 → 마지막으로 손댄 쪽(`lastEdited`)을 기준으로 반대편을 다시 계산한다.
  - **시장가에서는 연동하지 않는다.**
- **비율 버튼 10 / 25 / 50 / 100%** (`:45, 276-295`):
  - 매수: `총액 = availableBuy × ratio / 100`. 지정가면 수량도 함께 계산.
  - 매도: `수량 = availableSell × ratio / 100` (6자리). 지정가면 총액도 함께 계산.
- 코인·주문대상·탭이 바뀌면 가격/수량/총액/에러를 모두 비운다(`:217-222`).
- `초기화` 버튼은 세 입력과 에러를 비운다.

#### 4.4.4 주문 가능 금액

`GET /api/orders/available` 을 BUY/SELL 각각 호출해 병렬로 받는다(`:142-163`).

- `availableBuy` = 기준통화 잔고(매수 가능 금액). 표시 단위는 `baseCurrency`, 소수 0자리.
- `availableSell` = 해당 코인 보유 수량(매도 가능 수량). 표시 단위는 코인 심볼, 소수 6자리.
- 서버는 지갑 소유자 검증 후 BUY 면 `quoteCoinId`, SELL 이면 `tradedCoinId` 의 **가용 잔고**(잠금분 제외)를 돌려준다.
- 조회 실패 시 두 값을 0으로 두고 에러 문구를 붉게 표시한다.

#### 4.4.5 수수료

- 화면에는 `수수료 {feeRate*100 를 소수 2자리로}%` 와 `최소 주문 5,000 {baseCurrency}` 를 표시한다(`:654-657`). 웹은 `feeRate={0.0005}` 를 `MarketPage.tsx:247` 에서 상수로 넘긴다 → `수수료 0.05%`. **최소 주문 문구는 하드코딩이므로 바이낸스(USDT)에서도 `최소 주문 5,000 USDT` 로 나온다(실제 서버 기준은 5 USDT).** Flutter 에서는 거래소별 수수료율(§4.1.2)과 기준통화별 최소 주문 금액을 정확히 표시한다.
- **실제 수수료 계산은 서버가 한다.** `fee = filledPrice × quantity × feeRate` (`trading/domain/vo/Fill.java:8-9`).
- 정산식(`trading/domain/vo/OrderMode.java:159-170`):
  - 매수 차감액 = `filledPrice × quantity + fee`
  - 매도 입금액 = `filledPrice × quantity − fee`
  - 지정가 매수 선점(잠금)액 = `limitPrice × quantity × (1 + feeRate)`
- 클라이언트는 수수료를 주문 요청에 담지 않는다. 표시용 예상치가 필요하면 `주문총액 × feeRate` 로 계산한다.

#### 4.4.6 검증 규칙

클라이언트(`:312-338`, 순서대로):

1. 주문 대상 미해석 → `선택한 거래소/코인의 주문 매핑이 없습니다.`
2. 시장가 매수인데 총액 ≤ 0 → `주문 총액을 입력해 주세요.`
3. 시장가 매수가 아닌데 수량 ≤ 0 → `주문 수량을 입력해 주세요.`
4. 지정가인데 가격 ≤ 0 → `지정가를 입력해 주세요.`

서버(`trading/domain/vo/OrderAmountPolicy.java`, `PlaceOrderRequest.java`):

- `volume`, `price` 는 각각 `@Positive` (보내는 경우에 한함).
- 주문 금액 하한/상한: **KRW 5,000 ~ 1,000,000,000**, **USDT 5 ~ 상한 없음**. 위반 시 `BELOW_MIN_ORDER_AMOUNT` / `ABOVE_MAX_ORDER_AMOUNT`.
- 시장가 매수는 `volume` 을 보내면 거부되고(`rejectVolume`), 시장가 매도는 `price` 를 보내면 거부된다(`rejectPrice`) — `OrderMode.java:11,36`.
- 잔고 부족 시 `INSUFFICIENT_BALANCE`.

#### 4.4.7 주문 전송

`POST /api/orders` (`:344-352`, `order-api.ts:110-121`)

```json
{
  "clientOrderId": "<UUID v4>",
  "walletId": 12,
  "exchangeCoinId": 34,
  "side": "BUY" | "SELL",
  "orderType": "LIMIT" | "MARKET",
  "volume": 0.5,      // 시장가 매수면 생략
  "price": 95000000   // 지정가: 지정가격 / 시장가 매수: 주문 총액 / 시장가 매도: 생략
}
```

**시장가 매수에서 `price` 필드에 담는 값은 가격이 아니라 총액**이다(`OrderPanel.tsx:351`). 서버는 `OrderMode.MARKET_BUY.interpret` 에서 이를 총액으로 해석해 `수량 = 총액 / 현재가` 로 환산한다.

`clientOrderId` 는 멱등키다. 같은 키로 재요청하면 서버가 기존 주문을 그대로 돌려준다(`OrderController.java:47-55`). Flutter 에서도 UUID v4 를 생성해 넣고, **재시도 시 같은 키를 재사용**해야 중복 주문이 생기지 않는다.

성공 처리(`:354-361`): 세 입력을 비우고, 주문 가능 금액을 다시 조회하고, 거래내역 탭이 열려 있었다면 목록을 새로 읽는다. 서버 응답 메시지는 시장가면 `주문이 체결되었습니다.`, 지정가면 `주문이 등록되었습니다.`

실패 처리(`:74-79, 362-365`): 봉투의 `message`(없으면 `code`)를 붉은 문구로 패널에 표시한다. API 예외가 아니면 `요청 처리 중 오류가 발생했습니다.`

#### 4.4.8 체결 이벤트 반영

§3.4 를 참조한다. **현행 웹의 로컬 증분 로직(`OrderPanel.tsx:201-210`)은 실제로 작동하지 않으므로 이식하지 않는다.** REST 재조회로 확정 처리한다.

#### 4.4.9 거래내역 탭 (`:429-546`)

- 필터: `체결`(status=FILLED) / `미체결`(status=PENDING). 기본 `체결`.
- `GET /api/orders?walletId&exchangeCoinId&status&cursorOrderId&size=20`. 응답은 `{content, nextCursor, hasNext}`. `hasNext` 면 `더보기` 버튼으로 다음 커서를 이어 붙인다(리스트 누적). **모바일에서는 스크롤 끝 도달 시 자동 로드로 바꾼다.**
- 탭 진입·필터 변경·주문 대상 변경 시 목록을 처음부터 다시 읽는다.
- 항목 표시: 매수/매도 배지, 시장가/지정가, 상태 배지(체결/대기/취소/실패 — **서버 응답에 `status` 가 없으므로 요청 필터값을 그대로 쓴다**), 상대 시각(`방금 전`, `N분 전`, `N시간 전`, 24시간 이상은 `ko-KR` 절대 시각), 가격(`filledPrice ?? price ?? 0`)·수량(6자리)·금액.
- `PENDING` 항목에는 `취소` 버튼. `POST /api/orders/{orderId}/cancel {walletId}` 성공 시 목록에서 해당 항목을 제거하고 주문 가능 금액을 다시 읽는다. 실패 시 에러 문구 표시.
- 빈 목록: `체결 내역이 없습니다.` / `미체결 주문이 없습니다.`

---

### 4.5 긴급 자금 (EmergencyFundingCard)

`frontend/src/components/round/EmergencyFundingCard.tsx`, `frontend/src/contexts/RoundProvider.tsx:80-111`

표시 조건: `activeRound !== null` (`MarketPage.tsx:235-240`).

카드 본문:

- 제목 `긴급 자금 투입`, 부제 `라운드 진행 중 최대 3회까지 가능합니다.`
- 뱃지: `canCharge` 면 `사용 가능`, 아니면 `사용 불가`.
- `1회 상한` = `formatKRW(round.emergencyFundingLimit)`, `남은 횟수` = `{round.emergencyChargeCount}회`.
- `긴급 자금 투입하기` 버튼 → 다이얼로그. `canCharge` 가 아니면 비활성.

**`canCharge = round.status === "ACTIVE" && round.emergencyChargeCount > 0`** (`:29`).

다이얼로그:

- 금액 입력은 숫자만 허용한다(`replace(/[^0-9]/g, "")`, `:121`). 표시는 `ko-KR` 천 단위.
- 프리셋 3개: 상한의 25% / 50% / 100%, 각각 `Math.floor` (`:32-40`).
- **유효 금액: `0 < amount <= round.emergencyFundingLimit`** (`:30`). 초과하면 `상한을 초과했습니다. {상한} 이하로 입력해주세요.` 를 붉게 표시하고 `투입 확정` 버튼을 비활성화한다.
- 다이얼로그를 닫으면 입력 금액을 0으로 되돌린다.

확정 시(`RoundProvider.tsx:80-111`):

1. 로컬 선검사: 라운드 존재, `status === "ACTIVE"`, `emergencyChargeCount > 0`, `0 < amount <= limit`. 하나라도 어긋나면 **네트워크 호출 없이 `false`** 를 반환한다.
2. `POST /api/rounds/{roundId}/emergency-funding` 본문 `{exchangeId, amount, idempotencyKey}`. **`exchangeId` 는 현재 선택된 거래소의 id** 다(`MarketPage.tsx:239`). 즉 자금은 지금 보고 있는 거래소 지갑으로 들어간다.
3. `idempotencyKey` 는 **UUID v4 필수**(서버가 `java.util.UUID` 로 역직렬화 — 형식 위반 시 500).
4. 성공하면 응답의 `remainingChargeCount` 로 `emergencyChargeCount` 를 갱신하고 다이얼로그를 닫는다. 서버는 중복 요청(같은 키)을 감지하면 재차감 없이 현재 잔여 횟수를 돌려준다 (`RoundController.java:84-97`).
5. 실패하면 `false` 를 돌려주며 **다이얼로그는 열린 채로 남고 화면에는 아무 메시지도 뜨지 않는다**(콘솔 로그만). **Flutter 이식 시에는 스낵바 등 실패 안내를 반드시 넣는다.**

서버 규칙(`investmentround/domain/vo/EmergencyFundingAllowance.java`):

- 라운드 시작 시 상한은 **0 이상 1,000,000 이하**여야 한다(`INVALID_EMERGENCY_FUNDING_LIMIT`).
- 기본 충전 횟수는 **3회**.
- 충전 검증: 상한이 0이면 `EMERGENCY_FUNDING_DISABLED`, 남은 횟수가 0 이하면 `EMERGENCY_FUNDING_CHANCE_EXHAUSTED`, 금액이 0 이하이거나 상한 초과면 `INVALID_EMERGENCY_FUNDING_AMOUNT`.

---

### 4.6 모바일 UX 제안

웹은 1152px 폭(`max-w-6xl`)에 차트(960×440 SVG)·목록(8행 내부 스크롤)·주문 패널(360px 고정 사이드바)을 동시에 세운다. 폰 세로 화면(논리폭 360~430dp)에 이 셋을 동시에 올리면 각 영역이 200dp 미만이 되어 어느 것도 쓸 수 없다. 따라서 **한 화면에 하나의 역할**로 쪼갠다.

#### 4.6.1 화면 분해

| 웹 영역 | 모바일 배치 | 근거 |
|---|---|---|
| 거래소 탭 + 필터 칩 + 검색 + 코인 목록 | **마켓 탭(루트 화면)** — 상단 고정 검색창, 그 아래 거래소 세그먼트 + 필터 칩, 나머지 전부 코인 리스트 | 목록이 진입 후 가장 오래 머무는 화면이다. 웹의 8행(544px) 내부 스크롤 대신 화면 전체를 리스트에 준다. `ListView.builder(itemExtent: 68)` |
| 주요 코인 카드 3개 | 목록 상단에 **가로 스크롤 카드 열** 또는 기본 접힘 | 3열 그리드를 폰에 그대로 두면 카드 하나가 100dp 남짓이라 현재가가 잘린다 |
| 캔들 차트 + 차트 헤더 | 목록 행 탭 → **코인 상세 풀스크린 라우트** push. 상단에 심볼/현재가/등락률, 그 아래 간격 칩(가로 스크롤), 그 아래 차트 | 웹은 차트가 목록 위에 상시 노출되지만, 폰에서는 둘 다 보이면 차트 높이가 180dp 이하로 떨어져 캔들 몸통을 구분할 수 없다. 종횡비 960:440 ≈ 2.18:1 을 유지하려면 폭 360dp 기준 높이 165dp 가 필요하다 |
| 주문 패널 | 상세 화면 하단 **고정 CTA 2개(`매수`/`매도`)** → 탭하면 `DraggableScrollableSheet` 로 주문 바텀시트 | `OrderPanel` 은 유형 선택 + 주문가능 표시 + 입력 3개 + 비율 버튼 4개 + 수수료 안내 + 에러 영역 + 버튼 2개로 세로 길이가 길다. 시트를 `initialChildSize: 0.75`, `maxChildSize: 0.95` 로 열고 키보드가 올라오면 시트 내부만 스크롤시킨다 |
| 거래내역 탭 | 주문 바텀시트의 세 번째 탭이 아니라, **상세 화면의 하단 탭(차트 / 거래내역)** 으로 분리 | 바텀시트 안에 커서 무한 스크롤 목록을 넣으면 시트 드래그와 목록 스크롤 제스처가 충돌한다 |
| 긴급 자금 카드 | 마켓 화면 상단의 **접힌 배너**(남은 횟수만 표시) 또는 마이 화면으로 이동. 탭하면 기존 다이얼로그를 `showModalBottomSheet` 로 | 사용 빈도(라운드당 최대 3회)에 비해 자리를 너무 차지한다 |

#### 4.6.2 제스처 대응

| 웹 | 모바일 | 구현 |
|---|---|---|
| 드래그 / 휠 → 패닝 | 한 손가락 수평 드래그 | `GestureDetector.onHorizontalDragUpdate` → `movedCandles = (dx / slotWidth).round()` (웹과 동일 식) |
| Ctrl+휠 → 줌 | 두 손가락 핀치 | `onScaleUpdate` → `visibleCount = (기준 visibleCount / scale).round()` 를 `[12, 전체개수]` 로 가둔다. 앵커는 두 손가락 중점의 캔들 인덱스 |
| 호버 → 크로스헤어/툴팁 | **길게 눌러 끌기(long-press drag)** | `onLongPressStart` / `onLongPressMoveUpdate` / `onLongPressEnd`. 손을 떼면 해제한다. 히트 테스트는 손가락 가림을 감안해 x 허용 오차를 `candleWidth` 에서 `slotWidth` 로 넓힌다 |
| — | 가로 회전 | `OrientationBuilder` 로 가로 모드에서 차트를 풀스크린으로 확장. 960×440 종횡비가 가로에서 정확히 맞는다 |

툴팁은 손가락에 가리지 않도록 커서 오프셋 대신 **차트 상단 고정 위치**(손가락 반대편)에 띄운다.

#### 4.6.3 성능

- 실시간 티커는 **16ms 스로틀/프레임 콜백**으로 모아 한 번만 `setState` 한다. 배치 없이 STOMP 프레임마다 리빌드하면 600개 항목 리스트에서 프레임 드롭이 난다.
- 가격 플래시(100ms 테두리)는 코인별 `ValueNotifier`/`Selector` 로 구독 범위를 좁힌다. 리스트 전체 리빌드로 처리하면 플래시 하나에 600개 행이 다시 그려진다.
- 검색 색인(초성/자모)은 **정적 코인 목록 수신 시 1회만** 계산해 `Map<String, ({String chosung, String jamo})>` 로 들고 있는다.

#### 4.6.4 유지해야 할 동작

1. 코인을 고르지 않았을 때의 차트 대상은 **검색으로 걸러내기 전** 전체 목록을 현재 정렬로 세운 첫 코인이다.
2. 거래소를 바꾸면 검색어·필터·선택 코인을 초기화하되 정렬은 유지한다.
3. 초성 검색은 `startsWith`, 자모 검색은 `contains` 다.
4. 시장가 매수의 `price` 필드는 총액이다.
5. `clientOrderId`/`idempotencyKey` 는 재시도 시 같은 값을 써야 한다.

---

## 5. 화면: 포트폴리오와 입출금

### 5.0 공통 전제

| 항목 | 내용 | 근거 |
|---|---|---|
| 라우트 | `/portfolio`(투자내역), `/wallet`(입출금). 둘 다 `ProtectedRoute` 하위로 인증 필수 | `App.tsx:34-35` |
| 거래소 선택 | 두 화면 모두 상단에 `ExchangeTabs`. 선택값은 URL 쿼리 `?exchange=<key>` 에 보존되며, 미지정 시 `activeRound.wallets[0]` 에 해당하는 거래소가 기본값 | `PortfolioPage.tsx:36,52-53`, `WalletPage.tsx:36,52-53` |
| walletId 해석 | 화면은 `exchangeId` 가 아니라 **walletId** 로 API를 호출한다. `activeRound.wallets.find(w => w.exchangeId === exchange.id).walletId` | `PortfolioPage.tsx:65-68`, `WalletPage.tsx:66-70` |
| 인증 전송 | 프론트 코드 전체에 `Authorization` 헤더·`credentials` 옵션이 없다. 동일 출처 쿠키 세션 전제이며, 서버는 `@LoginUser` 로 userId 를 주입한다 | `client.ts:47-54` |

색 토큰과 포맷 규칙은 §8 을 따른다.

---

### 5.1 포트폴리오 화면 (`/portfolio`)

#### 5.1.1 구성 요소 (`PortfolioPage.tsx:87-154`)

1. `Header` (공통)
2. 페이지 헤더: 제목 "투자내역", 부제 `"{거래소명} 기준 · {기준통화} 마켓"`, 우측에 `ExchangeTabs`
3. 본문 2열 그리드 (`lg:grid-cols-[340px_1fr]`, 모바일 1열)
   - 좌: `AssetSummaryCard` → `DonutChart`
   - 우: `HoldingsTable`
4. 하단 각주: `"* 모의투자 데이터입니다."`

#### 5.1.2 데이터 로딩과 갱신

- 호출 API: `GET /api/wallets/{walletId}/portfolio` 1회 (`portfolio-api.ts:19-21`)
- 트리거: `user`, `activeRound`, `selectedExchange` 중 하나라도 바뀌면 재실행된다(`PortfolioPage.tsx:62-85`). 즉 **최초 진입 시 1회 + 거래소 탭 변경 시 1회**가 전부다.
- **폴링·WebSocket 구독이 없다.** 주문이 체결되거나 시세가 움직여도 이 화면은 스스로 갱신되지 않는다(`currentPrice` 는 응답 시점의 서버 값 고정). 수동 새로고침 수단도 없다. → **Flutter 는 pull-to-refresh 를 필수로 넣는다.**
- 응답 → 화면 모델 변환(`:15-33`): 모든 수치는 `Number(...)` 로 캐스팅한다. Flutter 에서는 정밀도 손실을 피하기 위해 `Decimal`(pkg:decimal) 사용을 권한다.

응답 스키마 (`portfolio/adapter/in/dto/response/MyHoldingsResponse.java`, `HoldingSnapshotResponse.java`)

```
MyHoldingsResponse {
  exchangeId: number,
  baseCurrencyBalance: BigDecimal,   // 사용 가능 기준통화 잔고
  baseCurrencySymbol: string,        // "KRW" | "USDT"
  holdings: [{ coinId, coinSymbol, coinName, quantity, avgBuyPrice, currentPrice }]
}
```

#### 5.1.3 자산 요약 지표 계산식 (`AssetSummaryCard.tsx:12-33`)

`holdings` 배열만으로 클라이언트가 전부 계산한다. **서버는 합계를 주지 않는다.**

```
totalBuy   = Σ (avgBuyPrice_i × quantity_i)          // 총매수
totalEval  = Σ (currentPrice_i × quantity_i)         // 총평가
totalAsset = availableCash + totalEval               // 총 보유자산
profitLoss = totalEval − totalBuy                    // 평가손익
profitRate = totalBuy > 0 ? (profitLoss / totalBuy) × 100 : 0   // 수익률(%)
```

표시 규칙

| 항목 | 표시 | 비고 |
|---|---|---|
| 보유 {기준통화} | `formatCurrency(availableCash, base)` | 카드 최상단, 작은 글씨 |
| 총 보유자산 | `formatCurrency(totalAsset, base)` | 가장 큰 숫자(2xl, bold). 캡션 `"보유 {base} + 코인 평가 합계"` |
| 총매수 / 총평가 / 평가손익 / 수익률 | 2×2 그리드 카드 | 평가손익·수익률만 색상 적용 |
| 부호·색 | `profitLoss > 0` → 앞에 `+` 붙이고 positive 색, `< 0` → negative 색(음수 부호는 포매터가 자체 출력), `= 0` → 기본색·부호 없음 | `:17-32` |
| 수익률 | `profitRate.toFixed(2) + "%"`, 양수면 `+` 접두 | |

#### 5.1.4 도넛 차트 (`DonutChart.tsx`)

세그먼트 구성 규칙 (`buildSegments`, 23-65행)

1. `total = availableCash + Σ(currentPrice × quantity)`. `total === 0` 이면 세그먼트 없음(빈 도넛 + 범례 없음).
2. 코인 세그먼트: `value = currentPrice × quantity`, `value > 0` 인 것만, **value 내림차순 정렬**.
3. 코인이 6개 이하면 전부 표시. 7개 이상이면 **상위 5개 + "기타"**(나머지 합, 색 `#8b949e`).
4. `availableCash > 0` 이면 기준통화 세그먼트를 **맨 앞에** 삽입(라벨=기준통화 심볼, 색 `#c2b8ab`). `availableCash <= 0` 이면 생략.
5. 코인 색: `COIN_COLORS` 매핑(§8.1.5), 미등록 심볼은 `#8b949e`.
6. 세그먼트 최대 개수는 7(현금 + 코인 6).

기하 (`DonutChart.tsx:73-128`)

- 캔버스 180×180, 기본 두께 28, hover 시 34. 반지름 `r = (180 − 34) / 2 = 73`.
- 조각 시작 각도는 앞선 조각들의 `ratio` 누적합. **12시 방향에서 시작해 시계 방향**으로 그린다(`strokeDashoffset = C × (0.25 − start)`).
- 각 조각의 스윕 각도 = `ratio × 360°`. 끝 모양은 `butt`(둥글게 하지 않음).
- 배경 원(회색 `--secondary`)을 먼저 깔고 그 위에 조각을 얹는다.
- 중앙 라벨: 위에 `"총 자산"`(10px), 아래에 `formatCurrency(totalAsset, base)`.
- Hover 상호작용: 조각 또는 범례에 마우스를 올리면 해당 조각 두께 28→34, 나머지 조각·범례는 opacity 0.4, 범례 행에 배경 강조. 300ms 트랜지션.
- 범례: 세그먼트 순서 그대로 세로 나열. 좌측에 색 점 + 라벨, 우측에 `(ratio × 100).toFixed(1) + "%"`.

Flutter: `CustomPaint` + `Canvas.drawArc`(`StrokeCap.butt`, `PaintingStyle.stroke`). hover 대신 **조각/범례 탭**으로 강조 상태를 토글한다.

#### 5.1.5 보유 종목 표 (`HoldingsTable.tsx`)

파생값 (`computeHolding`, 22-28행)

```
evalAmount = currentPrice × quantity
buyAmount  = avgBuyPrice × quantity
profitLoss = evalAmount − buyAmount
profitRate = buyAmount > 0 ? (profitLoss / buyAmount) × 100 : 0
```

컬럼 (좌→우, `:30, 56-64`)

| # | 키 | 헤더 | 값 | 포맷 | 정렬(가로) | 그리드 폭 |
|---|---|---|---|---|---|---|
| 1 | `name` | 코인명 | 심볼(13px, semibold) + 한글명(11px, muted) 2줄 | – | 좌 | `1.4fr` |
| 2 | `quantity` | 보유수량 | `formatQuantity(quantity)` | 등폭 | 우 | `minmax(80px,1fr)` |
| 3 | `avgBuyPrice` | 평균매수가 | `formatPrice(avgBuyPrice, base)` | muted | 우 | 〃 |
| 4 | `currentPrice` | 현재가 | `formatPrice(currentPrice, base)` | semibold | 우 | 〃 |
| 5 | `evalAmount` | 평가금액 | `formatCurrencyCompact(evalAmount, base)` | 등폭 | 우 | 〃 |
| 6 | `profitLoss` | 평가손익 | `formatCurrencyCompact`, 양수면 `+` 접두, 손익 색상 | semibold | 우 | 〃 |
| 7 | `profitRate` | 수익률 | `toFixed(2)%` 를 **알약(pill) 배지**로. 양수 `bg-positive/15 text-positive`, 음수 `bg-negative/20 text-negative`, 0 은 muted 텍스트만 | – | 우 | `minmax(75px,0.8fr)` |

정렬 동작 (`hooks/useSort.ts:20-49`)

- 헤더 전체가 버튼. 같은 키를 다시 누르면 `desc ↔ asc` 토글, 다른 키를 누르면 그 키로 바꾸고 방향은 `desc` 로 초기화.
- **초기 정렬 없음**(`defaultKey` 미지정 → `sortKey === null` → 서버 응답 순서 그대로).
- `name` 은 `coinSymbol.localeCompare`, 나머지는 수치 차분 비교.
- 행 최소 폭 700px, 좁으면 가로 스크롤. 행 hover 시 배경 `primary/3%`.

#### 5.1.6 빈 상태·로딩·에러

| 상황 | 표시 | 근거 |
|---|---|---|
| `loading === true` | 본문 전체를 `"로딩 중..."` 텍스트 한 줄로 대체(스켈레톤·스피너 없음) | `PortfolioPage.tsx:113-114` |
| 라운드 없음(`activeRound === null`) | `NoRoundNotice`: `"진행 중인 라운드가 없어 포트폴리오가 비어 있습니다."` + `[새 라운드 시작]` 버튼 → `/round/new` | `:150`, `NoRoundNotice.tsx:8-21` |
| API 실패 | `console.error` 후 `portfolio = null`. 라운드가 있으면 `"포트폴리오 데이터를 불러올 수 없습니다."` 한 줄. **재시도 버튼 없음, 에러 메시지 노출 없음** | `:75-78, 146-148` |
| 보유 코인 0개 | 표 본문 자리에 높이 192px 중앙 정렬 `"보유 중인 코인이 없습니다."`. 요약 카드와 도넛은 그대로 렌더(도넛은 현금 1조각) | `HoldingsTable.tsx:89-92` |

---

### 5.2 입출금 화면 (`/wallet`)

#### 5.2.1 구성 요소 (`WalletPage.tsx:170-291`)

1. `Header`
2. 페이지 헤더: 제목 "입출금", 부제 `"자산 관리 · 입금/출금 내역 확인"`, 우측 `ExchangeTabs`
3. `WalletSummary` (거래소 총 자산 카드)
4. `WalletAssetTable` (보유 자산 목록). 코인 선택 시 데스크톱은 우측 340px 사이드 패널(`sticky top-24`)이 열리며 `WalletAssetDetail` + `TransferHistoryPanel` 을 담고, 모바일(<1024px)은 하단 바텀시트(`max-h-85vh`, 상단 그랩 핸들, 배경 딤+블러, 배경 탭 시 닫힘, 열려 있는 동안 `body` 스크롤 잠금)로 같은 두 컴포넌트를 표시한다(`:159-168, 250-275`).
5. 하단 각주: `"* 모의투자 데이터입니다. 실제 자산이 아닙니다."`
6. `TransferModal` — `WalletAssetDetail` 의 [출금] 버튼으로만 열린다(`:277-288`).

거래소 탭을 바꾸면 `selectedCoin` 이 `null` 로 초기화된다(`:150-153`).

#### 5.2.2 데이터 로딩과 갱신 (`WalletPage.tsx:63-131`)

`user`, `activeRound`, `selectedExchange` 변경 시 아래 3개를 `Promise.all` 로 **동시 호출**한다.

| API | 용도 |
|---|---|
| `GET /api/wallets/{walletId}/balances` | 잔고(코인별 available/locked, 기준통화 잔고) |
| `GET /api/exchanges/{exchangeId}/coins` | 거래소 상장 코인 전체 목록(심볼·한글명·**가격** 포함) |
| `GET /api/wallets/{walletId}/transfers?size=50` | 송금 내역 |

목록 조립 규칙 (`:80-106`)

1. 잔고 응답을 `coinId → {available, locked}` 맵으로 변환.
2. 첫 항목은 **기준통화**: `{coinId: null, coinSymbol: baseCurrencySymbol, coinName: (KRW면 "원화" 아니면 심볼 그대로), available, locked, currentPrice: 1}`.
3. 이어서 **상장 코인 전체**를 나열한다. 잔고가 없는 코인도 `available=0, locked=0` 으로 포함한다(즉 목록에는 보유하지 않은 코인이 대다수다).
4. 코인의 `currentPrice` 는 웹에서 **0 으로 고정**된다(`:103`, 주석 "WebSocket에서 업데이트 예정"). 실제 구독 코드는 없다. → **Flutter 는 `GET /api/exchanges/{id}/coins` 응답의 `price` 를 사용한다**(§5.2.8 #3).

갱신 트리거: 최초 진입, 거래소 탭 변경, **송금 성공 시 `onSuccess` → `loadWalletData()` 전체 재호출**(`:282`). 그 외 자동 갱신 없음.

#### 5.2.3 WalletSummary (`WalletSummary.tsx`)

```
totalAsset    = Σ ((available_i + locked_i) × currentPrice_i)   // 전 항목(기준통화 포함)
baseTotal     = baseCoin.available + baseCoin.locked
```

- 헤더: `"{거래소명} 총 자산"` + 눈 아이콘 토글(`Eye`/`EyeOff`, 기본 표시 상태). 숨김 시 금액을 `"••••••••"` 로 치환하고 보조 금액에는 블러 처리.
- 큰 숫자: `formatCurrency(totalAsset, base)` (3xl, bold).
- 보조 줄: `"보유 {base} {formatCurrency(baseTotal)}"`, 그리고 `baseLocked > 0 && visible` 일 때만 `"(사용 가능 {…} / 잠금 {…})"` 추가.

#### 5.2.4 WalletAssetTable (`WalletAssetTable.tsx`)

툴바(105-129행): 좌측 제목 `"보유 자산"`, 우측에 코인 검색 입력(플레이스홀더 `"코인 검색"`, 심볼·한글명 부분일치, 대소문자 무시)과 `"소액 제외"` 체크박스.

- 소액 제외 기준: `SMALL_AMOUNT_THRESHOLD = { KRW: 1000, USDT: 1 }`, 없으면 1. `totalValue >= threshold` 인 항목만 남긴다(`:61-66`).

파생값: `total = available + locked`, `totalValue = total × currentPrice`.

컬럼 (`:36, 95-100`)

| # | 키 | 헤더 | 값 |
|---|---|---|---|
| 1 | `name` | 코인 | 심볼 + 한글명 2줄 |
| 2 | `total` | 보유수량 | 1줄: 수량, 2줄: `≈ formatCurrencyCompact(totalValue, base)` (기준통화 행에는 환산액 미표시) |
| 3 | `available` | 사용가능 | 수량 |
| 4 | `locked` | 잠금 | `> 0` 이면 자물쇠 아이콘 + 수량(색 `chart-4 #F0A030`), 아니면 `"—"` |

- 수량 표기: 기준통화면 `toLocaleString("ko-KR")`, 코인이면 `formatQuantity()`.
- 잔고가 0인 코인(`hasBalance === false`)은 2·3열을 `"—"` 로 표시하고 흐리게(`text-muted-foreground/40`) 처리한다. 기준통화 행은 잔고 0이어도 항상 값 표시.
- 정렬: **기본 `total` 내림차순**. `available`·`locked` 정렬은 **수량이 아니라 평가액**(`available × currentPrice`)으로 비교한다(75-76행).
- 가상 스크롤: 행 높이 72px 고정, 8행(576px) 높이의 자체 스크롤 상자. 표 최소 폭 640px.
- 행 클릭/Enter → 선택. 이미 선택된 행을 다시 누르면 선택 해제(`onSelectCoin(null)`). 선택 행 배경 `primary/6%`.
- 빈 목록: 높이 192px 중앙 `"보유 중인 자산이 없습니다."`

#### 5.2.5 WalletAssetDetail (`WalletAssetDetail.tsx`)

- 헤더: 심볼(bold) + 한글명, 우측 X 닫기 버튼.
- 잔고: `total = available + locked` 를 2xl bold 로. 코인이면 그 아래 `≈ formatCurrency(total × currentPrice, base)`.
- **[출금] 버튼은 코인에만 노출**된다(`isBase === false` 조건, 54-64행). 기준통화(KRW/USDT)는 송금할 수 없다. **입금 버튼은 존재하지 않는다.**
- "잔고 상세" 섹션: `사용 가능` 행, `잠금` 행(잠금 > 0이면 `"주문 대기"` 배지 + `chart-4` 색, 아니면 `"—"`).

#### 5.2.6 TransferModal — 송금 (`TransferModal.tsx`)

입력 필드

| 필드 | 위젯 | 규칙 |
|---|---|---|
| 도착 거래소 | Select(플레이스홀더 `"거래소 선택"`) | 후보 = 현재 라운드의 지갑 중 **현재 거래소를 제외한 전부**. 각 항목은 `{walletId, exchangeId(key), exchangeName}` (`WalletPage.tsx:133-148`) |
| 출금 수량 | number 입력(스피너 숨김, `step="any"`, `min=0`) + [최대] 버튼 | [최대] → 입력값을 `coin.available.toString()` 로 채운다(`:63-65`) |
| 보조 문구 | `"가용: {formatDisplay(coin.available)} {coin.coinSymbol}"` | 기준통화면 `toLocaleString`, 코인이면 `formatQuantity` |

검증 (`:52-61`)

```
amount = parseFloat(amountStr) || 0            // 파싱 실패 시 0
if (!selectedDestination)      → "도착 거래소를 선택해주세요."
if (amount <= 0)               → "수량을 입력해주세요."
else if (amount > coin.available) → "가용 잔고를 초과합니다."
```

- 오류 메시지는 **한 번이라도 제출을 시도한 뒤(`submitted === true`)에만** 표시된다. → **Flutter 는 입력 즉시 실시간 검증한다.**
- 제출 버튼은 항상 활성(제출 중일 때만 `disabled`). 검증 실패 시 요청을 보내지 않고 오류 문구만 켠다.
- `coin.coinId === null`(기준통화)이면 요청하지 않고 조용히 반환한다(72행).

확인 절차 — **웹에는 별도 확인 단계가 없다.** `[출금하기]` 한 번으로 곧바로 API를 호출한다(`:67-93`).

```
POST /api/transfers
{
  idempotencyKey: crypto.randomUUID(),   // UUID v4, 서버는 UUID 타입으로 파싱
  fromWalletId: number,                  // 현재 지갑
  toWalletId:   number,                  // 선택한 도착 지갑
  coinId:       number,                  // 기준통화 불가
  amount:       number                   // @Positive
}
→ 201 CREATED, data: { transferId, status }
```

- 서버 검증(`TransferCoinService.java:41-59`, `Transfer.java:26-38`, `WalletBalance.java:36-39`): 동일 지갑 송금 금지, 두 지갑이 같은 라운드여야 함, 출발 지갑 소유자 검증, 가용 잔고 부족 시 `INSUFFICIENT_BALANCE`.
- 서버는 송금을 **동기·즉시 완료** 처리한다. `status` 는 항상 `SUCCESS` 이고 `completedAt = createdAt` 이다.
- 멱등: 같은 `idempotencyKey` 재요청은 기존 `transferId` 를 `SUCCESS` 로 되돌려준다. 따라서 **네트워크 재시도 시 키를 재생성하지 말고 동일 키를 그대로 보내야 한다.**
- 성공: `onSuccess()` → `loadWalletData()` 전체 재조회, 이어서 모달 닫기. 웹에는 성공 토스트가 없다.
- 실패: 웹은 서버 메시지를 무시하고 고정 문구 `"송금에 실패했습니다."` 만 붉게 표시한다(88-90행). → **Flutter 는 서버 `message` 를 매핑해 보여준다**(§5.4.4).
- 제출 중: 버튼 라벨이 `"출금 중..."` 으로 바뀌고 비활성화된다.
- 모달을 닫으면 `selectedDestination`, `amountStr`, `submitted` 는 초기화되지만 `error` 는 초기화되지 않는다(95-102행 — 다음에 열 때 이전 오류 문구가 남는 결함).

#### 5.2.7 TransferHistoryPanel — 송금 내역 (`TransferHistoryPanel.tsx`)

- 배치: 코인을 선택했을 때만 보인다. 항상 `assetFilter = 선택한 코인 심볼` 이 걸린다.
- 필터 2줄(모두 클라이언트 필터): 유형(`전체/입금/출금`), 상태(`전체/진행중/완료`).
- 정렬: `requestedAt` 내림차순(최신 순, 101행).
- 항목(카드): 1줄 요청 시각(`Intl.DateTimeFormat("ko-KR", {year:"numeric", month:"2-digit", day:"2-digit", hour:"2-digit", minute:"2-digit"})`), 2줄 좌측 심볼+한글명, 중앙 방향 아이콘(`ArrowDownLeft`=입금 / `ArrowUpRight`=출금)+라벨, 우측 `formatQuantity(amount) {심볼}`.
- 항목 탭 → 상세 다이얼로그: 제목 `"{심볼} {입금|출금} 상세"`, 부제 `"{요청시각} 요청 · {거래소명}"`, 본문 2×2 그리드(수량 / 상태 배지 / 출발 거래소 / 도착 거래소), `completedAt` 이 있으면 완료 시각 행 추가.
- 상태 배지 라벨·색(32-48행): `PENDING 대기`(chart-4), `PROCESSING 처리중`(chart-3), `COMPLETED 완료`(emerald), `FAILED 실패`(destructive), `RETURNED 반환`(orange), `DELAYED 지연`(amber). **서버 상태는 `SUCCESS` 단일값이므로 이 매핑은 전부 `undefined` 가 된다**(§5.2.8 #2).
- **페이지네이션 없음.** 최초 로드 시 `size=50` 한 번만 조회하고 `hasNext`/`nextCursor` 를 사용하지 않는다.
- 빈 상태: 높이 192px 중앙 `"조건에 맞는 입출금 내역이 없습니다."`

송금 내역 API 계약 (`TransferHistoryController.java`, `FindTransferHistoryRequest.java`)

```
GET /api/wallets/{walletId}/transfers?type=&cursorTransferId=&size=
  type              : ALL | DEPOSIT | WITHDRAW (기본 ALL)
  cursorTransferId  : Long (마지막으로 받은 transferId)   ← 웹은 이 이름을 잘못 보내고 있다
  size              : 1~50 (@Min(1) @Max(50), 기본 20)
→ data: { content: [{transferId, type, coinId, coinSymbol, amount, status, createdAt, completedAt}],
          nextCursor: Long|null, hasNext: boolean }
```

#### 5.2.8 웹 구현의 알려진 결함 — Flutter 에서는 바로잡아 구현할 것

| # | 결함 | 근거 | Flutter 구현 지침 |
|---|---|---|---|
| 1 | **송금 내역이 항상 비어 보인다.** `mapTransferItem` 이 `exchangeId: ""` 로 채우는데, 패널은 `item.exchangeId === exchangeId`("upbit" 등)로 필터한다 → 교집합이 공집합 | `WalletPage.tsx:26` vs `TransferHistoryPanel.tsx:88` | 내역은 이미 walletId 로 조회하므로 거래소 필터 자체를 두지 않는다 |
| 2 | **상태값 불일치.** 서버 `TransferStatus` 는 `SUCCESS` 하나뿐인데 프론트는 6종을 가정한다 → 라벨 매핑이 `undefined` | `wallet/domain/vo/TransferStatus.java`, `lib/types/wallet.ts:26` | 상태는 `SUCCESS` 단일값으로 모델링하고 `"완료"` 배지(emerald)로 표시한다. 상태 필터 탭은 제거한다 |
| 3 | **코인 평가액이 전부 0.** 지갑 화면의 코인 `currentPrice` 가 0으로 고정되어 총 자산이 사실상 기준통화 잔고만 반영하고, `≈ 환산액`·`소액 제외`·`보유수량 정렬`이 모두 무의미해진다 | `WalletPage.tsx:103` | `GET /api/exchanges/{id}/coins` 응답에 이미 `price` 가 들어 있다. 이 값을 `currentPrice` 로 사용한다 |
| 4 | **커서 파라미터 이름 불일치.** 프론트는 `cursor`(문자열)를 보내지만 서버는 `cursorTransferId`(Long)를 받는다 → 커서가 무시된다. 또한 프론트가 붙이는 `userId` 쿼리는 서버가 쓰지 않는다 | `transfer-api.ts:47-53` vs `FindTransferHistoryRequest.java` | `cursorTransferId` 로 보내고 `userId` 는 보내지 않는다 |
| 5 | **상세의 상대 거래소 한쪽이 빈칸.** 출금이면 `toExchangeName=""`, 입금이면 `fromExchangeName=""` | `WalletPage.tsx:27-28` | API에 상대 지갑 정보가 없다. 상대 거래소 칸을 노출하지 않거나 `"—"` 로 표시한다 |
| 6 | 두 화면 모두 실시간 갱신·수동 새로고침 수단이 없다 | §5.1.2, §5.2.2 | pull-to-refresh 를 필수로 넣는다 |

---

### 5.3 (숫자 포맷 규칙)

§8.5 의 `formatters.ts` → Dart 이식 규칙을 따른다.

---

### 5.4 모바일(Flutter) UX 제안

#### 5.4.1 화면 골격

- 두 화면 모두 `Scaffold` + 하단 `NavigationBar`(마켓/투자내역/입출금/랭킹/복기). 거래소 탭은 `SegmentedButton` 또는 상단 pill 형태 `TabBar`(업비트·빗썸·바이낸스, 부제로 KRW/USDT)로 두고, 선택 상태는 라우트 쿼리(`?exchange=`)에 유지해 딥링크·복원이 가능하게 한다.
- 모든 목록에 `RefreshIndicator`(당겨서 새로고침)를 붙인다. 웹에는 갱신 수단이 없으므로 이 화면들의 유일한 수동 갱신 경로가 된다.
- 로딩은 `"로딩 중..."` 텍스트 대신 **스켈레톤 카드**(요약 1장 + 리스트 3장)로 대체한다.
- 에러는 중앙 정렬 안내 문구 + `[다시 시도]` 버튼(`FilledButton.tonal`)으로 재조회 가능하게 한다. 웹처럼 조용히 실패시키지 않는다.

#### 5.4.2 포트폴리오: 표 → 카드 리스트

`CustomScrollView` 로 다음 순서를 세로 배치한다.

1. **요약 카드**(`SliverToBoxAdapter`): 상단 `보유 {base}` 소형 텍스트, 그 아래 `총 보유자산` 대형 숫자, 그 아래 2×2 그리드(총매수·총평가·평가손익·수익률). 계산식은 §5.1.3 그대로.
2. **자산 구성 카드**: 도넛은 `CustomPainter` 로 직접 그린다. 12시 시작·시계 방향·`StrokeCap.butt`·두께 28(선택 시 34), 반지름 73, 중앙에 `총 자산`. hover 대신 **조각/범례 탭**으로 강조 상태를 토글하고, 선택된 조각의 라벨·금액·비율을 중앙 라벨에 잠시 치환해 보여준다.
3. **정렬 바**: `[정렬: 평가금액 ▼]` 칩 하나. 탭하면 `showModalBottomSheet` 로 7개 정렬 키와 오름/내림 토글을 제시한다. 기본값은 **평가금액 내림차순**을 권한다(웹은 응답 순서 그대로라 사용자가 볼 기준이 없다).
4. **보유 종목 카드 리스트**(`SliverList.builder`): 카드 1장 = 코인 1종.
   - 상단 행: 좌측 심볼(bold) + 한글명(작게), 우측 수익률 pill 배지.
   - 중단 행: 좌측 `평가금액`(대형), 우측 `평가손익`(부호·색 적용).
   - 하단 행(2열 미니 그리드): `보유수량` / `평균매수가` / `현재가`. 폭이 좁으면 `ExpansionTile` 로 접어 둔다.
   - 카드 탭 → 해당 코인의 마켓 상세(주문 화면)로 이동시키는 것을 권한다.
   - 빈 상태: `"보유 중인 코인이 없습니다."` + `[코인 사러 가기]` 버튼 → 마켓.

#### 5.4.3 입출금: 자산 리스트와 상세

1. **총 자산 카드**: 거래소명 + 눈 아이콘 토글(숨김 시 `"••••••••"`). 잠금 잔고가 있을 때만 `(사용 가능 … / 잠금 …)` 보조 줄.
2. **툴바**: 검색 `TextField`(심볼·한글명 부분일치) + `FilterChip("소액 제외")`. 상장 코인 전량(최대 600여 개)이 들어오므로 `ListView.builder` 로 지연 생성한다.
3. **자산 카드 리스트**: 카드 1장 = 코인 1종. 좌: 심볼 + 한글명. 우: 총 수량(bold) / 그 아래 `≈ 환산액`. 잠금이 있으면 자물쇠 아이콘 + `주문 대기` 배지를 우측 하단에 작게. 잔고 0인 코인은 흐리게(`opacity .4`) 표시하고 `"—"` 로 채운다.
4. **자산 상세**: 카드 탭 → `showModalBottomSheet(isScrollControlled: true)`. 내용: 총 수량 대형 숫자, 환산액, `[출금]` 버튼(**코인만**), 잔고 상세(사용 가능/잠금), 그리고 그 아래 해당 코인의 송금 내역.

#### 5.4.4 송금 플로우 (모바일 3단계)

웹의 단일 모달을 **전체 화면 3단계 마법사**(`Navigator.push` 로 띄우는 `Scaffold`)로 바꾼다. 모바일에서는 숫자 키패드가 화면 절반을 덮으므로 모달 안에 셀렉트·입력·오류·버튼을 모두 넣으면 조작이 불가능해진다.

- **1단계 · 도착 거래소 선택**: 라운드 내 다른 거래소를 카드 리스트로(현재 거래소 제외). 탭 즉시 2단계로 진행. 후보가 1개뿐이면 이 단계를 건너뛰고 자동 선택한다.
- **2단계 · 수량 입력**: 상단에 `{심볼} 출금`, 큰 숫자 입력 필드(`TextInputType.numberWithOptions(decimal: true)`, 입력 포매터 `FilteringTextInputFormatter.allow(RegExp(r'^\d*\.?\d*'))`), 바로 아래 `가용: {수량} {심볼}`. 빠른 비율 버튼 `[25%] [50%] [최대]`. 검증은 §5.2.6 규칙을 그대로 쓰되 **입력 즉시 실시간 검증**하고, 위반 시 하단 CTA를 비활성화한다.
- **3단계 · 확인**: 웹에 없는 단계를 신설한다. `출발 거래소 → 도착 거래소`, `수량 + 심볼`, `출금 후 남는 가용 잔고` 를 요약해 보여주고 `[출금하기]` 로 확정한다. 모의투자라도 실전 리허설이 제품 목적이므로 확인 단계가 정당하다.
- **요청·결과**: `idempotencyKey` 는 **2단계 진입 시 1회 생성해 화면 상태에 보관**하고, 타임아웃 재시도 시 같은 키를 재사용한다. 성공 시 `Navigator.popUntil` 로 지갑 화면까지 되돌아가 `SnackBar("{수량} {심볼} 출금 완료")` 를 띄우고 `loadWalletData()` 를 재호출한다. 실패 시에는 서버 `message` 를 사람이 읽을 수 있는 문구로 매핑해 보여준다.

| 서버 코드 | 사용자 문구 |
|---|---|
| `INSUFFICIENT_BALANCE` | 가용 잔고가 부족합니다. |
| `SAME_WALLET_TRANSFER` | 같은 거래소로는 송금할 수 없습니다. |
| `DIFFERENT_ROUND_TRANSFER` | 다른 라운드의 지갑으로는 송금할 수 없습니다. |
| `WALLET_NOT_OWNED` / `WALLET_ACCESS_DENIED` | 접근 권한이 없는 지갑입니다. |
| 그 외 | 송금에 실패했습니다. 잠시 후 다시 시도해 주세요. |

#### 5.4.5 송금 내역

- 자산 상세 바텀시트 하단에 `"입출금 내역"` 섹션으로 두되, 코인 필터가 걸리지 않은 **전체 내역 화면**(지갑 화면 상단 앱바의 히스토리 아이콘 → 별도 페이지)도 함께 제공한다. 웹은 코인을 선택해야만 내역을 볼 수 있어 접근성이 나쁘다.
- 유형 필터(`전체/입금/출금`)는 **서버 파라미터 `type` 으로 넘긴다**(클라이언트 필터가 아니라). 상태 필터는 서버 상태가 단일값이므로 제거한다.
- **무한 스크롤**: `size=20` 으로 시작해 리스트 끝에서 `cursorTransferId = 마지막 transferId` 로 다음 페이지를 이어 붙인다(`hasNext === false` 면 종료). 웹의 `size=50` 단발 조회를 대체한다.
- 항목: `ListTile` 형태 — 선행 아이콘(입금 `arrow_downward` / 출금 `arrow_upward`), 제목 `{수량} {심볼}`, 부제 `{yyyy.MM.dd HH:mm}`, 후행 `완료` 배지. 탭하면 상세 바텀시트(수량·상태·요청 시각·완료 시각).

#### 5.4.6 웹 위젯 → Flutter 대응표

| 웹 | Flutter |
|---|---|
| `ExchangeTabs`(pill 그룹) | `SegmentedButton` 또는 `TabBar(isScrollable: true)` |
| `HoldingsTable` / `WalletAssetTable`(그리드 + 가상 스크롤) | `SliverList.builder` + 카드 위젯 (별도 가상화 라이브러리 불필요) |
| 정렬 헤더 버튼 | 정렬 칩 + `showModalBottomSheet` 선택지 |
| `DonutChart`(SVG stroke-dasharray) | `CustomPaint` + `Canvas.drawArc`(`StrokeCap.butt`, `style: PaintingStyle.stroke`) |
| `TransferModal`(shadcn `Dialog`) | 전체 화면 `Navigator.push` 3단계 마법사 |
| 데스크톱 사이드 패널 / 모바일 바텀시트 이중 렌더 | `showModalBottomSheet` 단일 경로 |
| `Select` | `DropdownButtonFormField` 또는 1단계 선택 리스트 |
| `Eye/EyeOff` 잔액 숨김 | `IconButton` + `AnimatedSwitcher` 로 `"••••••••"` 치환 |
| `Intl.DateTimeFormat("ko-KR", …)` | `DateFormat('yyyy.MM.dd HH:mm', 'ko_KR')` (`intl`) |
| `crypto.randomUUID()` | `Uuid().v4()` (`uuid` 패키지) |

---
## 6. 화면: 랭킹과 투자 복기

### 6.1 공통 전제

| 항목 | 값 | 근거 |
|---|---|---|
| 웹 라우트 | `/ranking`, `/regret` | `App.tsx:36-37` |
| 하단/상단 내비 라벨 | "랭킹", "투자 복기" | `components/layout/Header.tsx:12-13` |
| 인증 예외 경로 | `/api/rankings`, `/api/rankings/stats` 는 비로그인 조회 가능 | `WebConfig.java:18-19` |

주의: 웹 프론트는 `getMyRanking`·`getRegretReport`·`getRegretChart` 호출 시 `userId` 를 쿼리 파라미터로 함께 보내지만, 서버 DTO 에는 `userId` 필드가 없어 무시된다. Flutter 에서는 `userId` 를 보내지 말고 **세션 쿠키 유지만 보장**하면 된다.

---

### 6.2 랭킹 화면 (`/ranking`)

#### 6.2.1 조회 파라미터

기간은 URL 쿼리 파라미터 `period` 로 관리한다. `weekly`/`monthly` 가 아니면 무조건 `daily` 로 강제된다(`RankingPage.tsx:121-124`). 탭 클릭은 `setSearchParams({ period })` 로 URL 을 갱신하며, 이 값이 바뀌면 전체 데이터를 재조회한다.

| 탭 키 | 라벨 | 서버 enum |
|---|---|---|
| `daily` | 일간 | `DAILY` |
| `weekly` | 주간 | `WEEKLY` |
| `monthly` | 월간 | `MONTHLY` |

#### 6.2.2 API 명세

계약은 §1.6.7 을 참조한다. 응답 타입 요약:

```
CursorPageResponseDto<T> { content: T[]; nextCursor: number | null; hasNext: boolean }
RankingItem   { rank: int; userId: int; nickname: string; profitRate: double; tradeCount: int }
MyRanking     { rank: int; nickname: string; profitRate: double; tradeCount: int }
RankingStats  { totalParticipants: int; maxProfitRate: double; avgProfitRate: double }
RankerPortfolio { userId: int; nickname: string; rank: int; profitRate: double;
                  holdings: [{ coinSymbol: string; exchangeName: string; assetRatio: double; profitRate: double }] }
```

`profitRate`·`maxProfitRate`·`avgProfitRate`·`assetRatio`·`RankerHolding.profitRate` 는 서버에서 `BigDecimal` 로 직렬화된다. Flutter 에서는 `double.parse(value.toString())` 로 통일해 파싱한다.

**`profitRate` 의 단위는 퍼센트 값 그 자체다**(예: `12.34` → `+12.34%`). 티커의 `changeRate`(비율)와 단위가 다르다는 점에 주의한다.

#### 6.2.3 첫 로드 절차 (`RankingPage.tsx:147-192`)

`period`, `user`, `retryCount` 중 하나라도 바뀌면 아래를 수행한다.

1. 확장 행·포트폴리오 캐시·에러 상태를 모두 초기화한다.
2. `Promise.allSettled` 로 세 요청을 병렬 발사한다: `getRankings({period, size:20})`, `getRankingStats(period)`, 로그인 상태면 `getMyRanking(...)` (비로그인이면 `null` 로 대체하여 호출 자체를 생략).
3. **랭킹 목록이 실패하면** 전체를 에러 화면으로 전환한다(목록·통계·내 랭킹 모두 비움).
4. 랭킹 목록이 성공하면, 통계·내 랭킹은 개별 실패를 허용하고 각각 `null` 로 둔다.

#### 6.2.4 화면 구성과 표 컬럼

- 페이지 헤더: 트로피 아이콘 + "랭킹" + 부제 `"{기간라벨} 수익률 기준 순위"`.
- 좌측 사이드(데스크톱 280px 고정, `lg:sticky top-24`): ① **내 랭킹** 카드 ② **{기간라벨} 통계** 카드.
- 우측: ① 1~3위 카드 3열 그리드 ② 4위 이하 리스트 ③ "랭킹 더보기" 버튼.

리스트 행(4위 이하)의 컬럼 구성(`:400-416`):

| 위치 | 내용 | 서식 |
|---|---|---|
| 좌 | 순위 배지 | 32×32 원형, 굵은 숫자 |
| 중 | 닉네임 / `{tradeCount}회 거래` | 닉네임 1줄 말줄임, 보조행 11px |
| 우 | 수익률 | `formatProfitRate` = `양수면 "+" + toFixed(2) + "%"`, 양수 `text-positive` / 음수 `text-negative` |

Top3 카드는 같은 정보를 세로로 배치하되 수익률을 18px 굵게 강조한다.

통계 카드는 참여자 수(`toLocaleString("ko-KR")`), 최고 수익률(항상 `text-positive`), 평균 수익률(부호에 따라 색상 분기)을 보여준다.

#### 6.2.5 내 순위 강조

- 별도의 "리스트 내 하이라이트"는 없다. 내 순위는 **사이드 카드 한 곳에서만** 표시된다(`:298-322`).
- `/api/rankings/me` 는 집계에 포함되지 않은 사용자에게 `data: null` 을 반환한다. 이때 카드는 "아직 순위가 없습니다. / 거래를 시작하면 다음 집계에 반영됩니다." 문구를 노출한다.
- 카드 내부: 순위 배지 + 닉네임 + `{tradeCount}회 거래` + 수익률(강조 박스, 20px 모노스페이스).

#### 6.2.6 더보기(커서 페이지네이션)

`hasNext == true && nextCursor != null && !isLoadingMore` 일 때만 동작한다(`:194-210`).

- 요청: `getRankings({ period, cursorRank: nextCursor, size: 20 })`
- 성공: `entries` 뒤에 append 하고 `hasNext`/`nextCursor` 갱신.
- 실패: 화면 전체를 에러로 바꾸지 않고 목록 하단에 `"추가 랭킹을 불러오지 못했습니다."` 만 인라인 표시.
- 버튼 라벨: 평상시 "랭킹 더보기", 로딩 중 "불러오는 중..." (비활성화).

#### 6.2.7 행 펼침 — 랭커 포트폴리오

행/카드를 누르면 해당 `userId` 를 펼친다(단일 확장, 같은 행 재클릭 시 닫힘). 펼치는 순간에만 `GET /api/rankings/{userId}/portfolio` 를 호출하며, **유저별로 결과를 캐싱하여 재호출하지 않는다**(`:212-241`).

표시(`:428-456`): 보유 코인마다 `coinSymbol` + 비중(`%`, 소수 1자리) + 가로 막대(비중 폭). 수량은 공개하지 않는다.

비중 정규화 로직이 중요하다.

```ts
// RankingPage.tsx:26-28
function asRatio(value: number): number { return value > 1 ? value / 100 : value; }
// 표시: (ratio * 100).toFixed(1) + "%"
// 막대 폭: Math.max(0, Math.min(100, ratio * 100))
```

즉 서버가 `0.35` 로 주든 `35` 로 주든 35.0% 로 렌더한다. **Flutter 도 동일 규칙을 이식한다.**

에러 처리는 행 단위다: 로딩 중 "포트폴리오를 불러오는 중입니다...", 실패 시 "포트폴리오를 불러오지 못했습니다.", `holdings` 가 비면 "보유 자산 정보가 없습니다."

서버 측 제약:
- 랭킹 집계 자체가 없으면 `RANKING_NOT_FOUND`.
- **101위 이하는 열람 불가** → `PORTFOLIO_VIEW_NOT_ALLOWED`. 현재 화면은 20개씩만 로드하므로 100위 초과 행을 펼치면 실패 문구가 뜬다. **Flutter 에서는 `rank > 100` 인 행의 펼침을 막고 "100위까지만 포트폴리오를 공개합니다." 를 안내한다.**

#### 6.2.8 집계 전(랭킹 없음) 상태

서버는 배치 미실행을 **오류가 아니라 빈 결과**로 응답한다. `findLatestReferenceDate` 가 비면 `RankingCursorResult.empty()` / `RankingStatsResult.empty()` 를 반환한다(`GetRankingsService.java:27-39`, `GetRankingStatsService.java:19-27`).

프론트는 `entries.length === 0` 을 집계 전으로 간주하고 다음을 렌더한다(`:59-117, 328-329, 364`).

- 상단: 1·2·3 자리표시 카드 3장 — 파선 테두리, 회색 배지 숫자, `--%`, 캡션 "집계 대기".
- 하단: 트로피 아이콘 + **"아직 집계된 랭킹이 없습니다"** + "랭킹은 매일 밤 자정에 집계됩니다. / 지금 거래를 시작하면 내일 첫 순위에 오릅니다." + `[거래하러 가기]` 버튼 → `/market` 이동.
- 통계 카드는 `StatsPlaceholder`(참여자 0명 / 최고 `--` / 평균 `--`, 투명도 60%)로 대체한다. 이때 `stats` 응답이 성공이어도 플레이스홀더가 우선한다.

#### 6.2.9 로딩·에러 상태 (랭킹)

| 상태 | 표시 |
|---|---|
| 최초 로딩 | 카드 안 텍스트 "랭킹 데이터를 불러오는 중입니다..." (스켈레톤 아님) |
| 목록 조회 실패 | `AlertCircle` + "랭킹을 불러오지 못했습니다" + "일시적인 문제일 수 있습니다. 잠시 후 다시 시도해주세요." + `[다시 시도]` → `retryCount++` 로 전체 재조회 |
| 통계만 실패 | 통계 카드에 "통계를 불러오지 못했습니다." |
| 내 랭킹만 실패 | 내 랭킹 카드가 `null` 케이스 문구로 표시됨 |
| 더보기 실패 | 목록 하단 빨간 소형 텍스트 |

---

### 6.3 투자 복기 화면 (`/regret`)

#### 6.3.1 도메인 의미

라운드를 시작할 때 사용자는 다섯 가지 투자 원칙(손절률·익절률·추격 매수 금지·물타기 제한·과매매 제한)을 정한다. 복기 기능은 **실제 체결 이력을 이 원칙과 대조**하여 위반 거래를 찾아내고, "원칙을 지켰다면 거래했을 금액(`expected_amount`)" 과 "실제 거래 금액(`actual_amount`)" 의 차이(`loss_amount`, 양수면 손해)를 규칙 유형별 공식으로 산출한다(`api/docs/regretanalysis/business-rules.md:9-139`).

핵심 산출물은 세 가지다.

1. **놓친 수익(`missedProfit`)** = 모든 위반의 `loss_amount` 합계(음수면 0으로 절사) — `RegretReport.sumLossAmounts`(`sum.max(BigDecimal.ZERO)`).
2. **규칙 준수 자산 곡선(`ruleFollowedAsset`)** = `실제 자산(d) + 그날까지의 누적 위반 손실(d)` — `CumulativeLossTimeline.calculateRuleFollowedAsset`.
3. **BTC 홀드 벤치마크(`btcHoldAsset`)** = 시드머니를 시작일에 전부 BTC 로 매수해 보유했을 때의 일별 평가액 — `BtcBenchmark.calculate`.

#### 6.3.2 조회 대상 결정과 API

`roundId` 와 `exchangeId` 는 `RoundContext.activeRound` 에서 얻는다. **웹은 활성 라운드의 첫 번째 지갑만 본다**(`RegretPage.tsx:49-53`).

```ts
const firstWallet = activeRound.wallets[0];   // { walletId, exchangeId }
if (!firstWallet) return;                     // 요청 자체를 하지 않음
```

두 요청(`/regret`, `/regret/chart`)은 `Promise.all` 로 동시에 나간다. 계약은 §1.6.8 참조.

프론트는 서버의 `totalDays` 를 쓰지 않고 `assetHistory.length` 로 재계산한다. 또한 마커의 `type` 은 **항상 `"loss"` 로 고정**된다 — `"gain"` 분기는 코드에만 존재하고 실제로 도달하지 않는다.

**Flutter 개선 제안**: 거래소 선택기를 추가해 `activeRound.wallets` 의 모든 거래소를 볼 수 있게 하고, 응답의 `exchangeName`·`currency` 를 표기해 통화 단위(KRW/USDT) 혼동을 막는다. `analysisStart~analysisEnd` 를 "분석 구간"으로, `impactGap` 을 규칙별 영향도로 노출하면 웹보다 정보량을 늘릴 수 있다.

#### 6.3.3 RuleType 매핑 (필수 이식 대상)

| 프론트 enum | 서버 enum | 라벨 | 색상 | 임계값 단위 | 아이콘(lucide) |
|---|---|---|---|---|---|
| `STOP_LOSS` | `LOSS_CUT` | 손절 | `#ED4B9E` | `%` | TrendingDown |
| `TAKE_PROFIT` | `PROFIT_TAKE` | 익절 | `#31D0AA` | `%` | TrendingUp |
| `NO_CHASE_BUY` | `CHASE_BUY_BAN` | 추격 매수 금지 | `#FFB237` | `%` | Ban |
| `AVERAGING_LIMIT` | `AVERAGING_DOWN_LIMIT` | 물타기 제한 | `#e84142` | `회` | Layers |
| `OVERTRADE_LIMIT` | `OVERTRADING_LIMIT` | 과매매 제한 | `#1FC7D4` | `회` | Timer |

근거: `mappers.ts:13-27`, `lib/types/regret.ts:56-70`, `regret-api.ts:71-77`(단위는 서버 `thresholdUnit` 을 무시하고 프론트 상수로 결정), `MeVsMe.tsx:15-21`.

`ruleImpacts` 는 위반이 0건인 규칙도 포함해 **라운드에 설정된 전체 규칙**을 `ruleId` 오름차순으로 반환한다. 따라서 토글 목록이 비는 경우는 "라운드에 규칙이 하나도 없을 때" 뿐이다.

#### 6.3.4 "만약 규칙을 지켰다면" 비교 — MeVsMe

MeVsMe 는 **규칙 체크박스 토글 패널**이다. 토글은 화면 로컬 상태이며 서버에 저장되지 않고, 초기값은 5종 전부 활성 + BTC 홀드 활성이다(`RegretPage.tsx:27-30`).

토글이 바꾸는 것은 **차트의 시뮬레이션 라인 하나뿐**이다. 상단 3-stat 카드(실제 / 규칙 준수 시 / 위반)는 서버 `summary` 를 그대로 보여주므로 토글과 무관하다.

시뮬레이션 계산식(`lib/types/regret.ts:72-94`):

```ts
RULE_IMPACT_WEIGHTS = {           // 합계 1.0
  STOP_LOSS: 0.30, NO_CHASE_BUY: 0.25, TAKE_PROFIT: 0.20,
  OVERTRADE_LIMIT: 0.15, AVERAGING_LIMIT: 0.10,
};

totalWeight = Σ RULE_IMPACT_WEIGHTS[켜진 규칙]
simulation[i] = Math.round( actual[i] + (ruleFollowed[i] - actual[i]) * totalWeight )
```

즉 실제 곡선과 규칙 준수 곡선 사이를 **가중치 합만큼 선형 보간**한 근사값이며, 전부 켜면 `totalWeight = 1.0` 이 되어 서버의 `ruleFollowedAsset` 과 정확히 일치한다. 서버 재계산은 일어나지 않는다. **Flutter 도 동일 상수·동일 반올림(`round`)으로 이식해야 웹과 값이 일치한다.**

MeVsMe 행 구성(`MeVsMe.tsx:60-106`):

| 요소 | 내용 |
|---|---|
| 체크박스 | 활성 시 `positive` 색 채움 + 체크 아이콘, 비활성 시 행 전체 opacity 40% |
| 아이콘 | 규칙 색상 12% 배경 위 규칙 아이콘 |
| 라벨 | 규칙 한국어 라벨 |
| 임계값 | `thresholdValue > 0` 이면 `+` 접두 → 예: `+10%`, `3회` (규칙 색상으로 표기) |
| 위반 배지 | `위반 {violationCount}` (negative 12% 배경) |

`ruleToggles` 가 비면 "설정한 투자 원칙이 없습니다." 를 표시한다.

벤치마크 섹션은 현재 **하드코딩 1건**이다: `{ id: "btc-hold", label: "BTC만 홀드한 나", color: "#f7931a", profitRate: 0 }` (`RegretPage.tsx:41-43`). 따라서 웹에서 이 항목의 수익률은 항상 `+0%` 로 표시되며(값이 서버와 연결되어 있지 않다), 체크박스는 차트의 BTC 라인 표시 여부만 토글한다. **Flutter 이식 시에는 `btcHoldValues` 의 첫 값과 마지막 값으로 수익률을 직접 계산해 채운다: `(last / first - 1) * 100`.**

#### 6.3.5 규칙 위반 거래 목록 — ViolationTradeList

- 헤더: 경고 삼각형 + "규칙 위반 거래", 우측에 필터 탭.
- 필터: `전체 {n}` / `손실 {n}` / `수익 {n}`. 판정 기준은 `LOSS: profitLoss < 0`, `PROFIT: profitLoss >= 0` (`:51-61`). 즉 **`profitLoss == 0` 은 수익으로 분류된다.**
- 행 구성(`:101-152`): `coinSymbol`(굵게) · `date`(`M/D`) · [감정 배지] · 위반 규칙 태그 N개(아이콘 + 라벨, 규칙 색 8% 배경) · 우측 끝 손익(`profitLoss >= 0` 이면 `+` 접두, `toLocaleString("ko-KR")`, 손실이면 negative 색).
- 정렬은 적용하지 않는다 → **서버 반환 순서 그대로**. 서버는 주문 단위 위반(같은 `orderId` 로 그룹핑, 위반 규칙을 배열로 합침)을 먼저, 모니터링 위반(`orderId == null`)을 뒤에 붙인다.
- 필터 결과가 비면 "해당 조건의 위반 거래가 없습니다."

**감정(emotion) 배지는 현재 절대 렌더되지 않는다.** `ViolationTrade.emotion` 은 선택 필드인데 매핑이 이 필드를 채우지 않으며, 백엔드 응답에도 대응 필드가 없다. **Flutter 에서는 이 배지를 구현 범위에서 제외한다.**

날짜 라벨은 API 계층에서 `M/D` 로 미리 포맷된다. **Flutter 에서는 `occurredAt` 원본을 모델에 보관하고 표시 시 포맷하는 편이 낫다**(상세 시트에서 시각까지 노출 가능).

---

### 6.4 RegretChart — 시각화 사양 (fl_chart 이식용)

`frontend/src/components/regret/RegretChart.tsx` 는 라이브러리 없이 SVG 를 직접 그린다. 아래 값은 그대로 이식 가능하다.

#### 6.4.1 캔버스·스케일

| 항목 | 값 | 근거 |
|---|---|---|
| 뷰박스 | `700 × 280` (반응형: 폭 100%) | `:28-29` |
| 패딩 | top 20 / right 20 / bottom 32 / left 60 | `:30` |
| X 스케일 | 인덱스 등간격. `x(i) = PAD.left + i/(n-1) * plotW` | `:61` |
| Y 스케일 | 표시 중인 **모든 시리즈**(actual, simulation, btcHold(활성 시))의 min/max 에 `(max-min)*0.1` 패딩. 범위가 0이면 패딩 1 | `:46-55` |
| Y 눈금 | `yMin ~ yMax` 5등분(5개) | `:72-75` |
| 렌더 조건 | `snapshots.length < 2` 이면 차트를 그리지 않는다 | `:44` |

fl_chart 대응: `LineChart(minY: yMin, maxY: yMax, minX: 0, maxX: (n-1))`, 각 시리즈는 `spots: [FlSpot(i.toDouble(), value)]`, `horizontalInterval: (yMax - yMin) / 4`.

#### 6.4.2 축 라벨

Y축 라벨 포맷(`formatKRWShort`, `:16-26`):

```
|v| >= 1억  → "{억}억{만}만"  (만 단위가 0이면 "{억}억")
|v| >= 1만  → "{round(v/10000)}만"
그 외        → toLocaleString("ko-KR")
```

X축 라벨 간격(`getTickInterval`, `regret.ts:104-110`):

| totalDays | 라벨 간격(일) |
|---|---|
| ≤ 14 | 1 |
| ≤ 60 | 7 |
| ≤ 180 | 14 |
| > 180 | 30 |

라벨 문자열은 API 계층에서 결정된다: `totalDays ≤ 180` 이면 `M/D`, 초과하면 `yyyy.MM`. 마지막 날짜는 직전 라벨과 `tickInterval / 2` 초과로 떨어져 있을 때만 추가로 찍는다(`:84-87`).

#### 6.4.3 시리즈

| 시리즈 | 데이터 | 색 | 굵기 | 스타일 | 표시 조건 |
|---|---|---|---|---|---|
| 실제 | `snapshots[i].actual` | `var(--primary)` | 2.2 | 실선 | 항상 |
| 규칙 준수 시뮬레이션 | `simulationLine[i]` | `var(--negative)` | 1.8 | 점선 `dash 6 4` | 켜진 규칙이 1개 이상일 때만 |
| BTC 홀드 | `btcHoldValues[i]` | `#f7931a` | 1.5 | 실선, opacity 0.5 | `btcHoldEnabled` 일 때만 |
| 위반 마커 | `markers[]` | loss=`var(--negative)` / gain=`var(--warning)` | 반지름 3 | 흰 테두리 1.5 | 항상(실데이터는 loss 만) |

마커는 `date` **문자열**로 스냅샷 인덱스를 역탐색해 좌표를 얻는다(`:90-97`). **Flutter 에서는 문자열 비교 대신 `snapshotDate`(DateTime) 로 매칭하도록 개선한다.**

fl_chart 권장 데이터 모델:

```dart
class RegretChartData {
  final List<AssetSnapshot> snapshots; // date(DateTime), label(String), actual(double), ruleFollowed(double)
  final List<double> btcHoldValues;    // snapshots 와 동일 길이·동일 인덱스
  final List<ViolationMarker> markers; // date(DateTime), value(double)
  final int totalDays;                 // = snapshots.length
}
// 시뮬레이션은 저장하지 않고 enabledRules 로부터 파생 계산한다.
```

위반 마커는 별도의 `LineChartBarData`(선 투명, `dotData` 만 활성)로 오버레이하거나 `ScatterChart` 를 겹쳐 그린다.

#### 6.4.4 상단 요약과 범례

- 최상단: 라벨 "놓친 수익", 값 `missedProfit.toLocaleString("ko-KR")` + `KRW`, 30px 굵게 negative 색.
- 3-stat 카드: `실제 {actualProfitRate}%` / `규칙 준수 시 {ruleFollowedProfitRate}%` / `위반 {totalViolations}건`. 앞의 두 값은 **소수점 절삭 없이 원본 그대로 출력**되고 부호에 따라 색이 갈린다. 서버 `BigDecimal` scale 이 그대로 노출되므로 **Flutter 에서는 `toStringAsFixed(2)` 로 다듬을 것을 권장한다.**
- 범례: 실제(실선) / 규칙 준수 시뮬레이션(점선, 활성 시) / BTC 홀드(활성 시) / 위반 지점(점).

#### 6.4.5 인터랙션(웹)

마우스 X 좌표에서 가장 가까운 인덱스를 찾아(`:111-125`) 세로 점선 + 각 시리즈의 강조 점 + 툴팁을 띄운다. 툴팁 내용: `fullDate`(`yyyy-MM-dd`), 실제 값, 시뮬레이션 값(활성 시), BTC 홀드 값(활성 시), 해당 날짜가 위반이면 "규칙 위반 (손실)". 툴팁이 우측 75% 를 넘으면 좌측으로 뒤집는다.

---

### 6.5 빈 상태 / 로딩 / 에러 (투자 복기)

렌더 분기는 `RegretPage.tsx:113-148` 에서 다음 우선순위로 결정된다.

| 순위 | 조건 | 표시 |
|---|---|---|
| 1 | `loading` | 텍스트 "로딩 중..." |
| 2 | `!activeRound` | `NoRoundNotice` — "진행 중인 라운드가 없어 복기할 내역이 없습니다." + `[새 라운드 시작]` → `/round/new` |
| 3 | `loadFailed` | 텍스트 "복기 데이터를 불러올 수 없습니다." (**재시도 버튼 없음**) |
| 4 | 정상 | 차트 + (MeVsMe · ViolationTradeList) |

추가 규칙:

- `user` 또는 `activeRound`, `activeRound.wallets[0]` 이 없으면 **요청 자체를 보내지 않는다**. `loading` 초기값이 `false` 이므로 라운드가 없으면 곧바로 `NoRoundNotice` 가 보인다.
- 배치 전 상태에서도 서버는 200 을 반환한다. 요약은 0-채움, 차트 `assetHistory` 는 스냅샷이 없으면 빈 배열. 이때 차트 영역은 파선 그리드 5줄과 중앙 문구 **"아직 집계된 자산 추이가 없습니다"** 만 그린다(`RegretChart.tsx:167-188`). 요약 카드는 `0 KRW`, `0%`, `0건` 으로 표시된다.
- 화면 하단에는 상태와 무관하게 고지 문구가 항상 붙는다: `"* 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다."`

---

### 6.6 모바일 UX 제안

#### 6.6.1 랭킹

1. **기간 탭**: 3분할 `SegmentedButton`(또는 `TabBar`)로 대체하고, 선택값을 라우터 쿼리(`/ranking?period=weekly`)에 유지해 딥링크·뒤로가기를 보존한다.
2. **사이드바 해체**: 데스크톱 좌측 컬럼을 세로 스택으로 편다. 순서는 ① 내 랭킹 카드 ② 통계 요약(참여자/최고/평균을 가로 3분할 소형 타일) ③ Top3 ④ 리스트.
3. **내 순위 스티키 바**: 스크롤로 내 랭킹 카드가 화면 밖으로 나가면 하단에 고정 바(순위 · 닉네임 · 수익률)를 띄우고, 탭하면 목록에서 내 위치로 점프한다. 리스트 안의 내 행은 `primary` 테두리로 강조한다 — 웹에는 없는 개선이지만 모바일 세로 화면에서 체감 가치가 크다.
4. **무한 스크롤**: "랭킹 더보기" 버튼 대신 스크롤 임계치 도달 시 자동으로 `cursorRank` 페이지를 이어 붙인다. 실패 시에만 목록 하단에 "다시 시도" 행을 노출한다.
5. **당겨서 새로고침**: `RefreshIndicator` 로 `retryCount` 재조회 경로를 재사용한다.
6. **포트폴리오는 바텀시트로**: 행 확장(ExpansionTile)은 좁은 화면에서 스크롤 위치를 흔든다. 행 탭 → `showModalBottomSheet` 로 랭커 닉네임·순위·수익률·보유 비중 막대를 크게 보여준다. 캐싱(유저별 1회 조회)은 그대로 유지한다.
7. **100위 초과 처리**: `rank > 100` 행은 탭을 막고 "포트폴리오는 100위까지 공개됩니다." 스낵바를 띄워 불필요한 실패 요청을 없앤다.
8. **Top3 표현**: 3열 그리드는 모바일에서 글자가 뭉개진다. 1위를 크게 두고 2·3위를 아래 2열로 배치하거나, 순위 배지에 금/은/동 색을 부여해 한 줄 리스트로 흡수한다.
9. **색상 접근성**: 수익률은 색상만이 아니라 `+`/`−` 부호와 상승/하락 아이콘을 병기한다.

#### 6.6.2 투자 복기

1. **거래소 선택기 추가**: 웹은 `wallets[0]` 에 고정되어 다른 거래소의 복기를 볼 수 없다. 상단에 거래소 칩/드롭다운을 두고 선택한 `exchangeId` 로 두 API 를 재호출한다.
2. **차트 상호작용**: 마우스 호버가 없으므로 **드래그 크로스헤어**(`LineTouchData(handleBuiltInTouches: true)`)로 대체하고, 툴팁은 차트 위 오버레이 대신 **차트 하단 고정 값 패널**(날짜 · 실제 · 시뮬레이션 · BTC 홀드 · 위반 여부)로 내린다.
3. **가로 전체화면 보기**: 차트 우상단에 확대 아이콘을 두고, 탭하면 가로 모드 전체화면 차트를 연다. 기간이 30일을 넘어가면 X 라벨이 뭉치므로 실효가 크다.
4. **규칙 토글을 차트 바로 아래 칩으로**: MeVsMe 의 세로 체크박스 리스트를 가로 스크롤 `FilterChip` 행(규칙 색 + 라벨 + 위반 수 배지)으로 압축하고, 임계값과 위반 상세는 칩 롱프레스 시 바텀시트로 보여준다. 토글 시 차트 라인은 기본 애니메이션(250ms)으로 부드럽게 전환한다.
5. **"놓친 수익" 히어로**: 최상단에 큰 숫자 + 한 줄 해설("규칙을 지켰다면 이만큼 더 벌었습니다")을 두고, 3-stat 은 그 아래 3분할 타일로 유지한다. 스크롤 시 축약된 값을 앱바에 고정 노출한다.
6. **BTC 홀드 벤치마크 수익률 채우기**: 하드코딩된 `0%` 대신 `btcHoldValues` 의 시작/끝 값으로 `(last/first - 1) * 100` 을 계산해 표기한다.
7. **위반 거래 목록**: 필터는 `SegmentedButton`(전체/손실/수익 + 건수), 목록은 `ListView.builder`. 한 행에 규칙 태그가 여러 개면 가로가 부족하므로 **2행 레이아웃**(1행: 코인 · 날짜 · 손익 / 2행: 규칙 태그 가로 스크롤)으로 바꾼다. 행 탭 시 상세 바텀시트(체결 시각 `occurredAt`, `orderId`, 위반 규칙별 설명)를 띄운다.
8. **집계 전 안내**: 차트 자리에는 "아직 집계된 자산 추이가 없습니다" 대신 배치 주기까지 안내한다("복기 리포트는 매일 밤 집계됩니다. 내일 다시 확인해 주세요."). 스켈레톤은 쓰지 않는다(빈 상태를 로딩으로 오인시키기 때문).
9. **에러 복구 경로 추가**: 웹의 `loadFailed` 는 재시도 수단이 없다. 모바일에서는 "다시 시도" 버튼과 당겨서 새로고침을 모두 제공한다.
10. **고지 문구 유지**: 하단 `* 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다.` 는 시뮬레이션 근사(가중치 보간)를 사용자에게 알리는 유일한 장치이므로 반드시 이식한다.

---

## 7. 화면: 로그인·라운드·마이페이지

### 7.1 로그인 (`/login`)

#### 7.1.1 화면 구성 (`frontend/src/pages/LoginPage.tsx`)

단일 카드 레이아웃이며 **이메일/비밀번호 로그인은 존재하지 않는다.** 소셜 로그인 2종이 전부다.

| 요소 | 내용 | 근거 |
|---|---|---|
| 로고 | `Activity` 아이콘(9×9, 라운드 사각형 primary 배경) + "Trypto" 텍스트 | `:31-36` |
| 서브카피 | "큰 돈 잃을 걱정 없이 해보는 실전 리허설" | `:37-39` |
| 카카오 버튼 | 배경 `#FEE500`, 글자 `#191600`, 높이 48px, 아이콘 `MessageCircle`. 라벨 "카카오로 로그인" / 진행 중 "카카오로 로그인 중…" | `:45-53` |
| 구글 버튼 | 흰 배경 + 테두리, 글자 `#1f1f1f`, 높이 48px, 4색 구글 SVG. 라벨 "구글로 로그인" / 진행 중 "구글로 로그인 중…" | `:54-79` |
| 버튼 비활성 조건 | 해당 제공자 설정 미완(`!isSocialConfigured`) 또는 다른 제공자가 진행 중(`pendingProvider !== null`) | `:48, 57` |
| 설정 누락 안내 | **개발 모드에서만** 노출. "카카오·구글 로그인 설정(.env.local)이 필요합니다." | `:80-85` |
| 오류 배너 | `error` 문자열을 destructive 배경 박스로 표시 | `:87-91` |

로그인 흐름(PKCE)의 상세는 §2.2 를, 모바일 대응은 §2.4 를 참조한다.

### 7.2 소셜 콜백 (`/auth/:provider/callback`)

`frontend/src/pages/SocialCallbackPage.tsx`. 이 경로는 **인증 여부와 무관하게 항상 처리**되는 라우트다 (`App.tsx:24`).

- `provider` 가 `kakao`/`google` 이 아니면 "지원하지 않는 소셜 제공자입니다." 오류 카드 + "로그인 화면으로 돌아가기" 버튼.
- 팝업 판별: `sessionStorage["oauth_popup"] === "1"` 이거나 `window.opener` 가 유효하면 팝업으로 본다.
- **팝업인 경우(PopupRelay)**: 검증·교환을 하지 않는다. `?code&state&error` 를 그대로 읽어 BroadcastChannel 로 주 창에 보내고 `window.close()`. 창이 안 닫힐 때를 대비해 "로그인 처리 중… 이 창은 닫으셔도 됩니다." 문구를 남긴다.
- **주 창인 경우(MainWindowExchange)**: state 검증 → `socialLogin()` 교환 → `loginWithSocial()` → `navigate("/market", {replace:true})`. 인가 코드는 일회용이므로 `useRef` 로 단 한 번만 교환한다.

즉 콜백 화면 자체는 UI 산출물이 거의 없는 **중계·교환 전용 화면**이며, 표시하는 것은 스피너와 오류 카드뿐이다. **모바일에서는 이 화면을 만들지 않는다**(§2.4).

### 7.3 라운드 생성 (`/round/new`)

`frontend/src/pages/RoundCreatePage.tsx`. 상단 헤더(`RoundCreateHeader`: 로고 + 닉네임 + 로그아웃), 타이틀 "투자 라운드 시작", 서브카피 "시드머니와 투자 원칙을 설정하고 모의투자를 시작하세요." 본문은 `자금 설정` → `투자 원칙` → 제출 버튼 순서다.

#### 7.3.1 자금 설정 (`SeedMoneyCard.tsx`)

두 개의 독립 카드로 구성된다.

| 항목 | 시작 자금(시드머니) | 긴급 자금 투입 상한 |
|---|---|---|
| 아이콘/설명 | `Wallet`, "모의투자에 사용할 초기 자본금" | `ShieldPlus`, "1회당 최대 투입 금액" |
| 상단 표시 | `₩{천단위 구분}` 대형 모노 폰트, 0이면 `₩0` 을 흐리게 | 동일 |
| 보조 표시 | 값>0 일 때 `formatKRW` 축약(예: `1,000만원`) | 동일 |
| 입력 | 텍스트 입력, `inputMode=numeric`. 숫자 이외 문자는 `replace(/[^0-9]/g, "")` 로 제거, 빈 값이면 0 (`:67-77, 113-123`) | 동일 |
| 프리셋 | 100만 / 500만 / 1,000만 / 5,000만 (`:7-12`) | 10만 / 50만 / 100만 (`:14-18`) |
| 부가 안내 | 없음 | "라운드 진행 중 최대 3회까지 긴급 자금을 투입할 수 있습니다" (`:131-135`) |

프리셋 버튼(`PresetButtons.tsx`)은 현재 값과 정확히 일치할 때 활성(그라디언트 `primary → #9A6AFF`) 스타일로 바뀐다. 프리셋은 토글이 아니라 **값 덮어쓰기**다.

**입력 범위에 대한 서버 제약(웹에는 클라이언트 검증이 없음)**

- 긴급 자금 상한은 백엔드에서 `0 ≤ limit ≤ 1,000,000` 이며 초과 시 `INVALID_EMERGENCY_FUNDING_LIMIT`(400). 웹은 100만 원 초과 입력을 막지 않아 제출 시점에 실패한다. **Flutter 는 입력 단계에서 1,000,000 상한을 강제할 것.**
- 시드머니는 백엔드 `@DecimalMin("0")` 만 있고 상한이 없다. 단 거래소별 시드 범위 정책(§1.6.4)이 별도로 적용된다.
- 긴급 충전 횟수는 서버가 항상 3회로 시작한다.

#### 7.3.2 투자 규칙 전체 목록 (`components/round/rules.ts`)

`RULE_CONFIGS` 에 정의된 3종이 **화면에서 선택 가능한 전부**다. `STOP_LOSS`, `TAKE_PROFIT` 은 백엔드에 위반 판정 로직이 없어(`Rule.java` 의 `LossCutRule.check()` / `ProfitTakeRule.check()` 가 `Optional.empty()` 반환) 설정 대상에서 의도적으로 제외되었다 (`rules.ts:8-12`).

| 규칙 키(프론트) | 백엔드 enum | 라벨 | 설명 | 기본값 | 입력 범위 | 입력 방식 | 단위 |
|---|---|---|---|---|---|---|---|
| `NO_CHASE_BUY` | `CHASE_BUY_BAN` | 추격 매수 금지 | 급등 코인 매수를 방지 | 15 | 1 ~ 50 (step 1) | 슬라이더 | `%` |
| `AVERAGING_LIMIT` | `AVERAGING_DOWN_LIMIT` | 물타기 제한 | 손실 중인 코인의 추가 매수 횟수 제한 | 3 | 1 ~ 10 (정수) | 숫자 입력 | `회` |
| `OVERTRADE_LIMIT` | `OVERTRADING_LIMIT` | 과매매 제한 | 하루 거래 횟수 제한 | 10 | 1 ~ 50 (정수) | 숫자 입력 | `회/일` |
| `STOP_LOSS` | `LOSS_CUT` | (손절) | 판정 미구현 — 생성 화면에서 제외 | — | — | — | `%` |
| `TAKE_PROFIT` | `PROFIT_TAKE` | (익절) | 판정 미구현 — 생성 화면에서 제외 | — | — | — | `%` |

마이페이지·복기 표시용 라벨/단위는 5종 전부 정의되어 있다 (`MyPage.tsx:26-32`) — 과거 라운드가 손절·익절을 갖고 있을 수 있으므로 **표시 계층에서는 5종 모두 처리해야 한다.**

각 규칙 카드(`InvestmentRuleCard.tsx`)의 동작:
- 카드 전체가 `role="switch"` 이며 클릭/Enter/Space 로 켜고 끈다 (`:38-49`).
- 꺼진 상태: 테두리 없음 + `bg-secondary/30`, 텍스트 흐림. 켜진 상태: `border-primary/50` + 카드 배경, 아래에 입력 영역이 펼쳐진다 (`:31-36, 65`).
- 숫자 입력은 `Math.max(min, Math.min(max, n))` 으로 즉시 clamp 하며, 파싱 실패(`NaN`) 시 값을 바꾸지 않는다 (`:88-91`).
- 슬라이더는 step 1 (`:72`).

섹션 헤더(`InvestmentRulesSection.tsx`)는 활성 개수가 1개 이상이면 `{n}개 활성` 배지를, 0개면 "최소 1개 이상의 원칙을 활성화해주세요" 안내를 띄운다.

#### 7.3.3 제출 규칙

- 제출 가능 조건: `seed > 0 && emergencyLimit > 0 && 활성 규칙 ≥ 1` (`RoundCreatePage.tsx:38`).
- 불충족 시 버튼 비활성 + "시드머니, 긴급 자금 상한, 투자 원칙 1개 이상 설정이 필요합니다."
- 제출 중 라벨은 "생성 중...", 기본 라벨은 `Rocket` 아이콘 + "라운드 시작하기".
- 실패 시(예외를 `createRound` 가 삼키고 `null` 반환) "라운드 생성에 실패했습니다. 입력값을 다시 확인해 주세요." 를 표시한다. **서버의 구체적 오류 메시지는 웹에서 버려진다.** → **Flutter 는 서버 오류 코드별 메시지를 노출한다**(`ACTIVE_ROUND_EXISTS` → "이미 진행 중인 라운드가 있습니다", `INVALID_EMERGENCY_FUNDING_LIMIT` → "긴급 자금 상한은 100만원 이하여야 합니다").
- 성공 시 `/market` 으로 `replace` 이동.

요청 바디는 시드를 거래소별 배열로 펼쳐 보낸다 — 업비트(1)에 전액, 빗썸(2)·바이낸스(3)에 0 (`round-api.ts:104-118`).

```
POST /api/rounds
{
  "seeds": [ {"exchangeId":1,"amount":10000000}, {"exchangeId":2,"amount":0}, {"exchangeId":3,"amount":0} ],
  "emergencyFundingLimit": 1000000,
  "rules": [ {"ruleType":"CHASE_BUY_BAN","thresholdValue":15} ]
}
```

(웹은 바디에 `userId` 도 넣으나 서버는 `@LoginUser` 세션에서 사용자 ID 를 얻으므로 무시된다. 보내지 않는다.)

### 7.4 라운드 도메인 규칙

#### 7.4.1 라우팅 가드

§2.7 을 참조한다.

#### 7.4.2 라운드가 없을 때의 동작

- 각 화면은 `NoRoundNotice` 로 대체된다. 문구는 화면별로 다르고 "새 라운드 시작" 버튼이 `/round/new` 로 보낸다 (`NoRoundNotice.tsx:8-20`).

| 화면 | 문구 |
|---|---|
| 마이페이지 (기본값) | 진행 중인 라운드가 없습니다. |
| 포트폴리오 | 진행 중인 라운드가 없어 포트폴리오가 비어 있습니다. |
| 입출금 | 진행 중인 라운드가 없어 지갑이 없습니다. |
| 투자 복기 | 진행 중인 라운드가 없어 복기할 내역이 없습니다. |

- 공통 헤더는 라운드가 없을 때만 "라운드 시작" 버튼을 노출한다 (`showRoundStart = !isRoundLoading && !hasActiveRound`, `Header.tsx:21, 54-61, 111-119`). **모바일에서는 하단 탭 바에 자리가 없으므로, 라운드가 없을 때만 마켓 화면 상단에 배너 형태로 띄운다.**
- 마켓 화면은 시세 조회 자체는 계속 되며, 주문 대상 해석이 `NO_ROUND` 로 실패한다.
- 마켓의 긴급 자금 카드는 활성 라운드가 있을 때만 렌더된다.

#### 7.4.3 라운드 종료 흐름

마이페이지에서만 종료할 수 있다 (`MyPage.tsx:247-270`).

1. "라운드 종료"(destructive) 버튼 → 확인 다이얼로그: 제목 "라운드를 종료하시겠습니까?", 본문 "종료된 라운드는 복구할 수 없습니다. 현재 보유한 모든 자산과 주문 내역이 초기화됩니다.", 버튼 [취소] [종료].
2. [종료] → `POST /api/rounds/{roundId}/end` (바디 없음).
3. 성공 시 `clearRound()` 로 로컬 활성 라운드를 비우고, 확인 다이얼로그를 닫은 뒤 **완료 다이얼로그**를 연다 (`:97-107`).
4. 완료 다이얼로그: 제목 "수고하셨어요. 한 라운드를 마무리했습니다.", 본문 "시세는 계속 둘러보실 수 있어요. 준비되면 새 라운드를 시작해보세요.", [확인].
5. [확인] 또는 다이얼로그 바깥 닫기 → `/market` 으로 replace 이동.
6. 실패 시 콘솔 로그만 남고 화면에는 아무 변화가 없다. **Flutter 에서는 스낵바로 실패를 알릴 것.**

종료 후 `totalRoundCount` 는 그대로이므로 `hasEverStartedRound` 가 참으로 남아 `ProtectedRoute` 가 `/round/new` 로 밀어내지 않는다. 즉 **"라운드 없이 둘러보기" 상태**로 진입한다.

**모바일**: 라운드 종료 확인은 `Dialog` 대신 **하단 시트 + 파괴적 액션(빨강)** 으로 바꾸고, 완료 안내는 다이얼로그를 유지한다(사용자가 "마무리"를 인지해야 하는 지점이므로 화면 전환 전 명시적 확인이 필요하다).

#### 7.4.4 `RoundProvider` 가 들고 있는 상태와 노출 함수

`frontend/src/contexts/RoundProvider.tsx`, 계약은 `RoundContext.tsx:8-18`.

**상태**

| 필드 | 타입 | 의미 |
|---|---|---|
| `activeRound` | `InvestmentRound \| null` | 활성 라운드 전체(규칙·지갑 포함) |
| `hasActiveRound` | `boolean` | `activeRound !== null` |
| `hasEverStartedRound` | `boolean` | `totalRoundCount > 0` |
| `isRoundLoading` | `boolean` | 초기값 `true`. 인증 복구가 끝나기 전에는 계속 `true` 로 유지한다 (`:21-24`) |

**적재 규칙 (`refreshActiveRound`, `:20-53`)**

- `isAuthLoading` 이면 판단을 미루고 `isRoundLoading=true` 유지.
- 비로그인이면 `activeRound=null`, `totalRoundCount=0`, 로딩 종료.
- 로그인 상태면 `GET /api/rounds/active` 와 `GET /api/rounds/summary` 를 **병렬로** 호출한다.
- 실패하면 둘 다 초기화하고 콘솔 로그만 남긴다(화면 오류 표시 없음).
- `user` 또는 `isAuthLoading` 이 바뀔 때마다 자동 재실행된다.

`fetchActiveRound` 는 서버 오류 코드가 `ROUND_NOT_ACTIVE` 이면 예외 대신 `null` 을 반환한다 (`round-api.ts:127-137`). 그 외 오류는 그대로 던진다. **Flutter 에서도 이 코드 분기를 반드시 재현해야 한다** (서버는 409 + `code: "ROUND_NOT_ACTIVE"` 로 응답).

**노출 함수**

| 함수 | 시그니처 | 동작 |
|---|---|---|
| `createRound` | `(CreateRoundParams) => Promise<InvestmentRound \| null>` | `POST /api/rounds`. 성공 시 `activeRound` 갱신 + `totalRoundCount = max(prev+1, round.roundNumber)`. 실패 시 `null` 반환(예외를 삼킴) |
| `clearRound` | `() => void` | 로컬 `activeRound` 만 `null` 로. 서버 호출 없음 |
| `refreshActiveRound` | `() => Promise<void>` | 위 적재 규칙 재실행 |
| `getWalletId` | `(exchangeId: number) => number \| null` | `activeRound.wallets` 에서 해당 거래소의 `walletId` 를 찾는다. 라운드가 없거나 지갑이 없으면 `null` |
| `chargeEmergencyFunding` | `(amount: number, exchangeId: number) => Promise<boolean>` | §4.5 참조 |

### 7.5 마이페이지 (`/mypage`)

`frontend/src/pages/MyPage.tsx`. 공통 `Header` + 타이틀 "마이페이지" / "프로필 관리 및 투자 라운드 현황". 본문은 데스크톱 2열 그리드(프로필 / 현재 라운드), 그 아래 피드백 카드가 2열 폭을 차지한다.

#### 7.5.1 프로필 카드

- **닉네임 변경**: 기본은 `{닉네임}` + [수정] 버튼. [수정] 클릭 시 인라인 입력창(폭 144px) + [저장]. Enter 로도 저장된다 (`:139-167`).
  - 저장 로직(`:82-95`): 입력값을 `trim()` 한 뒤 **빈 문자열이거나 기존 닉네임과 같으면 API 호출 없이 편집 모드만 종료**한다. 그 외에는 `PUT /api/users/me/nickname { nickname }` 호출 후 로컬 사용자 상태를 갱신한다. 실패해도 화면에는 아무 표시가 없고 콘솔 로그만 남으며 편집 모드는 닫힌다.
  - **서버 제약: 2 ~ 20자** (`Nickname.java:7-13`, 위반 시 `INVALID_NICKNAME_LENGTH` 400). 웹은 길이 검증을 하지 않는다. **Flutter 는 2~20자를 입력 단계에서 검증하고 실패를 사용자에게 보여줄 것.**
- **가입일**: 마운트 시 `GET /api/users/me` 를 호출해 `createdAt` 을 받아 `ko-KR` 형식(`2026년 7월 15일`)으로 표시한다. 로딩 전/실패 시 `-`.

#### 7.5.2 현재 라운드 카드

활성 라운드가 있을 때 (`:183-271`):

- `라운드 {roundNumber}` + 상태 배지 — `ACTIVE`→"진행중"(default), `BANKRUPT`→"파산"(destructive), `ENDED`→"종료"(secondary) (`:34-44`).
- `시작일: {startedAt 을 ko-KR 날짜로}`.
- 3칸 통계 그리드: **시드머니**(`initialSeed`), **긴급자금 상한**(`emergencyFundingLimit`), **남은 충전**(`emergencyChargeCount`회). 금액은 `value.toLocaleString("ko-KR") + "원"` 형식이다 — 마이페이지 전용 로컬 함수이며 **축약하지 않는다** (`:46-48`).
- **투자 원칙 목록**: `{라벨}` / `{임계값}{단위}` 행 나열. 비어 있으면 "설정된 원칙이 없습니다."
- **라운드 종료** 버튼 (§7.4.3).

활성 라운드가 없으면 `NoRoundNotice`.

**모바일**: 3칸 통계는 폭이 좁으므로 **2열 + 1열**(시드머니 전체 폭 / 상한·남은 충전 2열)로 재배치한다.

#### 7.5.3 피드백 보내기 (`components/feedback/FeedbackCard.tsx`)

- 제목 "피드백 보내기", 설명 "사용하면서 느낀 점이나 개선했으면 하는 부분을 알려주세요."
- 5행 textarea, placeholder "어떤 점이 좋았고, 무엇이 아쉬웠나요?", `maxLength=1000` (`:44-54`).
- **길이 규칙: `content.trim().length` 기준 20자 이상 1000자 이하**. 20자 미만이면 "최소 20자 이상 입력해주세요.", 이상이면 `{n} / 1000자` 카운터를 표시한다 (`:7-8, 16-17, 57-61`). 서버도 동일하게 strip 후 20~1000자를 강제한다 (`FeedbackContent.java:7-16`, 위반 시 `INVALID_FEEDBACK_LENGTH` 400).
- 전송: `POST /api/feedbacks { content: trim된 값 }`. 성공 시 입력창을 비우고 "피드백이 접수되었습니다. 소중한 의견 감사합니다." 를 표시한다. 입력이 다시 바뀌면 이 성공 문구는 사라진다.
- 실패 시 서버 메시지(`ApiClientError.message`) 또는 "피드백 전송에 실패했습니다." 를 destructive 텍스트로 표시.
- 버튼 라벨: "보내기" / 전송 중 "보내는 중...".

**모바일**: 카운터를 입력창 우측 하단에 붙이고, 전송 버튼은 키보드 위 고정 바에 둔다. 20자 미만일 때 버튼 비활성 + 안내 문구는 웹과 동일하게 유지한다.

#### 7.5.4 존재하지 않는 기능 (오해 방지)

- **포트폴리오 공개 설정: 웹·백엔드 모두 존재하지 않는다.** 프론트 전체 검색에서 관련 상태·API·UI 가 하나도 없다.
- **회원 탈퇴: 웹 UI 에는 없다.** 백엔드에는 `DELETE /api/users/me` 가 구현되어 있다. 웹 기능 동등성만 목표라면 구현 범위에서 제외하되, **iOS 배포 시에는 필수다**(R11).
- 로그아웃은 헤더에만 있다. **모바일에는 상시 노출 헤더가 없으므로 마이페이지 하단에 로그아웃 버튼을 추가한다.**

### 7.6 모바일 UX 제안 (요약)

- **소셜 로그인**: 팝업·BroadcastChannel·bfcache 대응을 옮기지 않는다. `flutter_web_auth_2` 로 시스템 인앱 브라우저를 띄우고 콜백 URL 을 그대로 받는 한 갈래로 단순화한다(§2.4).
- **라운드 생성**: 한 페이지에 자금·규칙·제출이 모두 쌓여 있는 웹 구조를 **2단계 스텝(자금 → 원칙) + 하단 고정 CTA** 로 나눠 스크롤 부담을 줄인다. 상단에 진행 표시(1/2, 2/2)를 둔다.
  - 금액 입력은 **커스텀 숫자 키패드(0~9, ⌫, 00)** 를 권장한다. 프리셋 칩은 2×2(시드) / 3×1(긴급) 그리드로 배치한다.
  - **긴급 자금 상한 1,000,000원을 입력 단계에서 강제한다.** 초과 시 즉시 인라인 오류("상한은 100만원입니다").
  - `NO_CHASE_BUY` 는 슬라이더 + [−]/[+] 스텝 버튼을 함께 제공하고, 숫자 입력형 두 규칙은 텍스트 필드 대신 **스테퍼(−/+)** 로 바꾼다(범위가 1~10, 1~50 으로 좁다).
- **라운드 상태와 내비게이션**: 웹의 라우트 가드 3종은 go_router `redirect` 로 1:1 이식한다(§2.7.3). `isAuthLoading || isRoundLoading` 동안 웹은 아무것도 렌더하지 않지만, 모바일에서는 흰 화면이 깜빡이므로 **스플래시(로고 + 로딩)를 유지**한다.
- **마이페이지**: 2열 그리드를 **세로 1열 카드 스택**으로 바꾼다(프로필 → 현재 라운드 → 피드백 → 로그아웃). 닉네임 인라인 편집은 **행 탭 → 하단 시트(입력 + 저장)** 로 바꾸고, 2~20자 검증과 실패 스낵바를 붙인다.

---
## 8. 디자인 시스템

### 8.1 색 토큰

#### 8.1.1 다크 모드 지원 여부 — **미지원 (light 전용)**

코드로 확정한 근거는 다음과 같다.

| 확인 항목 | 결과 | 근거 |
|---|---|---|
| `.dark` 클래스 색 정의 블록 | 없음. `:root` 블록 단 하나뿐 | `frontend/src/index.css:54-90` |
| `@custom-variant dark` 선언 | 없음 | `index.css` 전체, 그리고 `@import "shadcn/tailwind.css"` 의 실체인 `node_modules/shadcn/dist/tailwind.css`(전문 확인 — variant 9개와 `no-scrollbar` 유틸리티만 있고 dark 관련 선언 없음) |
| `prefers-color-scheme` 미디어 쿼리 | 없음 | `frontend/src` 전체 grep 0건 |
| `documentElement.classList` 로 `dark` 토글하는 코드 | 없음. ThemeProvider/useTheme 도 없음 | `frontend/src` 전체 grep 0건 |

`components/ui/button.tsx`, `select.tsx`, `switch.tsx` 에 `dark:` 유틸리티가 남아 있으나 이는 shadcn 기본 템플릿의 잔재이다. **다크 색 토큰이 하나도 정의되어 있지 않으므로** OS 다크 모드에서도 팔레트는 라이트 그대로다.

> **Flutter 지침**: `MaterialApp` 에 `theme`(라이트) 하나만 정의하고 `darkTheme` 은 정의하지 않으며, `themeMode: ThemeMode.light` 를 명시해 OS 설정과 무관하게 라이트로 고정한다.

#### 8.1.2 상승/하락 색 — **초록 상승 / 붉은 분홍 하락**

국제 관행을 따르며, 한국 관행인 빨강 상승·파랑 하락이 **아니다.**

| 방향 | 토큰 | 값 | 근거 |
|---|---|---|---|
| 상승·이익 | `--positive` | `#2ECC87` (초록) | `index.css:88` |
| 하락·손실 | `--negative` | `#E85D75` (붉은 분홍) | `index.css:89` |

적용 지점: 코인 목록의 현재가 글자색과 등락률 배지, 캔들 차트의 봉 색(`isUp ? "var(--positive)" : "var(--negative)"`), 보유자산 수익률, 랭킹 수익률, 복기 화면.

#### 8.1.3 색 토큰 전체 (`frontend/src/index.css:54-90`)

| 토큰 | light 값 | 용도 |
|---|---|---|
| `--background` | `#F8F7F4` | 페이지 배경(따뜻한 오프화이트). 탭 활성 배경, outline 버튼 배경에도 사용 |
| `--foreground` | `#1A1A2E` | 본문 기본 글자색(짙은 남색빛 검정) |
| `--card` | `#FFFFFF` | 카드·테이블·바텀시트 배경 |
| `--card-foreground` | `#1A1A2E` | 카드 내부 글자색 |
| `--popover` | `#FFFFFF` | Select 드롭다운 등 팝오버 배경 |
| `--popover-foreground` | `#1A1A2E` | 팝오버 글자색 |
| `--primary` | `#6C5CE7` (보라) | 주요 버튼, 활성 정렬 아이콘, 슬라이더 채움/썸 테두리, 포커스 링, 복기 차트 실제 수익 곡선 |
| `--primary-foreground` | `#FFFFFF` | primary 위 글자 |
| `--secondary` | `#F0EEEB` | secondary 버튼 배경, 테이블 헤더 배경(`bg-secondary/30`) |
| `--secondary-foreground` | `#1A1A2E` | secondary 위 글자 |
| `--muted` | `#F0EEEB` | Tabs 트랙 배경, Slider 트랙 배경 |
| `--muted-foreground` | `#7C7C8A` | 보조 텍스트, 차트 축 라벨, 플레이스홀더 |
| `--accent` | `#F0EEEB` | hover 배경 |
| `--accent-foreground` | `#1A1A2E` | hover 시 글자 |
| `--destructive` | `#E85D75` | 파괴적 액션 버튼(negative 와 동일 값) |
| `--border` | `#E8E6E1` | 모든 테두리, 구분선, 차트 그리드선 |
| `--input` | `#F0EEEB` | 입력 요소의 **테두리 색**(`border-input`), Switch 꺼짐 상태 트랙 배경 |
| `--ring` | `#6C5CE7` | 포커스 링(primary 와 동일 값) |
| `--chart-1` | `#6C5CE7` | 차트 계열 1 (보라) |
| `--chart-2` | `#0ABFBC` | 차트 계열 2 (청록) — 실제 사용처 없음 |
| `--chart-3` | `#E85D75` | 송금 `PROCESSING` 상태 배지 |
| `--chart-4` | `#F0A030` (주황) | 즐겨찾기 별, 긴급 자금 카드, 송금 `PENDING` 배지, 잠긴 수량 강조 |
| `--chart-5` | `#2ECC87` | 차트 계열 5 (초록) — 실제 사용처 없음 |
| `--warning` | `#F0A030` | 주문 `PENDING` 배지, 주문 패널 경고 박스 테두리/배경 |
| `--positive` | `#2ECC87` | **상승·이익** |
| `--negative` | `#E85D75` | **하락·손실** |
| `--sidebar` 외 7종 | `#F8F7F4` 등 | shadcn 보일러플레이트. **저장소 전체에서 사용처 0건이므로 Flutter 이식 대상에서 제외한다** |

**알파 적용 규칙**: Tailwind 의 `/N` 표기는 해당 색의 불투명도 N% 를 뜻한다. 실제 사용 조합은 다음과 같다.

| 표기 | Flutter 환산 |
|---|---|
| `bg-positive/15` | `Color(0xFF2ECC87).withValues(alpha: 0.15)` |
| `bg-negative/15`, `bg-negative/20`, `bg-negative/12` | `#E85D75` 에 각각 0.15 / 0.20 / 0.12 |
| `bg-warning/10`, `border-warning/30` | `#F0A030` 에 각각 0.10 / 0.30 |
| `bg-chart-4/15`, `bg-chart-4/10` | `#F0A030` 에 0.15 / 0.10 |
| `hover:bg-primary/[0.03]`, `bg-primary/[0.04]` | 행 hover / 선택 배경. `#6C5CE7` 에 0.03 / 0.04 |
| `border-border/40`, `border-border/30` | `#E8E6E1` 에 0.40 / 0.30 |
| `bg-secondary/30` | `#F0EEEB` 에 0.30 |
| `text-muted-foreground/40` | `#7C7C8A` 에 0.40 |
| `text-foreground/60` | 비활성 탭 글자. `#1A1A2E` 에 0.60 |

#### 8.1.4 정의되지 않은 토큰을 참조하는 결함 1건

`components/market/OrderPanel.tsx:387,399,405` 이 `text-warning-foreground` 를 사용하나 `--color-warning-foreground` 는 `@theme inline` 에 **정의되어 있지 않다**(`index.css:44-46` 에는 `positive`/`negative`/`warning` 만 있음). Tailwind v4 는 매칭되는 토큰이 없으면 해당 유틸리티 CSS 자체를 생성하지 않으므로, 이 경고 박스의 글자색은 실제로는 상속된 `--foreground`(`#1A1A2E`)로 렌더링된다.

> **Flutter 지침**: 현재 화면에 보이는 결과를 그대로 재현한다. 경고 박스 텍스트 색은 `#1A1A2E`(onSurface), 테두리는 `#F0A030` 30%, 배경은 `#F0A030` 10%. (원 의도는 주황 글자였을 것으로 보이나 임의로 고치지 않는다.)

#### 8.1.5 코인 심볼 고유 색 (`frontend/src/lib/types/coins.ts:11-47`)

`getCoinColor(symbol)` 은 아래 맵에서 조회하고, 미등록 심볼은 **`#8b949e`** 를 반환한다.

`BTC #f7931a`, `ETH #627eea`, `XRP #00aae4`, `SOL #9945ff`, `DOGE #c2a633`, `ADA #0033ad`, `AVAX #e84142`, `DOT #e6007a`, `LINK #2a5ada`, `MATIC #8247e5`, `ATOM #2e3148`, `UNI #ff007a`, `AAVE #b6509e`, `SAND #04adef`, `MANA #ff2d55`, `BNB #f3ba2f`, `ARB #28a0f0`, `OP #ff0420`, `EOS #000000`, `TRX #ef0027`, `QTUM #2e9ad0`, `JUP #00d18c`, `BONK #f8a100`, `RAY #6c5ce7`, `ORCA #ffda44`, `MNGO #e4572e`, `PYTH #7b61ff`, `WIF #c08b5c`, `RENDER #1a1a2e`, `HNT #474dff`, `MSOL #9945ff`

---

### 8.2 타이포그래피

#### 8.2.1 폰트 패밀리

| 역할 | 스택 | 정의 위치 | 로드 방식 |
|---|---|---|---|
| 본문 (`--font-sans`, 기본) | `'Pretendard Variable', 'Pretendard', 'Noto Sans KR', system-ui, -apple-system, sans-serif` | `index.css:47` | jsDelivr CDN, Pretendard v1.3.9 variable dynamic-subset (`index.html:10`) |
| 제목 (`--font-display`) | `'Noto Sans KR', 'Pretendard Variable', system-ui, sans-serif` | `index.css:48` | Google Fonts, weight 500/600/700/800 (`index.html:9`) |
| 숫자 (`font-mono`) | **`@theme` 에 미정의** → Tailwind v4 기본 모노 스택 | — | 시스템 폰트 |

`font-mono` 는 56회, `tabular-nums`(고정폭 숫자)는 18개 파일에서 사용되며, 가격·수량·수익률 등 **모든 수치 표시는 `font-mono` + `tabular-nums` 조합**이다.

#### 8.2.2 body 기본 스타일 (`index.css:96-101`)

```
font-weight: 400;
font-feature-settings: "cv02", "cv03", "cv04", "cv11";
letter-spacing: -0.01em;
-webkit-font-smoothing: antialiased;
```

#### 8.2.3 `.font-display` (`index.css:105-108`)

`font-family: var(--font-display); font-weight: 800;` — 7개 페이지의 h1 이 전부 `font-display text-3xl tracking-tight` 조합이다.

#### 8.2.4 크기 스케일 (실사용 빈도 순, Tailwind v4 기본값 기준)

| 클래스 | 사용 횟수 | font-size | line-height | height(=lh/fs, Flutter) |
|---|---|---|---|---|
| `text-sm` | 143 | 14px | 20px | 1.4286 |
| `text-xs` | 96 | 12px | 16px | 1.3333 |
| `text-[11px]` | 31 | 11px | 기본 | — |
| `text-lg` | 18 | 18px | 28px | 1.5556 |
| `text-base` | 9 | 16px | 24px | 1.5 |
| `text-3xl` | 9 | 30px | 36px | 1.2 |
| `text-2xl` | 8 | 24px | 32px | 1.3333 |
| `text-[13px]` | 7 | 13px | 기본 | — |
| `text-[10px]` | 7 | 10px | 기본 | — |
| `text-4xl` | 2 | 36px | 40px | 1.1111 |

#### 8.2.5 굵기 스케일

| 클래스 | 사용 횟수 | weight |
|---|---|---|
| `font-semibold` | 75 | 600 |
| `font-medium` | 57 | 500 |
| `font-bold` | 56 | 700 |
| `font-extrabold` | 7 | 800 |
| (body 기본) | — | 400 |

#### 8.2.6 자간

| 클래스 | 사용 횟수 | 값 | Flutter 환산 |
|---|---|---|---|
| (body 기본) | 전역 | `-0.01em` | `letterSpacing: fontSize * -0.01` |
| `tracking-tight` | 21 | `-0.025em` | `letterSpacing: fontSize * -0.025` (h1 30px → `-0.75`) |
| `tracking-wide` | 3 | `0.025em` | `letterSpacing: fontSize * 0.025` (코인 심볼 13px → `0.325`) |

---

### 8.3 반경 · 간격 · 그림자 · 테두리

#### 8.3.1 반경 (`index.css:6-12, 55`)

기준값 `--radius: 0.625rem = 10px`.

| Tailwind 클래스 | 계산식 | 실제 값 | 사용 횟수 |
|---|---|---|---|
| `rounded-xs` | (v4 기본) | 2px | 1 |
| `rounded` | (v4 기본) | 4px | 3 |
| `rounded-sm` | `radius - 4px` | **6px** | 1 |
| `rounded-md` | `radius - 2px` | **8px** | 20 |
| `rounded-lg` | `radius` | **10px** | 31 |
| `rounded-xl` | `radius + 4px` | **14px** | 61 |
| `rounded-2xl` | `radius + 8px` | **18px** | 12 (+ `rounded-t-2xl` 1) |
| `rounded-3xl` | `radius + 12px` | 22px | 0 |
| `rounded-4xl` | `radius + 16px` | 26px | 0 |
| `rounded-full` | — | 9999px (`StadiumBorder`) | 38 |

**핵심**: 카드/테이블 컨테이너의 기본 반경은 `rounded-xl` = **14px**, 버튼/입력의 기본 반경은 `rounded-md` = **8px**, 배지는 `rounded-full` 이다.

#### 8.3.2 간격

Tailwind 기본 4px 단위 스케일을 그대로 쓴다(`gap-1`=4px, `gap-1.5`=6px, `gap-2`=8px, `gap-3`=12px, `gap-6`=24px, `px-4`=16px, `px-5`=20px, `px-6`=24px, `py-0.5`=2px, `py-3.5`=14px).

레이아웃 폭 규칙:

| 항목 | 값 | 근거 |
|---|---|---|
| 콘텐츠 최대 폭 | `max-w-6xl` = 1152px, 가운데 정렬 | 7개 페이지 공통 |
| 좌우 패딩 | `px-4` = 16px | 동일 |
| 헤더 높이 | `h-14` = 56px | `Header.tsx:25` |
| 본문 상하 패딩 | `py-6`(24px) 또는 `py-8`(32px) | 페이지별 |
| 로그인 카드 폭 | `max-w-[380px]` | `LoginPage.tsx:28` |

> **Flutter 지침**: 모바일에서는 `max-w-6xl` 제약이 의미 없으므로 화면 폭을 그대로 쓰고, **좌우 패딩 16px** 만 유지한다.

#### 8.3.3 그림자 (`index.css:49-51`)

| 토큰/클래스 | 값 | 사용 횟수 |
|---|---|---|
| `shadow-card` | `0 0 0 1px var(--border)` — **블러 없는 1px 링. 사실상 테두리와 동일** | 8 |
| `shadow-card-hover` | `0 2px 8px rgba(0,0,0,0.04), 0 0 0 1px var(--border)` | 5 |
| `shadow-card-active` | `0 4px 12px rgba(0,0,0,0.06), 0 0 0 1px var(--border)` | 2 |
| `shadow-sm` (v4 기본) | `0 1px 3px rgb(0 0 0/.1), 0 1px 2px -1px rgb(0 0 0/.1)` | 13 |
| `shadow-xs` (v4 기본) | `0 1px 2px 0 rgb(0 0 0/.05)` | 4 |
| `shadow-md`, `shadow-lg`, `shadow-xl` | v4 기본값 | 2 / 2 / 1 |

> **Flutter 지침**: `shadow-card` 계열의 1px 링은 그림자가 아니라 **`Border.all(color: Color(0xFFE8E6E1), width: 1)`** 로 구현한다. hover/active 의 블러 성분만 `BoxShadow` 로 옮긴다(모바일에는 hover 가 없으므로 `shadow-card-hover` 는 탭 눌림 상태나 생략으로 대체한다).

#### 8.3.4 테두리

- 전역 기본 테두리 색은 `--border`(`#E8E6E1`)이다 (`index.css:93-95`).
- 두께는 예외 없이 **1px**. 단 복기 화면의 체크 표식만 `border-2`(2px).
- 리스트 행 구분선은 `border-b border-border/30`, 섹션 구분선은 `border-b border-border/40` 이다.

#### 8.3.5 스크롤바 (`index.css:152-166`)

폭/높이 6px, 트랙 투명, 썸 `#D5D3CE`(hover `#B5B3AE`), 썸 반경 3px. Flutter 에서는 `Scrollbar(thickness: 6, radius: Radius.circular(3), thumbVisibility: false)` 로 대응하거나, 모바일 관례상 생략한다.

---

### 8.4 애니메이션

#### 8.4.1 페이지 진입 애니메이션 (`index.css:112-141`)

```
@keyframes fade-in-up {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}
```

| 클래스 | 지속시간 | 이징 | 지연 |
|---|---|---|---|
| `.animate-enter` | 0.5s | `cubic-bezier(0.22, 1, 0.36, 1)` | 0ms |
| `.animate-enter-delay-1` | 0.5s | 동일 | 80ms |
| `.animate-enter-delay-2` | 0.5s | 동일 | 160ms |
| `.animate-enter-delay-3` | 0.5s | 동일 | 240ms |
| `.animate-enter-delay-4` | 0.5s | 동일 | 320ms |

`fill-mode: both` 이므로 지연 구간 동안 시작 프레임(`opacity: 0`)이 유지된다. 7개 페이지의 헤더 섹션이 `animate-enter` 를, 하위 블록이 순차적으로 `-delay-1~3` 을 사용한다.

> **Flutter 지침**: `cubic-bezier(0.22, 1, 0.36, 1)` 는 `const Cubic(0.22, 1.0, 0.36, 1.0)` 으로 정확히 재현된다. `AnimationController(duration: Duration(milliseconds: 500))` 에 `Interval(delayFraction, 1.0, curve: Cubic(...))` 를 걸거나, 지연별 컨트롤러를 두고 `FadeTransition` + `Transform.translate` 를 조합한다. 12px 는 논리 픽셀 오프셋이므로 `Tween<Offset>` 대신 `Tween<double>` + `Transform.translate(offset: Offset(0, v))` 가 정확하다.

#### 8.4.2 가격 플래시 효과

§3.3.3 을 참조한다. 요약: `tickedAt` 변화로 트리거, 100ms 즉시 on/off(페이드 없음), 테두리만 변경.

#### 8.4.3 카드 hover (`index.css:144-150`)

`.card-interactive`: `transition: box-shadow 0.2s ease, transform 0.2s ease`. hover 시 `box-shadow: var(--shadow-card-hover)` + `transform: translateY(-1px)`.

> 모바일에는 hover 가 없다. Flutter 에서는 `InkWell` 의 탭 리플/하이라이트(`highlightColor: primary 3%`)로 대체하고 `translateY` 는 생략한다.

#### 8.4.4 그 외 (tw-animate-css 기반)

| 위치 | 효과 | 근거 |
|---|---|---|
| Dialog 오버레이 | `bg-black/50` + fade in/out | `components/ui/dialog.tsx:40` |
| Dialog 콘텐츠 | fade + zoom `95% → 100%`, **200ms** | `dialog.tsx:62` |
| 바텀시트(입출금) | 아래에서 슬라이드 인, **300ms**, `rounded-t-2xl`(18px), `bg-card`, `shadow-card-active`, `max-h-[85vh]` | `WalletPage.tsx:257` |
| Select 콘텐츠 | fade + zoom 95%, 방향별 `slide-in-from-*-2`(8px) | `components/ui/select.tsx:65` |
| 로딩 스피너 | `Loader2` 아이콘 + `animate-spin` (1s linear infinite), 24×24 | `SocialCallbackPage.tsx:67,107` |
| 행/버튼 색 전이 | `transition-colors` (Tailwind 기본 150ms ease) | 다수 |

---

### 8.5 formatters.ts → Dart 이식 규칙

원본: `frontend/src/lib/formatters.ts` (전체 118줄). **모든 페이지가 이 파일의 함수만 사용한다.**

#### 8.5.0 공통 전제

- JS `toLocaleString("ko-KR")` 과 `toLocaleString("en-US")` 는 이 프로젝트가 쓰는 숫자 범위에서 **천단위 구분자 `,`, 소수점 `.` 로 동일**하다. 따라서 Dart 에서는 로케일을 구분할 필요 없이 `NumberFormat` 패턴만 맞추면 된다.
- **옵션을 주지 않은 `toLocaleString`** 의 기본값은 `minimumFractionDigits: 0, maximumFractionDigits: 3` 이다. → Dart 패턴 `#,##0.###`
- JS `Math.round` 는 half-up(+∞ 방향)이나, 이 파일은 **항상 `Math.abs()` 를 거친 양수에만** 적용하므로 Dart 의 `.round()` 와 결과가 완전히 일치한다.
- JS `toFixed(n)` ↔ Dart `toStringAsFixed(n)` 은 동일 동작이다.

권장 Dart 포매터 상수:

```dart
final _grp    = NumberFormat('#,##0.###');      // 기본: 소수 0~3
final _int0   = NumberFormat('#,##0');          // 소수 0
final _fix2   = NumberFormat('#,##0.00');       // 소수 정확히 2
final _fix4   = NumberFormat('#,##0.0000');     // 소수 정확히 4
final _f2to4  = NumberFormat('#,##0.00##');     // 소수 2~4
final _f4to8  = NumberFormat('#,##0.0000####'); // 소수 4~8
```

#### 8.5.1 `formatKRW(value)` — 원화, 단위 포함 (카드·요약용) `formatters.ts:8-19`

| 조건 (`abs = value.abs()`) | 규칙 |
|---|---|
| `abs >= 1억` | `억 = (abs / 1e8).floor()`, `만 = ((abs % 1e8) / 1e4).round()`. `만 > 0` 이면 `"{부호}{억}억 {만:천단위}만원"`, `만 == 0` 이면 `"{부호}{억}억원"` |
| `abs >= 1만` | `"{부호}{(abs/1e4).round():천단위}만원"` |
| 그 외 | `"{부호}{abs.round():천단위}원"` |

부호는 `value < 0` 일 때 `"-"`, 아니면 `""`.

| 입력 | 출력 |
|---|---|
| `0` | `0원` |
| `1234` | `1,234원` |
| `9999` | `9,999원` |
| `10000` | `1만원` |
| `15000` | `2만원` ← `1.5` 를 반올림하므로 2 |
| `12345678` | `1,235만원` |
| `99999999` | `10,000만원` ← 1억 미만이라 만원 분기 |
| `100000000` | `1억원` |
| `123456789` | `1억 2,346만원` |
| `999999999` | `9억 10,000만원` ← 만 자리가 10,000 으로 반올림되는 실제 동작. **그대로 재현할 것** |
| `1000000000` | `10억원` |
| `-50000` | `-5만원` |

#### 8.5.2 `formatKRWCompact(value)` — 원화, 단위 없음 (테이블용) `formatters.ts:23-34`

`formatKRW` 와 동일하되 접미사가 `억원`/`만원` → `억`/`만` 이고, **1만 미만 분기만 다르다**: `value.toLocaleString("ko-KR")` 즉 **부호 있는 원본 값을 반올림 없이** 소수 0~3자리로 출력한다.

| 입력 | 출력 |
|---|---|
| `0` | `0` |
| `1234` | `1,234` |
| `1234.5678` | `1,234.568` ← 소수 3자리까지 |
| `-1234` | `-1,234` |
| `10000` | `1만` |
| `123456789` | `1억 2,346만` |
| `100000000` | `1억` |

#### 8.5.3 `formatCurrency(value, baseCurrency)` — 통화별, 단위 포함 `formatters.ts:38-43`

- `baseCurrency == "USDT"` → `"$" + value.toLocaleString("en-US", {min:2, max:2})` — **소수 정확히 2자리**
- **그 외 전부**(`KRW`, `SOL` 포함) → `formatKRW(value)`

| 입력 | 출력 |
|---|---|
| `(1234.5, "USDT")` | `$1,234.50` |
| `(0, "USDT")` | `$0.00` |
| `(-12.345, "USDT")` | `$-12.35` ← **달러 기호 뒤에 마이너스**가 온다. 템플릿이 `` `$${...}` `` 이기 때문. 그대로 재현할 것 |
| `(123456789, "KRW")` | `1억 2,346만원` |
| `(123456789, "SOL")` | `1억 2,346만원` ← SOL 도 원화 분기로 떨어진다 |

#### 8.5.4 `formatCurrencyCompact(value, baseCurrency)` `formatters.ts:47-52`

`USDT` 이면 8.5.3 과 동일(`$` + 소수 2자리), 그 외에는 `formatKRWCompact(value)`.

#### 8.5.5 `formatFiatEstimate(value, baseCurrency)` `formatters.ts:56-58`

`"≈ " + formatCurrency(value, baseCurrency)` — `≈`(U+2248) 다음에 **공백 하나**.

| 입력 | 출력 |
|---|---|
| `(1234.5, "USDT")` | `≈ $1,234.50` |
| `(52340000, "KRW")` | `≈ 5,234만원` |

#### 8.5.6 `formatQuantity(quantity)` — 코인 수량 `formatters.ts:62-67`

로케일 `en-US`. 조건은 **원본 값**(절댓값 아님) 기준이다.

| 조건 | 소수 자릿수 | Dart 패턴 |
|---|---|---|
| `q >= 1,000,000` | 정확히 0 | `#,##0` |
| `q >= 1,000` | 정확히 2 | `#,##0.00` |
| `q >= 1` | 정확히 4 | `#,##0.0000` |
| 그 외 (0 이하 및 소수) | 최소 4, 최대 8 | `#,##0.0000####` |

| 입력 | 출력 |
|---|---|
| `1234567.89` | `1,234,568` |
| `1234.5` | `1,234.50` |
| `1.5` | `1.5000` |
| `12.345678` | `12.3457` |
| `0.5` | `0.5000` |
| `0.000012345678` | `0.00001235` |
| `0` | `0.0000` |
| `-5` | `-5.0000` ← 음수는 마지막 분기로 떨어진다 |

#### 8.5.7 `formatPrice(price, baseCurrency)` — 가격 (**통화 기호 미포함**) `formatters.ts:71-83`

| baseCurrency | 조건 | 규칙 |
|---|---|---|
| `SOL` | `p >= 1` | en-US, 소수 정확히 4 |
| `SOL` | `p >= 0.0001` | en-US, 소수 4~8 |
| `SOL` | 그 외 | `p.toExponential(2)` — 예: `0.00001` → `1.00e-5` |
| `USDT` | `p >= 100` | en-US, 소수 정확히 2 |
| `USDT` | `p >= 1` | en-US, 소수 2~4 |
| `USDT` | 그 외 | en-US, 소수 정확히 4 |
| 그 외(`KRW`) | — | ko-KR 기본 (소수 0~3) |

| 입력 | 출력 |
|---|---|
| `(84500000, "KRW")` | `84,500,000` |
| `(1234.5678, "KRW")` | `1,234.568` |
| `(0.5, "KRW")` | `0.5` |
| `(1234.5, "USDT")` | `1,234.50` |
| `(1.5, "USDT")` | `1.50` |
| `(1.23456, "USDT")` | `1.2346` |
| `(0.5, "USDT")` | `0.5000` |
| `(2.5, "SOL")` | `2.5000` |
| `(0.00001, "SOL")` | `1.00e-5` |

> **주의**: `formatPrice` 는 기호를 붙이지 않는다. 호출부에서 `getCurrencySymbol(baseCurrency)` 를 앞에 이어 붙인다. USDT 는 `getCurrencySymbol` 이 빈 문자열을 반환하므로 **코인 목록의 USDT 가격에는 `$` 가 붙지 않는다.**
>
> Dart `toExponential(2)` 대응: `p.toStringAsExponential(2)` 는 `1.00e-5` 를 반환하여 JS 와 동일하다.

#### 8.5.8 `formatVolume(volume, baseCurrency)` — 거래대금 `formatters.ts:87-95`

억/만 축약을 **하지 않는다**. 소수는 전부 기본값(0~3자리).

| baseCurrency | 규칙 |
|---|---|
| `SOL` | `"◎"(U+25CE) + en-US 천단위` |
| `USDT` | `"$" + en-US 천단위` |
| 그 외(`KRW`) | ko-KR 천단위, **기호 없음** |

| 입력 | 출력 |
|---|---|
| `(123456789, "KRW")` | `123,456,789` |
| `(1234567.891, "USDT")` | `$1,234,567.891` |
| `(1234.5, "SOL")` | `◎1,234.5` |

#### 8.5.9 `formatChangeRate(rate)` — 변동률 `formatters.ts:99-103`

입력은 **비율**(0.0234 = 2.34%)이다.

```
percent = rate * 100
sign    = percent > 0 ? "+" : ""      // 0 이면 부호 없음
return "${sign}${percent.toStringAsFixed(2)}%"
```

| 입력 | 출력 |
|---|---|
| `0.0234` | `+2.34%` |
| `-0.0125` | `-1.25%` |
| `0` | `0.00%` ← `+` 가 붙지 않는다 |
| `-0.00001` | `-0.00%` ← 실제 동작. 그대로 재현할 것 |
| `1.5` | `+150.00%` |

> 천단위 구분자가 없다(`toFixed` 사용). `+1234.56%` 처럼 나온다.

#### 8.5.10 `getCurrencySymbol(baseCurrency)` `formatters.ts:107-111`

| 입력 | 출력 |
|---|---|
| `"KRW"` | `₩` (U+20A9) |
| `"SOL"` | `◎` (U+25CE) |
| **그 외 전부(`USDT` 포함)** | `""` (빈 문자열) |

#### 8.5.11 `SMALL_AMOUNT_THRESHOLD` `formatters.ts:115-118`

소액 자산 필터 기준값. `{ KRW: 1000, USDT: 1 }`. 조회 시 **키가 없으면 기본값 1** 을 쓴다.

---

### 8.6 Flutter ThemeData 매핑안

#### 8.6.1 ColorScheme (Material 3, light 전용)

```dart
const _scheme = ColorScheme(
  brightness: Brightness.light,
  primary:                 Color(0xFF6C5CE7),  // --primary
  onPrimary:               Color(0xFFFFFFFF),  // --primary-foreground
  secondary:               Color(0xFFF0EEEB),  // --secondary
  onSecondary:             Color(0xFF1A1A2E),  // --secondary-foreground
  error:                   Color(0xFFE85D75),  // --destructive
  onError:                 Color(0xFFFFFFFF),
  surface:                 Color(0xFFFFFFFF),  // --card / --popover
  onSurface:               Color(0xFF1A1A2E),  // --card-foreground / --foreground
  surfaceContainerLowest:  Color(0xFFFFFFFF),  // --card
  surfaceContainerLow:     Color(0xFFF8F7F4),  // --background
  surfaceContainer:        Color(0xFFF0EEEB),  // --muted / --accent / --input
  surfaceContainerHigh:    Color(0xFFF0EEEB),
  surfaceContainerHighest: Color(0xFFF0EEEB),
  onSurfaceVariant:        Color(0xFF7C7C8A),  // --muted-foreground
  outline:                 Color(0xFFE8E6E1),  // --border
  outlineVariant:          Color(0xFFE8E6E1),  // --border
);
```

| 웹 토큰 | Material 3 슬롯 | 값 | 비고 |
|---|---|---|---|
| `--background` | `ThemeData.scaffoldBackgroundColor` (+ `surfaceContainerLow`) | `#F8F7F4` | M3 ColorScheme 에는 `background` 슬롯이 없으므로 ThemeData 레벨에서 지정 |
| `--foreground` | `onSurface` | `#1A1A2E` | |
| `--card`, `--popover` | `surface`, `surfaceContainerLowest` | `#FFFFFF` | `CardTheme.color`, `DialogTheme.backgroundColor` 도 동일 |
| `--primary`, `--ring` | `primary` / `focusColor` | `#6C5CE7` | 두 토큰 값이 동일 |
| `--secondary`, `--muted`, `--accent`, `--input` | `secondary`, `surfaceContainer` | `#F0EEEB` | 네 토큰 값이 전부 동일 |
| `--muted-foreground` | `onSurfaceVariant` | `#7C7C8A` | |
| `--destructive` | `error` | `#E85D75` | `--negative` 와 값이 같음 |
| `--border` | `outline`, `outlineVariant`, `DividerThemeData.color` | `#E8E6E1` | |
| `--positive` / `--negative` / `--warning` / `--chart-1~5` | **대응 슬롯 없음** → ThemeExtension | | |
| `--sidebar-*` (8종) | **이식 제외** | — | 사용처 0건 |

#### 8.6.2 ThemeExtension (M3 슬롯이 없는 토큰)

```dart
@immutable
class TryptoColors extends ThemeExtension<TryptoColors> {
  final Color positive;       // #2ECC87 — 상승/이익
  final Color negative;       // #E85D75 — 하락/손실
  final Color warning;        // #F0A030 — 대기/경고
  final Color chart1;         // #6C5CE7
  final Color chart2;         // #0ABFBC
  final Color chart3;         // #E85D75
  final Color chart4;         // #F0A030
  final Color chart5;         // #2ECC87
  final Color flashNeutral;   // #7C7C8A @ 40% — same 플래시 테두리
  final Color scrollbarThumb; // #D5D3CE
  // copyWith / lerp 구현 생략
}
```

#### 8.6.3 TextTheme

폰트는 **Pretendard(본문·기본)** 와 **Noto Sans KR(제목)** 을 앱 에셋으로 번들하고, 수치용 **모노스페이스 패밀리 1종**(예: Roboto Mono)을 추가 번들한다. 웹은 CDN 을 쓰지만 모바일 앱은 오프라인 동작을 위해 번들이 필요하다.

```dart
// ThemeData
fontFamily: 'Pretendard',
// 웹 body 의 font-feature-settings: "cv02","cv03","cv04","cv11" 재현
// → TextStyle 에 fontFeatures: [FontFeature('cv02'), FontFeature('cv03'),
//                              FontFeature('cv04'), FontFeature('cv11')]
```

| 웹 조합 (사용처) | M3 TextTheme 슬롯 | fontFamily | fontSize | height | fontWeight | letterSpacing |
|---|---|---|---|---|---|---|
| `text-4xl` (강조 수치) | `displaySmall` | Pretendard | 36 | 1.1111 | 700 | -0.36 |
| `font-display text-3xl tracking-tight` (**7개 페이지 h1**) | `headlineMedium` | **Noto Sans KR** | 30 | 1.2 | **800** | **-0.75** |
| `text-2xl` (섹션 제목·큰 수치) | `headlineSmall` | Pretendard | 24 | 1.3333 | 700 | -0.24 |
| `text-lg` (다이얼로그 제목·카드 제목) | `titleLarge` | Pretendard | 18 | 1.5556 | 600 | -0.18 |
| `text-base` | `titleMedium` | Pretendard | 16 | 1.5 | 600 | -0.16 |
| `text-sm` (**최다 사용, 기본 본문**) | `bodyMedium` | Pretendard | 14 | 1.4286 | 400 | -0.14 |
| `text-sm font-medium` (버튼·탭·라벨) | `labelLarge` | Pretendard | 14 | 1.4286 | 500 | -0.14 |
| `text-[13px]` (코인 심볼 등) | `bodySmall` | Pretendard | 13 | 1.4 | 600 | -0.13 (심볼은 `tracking-wide` → +0.325) |
| `text-xs` (배지·보조 라벨) | `labelMedium` | Pretendard | 12 | 1.3333 | 500 | -0.12 |
| `text-[11px]` (코인 한글명·미세 라벨) | `labelSmall` | Pretendard | 11 | 1.36 | 400 | -0.11 |
| `text-[10px]` (초소형 배지) | (커스텀) | Pretendard | 10 | 1.4 | 500 | -0.10 |
| `font-mono tabular-nums` (**모든 수치**) | (커스텀 `numeric` 스타일) | **RobotoMono** | 호출처 크기 | — | 500~700 | 0 |

**수치 스타일 규칙** — 웹은 `font-mono` + `tabular-nums` 를 항상 함께 쓰므로, Flutter 에서도 전용 헬퍼를 둔다.

```dart
TextStyle numeric({double size = 14, FontWeight weight = FontWeight.w600, Color? color}) =>
  TextStyle(
    fontFamily: 'RobotoMono',
    fontSize: size,
    fontWeight: weight,
    color: color,
    fontFeatures: const [FontFeature.tabularFigures()],
    letterSpacing: 0,
  );
```

#### 8.6.4 컴포넌트 테마 매핑

| 웹 컴포넌트 | 근거 | Flutter 매핑 |
|---|---|---|
| Card (앱 실사용형) | `CoinTable.tsx:144` | `Card(elevation: 0, color: Color(0xFFFFFFFF), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14), side: BorderSide(color: Color(0xFFE8E6E1))), clipBehavior: Clip.antiAlias)` |
| Card (shadcn 원형) | `components/ui/card.tsx:10` | `rounded-xl`(14) + `py-6`(상하 24) + `shadow-sm` + 내부 `gap-6`(24). 헤더/콘텐츠/푸터 좌우 패딩 `px-6`(24) |
| Button `default` | `button.tsx:12,24` | `FilledButton`: 높이 36, 좌우 16, 반경 8, `bg #6C5CE7`, `fg #FFFFFF`, 텍스트 14/w500, 아이콘 16, 아이콘-텍스트 간격 8, `disabled` 시 불투명도 0.5 |
| Button `secondary` | `button.tsx:17-18` | `bg #F0EEEB`, `fg #1A1A2E` |
| Button `outline` | `button.tsx:15-16` | `bg #F8F7F4`, `border 1px #E8E6E1`, `shadow-xs` |
| Button `ghost` | `button.tsx:19-20` | 투명 배경, 눌림 시 `#F0EEEB` |
| Button `destructive` | `button.tsx:13-14` | `bg #E85D75`, `fg #FFFFFF` |
| Button `link` | `button.tsx:21` | `fg #6C5CE7`, 눌림 시 밑줄 |
| Button 크기 `xs/sm/lg/icon` | `button.tsx:25-31` | 높이 24 / 32 / 40, 아이콘 버튼 정사각 24·32·36·40. `xs` 는 텍스트 12, 아이콘 12 |
| Badge | `badge.tsx:8` | 반경 `StadiumBorder`, 좌우 8·상하 2, 텍스트 12/w500, 테두리 1px 투명, 아이콘 12, 간격 4 |
| Input | `input.tsx:11-12` | `TextField`: 높이 36, 좌우 12, 반경 8, `enabledBorder 1px #F0EEEB`, `focusedBorder 1px #6C5CE7`, 배경 투명, **모바일 텍스트 16**(`text-base`, `md:text-sm` 는 데스크톱 전용), 힌트색 `#7C7C8A`. 포커스 링(3px, primary 50%)은 Flutter 에 대응 개념이 없으므로 테두리 색 변경으로 대체 |
| Tabs (`default`) | `tabs.tsx:29,33,67,69` | 트랙: `bg #F0EEEB`, 반경 10, 내부 패딩 3, 높이 36. 탭: 반경 8, 텍스트 14/w500, 비활성 `#1A1A2E` 60%, 활성 `bg #F8F7F4` + `fg #1A1A2E` + `shadow-sm` |
| Tabs (`line`) | `tabs.tsx:34,70` | 트랙 투명, 탭 간격 4, 활성 시 하단 2px 밑줄(`#1A1A2E`), 밑줄 위치 하단 -5px |
| Table | `table.tsx:13,58,71,84` | 텍스트 14. 헤더 셀 높이 40 / 패딩 8 / w500 / 좌측 정렬. 본문 셀 패딩 8. 행 하단 1px 구분선, 마지막 행 제외. 행 hover `#F0EEEB` 50% |
| Dialog | `dialog.tsx:40,62,71,126` | 배리어 `#000000` 50%. 콘텐츠: 반경 10, 패딩 24, 자식 간격 16, 테두리 1px, `shadow-lg`, 최대 폭 512, 화면 폭 대비 좌우 최소 16 여백. 닫기 버튼: 우상단 16/16, 아이콘 16, 불투명도 0.7. 제목 18/w600, 설명 14/`#7C7C8A` |
| Select | `select.tsx:40,65,112` | 트리거: 높이 36(`sm` 32), 좌우 12·상하 8, 반경 8, 테두리 1px `#F0EEEB`, 텍스트 14, 우측 셰브론 16px/불투명도 0.5. 드롭다운: `bg #FFFFFF`, 반경 8, 테두리 1px, `shadow-md`, 내부 패딩 4. 항목: 상하 6·좌 8·우 32, 반경 6, 텍스트 14, 선택 표식(체크 16px)은 우측 8 |
| Slider | `slider.tsx:42,48,56` | `SliderTheme`: 트랙 높이 6, 반경 full, 비활성 트랙 `#F0EEEB`, 활성 트랙 `#6C5CE7`. 썸 지름 16, `bg #FFFFFF`, 테두리 1px `#6C5CE7`, `shadow-sm`, 오버레이 반경 = 썸 + 4 |
| Switch | `switch.tsx:18,26` | 트랙 32×18.4(`sm` 24×14), 반경 full. 켜짐 `#6C5CE7`, 꺼짐 `#F0EEEB`. 썸 지름 16(`sm` 12), 색 `#F8F7F4`, 이동 거리 = 트랙폭 − 썸폭 − 2 |
| Separator | `separator.tsx:20` | 두께 1, 색 `#E8E6E1`. `Divider(height: 1, thickness: 1)` / `VerticalDivider` |
| Label | `label.tsx:14` | 텍스트 14/w500, 아이콘과 간격 8, 비활성 시 불투명도 0.5 |
| SortIcon | `SortIcon.tsx:11-14` | 12×12. 비활성: 양방향 화살표 아이콘, 불투명도 0.30. 활성: 위/아래 화살표, 색 `#6C5CE7` |
| AppBar | `Header.tsx:25` | 높이 56, `bg` 배경색, 하단 1px `#E8E6E1` 구분선, 좌우 패딩 16 |

#### 8.6.5 아이콘

웹은 `lucide-react` 를 쓴다(`components.json:13`). 기본 크기는 16px, 배지·소형 버튼 안에서는 12px 이다. Flutter 에서는 `lucide_icons` 패키지를 사용해 **동일한 아이콘 이름**으로 매핑하고, 기본 `IconThemeData(size: 16, color: Color(0xFF7C7C8A))` 를 둔다.

---

## 9. 부록 — 이식 착수 전 결정 사항 요약

| # | 결정 항목 | 선택지 | 권장 |
|---|---|---|---|
| 1 | OAuth redirect URI 방식 | 방안 A(App Links, 백엔드 무수정) / 방안 B(커스텀 스킴, 백엔드 수정) | **방안 A**. 단 iOS 최소 버전 17.4 수용 가능 여부를 먼저 확정한다 |
| 2 | 최소 지원 iOS 버전 | 17.4 이상 / 그 미만 | 방안 A 를 택하면 **17.4 이상**이 강제된다 |
| 3 | 세션 유지 방식 | 세션 ID 직접 보관 / `PersistCookieJar` | **세션 ID 직접 보관**(7일 절대 만료 회피) |
| 4 | 빌드 타깃 | 네이티브만 / Flutter Web 포함 | **네이티브만**. Web 을 포함하면 백엔드 CORS 추가가 필수가 된다 |
| 5 | 실시간 체결 푸시 | REST 재조회 대체 / 서버 수정 후 STOMP | **REST 재조회 대체**(웹도 미동작이므로 기능 손실 없음) |
| 6 | 회원 탈퇴 기능 | 포함 / 제외 | iOS 배포 시 **포함 필수**, Android 전용이면 선택 |
| 7 | 거래소 목록 | 앱 상수 하드코딩 / 서버 API 신설 | **앱 상수 하드코딩**(웹과 동일) |
| 8 | 프론트 별칭 enum | 서버 이름 그대로 / 프론트 별칭 유지 | **서버 이름 그대로**(`LOSS_CUT` 등) |
| 9 | 다크 모드 | 지원 / 미지원 | **미지원**(웹에 다크 토큰이 없다) |
| 10 | 라운드 시드 배분 UI | 업비트 전액(웹과 동일) / 거래소별 배분 | 웹과 동일하게 시작. 서버는 이미 배분을 지원하므로 후속 확장 가능 |
