# Trypto Mobile

코인 모의투자 플랫폼 trypto 의 Flutter 앱이다. 웹 프론트엔드의 9개 화면을 Android·iOS 네이티브로
이식했다. Flutter Web 은 지원하지 않는다 — 서버에 CORS 설정이 없다.

- 구현 계획서: [docs/plan.md](docs/plan.md) — 아키텍처 결정과 구현 순서. **결정문이다.**
- 사양서: [docs/web-spec.md](docs/web-spec.md) — 웹·백엔드 실측 사양.

## 실행

자격증명과 서버 주소는 전부 `--dart-define` 으로 주입한다. 저장소에 키를 커밋하지 않는다.

```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs   # models/*.g.dart

flutter run \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=WS_BASE_URL=ws://10.0.2.2:8080/ws \
  --dart-define=KAKAO_CLIENT_ID=... \
  --dart-define=GOOGLE_ANDROID_CLIENT_ID=... \
  --dart-define=GOOGLE_IOS_CLIENT_ID=...
```

| 키 | 개발(에뮬레이터) | 운영 |
|---|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8080` | `https://{도메인}` |
| `WS_BASE_URL` | `ws://10.0.2.2:8080/ws` | `wss://{도메인}/ws` |
| `KAKAO_CLIENT_ID` | 카카오 REST API 키 | 동일 |
| `GOOGLE_ANDROID_CLIENT_ID` / `GOOGLE_IOS_CLIENT_ID` | 플랫폼별 OAuth 클라이언트 ID | 동일 |
| `KAKAO_CALLBACK_SCHEME` / `GOOGLE_CALLBACK_SCHEME` | `trypto` (기본값) | 콘솔 정책에 따라 변경 |

로컬 백엔드는 `SESSION_COOKIE_SECURE=false` 여야 세션 쿠키가 저장·전송된다.

## 검증

```bash
flutter analyze          # 에러 0 이 커밋 조건이다
flutter test             # 순수 로직 · 인터셉터 계약 · 티커 성능 계약 · 위젯 5종
flutter build apk --debug
```

## 구조

```
lib/
  core/      env · api(Dio + 인터셉터 3종) · auth(PKCE·세션) · realtime(STOMP·TickerStore)
             format · json · router(가드) · theme · widgets
  models/    서버 DTO 전량 (json_serializable)
  features/  auth · round · market · portfolio · wallet · ranking · regret · mypage
```

이 앱의 뼈대는 셋이다.

1. **티커는 Riverpod 그래프를 통과하지 않는다.** `TickerStore` 가 STOMP 프레임을 심볼별
   `ValueNotifier` 로 접어 **프레임당 1회** flush 한다. 600행 × 초당 수백 틱을 provider 로
   전파하면 selector 비교만 초당 수만 회가 된다.
2. **세션은 `Set-Cookie` 에서 `SESSION` 값만 뽑아** 보안 저장소에 넣고 요청마다 헤더로 붙인다.
   쿠키 자동 관리(`cookie_jar`)를 쓰지 않는다 — 7일 절대 만료를 그대로 물려받기 때문이다.
3. **봉투 언랩과 401 판정은 인터셉터 한 곳에서만 한다.** 화면은 `ApiException` 하나만 안다.

## 남은 작업

`docs/plan.md` §9 의 외부 의존(제공자 콘솔 등록, 백엔드 자격증명 분기)과 폰트 에셋 반입이 남아
있다. 폰트 파일이 없는 동안 `pubspec.yaml` 의 `fonts:` 선언은 주석 처리되어 있고 타이포그래피는
시스템 폰트로 폴백된다.
