## 소셜 신원 표현

- `kakaoId` 처럼 제공자별 컬럼을 두지 않고 `(provider, provider_id)` 한 쌍으로 표현한다. null 이 없고, 제공자가 늘어도 컬럼이 늘지 않는다.
- 소셜 신원 값은 `social_account` 테이블에 존재하고, `user` 는 이를 ID 로 참조한다.
- 테이블 이름은 애그리거트 `SocialAccount` 와 일치시킨다. 소셜 신원(식별 값)과 소셜 계정(연결 상태까지 포함한 레코드)은 다른 개념이며, 이 테이블이 저장하는 것은 후자다.

`social_account` 테이블
소셜 계정 하나를 표현하는 영구 레코드다. 지금 이 계정으로 로그인하는 회원이 누구인지 `user_id` 로 가리키며, 이 컬럼은 회원 가입 시 채워지고 탈퇴 시 비워진다.

| 컬럼 | 제약 | 의미 |
|---|---|---|
| id | PK | 대리키. user 가 이 값으로 참조한다 |
| provider, provider_id | 유니크 | 소셜 계정. 이 값이 존재하는 곳은 이 테이블뿐이다 |
| user_id | nullable · 유니크 | 현재 이 계정으로 로그인하는 회원. NULL 이면 현재 주인이 없다(탈퇴) |
| created_at | | |

`user` 테이블
자신을 만든 소셜 신원을 ID 로 참조한다.

| 컬럼 | 제약 | 의미 |
|---|---|---|
| social_account_id | NOT NULL  | 이 회원을 만든 소셜 계정. 불변 기록으로 탈퇴 후에도 남아 재가입 제한 판정에 쓰인다 |

- 로그인의 기존 회원 찾기는 `social_account` 를 조회한다. `user_id` 가 채워져 있으면 그 회원으로 로그인한다. 탈퇴한 회원은 연결이 해제되어 있으므로 로그인되지 않는다.
- 제공자는 `Provider` enum(`KAKAO`, `GOOGLE`)으로 표현한다. 제공자가 늘면 enum 상수와 제공자별 연동 클라이언트만 늘어난다.

## User 애그리거트에서 email 제거

- 카카오는 이메일 미동의가 가능하고, 병합을 하지 않기로 해 email 을 식별자로 쓸 일이 없다. 구글도 회원 식별에는 `sub` 만 사용하며 이메일 scope 를 요청하지 않는다.
- 따라서 `User` 도메인 모델·`UserJpaEntity`·`user` 테이블에서 `email` 필드/컬럼과 unique 인덱스를 제거한다.

## 자동 닉네임은 시퀀스 기반으로 생성

신규 회원의 자동 닉네임은 `형용사 + "사용자" + 번호` 형태로 만들고, 번호는 전용 시퀀스 테이블(`nickname_sequence`, AUTO_INCREMENT)에서 발급받는다.

- 무작위 후보를 뽑아 DB 중복 검사로 거르는 방식은 쓰지 않는다. 검사와 저장 사이의 틈 때문에 검사가 유일성을 보증하지 못하고, 조합 공간이 차오를수록 재시도가 늘다가 결국 가입이 실패하는 상한이 생긴다.
- 시퀀스 번호는 겹칠 수 없으므로 중복 검사도, 저장 실패 재시도도 필요 없다. 회원 수 상한도 없다.
- `nickname` unique 제약은 최후 방어로 유지한다. 사용자가 닉네임 변경으로 자동 패턴과 같은 닉네임을 선점한 극단적 경우 해당 가입 시도 1회만 실패하고, 다음 시도는 새 번호를 받아 자동 회복된다.

## 로그인은 쿠키-세션 방식으로 유지

- 제공자 토큰은 로그인 순간에만 쓰고 저장하지 않는다. 서비스 자체 로그인 상태는 서버 세션 + 쿠키로 유지한다.
- Spring Security 는 도입하지 않는다. 세션 정보는 레디스에 담고 인증 판별 인터셉터에서 처리한다.
- 세션 쿠키에는 탈취·오용을 막는 속성을 설정한다.
  - `HttpOnly`: 브라우저의 자바스크립트가 이 쿠키를 읽지 못하게 한다(스크립트를 통한 세션 탈취 방지).
  - `Secure`: HTTPS 통신일 때만 쿠키를 전송한다. 로컬 HTTP 개발에선 꺼야 하므로 운영 환경에서만 켠다.
  - `SameSite=Lax`: 우리 사이트에서 시작된 요청에만 쿠키를 붙인다. 다른 사이트가 사용자의 세션을 몰래 도용하는 CSRF 를 막는다.
- 세션 유효 기간 기본 7일로 설정한다.

## 탈퇴 회원의 잔여 세션 차단
- 회원 한 명이 여러 기기에서 로그인하면 세션 id 가 여러 개 존재할 수 있다.
- 탈퇴하는 순간 모든 기기에서 접속이 불가능해야 한다.
- 다른 기기의 세션을 회원ID로 역조회하는 수단은 두지 않는다. 대신 인증 단계에서 회원이 탈퇴 상태인지 먼저 검사해, 탈퇴 회원은 살아있는 세션으로 접근해도 거부한다.
- 이를 통해 "탈퇴 시 모든 세션 로그아웃" 효과를 역인덱스 없이 만족한다.

## 로그인 흐름

제공자 공식 다이어그램은 서버가 인가를 시작하지만, 우리는 SPA에 맞춰 **프론트가 인가를 시작**한다. `client_secret` 은 여전히 백엔드에만 두고, 토큰 교환도 백엔드가 한다. 인가 코드가 프론트를 잠깐 거치므로 PKCE 로 코드 탈취를 막는다. 이 흐름은 카카오·구글이 동일하며, 제공자별로 인가/토큰/사용자 정보 주소와 응답 형태만 다르다.

1. 프론트가 PKCE 값(`code_verifier` 생성 → 그걸 해시한 `code_challenge`)과 CSRF 방지용 `state` 를 만들어 브라우저 로컬(sessionStorage)에 저장한다.
2. 프론트가 제공자 인가 URL(`client_id`, `redirect_uri`, `response_type=code`, `state`, `code_challenge`, `code_challenge_method=S256`)로 브라우저를 보낸다. 구글은 `scope=openid` 를 추가로 요구한다.
3. 제공자가 사용자 인증·동의 후 `redirect_uri`(프론트 콜백 페이지)로 `?code=&state=` 를 붙여 되돌린다.
4. 프론트 콜백 페이지가 저장해둔 `state` 와 돌아온 `state` 가 같은지 확인한다. 다르면 중단한다.
5. 프론트가 `code` 와 `code_verifier` 를 백엔드 `POST /api/auth/{provider}/login` 으로 보낸다.
6. 백엔드가 `code + code_verifier + client_secret` 으로 토큰을 교환하고, 회원번호 조회 → 회원 확인/등록 → 세션 발급(`Set-Cookie`)을 수행한다.
7. 백엔드가 로그인 결과(JSON)를 반환하고, 프론트가 홈으로 이동한다.

- 사용자 거부 등으로 제공자가 `?error=` 를 실어 오면 프론트가 실패 처리한다.
- 역할 분담: `state` 검증은 프론트, `client_secret` 을 쓰는 토큰 교환은 백엔드. 인가 코드는 일회용이고 `client_secret`·`code_verifier` 없이는 교환할 수 없어, 프론트를 거쳐도 안전하다.
- 프론트가 인가 URL을 만드는 데 필요한 `client_id`·`redirect_uri`·인가 엔드포인트는 제공자별로 프론트 환경설정에 둔다. `redirect_uri` 는 제공자 콘솔 등록값과 백엔드 토큰 교환에서 쓰는 값과 **동일**해야 한다.

## 제공자별 연동

회원 확인/등록·세션 발급 로직은 제공자와 무관하므로 응용 서비스(`LoginService`)는 하나만 둔다. 제공자 선택은 어댑터 계층 안에서 끝낸다.

- 제공자 인증은 연동형 도메인 서비스 `SocialAuthenticator.authenticate(provider, code, codeVerifier)` 로 표현한다. 응용 계층은 이 인터페이스 하나만 안다.
  - 아웃풋 포트(`...QueryPort`)로 두지 않는 이유: 조회 포트는 애그리거트 단위 규칙인데 `SocialIdentity` 는 VO 이고, 행위의 본질도 저장소 조회가 아니라 외부 시스템과의 인증(일회용 인가 코드 소모·토큰 교환)이기 때문이다.
- 구현체 `SocialAuthenticatorImpl`(adapter/out/service)은 `OAuthClient` 인터페이스의 제공자별 구현(`KakaoOAuthClient`, `GoogleOAuthClient`)을 `Map<Provider, OAuthClient>` 로 조립해 두고, 요청의 provider 값으로 위임한다. 제공자가 늘면 `OAuthClient` 구현 하나를 추가한다.
- 제공자별 연동 코드는 `adapter/out/oauth/` 하위에 둔다. 도메인 서비스 이름은 유비쿼터스 언어(인증기), 인프라 클래스 이름은 연동 실체(`~OAuthClient`)가 드러나게 짓는다.

| 항목 | 카카오 | 구글 |
|---|---|---|
| 토큰 교환 | `https://kauth.kakao.com/oauth/token` | `https://oauth2.googleapis.com/token` |
| 회원번호 조회 | `GET https://kapi.kakao.com/v2/user/me` 의 `id` | `GET https://openidconnect.googleapis.com/v1/userinfo` 의 `sub` |
| scope | (기본) | `openid` |

- 구글도 카카오와 동일하게 액세스 토큰으로 사용자 정보 엔드포인트를 호출해 회원번호(`sub`)를 얻는다. `id_token`(JWT) 검증 방식은 서명 검증·키 회전 관리가 추가로 필요하므로 도입하지 않는다. 토큰을 TLS 로 구글에서 직접 받으므로 사용자 정보 조회 방식으로 충분하다.
- 외부 HTTP 호출에는 connect/read 타임아웃을 설정한다(카카오 리뷰 반영 사항과 동일 기준).

### 클라이언트 유형별 자격증명

웹 프론트엔드와 네이티브 모바일 앱은 서로 다른 리다이렉트 주소로 인가를 시작한다. OAuth2 규격상 토큰 교환의 `redirect_uri` 는 인가 요청에 쓴 값과 완전히 같아야 하므로, 서버가 하나의 `redirect_uri` 만 알고 있으면 모바일 로그인은 `redirect_uri_mismatch` 로 실패한다.

- 클라이언트가 `redirectUri` 를 직접 보내면 서버가 임의의 주소로 인가 코드를 넘겨주는 오픈 리다이렉터가 된다. 따라서 클라이언트는 자기 유형(`clientType`)만 보내고, 서버는 미리 등록해 둔 자격증명 묶음 중 하나를 고른다.
- 자격증명은 `clientId` · `clientSecret` · `redirectUri` 세 값을 한 묶음으로 다룬다(`OAuthCredentials`). 구글은 Android/iOS 클라이언트에 웹과 다른 `clientId` 를 발급하고 `clientSecret` 을 발급하지 않으므로, `redirectUri` 하나만 교체하는 방식으로는 동작하지 않는다.
- `clientSecret` 이 비어 있으면 토큰 교환 폼에서 `client_secret` 필드를 아예 제외한다. 빈 문자열을 보내면 제공자가 인증 실패로 처리한다.
- 클라이언트 유형은 `ClientType` enum(`WEB`, `ANDROID`, `IOS`)으로 표현한다. 요청에 값이 없으면 `WEB` 으로 간주하여 기존 웹 프론트엔드와의 하위 호환을 유지한다.
- 안드로이드와 아이폰을 하나의 모바일 유형으로 묶지 않는다. 구글은 Android 클라이언트와 iOS 클라이언트에 서로 다른 `clientId` 를 발급하고, 토큰 교환의 `client_id` 는 인가 요청에 쓴 값과 같아야 하므로 두 운영체제가 자격증명을 공유하면 한쪽은 반드시 실패한다.
- 요청한 유형의 자격증명이 설정되지 않았으면 `SOCIAL_LOGIN_NOT_CONFIGURED` 로 응답한다. 자격증명은 유형별로 독립이므로 안드로이드·아이폰 자격증명이 비어 있어도 웹 로그인은 영향을 받지 않는다.

### 최초 로그인 동시성 문제

같은 소셜 계정으로 최초 로그인이 동시에 두 번 들어오면 회원이 중복 생성될 수 있다.

- `social_account` 의 (provider, provider_id) 유니크 제약이 같은 소셜 계정의 행을 하나로 강제한다.
- 처리: 소셜 신원 조회 → 없으면 회원 + 소셜 신원 생성 시도 → 유니크 위반이면 방금 다른 요청이 만든 것이므로 다시 조회해 그 회원으로 로그인한다.
- 탈퇴 후 재가입 경합은 비어 있는 연결(user_id)을 "비어 있을 때만" 채우는 조건부 갱신으로 차단한다.

## API 명세

인가 시작·콜백 수신은 프론트가 하므로 백엔드는 로그인 엔드포인트 하나만 둔다.

### `POST /api/auth/{provider}/login`

Path Variable

| 이름 | 설명 |
|------|------|
| provider | 소셜 제공자. `kakao` 또는 `google` (대소문자 구분 없음) |

Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | String | O | 제공자 인가 코드 |
| codeVerifier | String | O | PKCE 검증값(code_verifier) |
| clientType | String | X | 클라이언트 유형. `web` · `android` · `ios` 중 하나 (대소문자 구분 없음). 생략하면 `web` 으로 간주한다 |

- 성공: `200` + `Set-Cookie: SESSION=...` + 아래 바디

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "로그인되었습니다.",
  "data": { "userId": 7, "nickname": "구불한사용자1234", "newUser": true }
}
```

- 실패: 아래 에러 응답

## 에러 응답

| code | status | 설명 |
|------|--------|------|
| INVALID_PROVIDER | 400 | 지원하지 않는 제공자 |
| INVALID_CLIENT_TYPE | 400 | 지원하지 않는 클라이언트 유형 |
| SOCIAL_LOGIN_FAILED | 401 | 인가 코드 무효/만료, PKCE 검증 실패 등 인증 실패 |
| SIGNUP_RESTRICTED | 403 | 같은 소셜 신원의 탈퇴 회원이 재가입 제한 기간 내인 경우 가입 거부 |
| SOCIAL_SERVER_ERROR | 502 | 제공자 서버 오류·사용자 정보 조회 실패 |
| SOCIAL_LOGIN_NOT_CONFIGURED | 503 | 요청한 클라이언트 유형의 자격증명이 서버에 설정되지 않음 |

## 범위 밖 작업

- 이 기능은 로그인 → 세션 발급(세션에 `userId` 만 저장)까지만 다룬다.
