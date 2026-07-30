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
  --dart-define=KAKAO_NATIVE_APP_KEY=... \
  --dart-define=GOOGLE_SERVER_CLIENT_ID=...
```

| 키 | 개발(에뮬레이터) | 운영 |
|---|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8080` | `https://{도메인}` |
| `WS_BASE_URL` | `ws://10.0.2.2:8080/ws` | `wss://{도메인}/ws` |
| `KAKAO_NATIVE_APP_KEY` | 카카오 네이티브 앱 키 (기본값 있음) | 동일 |
| `GOOGLE_SERVER_CLIENT_ID` | 구글 웹 클라이언트 ID (기본값 있음) | 동일 |

두 제공자 모두 공식 SDK 로 앱에서 직접 토큰을 받으므로 클라이언트 ID·콜백 스킴을 주입하지 않는다.
카카오 네이티브 앱 키를 바꾸면 `AndroidManifest.xml` 과 `ios/Runner/Info.plist` 의
`kakao{네이티브앱키}` 스킴도 함께 고쳐야 한다.

로컬 백엔드는 `SESSION_COOKIE_SECURE=false` 여야 세션 쿠키가 저장·전송된다.

## 검증

```bash
flutter analyze          # 에러 0 이 커밋 조건이다
flutter test             # 순수 로직 · 인터셉터 계약 · 티커 성능 계약 · 위젯 5종
flutter build apk --debug
```

## 릴리스 빌드

**운영 배포는 반드시 아래 한 줄로 한다.** `--dart-define-from-file` 을 빠뜨리면 `Env` 의 기본값인
`http://10.0.2.2:8080`(에뮬레이터 전용 주소)이 그대로 박혀 실기기에서 서버에 붙지 못한다.

```bash
flutter build apk --release --dart-define-from-file=env/prod.json
```

산출물은 `build/app/outputs/flutter-apk/app-release.apk` 이며, 랜딩의 다운로드 버튼이 가리키는
GitHub 릴리스에 `trypto-android.apk` 로 올린다.

### 서명

`android/key.properties.example` 을 `android/key.properties` 로 복사해 채운다. 이 파일과
키스토어는 `.gitignore` 에 있어 저장소에 들어가지 않는다. 없으면 릴리스 빌드가 실패한다 —
디버그 키로 조용히 서명되지 않도록 일부러 막아 두었다.

**키스토어를 잃으면 이미 설치된 앱에 업데이트를 올릴 수 없다.** APK 를 직접 배포하므로 Play 앱
서명 같은 복구 수단이 없다. 키 파일과 비밀번호를 서로 다른 곳에 백업한다.

서명 키를 바꾸면 제공자 콘솔에 등록한 지문도 함께 바꿔야 로그인이 동작한다.

```bash
# 구글 콘솔에 등록할 SHA-1
keytool -list -v -keystore <키스토어> -alias trypto

# 카카오 콘솔에 등록할 키 해시
keytool -exportcert -alias trypto -keystore <키스토어> | openssl sha1 -binary | openssl base64
```

### 배포 전 확인

APK 에 박힌 서버 주소는 빌드 산출물에서 직접 확인할 수 있다.

```bash
unzip -o -q -j build/app/outputs/flutter-apk/app-release.apk lib/arm64-v8a/libapp.so -d /tmp/apkchk
grep -a -o -E '(https?|wss?)://[a-zA-Z0-9._:/-]{3,60}' /tmp/apkchk/libapp.so | sort -u
```

`10.0.2.2` 가 보이면 운영값 주입에 실패한 것이다.

## 구조

```
lib/
  core/      env · api(Dio + 인터셉터 3종) · auth(설정·세션) · realtime(STOMP·TickerStore)
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
