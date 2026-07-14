# Trypto Flutter 앱 최종 구현 계획서

본 문서는 설계안 3건(pragmatic / performance / maintainable)과 심사 3건(ticker-perf / auth-realtime / buildability)을 종합한 **확정 계획서**이다. 기준 사양은 `mobile/docs/web-spec.md`(3,434줄)이며, 본 문서와 사양서가 충돌하면 본 문서를 따른다.

기반 채택안은 **pragmatic**이다(심사 2건이 기준안으로 지목). 여기에 performance 안의 `TickerStore` 전량과 401 분리 규칙을, maintainable 안의 구현 순서 분할·시각 컨버터·가드 판별식을 이식하고, ticker-perf 심사가 세 안 모두에서 누락되었다고 지적한 렌더링 규칙 10건을 추가한다.

## 0. 확정 전제

| 항목 | 값 |
|---|---|
| 런타임 | Flutter 3.44 / Dart 3.12 |
| 대상 | Android + iOS 네이티브만. Flutter Web 미지원(서버에 CORS 없음 — R12) |
| 개발 검증 | Windows + Android 에뮬레이터. iOS 는 코드·설정만 준비 |
| OAuth | 방안 B. 로그인 바디에 `clientType: "MOBILE"`, 콜백 `trypto://auth/{provider}/callback` |
| 세션 | `Set-Cookie` 에서 `SESSION` 값만 추출 → 보안 저장소 → 요청마다 `Cookie` 헤더 직접 부착(R2) |
| 실시간 체결 | `/user/queue/events` 미동작(R3). 체결 반영은 REST 재조회로 확정 |
| 테마 | 라이트 고정(`themeMode: ThemeMode.light`) — 다크 토큰이 존재하지 않는다(R13) |
| 화면 | 9개 전량 이식 |
| 우선순위 | 동작하고 읽히는 코드 우선. 단 티커 성능(R5)과 정밀도(R6)는 타협하지 않는다 |

방안 B 채택의 부수 효과로 **iOS 17.4 제약이 소멸한다.** 해당 제약은 `ASWebAuthenticationSession` 의 https 콜백에만 걸리며 커스텀 스킴 콜백에는 적용되지 않는다. 최소 지원 iOS 는 13.0 으로 확정한다.

---

## 1. 결정 요약

| # | 항목 | 결정 | 근거 (한 줄) |
|---|---|---|---|
| 1 | 상태 관리 | **Riverpod(저빈도 전역) + TickerStore/심볼별 ValueNotifier(티커)** — 티커는 Riverpod 그래프를 통과하지 않는다 | 600행 × 초당 수백 틱을 provider 로 전파하면 selector 비교만 초당 수만 회가 된다 |
| 2 | 라우팅 | **go_router 전역 `redirect` 1개(순수 함수) + `StatefulShellRoute.indexedStack` 5탭.** 소셜 콜백은 라우트가 아니다 | `flutter_web_auth_2` 가 콜백 URL 을 `await` 반환값으로 주므로, 스킴을 라우터에 들이면 인텐트가 이중 수신된다 |
| 3 | 네트워킹 | **Dio 1개 + 인터셉터 3개(세션/봉투/401) + 얇은 Repository.** UseCase·DataSource 계층 없음 | 봉투 언랩·세션 헤더·401 은 전역 처리 지점이 하나여야 하고, 서버 DTO 가 이미 화면 모양이다 |
| 4 | 실시간 | **전역 싱글톤 `StompService`(직접 지수 백오프·구독 레지스트리) + `TickerStore`.** 티커 소비자는 **마켓 목록과 캔들 차트 둘**이며 토픽 구독은 **거래소당 하나**다. `TickerStore` 는 **경로가 둘**이다 — ⓐ **그리기: 프레임당 1회 flush**(목록·차트 공통) ⓑ **접기: 매 틱 동기 관찰자 훅**(캔들 고가·저가 보존, §5.4). `paused` 에서 소켓 종료, 마켓 탭 비활성 시 구독 해제 | 내장 재연결은 고정 지연만 지원하고, `indexedStack` 은 숨은 탭의 build·layout 비용을 그대로 청구한다. 봉의 고가·저가는 프레임당 마지막 값만 보면 실제보다 얕아지므로(§4.3.5.2) 접기와 그리기의 빈도를 분리한다 |
| 5 | 모델·직렬화 | **json_serializable 단독**(freezed·riverpod_generator 미도입). DTO 필드는 `double`, **연산 지점에서만 `Decimal` 승격**. 시각 컨버터 3종(`KstDateTime`/`Instant`/`LocalDate`), enum 은 서버 이름 그대로 | `num→.toDouble()` 실수를 구조적으로 봉쇄하되, nullable Decimal 컨버터 지옥과 티커 hot path 의 BigInt 연산을 원천 차단한다 |
| 6 | 차트 | **캔들 = CustomPainter, 복기 = fl_chart, 도넛 = CustomPainter.** 캔들은 **정적 차트가 아니다** — REST 캔들 + STOMP 티커를 합쳐 진행 중인 봉을 실시간으로 갱신한다(§4.3.5, §4.3.6) | fl_chart 의 `CandlestickChart` 는 캔버스 변환 줌이라 §4.3.3 의 "데이터 창 재절단 + y 스케일 재계산" 모델과 다르고, 실시간 봉을 매 프레임 갈아 끼우는 합성 모델도 표현하지 못한다. 복기는 §6.4 가 fl_chart 전제로 좌표를 기술했다 |
| 7 | 구조 | **feature-first + `models/`(DTO)·`core/` 만 공유** | 화면 하나를 고칠 때 폴더 하나만 연다. 횡단 DTO(`ActiveRound` 등)를 feature 에 두면 경계가 무너진다 |
| 8 | 패키지 | **런타임 13 + dev 4.** 코드 생성기는 `json_serializable` **하나만** | 생성기 3개(freezed+riverpod+json)는 Windows 에서 클린 빌드 60~120초를 물리면서 이득이 없다 |
| 9 | 테스트 | **순수 Dart 단위 + 인터셉터 + 성능 계약 + 위젯 2개.** 골든·E2E 없음 | 눈으로 못 잡는 것(포맷·계약·가드·리빌드 격리)만 고정한다 |
| 10 | 구현 순서 | **17 단위.** 인증(5)·티커 성능(9)을 앞으로 당기고, 정적 마켓(8)과 실시간(9)을 분리 | 성능 회귀가 나면 원인이 배칭 계층임이 확정되어야 한다 |

### 세 안의 충돌을 정리한 지점

- **freezed**: ticker-perf 심사는 `CoinRowState` 한 클래스에만 freezed 를 권했으나, **미도입으로 확정**한다. 생성기를 하나 늘리는 대신 `==`/`hashCode` 를 손으로 20줄 쓰고 성능 계약 테스트로 고정한다(§8-③).
- **timezone 패키지**: **미도입.** KST 는 1988년 이후 DST 가 없어 고정 `+9h` 로 정확하며, tz 데이터베이스 로딩을 부팅 경로에서 제거한다. 다만 컨버터는 3종 클래스로 못 박아 새 DTO 추가 시 선택을 강제한다.
- **아이콘**: `lucide_icons`(3년 전 배포, 최신 SDK 에서 `IconData` final class 컴파일 에러)는 **금지**한다. `lucide_icons_flutter` 를 쓴다.
- **하단 탭 구성**: 마켓 / 투자내역 / 입출금 / 랭킹 / 복기 5탭. 마이페이지는 각 탭 앱바 우상단에서 push 한다(송금은 1급 액션, 마이페이지는 설정이다).

---

## 2. 디렉토리 구조

```
mobile/
  lib/
    main.dart                        # ProviderScope + SessionStore 선적재 + runApp
    app.dart                         # MaterialApp.router (themeMode: light 고정)

    core/
      env.dart                       # --dart-define 주입값 (API_BASE_URL, WS_BASE_URL, *_CLIENT_ID)
      constants/
        exchanges.dart               # ExchangeIds — 1/2/3, 기준통화, 수수료율, 최소주문금액 (R7 단일 출처)
        order_policy.dart            # KRW 5,000~1,000,000,000 / USDT 5~무제한
      api/
        api_client.dart              # Dio 조립
        session_interceptor.dart
        envelope_interceptor.dart
        unauthorized_interceptor.dart
        api_exception.dart
        query.dart                   # null/빈 문자열 파라미터 제거 헬퍼
      auth/
        auth_config.dart             # 제공자별 authUrl · clientId · redirectUri · callbackScheme (단일 출처)
        pkce.dart                    # verifier 43자 / challenge S256 / state 22자
        session_store.dart           # flutter_secure_storage + 동기 메모리 캐시
      json/
        converters.dart              # KstDateTimeConverter / InstantConverter / LocalDateConverter
        decimal_x.dart               # double → Decimal 단일 승격 통로
      realtime/
        stomp_service.dart           # 연결·백오프·구독 레지스트리·lifecycle·connectivity
        ticker_store.dart            # 프레임당 1회 flush · 심볼별 notifier · 플래시 스윕
        order_filled_event.dart      # R3 — 모델만 정의, UI 미연결
      format/
        formatters.dart              # §8.5 전량. NumberFormat 은 최상위 final 캐시
        hangul.dart                  # toChosung / toJamo / isChosungQuery
        server_time.dart             # KST +9h 상수, 상대 시각 표기
      router/
        router.dart                  # GoRouter + StatefulShellRoute
        guard.dart                   # 순수 함수 guard()
        refresh.dart                 # auth+round Listenable.merge
      theme/
        theme.dart                   # ThemeData (§8.6.1, §8.6.4)
        trypto_colors.dart           # ThemeExtension (positive/negative/warning/chart1~5)
      widgets/
        async_view.dart              # AsyncValue → 로딩/에러+재시도/데이터
        exchange_segment.dart
        no_round_notice.dart
        empty_view.dart
        numeric_text.dart            # RobotoMono + tabularFigures
        profit_badge.dart
        app_snackbar.dart

    models/                          # 서버 DTO 전량 + *.g.dart (횡단 공유)
      envelope.dart  cursor_page.dart
      user.dart  round.dart  exchange_coin.dart  ticker.dart  candle.dart
      order.dart  wallet.dart  transfer.dart  portfolio.dart  ranking.dart  regret.dart
      enums.dart                     # Side/OrderType/RuleType/RankingPeriod/RoundStatus ...

    features/
      auth/       auth_repository.dart  auth_controller.dart  social_login.dart
                  login_page.dart  exchanging_overlay.dart
      round/      round_repository.dart  round_controller.dart  round_rules.dart
                  round_create_page.dart  emergency_funding_sheet.dart  round_end_sheet.dart
      market/     exchange_coin_repository.dart  market_controller.dart
                  market_page.dart  coin_row.dart  coin_search_field.dart  overview_cards.dart
                  candle_repository.dart  candle_scale.dart  candle_painter.dart
                  coin_detail_page.dart  candle_chart.dart  chart_fullscreen_page.dart
                  order_repository.dart  order_target.dart  order_form.dart
                  order_sheet.dart  order_history_tab.dart
      portfolio/  portfolio_repository.dart  portfolio_page.dart
                  portfolio_summary.dart  donut_painter.dart  holding_card.dart
      wallet/     wallet_repository.dart  transfer_repository.dart
                  wallet_page.dart  asset_detail_sheet.dart
                  transfer_wizard/  (step1_destination.dart  step2_amount.dart  step3_confirm.dart)
                  transfer_history_page.dart
      ranking/    ranking_repository.dart  ranking_page.dart  ranker_portfolio_sheet.dart
      regret/     regret_repository.dart  regret_page.dart  regret_chart.dart
                  rule_chips.dart  violation_list.dart
      mypage/     user_repository.dart  feedback_repository.dart  mypage_page.dart
                  nickname_sheet.dart  feedback_card.dart

  test/
    formatters_test.dart  hangul_test.dart  server_time_test.dart  decimal_x_test.dart
    envelope_interceptor_test.dart  session_401_test.dart
    guard_test.dart  order_form_test.dart  candle_scale_test.dart
    ticker_store_test.dart          # 성능 계약
    regret_simulation_test.dart
    widget/coin_row_isolation_test.dart
    widget/order_sheet_test.dart

  assets/fonts/                      # Pretendard / NotoSansKR / RobotoMono
  android/  ios/
  docs/  web-spec.md  plan.md
```

### 배치 원칙

- **`models/` 만 layer-first 로 공유한다.** `ActiveRound` 는 round·market·portfolio·wallet·regret 이 전부 쓴다. feature 안에 두면 `import '../round/...'` 가 사방에 생긴다.
- **`core/widgets/` 입장 기준은 "3개 이상 feature 가 쓸 때"** 로 못 박는다. 이 기준이 없으면 공용 폴더가 쓰레기통이 된다.
- feature 안을 `data/domain/presentation` 으로 3등분하지 않는다. 파일이 10개 남짓이면 평면이 더 빨리 읽힌다. 다만 **위젯 없이 테스트해야 할 순수 로직**(`order_target.dart`, `order_form.dart`, `candle_scale.dart`, `round_rules.dart`)은 반드시 별도 파일로 분리한다.
- `core/realtime/` 이 `features/market/` 밖에 있는 이유: STOMP 연결은 앱 수명 전역 자원이며 마켓 화면 생명주기에 종속되면 탭 전환마다 재연결한다.

---

## 3. pubspec.yaml

```yaml
name: trypto
description: Trypto 코인 모의투자 모바일 앱
publish_to: none
version: 1.0.0+1

environment:
  sdk: ">=3.6.0 <4.0.0"
  flutter: ">=3.27.0"

dependencies:
  flutter:
    sdk: flutter

  # 상태 관리 + DI
  flutter_riverpod: ^2.6.1

  # 라우팅
  go_router: ^14.6.2

  # 네트워크
  dio: ^5.7.0

  # 실시간
  stomp_dart_client: ^2.1.0
  connectivity_plus: ^6.1.0

  # 인증
  flutter_web_auth_2: ^4.1.0
  flutter_secure_storage: ^9.2.2
  crypto: ^3.0.6

  # 값·포맷
  decimal: ^3.2.1
  intl: ^0.20.2
  uuid: ^4.5.1

  # 직렬화
  json_annotation: ^4.9.0

  # UI
  fl_chart: ^1.0.0
  lucide_icons_flutter: ^3.1.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  build_runner: ^2.4.13
  json_serializable: ^6.9.0
  http_mock_adapter: ^0.6.1
  flutter_lints: ^5.0.0

flutter:
  uses-material-design: true
  fonts:
    - family: Pretendard
      fonts:
        - asset: assets/fonts/Pretendard-Regular.otf
          weight: 400
        - asset: assets/fonts/Pretendard-Medium.otf
          weight: 500
        - asset: assets/fonts/Pretendard-SemiBold.otf
          weight: 600
        - asset: assets/fonts/Pretendard-Bold.otf
          weight: 700
    - family: NotoSansKR
      fonts:
        - asset: assets/fonts/NotoSansKR-ExtraBold.otf
          weight: 800
    - family: RobotoMono
      fonts:
        - asset: assets/fonts/RobotoMono-Medium.ttf
          weight: 500
        - asset: assets/fonts/RobotoMono-Bold.ttf
          weight: 700
```

버전은 하한 제약(caret)이며, 프로젝트 생성 직후 `flutter pub upgrade` 로 Flutter 3.44 / Dart 3.12 최신 해석본을 고정한 뒤 `pubspec.lock` 을 커밋한다.

### 각 패키지의 채택 사유

| 패키지 | 용도 | 왜 그것인가 |
|---|---|---|
| `flutter_riverpod` | 전역 상태 + DI | go_router `redirect` 가 **위젯 트리 밖에서** auth·round 를 읽어야 한다(`ProviderContainer.read`). `provider` 는 `BuildContext` 를 요구해 이 지점에서 우회 코드를 만든다. codegen 없이 `NotifierProvider` 를 손으로 선언한다 |
| `go_router` | 선언적 라우팅 | 가드 3종(§2.7.2)을 `redirect` 한 곳으로 통합하고, `StatefulShellRoute.indexedStack` 으로 탭별 스택·스크롤을 보존한다 |
| `dio` | HTTP | 인터셉터 체인이 봉투 언랩·세션 헤더·401 전역 처리의 **전제**다. `http` 에는 인터셉터가 없다 |
| `stomp_dart_client` | STOMP 1.2 over raw WS | 서버는 SockJS 가 아니다(§3.1.1). 프레임 파서·하트비트·단독 LF 처리를 직접 짜지 않는다 |
| `connectivity_plus` | Wi-Fi ↔ 셀룰러 전환 감지 | 전환 직후 소켓은 살아 보이지만 죽어 있다(§3.5.2). 웹에 없는 모바일 고유 이벤트 |
| `flutter_web_auth_2` | OAuth 인가 화면 | Android=Custom Tabs, iOS=`ASWebAuthenticationSession`. 앱 내 WebView 는 구글 정책상 차단이라 대안이 없다. **콜백 URL 을 `Future` 로 돌려주므로 딥링크 라우팅이 통째로 불필요해진다** |
| `flutter_secure_storage` | 세션 ID · PKCE verifier/state | Keystore/Keychain. `shared_preferences` 는 평문이라 부적격 |
| `crypto` | PKCE `S256` | `sha256` 한 줄(§2.2.1 규격 유지) |
| `decimal` | 정밀 연산(R6) | 소수 8자리 수량의 **클라이언트 내부 산술**(수량↔총액 연동, 비율 버튼, 잔고 검증) |
| `intl` | 숫자·날짜 포맷 | §8.5 포매터 11종과 `DateFormat('yyyy.MM.dd HH:mm','ko_KR')` |
| `uuid` | `clientOrderId` / `idempotencyKey` | **UUID v4 필수** — 형식 위반 시 서버가 400 이 아니라 500 을 낸다(R8) |
| `json_annotation` / `json_serializable` / `build_runner` | DTO 직렬화 | DTO 40여 개에서 `(json['price'] as num).toDouble()` 을 손으로 쓰면 반드시 한 번은 `as double` 로 쓴다. KRW 정수 가격이 `int` 로 도착하는 순간 런타임에서 터진다(R6-2) |
| `fl_chart` | 투자 복기 자산 곡선 | 선 3개 + 마커 + 터치 툴팁. 사양서 §6.4 가 이 패키지 전제로 좌표를 기술했다 |
| `lucide_icons_flutter` | 아이콘 | 웹이 `lucide-react`. 이름이 1:1 대응하며, **최신 SDK 에서 컴파일되는 유일한 lucide 패키지**다 |
| `http_mock_adapter` (dev) | Dio 응답 스텁 | 봉투 4경로·401 분기 테스트 |

### 도입하지 않는 것

- **`freezed`** — 생성기 1개를 늘리는 대신 상태 4개의 `copyWith` 와 `CoinRowState` 의 `==` 를 손으로 쓴다. 필드 삭제 시 컴파일 오류가 나는 것은 타입 있는 Dart 클래스면 다 되는 일이지 freezed 의 기능이 아니다.
- **`riverpod_generator` / `custom_lint` / `riverpod_lint`** — provider 20개 남짓이면 수동 선언이 더 짧고, 애널라이저 플러그인은 IDE 를 눈에 띄게 느리게 만든다.
- **`timezone`** — 고정 `+9h` 로 정확하다(§5.4).
- **`retrofit` / `chopper`** — 봉투 안의 제네릭 `T` 를 꺼내려면 커스텀 컨버터를 짜야 하고, 커서 파라미터 이름이 엔드포인트마다 다르다(`cursorOrderId`/`cursorRank`/`cursorTransferId`/`cursor`).
- **`dio_cookie_manager` + `cookie_jar`** — 쿠키의 7일 절대 만료를 그대로 물려받는다(R2).
- **`kakao_flutter_sdk_user`** — 백엔드가 **인가 코드 + code_verifier** 를 요구하는데 SDK 는 액세스 토큰 방식이다(§2.4).
- **`app_links` / `uni_links`** — `flutter_web_auth_2` 와 콜백 인텐트가 충돌한다(§5.3).
- **`equatable`** — `==` 호출마다 `props` 리스트를 새로 할당한다. 초당 수백 회 호출되는 경로에 쓰레기를 만든다.
- **`lucide_icons`** — 최신 SDK 정적 분석에서 컴파일 에러가 난다.
- **가상 스크롤 패키지 / `hive` / `isar` / `get_it`** — 각각 `ListView.builder(itemExtent:)`, 요구사항 부재, Riverpod 이 대체한다.

---

## 4. 핵심 계층 설계

### 4.1 API 클라이언트

```
Repository → Dio(인터셉터 3단) → 서버
             ├ SessionInterceptor      : Cookie 헤더 부착 / Set-Cookie 회수·폐기
             ├ EnvelopeInterceptor     : 봉투 언랩 · 성공 판정 · ApiException 통일
             └ UnauthorizedInterceptor : 세션 만료 판정 → 세션 폐기 → auth = null
```

#### 4.1.1 세션 (R2)

```dart
class SessionStore {
  static const _key = 'session_id';
  final FlutterSecureStorage _storage;
  String? _cached;                       // 동기 캐시. onRequest 는 절대 await 하지 않는다.

  String? get sessionId => _cached;

  /// runApp() 이전에 1회 호출한다. 실패는 '세션 없음' 으로 강등한다.
  Future<void> load() async {
    try {
      _cached = await _storage.read(key: _key);
    } catch (_) {
      _cached = null;                    // 복호화 실패(앱 업데이트·키 회전)에 부팅이 막히면 안 된다
      await _safeDelete();
    }
  }

  Future<void> save(String id) async { _cached = id; await _storage.write(key: _key, value: id); }

  /// 멱등. 병렬 요청이 동시에 401 을 받아도 안전하다.
  Future<void> clear() async { if (_cached == null) return; _cached = null; await _safeDelete(); }
}
```

`SessionInterceptor`:
- `onRequest`: `_cached` 가 있으면 `Cookie: SESSION=<값>` 부착. **`await` 하지 않는다**(요청마다 플랫폼 채널을 왕복하면 프레임에 얹힌다).
- `onResponse`: `set-cookie` 헤더에서 `SESSION=` 값만 정규식으로 추출한다. **값이 비어 있으면(로그아웃·탈퇴의 `Max-Age=0`) 삭제**, 있으면 저장한다. **`Max-Age` 는 저장하지 않는다** — 서버의 슬라이딩 TTL 을 그대로 쓴다.

#### 4.1.2 봉투 언랩 (§1.2, §1.11.1)

```dart
class EnvelopeInterceptor extends Interceptor {
  static const _ok = {'SUCCESS', 'CREATED'};   // 201 이 CREATED / SUCCESS 둘 다 온다

  @override
  void onResponse(Response r, ResponseInterceptorHandler h) {
    final body = r.data;
    if (body is! Map<String, dynamic>) {
      return h.reject(_wrap(r, ApiException.invalidResponse(r.statusCode)));
    }
    final code = body['code'] as String?;
    if (!_ok.contains(code)) {
      return h.reject(_wrap(r, ApiException.fromEnvelope(body)));
    }
    r.data = body['data'];                     // 호출부는 봉투를 절대 보지 않는다
    h.next(r);
  }

  @override
  void onError(DioException e, ErrorInterceptorHandler h) {
    final body = e.response?.data;
    if (body is Map<String, dynamic> && body.containsKey('code')) {
      return h.reject(e.copyWith(error: ApiException.fromEnvelope(body)));
    }
    h.reject(e.copyWith(error: ApiException.network(e)));   // SocketException / timeout
  }
}
```

성공 판정은 반드시 `2xx && code ∈ {SUCCESS, CREATED}` 이다. `POST /api/feedbacks` 는 **201 + `code: SUCCESS`** 로 온다.

#### 4.1.3 401 전역 처리 (R10) — 판별식을 좁힌다

`SOCIAL_LOGIN_FAILED` 가 **401** 이다(§1.3). HTTP 401 만으로 판정하면 로그인 실패가 세션 폐기 경로를 타서 인증 상태가 흔들리고 redirect 가 재평가된다. 두 겹으로 막는다.

```dart
bool _isSessionExpired(RequestOptions req, ApiException e) {
  if (req.path.startsWith('/api/auth/')) return false;             // ① 인증 엔드포인트 제외
  return e.code == 'UNAUTHENTICATED' || (e.status == 401 && e.code == null);   // ② 판별식
}
// 참이면: sessionStore.clear() + authController.onSessionExpired() + roundController.reset()
// 화면 이동은 하지 않는다. auth = null → refreshListenable → redirect 가 /login 으로 보낸다.
```

**로그아웃·세션 만료 시 라운드 상태(`exchangeId → walletId` 맵)와 거래소 코인 캐시를 함께 비운다.** 남겨 두면 다음 로그인 사용자가 이전 사용자의 `walletId` 로 첫 요청을 보낸다.

#### 4.1.4 에러 → 사용자 메시지

**전역 코드→문구 매핑 테이블을 만들지 않는다.** 서버 `message` 는 이미 한국어 완성문이다(§1.11.4).

```dart
String get userMessage => (message?.isNotEmpty ?? false) ? message! : switch (code) {
  'NETWORK_ERROR'         => '네트워크에 연결할 수 없습니다.',
  'INTERNAL_SERVER_ERROR' => '일시적인 오류입니다. 잠시 후 다시 시도해 주세요.',
  _                       => '요청 처리 중 오류가 발생했습니다.',
};
```

**흐름을 바꾸는 코드만 상수로 승격한다.** 이 셋뿐이다.

| 코드 | 처리 |
|---|---|
| `ROUND_NOT_ACTIVE` (409) | 예외가 아니라 **`null`** 로 변환한다(§1.10.3). 이것을 놓치면 신규 사용자가 앱에 진입하지 못한다 |
| `RANKING_NOT_FOUND` (404) | 랭커 포트폴리오 → 빈 상태 |
| `PORTFOLIO_VIEW_NOT_ALLOWED` (403) | 100위 초과 → **요청 자체를 보내지 않고 선제 차단** |

문맥 문구가 필요한 곳은 **송금 화면 하나뿐**이다(§5.4.4 의 5개 코드). 화면 로컬 override 표로 덮어쓴다.

#### 4.1.5 잔가시 규칙 (전부 `core/api/query.dart` 한 곳에 가둔다)

- **쿼리 파라미터**: `null`·빈 문자열은 **키 자체를 생략**한다. `Map<String,dynamic> q(Map<String,dynamic> m) => {...m}..removeWhere((_, v) => v == null || v == '')`.
- **요청에서 `userId` 를 전면 제거한다**(R4-7). 서버는 `@LoginUser` 로 세션에서 얻는다.
- **바디 없는 POST**(`/api/auth/logout`, `/api/rounds/{id}/end`)는 `data` 를 넘기지 않는다 → `Content-Type` 이 붙지 않는다.
- **커서 이름은 엔드포인트마다 다르다.** Repository 시그니처에 그대로 노출한다(`cursorOrderId` / `cursorRank` / `cursorTransferId` / `cursor`). 억지로 추상화하지 않는다.
- **자동 재시도 인터셉터를 넣지 않는다.** 자동 재시도가 멱등키 규약을 흐린다. 재시도는 사용자의 명시적 조작(당김 새로고침·"다시 시도")으로만 하고, **그때 반드시 같은 키를 재사용한다**.

### 4.2 STOMP 클라이언트와 티커 배칭

#### 4.2.1 연결 수명

| 항목 | 값 | 근거 |
|---|---|---|
| 프로토콜 | raw WS + STOMP 1.2. **`StompConfig.sockJS` 금지** | §3.1.1 |
| heartbeat | in 10s / **out 8s** | §3.1.3 (모바일 타이머 유예 마진 2초) |
| connectionTimeout | 5s | §3.1.4 |
| 재연결 | 내장 재연결 끄고(`reconnectDelay: Duration.zero`) **직접 백오프** `min(1000 × 2^n, 30000)`, 첫 대기 **2초** | 내장은 고정 지연만 지원 |
| 구독 복원 | `onConnect` 에서 레지스트리 전량 재구독 | 자동 복원되지 않는다 |
| 연결 끊김 | 보관 중인 `unsubscribe` 핸들 전량 무효화 | 중복 구독 방지 |
| 방향 | **수신 전용.** `SEND` 경로 없음 | `@MessageMapping` 0건 |

```dart
void _onWebSocketDone() {
  if (_intentionalClose) { _intentionalClose = false; return; }   // ★ 의도적 종료는 백오프를 무장하지 않는다
  _invalidateSubscriptions();
  _attempts++;
  final ms = math.min(1000 * math.pow(2, _attempts).toInt(), 30000);
  _retryTimer = Timer(Duration(milliseconds: ms), _client.activate);
}

void forceReconnect() {
  _retryTimer?.cancel();                 // ★ 보류 중인 백오프 타이머를 반드시 취소한다
  _intentionalClose = true;
  _client.deactivate();                  // 대기하지 않는다. 즉시 새 클라이언트를 만든다
  _client = _build();
  _attempts = 0;
  _client.activate();
}
```

`_intentionalClose` 플래그가 없으면, 백그라운드 진입 시 끊은 소켓이 `onWebSocketDone` 을 태워 백오프 타이머를 예약하고, 포그라운드 복귀의 강제 재연결과 겹쳐 **이중 연결·중복 구독**이 생긴다.

#### 4.2.2 앱 lifecycle

`AppLifecycleListener` 를 서비스가 소유한다.

| 상태 | 동작 |
|---|---|
| **`paused` 만** | `_hiddenAt = now` 기록 후 소켓 즉시 종료(`_intentionalClose = true`) |
| `inactive` / `hidden` | **무시한다.** iOS 는 알림 센터를 살짝 내려도 뜬다. 여기서 재연결을 돌리면 순수한 낭비다 |
| `resumed` | `elapsed > 20s || !connected` → `forceReconnect()`. 이어서 **REST 스냅샷 1회 재조회**(누락 틱 보정)와 열린 화면의 데이터 재조회(지정가 지연 체결 반영) |
| 네트워크 전환 | `connectivity_plus` → **1초 디바운스** 후 `forceReconnect()`. resume 직후에도 이벤트가 튀므로 디바운스가 없으면 중복 발화한다 |

#### 4.2.3 마켓 탭 비활성 시 구독 해제 (필수)

`StatefulShellRoute.indexedStack` 은 선택된 자식만 **페인트**하고 **레이아웃은 모든 자식에 대해 수행**하며 Element 를 전부 살려 둔다. 따라서 구독을 끊지 않으면 **포트폴리오 탭을 보는 동안에도 마켓의 티커 리빌드 비용을 그대로 낸다.**

- 셸의 `currentIndex` 를 `ValueNotifier<bool> marketVisible` 로 중계한다.
- 비활성 → 티커 토픽 `unsubscribe` + flush 중단 + `_pending` 비움.
- 재활성 → 재구독 + `GET /api/exchanges/{id}/coins` 스냅샷 1회 재조회.

#### 4.2.4 `TickerStore` — R5 의 답 (타협 없음)

```dart
/// hasListeners 는 ChangeNotifier 의 @protected 멤버다. 서브클래싱해 공개 게터로 노출한다.
class RowNotifier extends ValueNotifier<CoinRowState> {
  RowNotifier(super.value);
  int? lastTickedAt;
  bool get isWatched => hasListeners;
}
class FlashNotifier extends ValueNotifier<FlashDir?> {
  FlashNotifier() : super(null);
  bool get isWatched => hasListeners;
}

/// 숫자 3개만 담는다. tickedAt 은 넣지 않는다 — 넣으면 매 틱 == 가 false 가 되어 no-op 억제가 무력화된다.
/// ==/hashCode 는 성능 계약이다. 손으로 쓰고 테스트로 고정한다.
class CoinRowState {
  final double price, changeRate, volume;
  const CoinRowState(this.price, this.changeRate, this.volume);
  @override bool operator ==(Object o) => o is CoinRowState
      && o.price == price && o.changeRate == changeRate && o.volume == volume;
  @override int get hashCode => Object.hash(price, changeRate, volume);
}

class TickerStore {
  final Map<String, RowNotifier>   _rows  = {};
  final Map<String, FlashNotifier> _flash = {};
  final Map<String, Ticker> _pending = {};      // 프레임 버퍼. 같은 심볼은 마지막 값이 이긴다
  final Map<String, int> _flashUntilMs = {};
  bool _scheduled = false;
  bool _active = true;                          // 마켓 탭 비활성 시 false

  /// STOMP 프레임마다 호출된다. 초당 수백 회. setState 를 절대 부르지 않는다.
  void ingest(List<Ticker> ticks) {
    if (!_active) return;
    for (final t in ticks) {
      if (!_rows.containsKey(t.symbol)) continue;    // 상장 목록 밖 심볼은 버린다 (§3.3.2-2)
      _pending[t.symbol] = t;                        // O(1), last-wins
    }
    _schedule(rescheduling: false);
  }

  void _schedule({required bool rescheduling}) {
    if (_scheduled) return;
    _scheduled = true;
    SchedulerBinding.instance.scheduleFrameCallback(_flush, rescheduling: rescheduling);
  }

  void _flush(Duration _) {
    _scheduled = false;
    final nowMs = DateTime.now().millisecondsSinceEpoch;

    for (final t in _pending.values) {
      final row = _rows[t.symbol]!;
      final flash = _flash[t.symbol]!;

      // 플래시는 '보이는 행' 에서만 계산한다. 화면 밖 행은 리스너가 0개다.
      if (flash.isWatched && row.lastTickedAt != null && t.timestamp != row.lastTickedAt) {
        final dir = t.price > row.value.price ? FlashDir.up
                  : t.price < row.value.price ? FlashDir.down
                  : FlashDir.same;
        flash.value = dir;
        _flashUntilMs[t.symbol] = nowMs + 100;       // §3.3.3 — 100ms 후 즉시 해제, 페이드 없음
      }
      row.lastTickedAt = t.timestamp;
      row.value = CoinRowState(t.price, t.changeRate, t.quoteTurnover);   // == 면 알림이 나가지 않는다
    }
    _pending.clear();

    _flashUntilMs.removeWhere((sym, until) {         // 심볼별 Timer 대신 flush 루프에서 스윕한다
      if (nowMs < until) return false;
      _flash[sym]!.value = null;
      return true;
    });
    if (_flashUntilMs.isNotEmpty) {
      _schedule(rescheduling: true);                 // ★ 콜백 실행 중 재등록은 rescheduling: true 필수
    }
    _orderDirty = true;                              // 정렬은 별도 캐던스(§4.2.5)
  }

  void switchExchange(List<ExchangeCoin> coins) {    // §3.3.2-5
    _pending.clear(); _flashUntilMs.clear();
    _rows..clear(); _flash..clear();
    for (final c in coins) {
      _rows[c.coinSymbol] = RowNotifier(CoinRowState(c.price, c.changeRate, c.volume));
      _flash[c.coinSymbol] = FlashNotifier();
    }
  }
}
```

여기에 성능 결정 다섯 개가 들어 있다.

1. **`Timer(16ms)` 가 아니라 `scheduleFrameCallback`.** transient callback 은 `handleBeginFrame` 에서, 즉 **같은 프레임의 build/layout 보다 앞서** 실행된다. 따라서 flush 가 만든 dirty 마크가 그 프레임 안에서 처리된다. `Timer` 는 `drawFrame` 직후에 발화할 수 있어 dirty 마크가 한 프레임을 통째로 기다린다.
2. **`CoinRowState` 에서 `tickedAt` 을 제거했다.** 동일가 재체결 시 숫자 위젯이 리빌드되지 않고 테두리만 깜빡인다. 암호화폐 호가는 같은 가격에 연속 체결되는 빈도가 매우 높다.
3. **심볼별 `Timer` 를 flush 루프의 만료 스윕으로 대체했다.** 타이머 수: 수백 → **0**.
4. **`isWatched` 게이트**로 화면 밖 행의 플래시 계산·알림을 건너뛴다.
5. **콜백 실행 중 재등록에 `rescheduling: true`.** 없으면 디버그 assert 가 터진다.

#### 4.2.5 리스트 렌더링 규칙 (전부 강제 사항)

```dart
ListView.builder(
  controller: _scrollController,
  itemExtent: 68,                        // 자식 측정 생략 → 스크롤 중 O(1) 위치 계산
  itemCount: order.length,
  // key 를 주지 않는다. cacheExtent 는 기본값을 유지한다.
  itemBuilder: (_, i) => CoinRow(row: store.row(order[i]), flash: store.flash(order[i]), ...),
)
```

1. **행에 `key: ValueKey(symbol)` 를 주지 않는다.** sliver 자식은 **index 슬롯**으로 매칭되므로, 순서가 바뀌면 키가 어긋나 `canUpdate == false` → deactivate + inflate 로 **Element/State/RenderObject 가 전부 재생성된다.** 키를 빼면 in-place 갱신이 되어 재정렬 비용이 절반 이하가 된다. 대신 **행 로컬 State 에 틱 의존 상태를 두지 않는다**(플래시 직전값은 store 가 소유한다).
2. **행별 `RepaintBoundary` 를 감싸지 않는다.** `ListView.builder` 가 `addRepaintBoundaries: true` 로 이미 넣는다. 대신 **행 내부**(테두리 ↔ 숫자)에만 둔다.
3. **행을 3분할한다.** 코인명/한글명은 `ValueListenableBuilder` **바깥**(앱 수명 동안 1회 빌드), 숫자 3개는 `RowNotifier` 구독, 플래시 테두리는 `FlashNotifier` 구독. 테두리 깜빡임이 숫자 텍스트의 레이아웃을 다시 돌리지 않는다.
4. **숫자 셀에 고정 폭(tight constraint)을 준다.** `RepaintBoundary` 는 페인트만 격리한다. 가격 문자열 폭이 바뀌면 `RenderParagraph` 가 `markNeedsLayout` 을 올리고 느슨한 제약의 `Row` 전체가 리레이아웃된다. 제약이 tight 하면 그 `RenderBox` 가 relayout boundary 가 되어 부모로 전파되지 않는다.
   ```dart
   SizedBox(width: 104, child: Align(alignment: Alignment.centerRight,
     child: Text(price, maxLines: 1, softWrap: false, overflow: TextOverflow.clip))),
   SizedBox(width: 76, child: _ChangeBadge(rate)),
   SizedBox(width: 96, child: Align(alignment: Alignment.centerRight, child: Text(volume, maxLines: 1))),
   ```
5. **정렬 캐던스를 티커와 분리한다.** 사용자 조작(정렬키·필터·검색어·거래소 변경) 시 **즉시**, 시세 변동에 따른 순서 갱신은 **1초 스로틀**. 이 규칙이 없으면 인덱스→심볼 매핑이 매 프레임 뒤집혀 심볼별 notifier 로 얻은 이득이 통째로 사라지고, 초당 60번 자리를 바꾸는 목록은 읽을 수도 없다.
6. **스크롤 중에는 재정렬을 얼린다.** `if (_scrollController.position.isScrollingNotifier.value) { _orderDirty = true; return; }` — 손가락 밑에서 행이 튀면 오탭이 난다.
7. **플래시 테두리는 `Stack(clipBehavior: Clip.none)`** 안의 `Positioned(left: 0, right: -8, top: -10, bottom: -10)` 이다. `Clip.none` 이 없으면 바깥으로 확장된 테두리가 잘린다.
8. **`Opacity` / `AnimatedOpacity` 위젯을 행·차트 트리에 쓰지 않는다.** 위젯마다 `saveLayer` 가 생겨 래스터 스레드가 먼저 무너진다. 알파는 **색으로만** 준다(`color.withValues(alpha: 0.4)`).
9. **포매터의 `NumberFormat` 인스턴스를 최상위 `final` 로 캐시한다.** 생성자가 패턴 문자열을 파싱하므로(10~30µs) 호출마다 만들면 보이는 행 10개 × 포맷 3회 = **프레임당 0.3~0.9ms**의 순수 낭비가 된다. 이는 티커 처리 총비용과 맞먹는다.
10. **델타 누산 금지.** 서버가 `DiscardOldestPolicy` 로 메시지를 버린다(§3.1.7). 티커는 완전성이 보장되지 않는 **스냅샷 스트림**이므로 항상 최신 값으로 덮어쓴다.

#### 4.2.6 체결 이벤트 (R3)

`/user/queue/events` 를 **구독하지 않는다.** 서버가 `Principal` 을 부착하지 않아 메시지가 폐기된다. `OrderFilledEvent{eventType, orderId, executedPrice, quantity, executedAt}` 모델과 빈 핸들러만 정의하고 사유를 주석으로 남긴다. 서버가 고쳐지면 구독 목적지 `/user/queue/events`(리터럴 `/user/{id}/...` 가 아니다) 한 줄로 켠다.

**체결 반영은 REST 재조회로 확정한다**: 주문 제출·취소 성공 직후 `GET /api/orders/available` 재호출, 지정가 지연 체결은 화면 진입·포그라운드 복귀·당김 새로고침 시 재조회.

### 4.3 인증 흐름 (딥링크 콜백)

#### 4.3.1 콜백을 라우터에 들이지 않는다

`flutter_web_auth_2` 는 자기 소유의 `CallbackActivity`(Android) / `ASWebAuthenticationSession`(iOS)으로 인텐트를 **소비**한 뒤 `authenticate()` 의 `Future` 로 URL 을 돌려준다. 여기에 `MainActivity` 의 intent-filter 나 `app_links` 를 더하면 같은 콜백이 두 경로로 들어와 **인가 코드가 두 번 교환되고(1회용이라 두 번째는 실패) 라우터 가드와 경쟁한다.**

- **`AndroidManifest.xml` 의 `trypto` 스킴 intent-filter 는 `com.linusu.flutter_web_auth_2.CallbackActivity` 에만 선언한다.** `MainActivity` 에는 어떤 커스텀 스킴도 붙이지 않는다.
- **`/auth/:provider/callback` 라우트를 만들지 않는다.** 가드에 예외 조항도 필요 없다.
- **"소셜 콜백 화면"은 로그인 화면의 상태로 구현한다**(§2.4 가 "`SocialCallbackPage` 에 해당하는 화면은 만들지 않는다" 고 명시). 기능 손실은 없다.

#### 4.3.2 상태 기계와 시퀀스

```
AuthStatus: idle → authorizing(외부 브라우저) → exchanging(POST /login) → authenticated | failed(메시지)
```

```dart
Future<void> start(Provider p) async {
  final v  = Pkce.verifier();                  // 32바이트 → base64url, 43자
  final ch = Pkce.challenge(v);                // SHA-256(v) → base64url
  final st = Pkce.state();                     // 16바이트 → base64url, 22자
  await secrets.put(p, v, st);                 // 프로세스 사망 대비. 성공·실패 무관 즉시 삭제한다.
  status = authorizing;

  final result = await FlutterWebAuth2.authenticate(
    url: AuthConfig.authorizeUrl(p, challenge: ch, state: st),
    callbackUrlScheme: AuthConfig.callbackScheme(p),   // ★ 제공자별 값. 단일 상수로 뭉치지 않는다
  );

  final uri = Uri.parse(result);
  _verify(uri, st);                            // §2.2.4 의 6종 실패 메시지를 그대로 사용
  await secrets.clear();                       // 성공·실패 무관, 검증 직후 즉시 삭제

  status = exchanging;
  final res = await api.post('/api/auth/${p.path}/login', data: {
    'code': uri.queryParameters['code'],
    'codeVerifier': v,
    'clientType': 'MOBILE',                    // 백엔드가 모바일 자격증명으로 토큰을 교환한다
  });
  // SessionInterceptor 가 Set-Cookie 를 회수해 저장한다.
  status = authenticated;                      // 화면 이동은 하지 않는다. redirect 가 /market 으로 보낸다.
}
```

#### 4.3.3 PKCE 비밀값의 수명 (콜드 스타트 대응)

1. verifier·state 는 `flutter_secure_storage` 에 저장한다.
2. **교환 성공·실패 무관 즉시 삭제한다.**
3. **앱 부팅 시 무조건 삭제한다.** 콜드 스타트는 인가 세션이 이미 소실되었다는 뜻이므로 남은 비밀값은 전부 오염값이다.

브라우저 세션 중 OS 가 프로세스를 죽이면 `await` 가 사라진다. 이때 복구 경로를 만들지 않는다 — Custom Tabs 는 호출한 앱을 살려 두므로 발생 빈도가 무의미하고, 위 세 규칙으로 오염이 닫힌다.

#### 4.3.4 `AuthConfig` — redirect URI 단일 출처

OAuth2 규격상 토큰 교환의 `redirect_uri` 는 인가 요청 값과 **문자열 단위로 완전히 동일**해야 한다. 앱은 이 문자열을 `AuthConfig` 한 곳에서만 만들고, 백엔드 허용 목록 값과 대조한 뒤 구현 5단계에 착수한다.

```dart
class AuthConfig {
  static String callbackScheme(Provider p) => switch (p) {
    Provider.kakao  => Env.kakaoCallbackScheme,    // 기본 'trypto'
    Provider.google => Env.googleCallbackScheme,   // 기본 'trypto'. 콘솔이 거부하면 여기만 바뀐다
  };
  static String redirectUri(Provider p) => '${callbackScheme(p)}://auth/${p.path}/callback';
  static String clientId(Provider p) => switch (p) {
    Provider.kakao  => Env.kakaoClientId,
    Provider.google => Platform.isAndroid ? Env.googleAndroidClientId : Env.googleIosClientId,
  };
}
```

구글의 Android 클라이언트와 iOS 클라이언트는 **별개**이고, 토큰 교환의 `client_id` 는 인가 요청에 쓴 것과 같아야 한다. `--dart-define` 키를 플랫폼별로 둘로 나눈다. 콘솔이 커스텀 스킴을 거부하면 `Env.*CallbackScheme` 값과 매니페스트 intent-filter 만 바뀌고 나머지 코드는 그대로다(§10 참조).

### 4.4 라우팅과 가드

```
/splash                                  (셸 밖. auth/round 로딩 대기)
/login                                   (셸 밖. 콜백 진행 오버레이를 내부 상태로 포함)
/round/new                               (셸 밖. 2단계 스텝)
/mypage                                  (셸 밖. rootNavigator push)
StatefulShellRoute.indexedStack          ← 하단 탭 5개. 탭별 스택·스크롤 보존
  ├ /market            ?exchange=upbit
  │   └ /market/coin/:symbol             (rootNavigator push — 탭바 위를 덮는다)
  │       └ /chart                       (가로 풀스크린)
  ├ /portfolio         ?exchange=
  ├ /wallet            ?exchange=
  │   ├ /wallet/transfer                 (3단계 마법사)
  │   └ /wallet/history                  (전체 송금 내역)
  ├ /ranking           ?period=weekly
  └ /regret            ?exchange=
*                      → /login          (예기치 않은 경로·딥링크 방어)
```

가드는 **순수 함수**다. 위젯 없이 표 기반으로 테스트한다.

```dart
String? guard({
  required String loc,
  required bool authLoading, required bool authed,
  required bool roundLoading, required bool hasActive, required bool hasEverStarted,
}) {
  if (authLoading)                         return loc == '/splash' ? null : '/splash';
  if (!authed)                             return loc == '/login'  ? null : '/login';
  if (roundLoading)                        return loc == '/splash' ? null : '/splash';
  if (loc == '/login' || loc == '/splash') return '/market';                    // PublicRoute
  if (loc == '/round/new')                 return hasActive ? '/market' : null; // RoundGuard
  if (!hasActive && !hasEverStarted)       return '/round/new';                 // ProtectedRoute
  return null;
}
```

**두 가지를 반드시 지킨다.**

1. **모든 로딩 분기를 `loc == X ? null : X` 로 쓴다.** `if (loading) return '/splash';` 형태는 `/splash` 에 있을 때도 `/splash` 를 반환해 `GoException: redirect loop detected` 로 **부팅 첫 프레임에 에러 화면**을 띄운다.
2. **"인증됨 → `/splash`·`/login` 이면 `/market`" 규칙은 반드시 round 로딩 검사보다 뒤에 온다.** 앞에 두면 로그인된 사용자의 모든 콜드 스타트(auth 복구 완료 + round 조회 중)에서 `/splash → /market → /splash` 가 반복되어 예외가 난다.

`refreshListenable` 에는 **auth·round 두 컨트롤러만** `Listenable.merge` 로 물린다. 티커를 연결하면 초당 수십 번 `redirect` 가 재평가된다. 화면 코드 어디에도 `context.go('/login')` 이 없다 — 401 인터셉터가 auth 를 비우면 redirect 가 알아서 보낸다.

바텀시트(주문·자산 상세·랭커 포트폴리오·긴급자금·라운드 종료)는 **라우트가 아니다**(`showModalBottomSheet`). 뒤로가기 처리는 Flutter 가 담당한다.

### 4.5 모델·직렬화

#### 4.5.1 정밀도 (R6) — Decimal / double 경계

**DTO 필드는 전부 `double` 로 선언하고, 연산 지점에서만 `Decimal` 로 승격한다.**

```dart
// json_serializable 이 double 필드에 대해 정확히 이 코드를 생성한다 → 수기 파싱 오타를 없앤다
(json['price'] as num).toDouble()      // KRW 정수 가격이 int 로 오므로 `as double` 은 런타임 오류

// core/json/decimal_x.dart — 유일한 승격 통로
extension DecimalX on double {
  Decimal get dec => Decimal.parse(toString());   // double.toString() 은 최단 왕복 표현을 보장한다
}
```

DTO 를 `Decimal` 로 선언하면 nullable 컨버터(`orderAmount`/`price`/`filledPrice`/`fee`/`endedAt`/`filledAt`/`completedAt`/`nextCursor` 등 8개 이상)를 짝으로 전부 만들어야 하고, 티커 hot path 에 `BigInt` 연산이 스며든다. **정밀도 손해는 없다** — `jsonDecode` 시점에 이미 `num` 이 만들어졌으므로 DTO 를 `Decimal` 로 선언해도 결국 `Decimal.parse(num.toString())` 과 같은 일을 한다.

**`Decimal` 로 승격하는 지점(전부 열거)**

| 위치 | 연산 |
|---|---|
| 주문 폼 | 수량↔총액 연동, 비율 버튼(`available × ratio / 100`), 수수료 예상치(`총액 × feeRate`) |
| 포트폴리오 | `totalBuy`, `totalEval`, `totalAsset`, `profitLoss`, `profitRate`, 도넛 세그먼트 `value` |
| 지갑 | 총자산·환산액, 소액 제외 임계 비교, 출금 후 남는 가용 잔고 |
| 송금 | `amount` 검증(`0 < amount <= available`) |

**`double` 로 두는 지점**: 티커 `price`/`changeRate`/`quoteTurnover`(초당 수백 건 × 600행 — `Decimal` 은 절대 올리지 않는다), 캔들 OHLC(즉시 픽셀 좌표로 뭉개진다), 랭킹 `profitRate`, `assetRatio`, 도넛 비율.

**나눗셈 함정**: `decimal` 의 `/` 는 `Rational` 을 반환한다. 스케일을 명시한다.
```dart
final qty = (total / price).toDecimal(scaleOnInfinitePrecision: 8);   // 8자리 절사
```

**요청 바디는 `toDouble()` 로 보낸다.** 전송 값의 유효자리는 최대 10자리(주문 상한 10억, 정수)이거나 소수 8자리(수량)이며 double 의 유효자리 안에 있고, 최단 왕복 표기가 원값을 복원한다.

**경계값 테스트로 고정한다**: `0.00012345`, `1e-8`(Dart 는 `0.00000001` 을 `"1e-8"` 로 출력한다 — BTC 먼지 수량이 정확히 이 구간이다), `152340000.5`, `1000000000`.

#### 4.5.2 시각 (R6) — 컨버터 3종

서버는 **캔들 `time` 만 UTC(Z 포함)이고 나머지는 전부 오프셋 없는 서버 로컬시각(Asia/Seoul)** 이다. 컨버터를 클래스로 못 박아 **새 DTO 를 추가할 때 어느 컨버터를 붙일지가 강제 선택지가 되게 한다.**

```dart
class ServerTime { static const kstOffset = Duration(hours: 9); }

/// LocalDateTime (오프셋 없음) — createdAt, filledAt, startedAt, endedAt, completedAt, occurredAt, executedAt
class KstDateTimeConverter implements JsonConverter<DateTime, String> {
  const KstDateTimeConverter();
  @override DateTime fromJson(String s) =>
      DateTime.parse('${s}Z').subtract(ServerTime.kstOffset).toLocal();
  @override String toJson(DateTime d) => d.toIso8601String();
}

/// Instant (Z 포함) — 캔들 time 전용
class InstantConverter implements JsonConverter<DateTime, String> { /* DateTime.parse(s).toLocal() */ }

/// LocalDate — snapshotDate, referenceDate, analysisStart/End. 타임존 변환을 하지 않는다.
class LocalDateConverter implements JsonConverter<DateTime, String> { /* DateTime.parse(s) */ }
```

nullable 필드는 `KstDateTimeConverter?` 짝을 별도로 둔다(시각은 3종 × nullable = 6개로 관리 가능한 규모다).

#### 4.5.3 enum 과 계약 정정

- **enum 은 서버 이름 그대로 쓴다**(`LOSS_CUT`, `CHASE_BUY_BAN`, `DAILY` …). 프론트 별칭 계층(§1.10.1)은 순수한 웹 관례이며 매핑 테이블만 하나 늘린다.
- **`@JsonKey(unknownEnumValue: X.unknown)`** 를 모든 enum 필드에 붙인다. 서버가 값을 추가해도 앱이 죽지 않는다.
- 한국어 라벨·단위(`%`/`회`)·색상·아이콘은 **표시 계층의 상수 맵 한 곳**에 모은다. `RuleType` 은 **5종 전부** 처리한다(과거 라운드가 손절·익절을 가질 수 있다). 단 라운드 **생성 화면**에서 고를 수 있는 것은 3종(`CHASE_BUY_BAN`/`AVERAGING_DOWN_LIMIT`/`OVERTRADING_LIMIT`)뿐이다 — 나머지 둘은 서버에 위반 판정 로직이 없다.
- **R4 의 12개 항목을 일괄 정정한다**: `UserProfileResponse.email` 제거 / 주문 내역 `status` 필드 제거 / 요청에서 `userId` 전면 제거 / 송금 커서는 `cursorTransferId: int`·`nextCursor: int?` / 캔들은 `time` 만 파싱 / 송금 상태는 `SUCCESS` 단일값 / `StartRoundResponse`(`userId`·`endedAt` 없음)와 `GetActiveRoundResponse`(있음)는 **별도 모델 2개**.

---

## 5. 화면별 구현 명세 (9개)

거래소 선택·랭킹 기간은 전부 **라우트 쿼리**에 유지한다(딥링크·복원이 공짜로 된다). 모든 목록에 `RefreshIndicator` 를 붙인다. 모든 실패는 **스낵바 또는 재시도 버튼**으로 알린다 — 웹의 "조용한 실패"(R9)를 전면 제거한다.

### 5.1 로그인 (`/login`)

| 항목 | 내용 |
|---|---|
| 위젯 | 로고(`Activity` 아이콘 + "Trypto") · 서브카피("큰 돈 잃을 걱정 없이 해보는 실전 리허설") · 카카오 버튼(`#FEE500`/글자 `#191600`/높이 48) · 구글 버튼(흰 배경 + 테두리 + 4색 SVG) · 오류 배너 |
| API | `POST /api/auth/{provider}/login` `{code, codeVerifier, clientType: "MOBILE"}` |
| 상태 | `AuthController`(`AuthStatus` 상태 기계). `exchanging` 동안 전체 차단 오버레이(로고 + 24×24 스피너 + "{카카오\|구글}로 로그인 중…") |
| 모바일 UX | **소셜 콜백 화면을 만들지 않는다.** 콜백은 `authenticate()` 의 `await` 반환값이다. 팝업·BroadcastChannel·bfcache 처리는 통째로 삭제한다 |
| 검증 실패 | §2.2.4 의 6종 메시지를 그대로 오류 배너에 표시한다 |
| 버튼 비활성 | `clientId` 또는 `redirectUri` 가 비면 비활성. 사유는 디버그 빌드에서만 노출 |

### 5.2 소셜 콜백 — 로그인 화면의 상태로 흡수

라우트가 아니다(§4.3.1). 웹의 `SocialCallbackPage` 는 중계·교환 전용 화면이며 UI 산출물이 스피너와 오류 카드뿐이다. 그 시각적 역할은 `exchanging_overlay.dart` 가 그대로 수행한다. **9개 화면 요구는 기능 기준으로 충족되며 라우트만 사라진다.**

### 5.3 라운드 생성 (`/round/new`)

| 항목 | 내용 |
|---|---|
| 구조 | **2단계 스텝 + 하단 고정 CTA.** 상단에 진행 표시(1/2, 2/2) |
| 1단계 · 자금 | 시드머니 카드(`₩` 대형 모노 + `formatKRW` 축약 보조 표시, 프리셋 100만/500만/1,000만/5,000만) · 긴급 자금 상한 카드(프리셋 10만/50만/100만). 커스텀 숫자 키패드(0~9, ⌫, 00) |
| 2단계 · 원칙 | 규칙 카드 3종. `CHASE_BUY_BAN`(슬라이더 1~50, 기본 15, `%`) / `AVERAGING_DOWN_LIMIT`(스테퍼 1~10, 기본 3, `회`) / `OVERTRADING_LIMIT`(스테퍼 1~50, 기본 10, `회/일`). 카드 전체가 스위치 |
| API | `POST /api/rounds` `{seeds: [{1, seed}, {2, 0}, {3, 0}], emergencyFundingLimit, rules: [...]}` — **`userId` 를 보내지 않는다** |
| 선제 검증 (R8) | **긴급 자금 상한 1,000,000원을 입력 단계에서 강제**(초과 시 "상한은 100만원입니다") · **동일 `ruleType` 중복 전송 차단**(서버가 400 이 아니라 500 을 낸다) · 시드 0 초과 시 거래소별 범위(국내 100만~5,000만, 해외 100~50,000) |
| 제출 조건 | `seed > 0 && emergencyLimit > 0 && 활성 규칙 ≥ 1` |
| 실패 | **서버 오류 메시지를 노출한다**(`ACTIVE_ROUND_EXISTS` → "이미 진행 중인 라운드가 있습니다"). 웹은 서버 메시지를 버린다 |
| 성공 | `activeRound` 갱신 + `totalRoundCount` 증가 → `/market` 으로 `go`(replace) |

### 5.4 마켓 (`/market`) — 앱의 심장

| 항목 | 내용 |
|---|---|
| 위젯 | 상단 고정 검색창(플레이스홀더 "코인명/심볼 검색 (초성 가능)") → 거래소 세그먼트(업비트/빗썸/바이낸스 + 부제 KRW/USDT) → 필터 칩(전체/상승/하락) → 주요 코인 가로 스크롤 카드(BTC/ETH/SOL) → **코인 리스트(화면 전체)** |
| 리스트 | `ListView.builder(itemExtent: 68)`. 컬럼: 코인명(심볼 13/w600 + 한글명 11) · 현재가 · 전일대비 배지 · 거래대금. `currentPrice <= 0` 이면 `-`, `volume <= 0` 이면 `-` |
| API | `GET /api/exchanges/{id}/coins`(진입·거래소 변경·포그라운드 복귀) |
| 실시간 | STOMP `/topic/tickers.{id}`. §4.2 의 규칙 전량 적용. 구독은 **거래소당 하나**이고 `TickerStore` 가 목록과 캔들 차트에 나눠 준다 — 웹은 목록과 차트가 같은 토픽을 각각 구독해 페이로드를 두 번 받지만(§4.3.9), **이식하지 않는다** |
| 검색 | 정적 목록 수신 시 **1회만** 초성·자모 색인 생성(`Map<symbol, (chosung, jamo)>`). **초성 질의는 `startsWith`, 자모는 `contains`** — 이 비대칭을 반드시 지킨다 |
| 정렬 | 기본 `volume desc`. 같은 키 재탭 → `desc↔asc` 토글, 다른 키 → 그 키 + `desc` 초기화. 시세 변동에 의한 재정렬은 **1초 스로틀 + 스크롤 중 동결** |
| 거래소 전환 | 검색어·필터·선택 코인 초기화, **정렬은 유지**. 티커 맵 비우고 새 토픽 구독 |
| 긴급 자금 | 상단 **접힌 배너**(남은 횟수만). 탭 → 바텀시트(프리셋 25/50/100%, `0 < amount <= limit` 실시간 검증, `idempotencyKey` UUID v4). **실패 시 스낵바**(웹은 조용히 실패한다) |
| 라운드 없음 | 상단에 "라운드 시작" 배너. 시세 조회는 계속되고 주문만 막힌다 |

**코인 상세** (`/market/coin/:symbol`, rootNavigator push)

캔들 차트는 **REST 캔들과 STOMP 티커를 함께 소비한다**(§4.3.1). 진행 중인 봉은 실시간 체결가로 만들고, 봉이 닫히면 서버 집계 값으로 교체한다. 정적 차트로 구현하면 이 화면은 요구를 충족하지 못한다.

*화면 골격*

- 상단: 심볼 · 현재가 · 등락률(**캔들이 아니라 실시간 티커 값**). 이 헤더와 캔들 `CustomPaint` 사이에 **`RepaintBoundary` 를 반드시 넣는다** — 헤더 텍스트는 티커가 올 때마다 갱신되고 캔들 레이어는 실시간 봉이 **보일 때만** 다시 칠해진다. 경계가 없으면 둘이 서로를 끌고 들어가 헤더 한 줄이 200개 캔들을 다시 칠한다.
- 간격 칩 6종(`1m`/`1h`/`4h`/`1d`/`1w`/`1M`, 기본 `1d`). 조회 개수와 최초 표시 개수는 §4.3.2 표 그대로. 상수 `LIVE_CANDLE_LIMIT = 4`, `RECONCILE_DELAY_MS = 15000`, `MIN_VISIBLE_COUNT = 12` 도 그 표에서 가져온다.
- 캔들 차트: `CustomPainter` **2레이어**(캔들·격자·축 / 크로스헤어·툴팁). 각각 `RepaintBoundary`. 롱프레스 드래그 중 캔들 레이어는 재페인트되지 않는다.
- 스케일·히트테스트·팬·줌은 **순수 함수 `CandleScale`** 로 분리한다(위젯 없이 테스트). `paddedMin = max(0, min - range×0.08)`, `candleWidth = clamp(slotWidth×0.62, 6, 16)`, `getX(i) = 20 + (i+0.5)×slotWidth`.
- 제스처: 수평 드래그 패닝(`movedCandles = (dx/slotWidth).round()`), 핀치 줌(앵커 비율 유지, `visibleCount` 를 `[12, 전체]` 로 clamp), **롱프레스 드래그 크로스헤어**(히트 테스트 x 허용 오차를 `candleWidth` → `slotWidth` 로 넓힌다 — 손가락 가림). 툴팁은 차트 **상단 고정 패널**.
- 하단 탭: **차트 / 거래내역**. 하단 고정 CTA 2개(`매수`/`매도`).

*① 두 출처의 소유권 — Riverpod 은 REST 만, 티커는 `TickerStore` 만*

| 데이터 | 소유자 | 갱신 빈도 | 결정 #1 과의 관계 |
|---|---|---|---|
| 서버 캔들 `List<Candle>` | Riverpod `FutureProvider.family<List<Candle>, CandleRequest>` | 진입·간격 변경·재조정 시 (분당 1회 미만) | 저빈도 전역이므로 Riverpod 그래프를 탄다 |
| 실시간 봉 `List<LiveCandle>`(≤4) | `TickerStore` 에 붙는 **`LiveCandleFolder`** | 초당 수백 | **Riverpod 을 통과하지 않는다.** 결정 #1 그대로 |

`CandleRequest` 는 `(exchangeCode, symbol, interval)` 의 값 동등 레코드다. 이것이 웹의 `requestKey` 를 대신한다(§4.3.7) — 코인·거래소·간격이 바뀌면 provider 키가 바뀌어 이전 캔들이 화면에 남지 않고, `LiveCandleFolder` 도 같은 키를 들고 있다가 키가 어긋나면 **빈 배열에서 다시 시작한다.**

*② `TickerStore` 에 원시 틱 관찰자 훅을 추가한다 (9단위에서 구현)*

`TickerStore._pending` 은 심볼당 **마지막 값만** 남기는 프레임 버퍼다(§4.2.4). 여기서 캔들을 접으면 **한 프레임 안의 체결이 버려져 봉의 고가·저가가 실제보다 얕아진다.** 따라서 접기는 `ingest` 안에서 **틱마다 동기로** 수행한다. `ingest` 는 `setState` 를 부르지 않으므로 이 훅은 리빌드를 유발하지 않는다.

```dart
// TickerStore (§4.2.4) 에 추가하는 것은 이 두 줄뿐이다.
void Function(Ticker)? _rawObserver;                 // 캔들 차트가 열려 있는 동안에만 non-null
void setRawObserver(void Function(Ticker)? o) => _rawObserver = o;

void ingest(List<Ticker> ticks) {
  if (!_active) return;
  for (final t in ticks) {
    if (!_rows.containsKey(t.symbol)) continue;
    _rawObserver?.call(t);        // ★ 매 틱. 캔들 접기(O(1), 할당 없음). 그리기는 하지 않는다
    _pending[t.symbol] = t;       // 기존 last-wins 프레임 버퍼 (그리기용)
  }
  _schedule(rescheduling: false);
}
```

- **프레임당 1회 flush 는 그대로다.** 훅은 `_pending` 도 flush 도 건드리지 않는다. 바뀌는 것은 "틱을 한 번 더 들여다본다"는 것뿐이다.
- 관찰자는 **하나뿐**이다(차트는 한 번에 하나만 열린다). 리스너 리스트를 만들지 않는다.
- 코인 상세는 마켓 셸 브랜치 **위로 push** 되므로 마켓 탭은 계속 선택 상태다 → `_active == true`, 구독 유지. **차트가 별도로 토픽을 구독하지 않는다.**
- 차트 화면 `dispose` 에서 `setRawObserver(null)`.

*③ `LiveCandleFolder` — 매 틱, 배열을 새로 만들지 않는다*

```dart
class LiveCandle {                       // ★ 가변 객체. 같은 봉 안에서는 필드만 고쳐 쓴다
  final DateTime bucket;
  final double open;                     // 그 봉에서 처음 본 체결가 (서버 시가가 아니다)
  double high, low, close;
  LiveCandle(this.bucket, double p) : open = p, high = p, low = p, close = p;
}

class LiveCandleFolder {
  CandleRequest? key;
  final List<LiveCandle> live = [];                  // 최대 4개
  final ValueNotifier<DateTime?> openedBucket = ValueNotifier(null);  // 봉이 바뀔 때만 발화
  int revision = 0;                                  // 값이 바뀌었는지 표시. flush 때만 읽는다
  bool dirty = false;

  void fold(Ticker t) {                              // ← TickerStore.ingest 가 매 틱 호출
    if (t.symbol != key!.symbol) return;             // 배열에서 자기 심볼만 고른다
    if (!t.price.isFinite || t.price <= 0) return;
    final b = normalizeCandleTime(t.timestamp, key!.interval);   // §4.3.5.1
    final opened = live.isEmpty ? null : live.last;

    if (opened != null && opened.bucket == b) {      // ① 같은 봉 → 제자리 갱신. 할당 0
      if (t.price > opened.high) opened.high = t.price;
      if (t.price < opened.low)  opened.low  = t.price;
      opened.close = t.price;                        // open 은 건드리지 않는다
      dirty = true;
      return;
    }
    if (opened != null && b.isBefore(opened.bucket)) return;      // ② 늦게 온 체결 → 버린다

    live.add(LiveCandle(b, t.price));                // ③ 새 봉 (간격당 1회. 매 틱이 아니다)
    if (live.length > 4) live.removeAt(0);           // LIVE_CANDLE_LIMIT
    dirty = true;
    openedBucket.value = b;                          // 재조정 타이머를 다시 건다 (§⑤)
  }
}
```

`fold` 는 **할당을 하지 않는다**(새 봉이 열리는 순간만 예외). 캔들 배열도, 병합 배열도 새로 만들지 않는다. 규칙 ①②③ 은 §4.3.5.2 의 `foldTick` 과 정확히 같다.

*④ 병합은 배열이 아니라 인덱스 뷰다 — `MergedCandles`*

웹의 `mergeLiveCandles` 는 매번 새 배열을 만든다(React 는 그래야 리렌더된다). Flutter 는 그럴 필요가 없다. **병합 결과를 materialize 하지 않고 인덱스로 읽는 뷰**로 둔다.

```dart
class MergedCandles {                   // §4.3.5.3 의 규칙을 '읽을 때' 적용한다
  final List<Candle> server;            // 불변 (Riverpod)
  final List<LiveCandle> live;          // 가변 (Folder)
  late final int _tailFrom;             // live 중 서버 뒤에 붙는 첫 인덱스
  late final bool _overlapsLast;        // live 가 서버 마지막 봉과 같은 구간을 갖는가

  int get length => server.length + (live.length - _tailFrom);

  Candle operator [](int i) {           // O(1)
    final lastIdx = server.length - 1;
    if (i < lastIdx) return server[i];                       // 과거 → 서버 그대로
    if (i == lastIdx) {                                      // 서버 마지막 봉
      if (!_overlapsLast) return server[i];
      final s = server[i], l = live[_tailFrom - 1];
      return Candle(s.time, s.open,                          // ★ 시가는 서버 값
          max(s.high, l.high), min(s.low, l.low), l.close);  // ★ 종가는 최신 체결가
    }
    final l = live[_tailFrom + (i - server.length)];         // 서버보다 미래 → 실시간 봉
    return Candle(l.bucket, l.open, l.high, l.low, l.close);
  }
}
```

- `_tailFrom` / `_overlapsLast` 는 **구조**다. `server` 가 교체되거나 **새 봉이 열릴 때만** 다시 계산한다(≤4회 비교). 같은 봉 안의 체결은 구조를 바꾸지 않고 값만 바꾸므로 재계산하지 않는다.
- 페인터는 **보이는 구간만**(≤120개) 인덱스로 읽는다. 틱당 배열 복사가 0이다.
- 서버 캔들이 0개여도 동작한다(`_tailFrom = 0` → 전부 실시간 봉).

*⑤ 그리기는 프레임당 1회 — `TickerStore` 의 flush 에 얹는다*

```dart
void _flush(Duration _) {                 // §4.2.4 의 기존 flush. 아래 3줄이 얹힌다
  ...                                     // 기존 행 갱신 · 플래시 스윕
  final f = _folder;
  if (f != null && f.dirty) { f.dirty = false; f.revision++; _chartRevision.value = f.revision; }
}
```

- `_chartRevision` 은 `ValueNotifier<int>` 하나다. 차트는 `ValueListenableBuilder` 로 이것만 구독한다. **초당 수백 틱이 들어와도 알림은 프레임당 최대 1회**다.
- `CandlePainter.shouldRepaint` → `old.revision != revision || 뷰포트가 바뀌었나`.
- **화면 밖 실시간 봉은 아예 다시 칠하지 않는다.** 과거로 패닝해(`followingLatest == false`) 실시간 봉이 표시 구간 밖이면 `revision` 이 올라도 페인터는 그것을 무시한다:
  `liveVisible = endIndex > (length - (live.length - _tailFrom) - 1)` — 거짓이면 `shouldRepaint` 는 `false` 다. y 스케일도 보이는 구간만으로 정해지므로(§4.3.3) 결과가 같다.
- 크로스헤어·툴팁은 **2번 레이어**다. 틱이 캔들 레이어를 갱신해도 크로스헤어 레이어는 다시 칠하지 않는다(그 반대도).

*⑥ 뷰포트 — 최신 봉 추종 (§4.3.8)*

- `followingLatest`(기본 `true`)이면 오른쪽 끝은 항상 `merged.length` 다. 새 봉이 열리면 화면이 저절로 따라간다.
- 사용자가 과거로 패닝하면 `followingLatest = false` 가 되고 `anchorEndIndex` 에 멈춘다. 이후 봉이 늘어나도 **보고 있는 구간은 흔들리지 않는다.**
- 패닝·핀치줌은 매번 `followingLatest = (boundedEndIndex >= merged.length)` 를 다시 계산한다 → **오른쪽 끝까지 되밀면 추종이 재개된다.**
- `endIndex = followingLatest ? merged.length : min(anchorEndIndex, merged.length)`. `min` 이 재조정으로 서버 캔들 수가 줄어드는 경우를 막는다.

*⑦ 재조정 — 닫힌 봉을 서버 값으로 교체 (§4.3.6)*

```dart
// openedBucket(ValueNotifier) 이 바뀔 때만 발화한다 — 틱마다가 아니다
_folder.openedBucket.addListener(() {
  _reconcileTimer?.cancel();
  if (_folder.openedBucket.value == null) return;
  _reconcileTimer = Timer(const Duration(milliseconds: 15000), () {
    ref.invalidate(candlesProvider(_request));   // REST 재조회 → 서버 캔들만 교체
  });
});
```

- 봉이 새로 열렸다 = 직전 봉이 닫혔다. 15초 뒤면 서버(InfluxDB 집계 주기 1분 + 오프셋 10초)가 그 봉을 확정했을 가능성이 높다.
- **`live` 를 비우지 않는다.** 새 서버 캔들이 그 봉을 포함하면 `MergedCandles` 의 읽기 규칙이 낡은 실시간 봉을 자동으로 흡수·무시한다. `LIVE_CANDLE_LIMIT = 4` 가 개수 상한을 준다.
- 재조회가 실패하거나 빈 배열이면 **아무것도 하지 않는다**(실시간 봉이 자리를 지킨다). `dispose` 와 `CandleRequest` 변경 시 타이머를 취소한다.
- **뷰포트·호버 상태는 건드리지 않는다.** 보고 있는 구간이 재조회로 튀면 안 된다.

*⑧ 거래량이 없다는 사실 (§4.3.10)*

캔들에 거래량 필드가 없어서 **체결가 하나로 봉을 완전히 만들 수 있다.** 티커의 `quoteTurnover` 는 24시간 누적 거래대금이라 봉 단위로 환산할 수 없고, 서버가 폭주 시 메시지를 버리므로(§3.1.7) 델타 누산도 불가능하다 — 거래량 칸이 있었다면 실시간 봉은 원리적으로 정확히 만들 수 없었다. 결과적으로 `Candle` 과 `LiveCandle` 이 같은 필드 집합을 가지며(위 ④의 `operator []` 가 성립하는 이유), 가격 축 하나만 그리면 된다. **거래량 서브차트를 만들지 않는다.**

**주문 바텀시트** (`DraggableScrollableSheet`, `initialChildSize: 0.75`, `maxChildSize: 0.95`)

- 주문 대상 해석(§1.10.2): `exchangeKey → exchangeId`(상수) → `walletId`(활성 라운드 `wallets`) → `exchangeCoinId`(코인 목록 캐시, 대소문자 무시). 실패 사유 3종(`NO_ROUND`/`COIN_UNLISTED`/`LOOKUP_FAILED`)별 경고 문구.
- `GET /api/orders/available` 을 BUY/SELL **병렬 2회** 호출.
- 입력 노출 규칙: 지정가 매수·매도(가격·수량·총액) / **시장가 매수(총액만, 수량 숨김)** / **시장가 매도(수량만, 총액 숨김)**.
- 수량↔총액 연동은 **지정가에서만**(`Decimal` 연산). 비율 버튼 10/25/50/100%. 가격 스텝 ±1,000.
- 전송: `POST /api/orders`. **시장가 매수의 `price` 는 가격이 아니라 총액이다.** `clientOrderId` 는 UUID v4, **재시도 시 같은 값 재사용**.
- 성공 → 입력 비움 + `available` 재조회 + (거래내역 탭 열려 있으면) 목록 재조회.
- 수수료·최소 주문 표시는 **거래소별 실제 값**(업비트 0.05% / 빗썸 0.25% / 바이낸스 0.1%, KRW 5,000 / USDT 5). 웹은 하드코딩이라 바이낸스에서도 "최소 주문 5,000 USDT" 로 나온다.

**거래내역 탭**: 필터(체결/미체결), `GET /api/orders?...&size=20` 커서 무한 스크롤(`cursorOrderId`). **응답에 `status` 가 없으므로 요청 필터값을 표시에 쓴다.** `PENDING` 항목에 취소 버튼 → `POST /api/orders/{id}/cancel {walletId}`.

### 5.5 포트폴리오 (`/portfolio`)

| 항목 | 내용 |
|---|---|
| 구조 | `CustomScrollView` — ① 요약 카드 ② 자산 구성(도넛) ③ 정렬 칩 ④ 보유 종목 **카드 리스트** |
| API | `GET /api/wallets/{walletId}/portfolio` 1회. **폴링·WS 없음** → `RefreshIndicator` 필수 |
| 요약 계산 | 서버가 합계를 주지 않는다. `totalBuy = Σ(avgBuyPrice×qty)`, `totalEval = Σ(currentPrice×qty)`, `totalAsset = cash + totalEval`, `profitLoss = totalEval - totalBuy`, `profitRate = totalBuy > 0 ? profitLoss/totalBuy×100 : 0`. **전부 `Decimal`** |
| 도넛 | `CustomPainter` + `Canvas.drawArc`. **12시 시작·시계 방향·`StrokeCap.butt`**·두께 28(선택 34)·반지름 73. 세그먼트: 현금 맨 앞(있을 때만) + 코인 value 내림차순, 7개 이상이면 상위 5 + "기타". hover 대신 **조각/범례 탭** 토글 |
| 카드 | 상단(심볼 + 한글명 / 수익률 pill) · 중단(평가금액 / 평가손익) · 하단(보유수량 / 평균매수가 / 현재가). 탭 → 마켓 상세로 이동 |
| 정렬 | 기본 **평가금액 내림차순**(웹은 응답 순서 그대로라 기준이 없다). 칩 탭 → 바텀시트 |
| 빈 상태 | "보유 중인 코인이 없습니다." + `[코인 사러 가기]` → 마켓. 라운드 없음 → `NoRoundNotice` |

### 5.6 입출금 (`/wallet`)

| 항목 | 내용 |
|---|---|
| API | `Future.wait` 3개: `GET /api/wallets/{id}/balances`, `GET /api/exchanges/{id}/coins`, `GET /api/wallets/{id}/transfers?size=20` |
| **R9 결함 수정** | 코인 `currentPrice` 를 **`/coins` 응답의 `price`** 로 채운다(웹은 0 고정이라 총자산·환산액·소액 제외·정렬이 전부 무의미하다) |
| 총자산 카드 | `Σ((available+locked) × currentPrice)`. 눈 아이콘 토글(숨김 시 `••••••••`). 잠금이 있을 때만 `(사용 가능 … / 잠금 …)` 보조 줄 |
| 자산 리스트 | 상장 코인 전량(600여). 검색(심볼·한글명 부분일치) + `FilterChip("소액 제외")`(KRW 1,000 / USDT 1 미만 제외). 잔고 0 코인은 흐리게 + `—` |
| 자산 상세 | 카드 탭 → 바텀시트. 총 수량 · 환산액 · **[출금] 버튼(코인만)** · 잔고 상세 · 해당 코인 송금 내역 |
| 송금 | **전체 화면 3단계 마법사**. ①도착 거래소(후보 1개면 자동 선택) ②수량(실시간 검증, `[25%][50%][최대]`) ③확인(출발→도착, 수량, **출금 후 남는 가용 잔고**) |
| 멱등키 | **2단계 진입 시 1회 생성해 화면 상태에 보관**하고 재시도 시 재사용한다. UUID v4 필수 |
| 실패 | §5.4.4 의 5개 코드를 문맥 문구로 매핑(`SAME_WALLET_TRANSFER` → "같은 거래소로는 송금할 수 없습니다") |
| 송금 내역 | 전체 내역 페이지(앱바 히스토리 아이콘). 유형 필터는 **서버 파라미터 `type`** 으로. **상태 필터 제거**(서버는 `SUCCESS` 단일값 — `완료` 배지만). 무한 스크롤은 **`cursorTransferId: int`**(웹은 이름을 틀려 페이지네이션이 동작하지 않는다) |

### 5.7 랭킹 (`/ranking?period=`)

| 항목 | 내용 |
|---|---|
| API | `Promise.allSettled` 대응: `GET /api/rankings`(비인증 가능) · `/stats`(비인증 가능) · `/me`(인증 시에만). **목록이 실패하면 전체 에러 화면**, 통계·내 랭킹은 개별 실패를 허용해 `null` 로 둔다 |
| 단위 | **`profitRate` 는 퍼센트 값 그 자체**(12.34 = +12.34%). 티커의 `changeRate`(비율)와 다르다 |
| 구조 | 기간 세그먼트(일간/주간/월간) → 내 랭킹 카드 → 통계 3분할 타일 → Top3 → 리스트(무한 스크롤 `cursorRank`) |
| 내 랭킹 | `data: null` 허용(미집계) → "아직 순위가 없습니다. / 거래를 시작하면 다음 집계에 반영됩니다." 스크롤로 카드가 나가면 **하단 스티키 바** |
| 랭커 포트폴리오 | 행 탭 → **바텀시트**(ExpansionTile 은 스크롤 위치를 흔든다). 유저별 1회 캐싱. 비중은 `asRatio(v) = v > 1 ? v/100 : v` 정규화 후 `(ratio×100).toFixed(1)%` |
| **100위 초과** | `rank > 100` 행은 **탭을 막고** "포트폴리오는 100위까지 공개됩니다." 스낵바. 불필요한 403 요청을 보내지 않는다 |
| 집계 전 | 목록이 비면 자리표시 카드 3장(파선 테두리, `--%`, "집계 대기") + "아직 집계된 랭킹이 없습니다" + `[거래하러 가기]`. 통계는 플레이스홀더가 우선한다 |

### 5.8 투자 복기 (`/regret?exchange=`)

| 항목 | 내용 |
|---|---|
| API | `GET /api/rounds/{roundId}/regret` + `/regret/chart`(둘 다 `exchangeId` 필수, 병렬) |
| **개선** | 웹은 `wallets[0]` 고정이다. **거래소 선택기를 추가**하고 응답의 `exchangeName`·`currency` 를 표기해 통화 혼동을 막는다 |
| 히어로 | "놓친 수익" 대형 숫자 + 한 줄 해설. 그 아래 3-stat 타일(실제 / 규칙 준수 시 / 위반 건수). 비율은 **`toStringAsFixed(2)`** 로 다듬는다 |
| 차트 | **fl_chart `LineChart`**. 실제(실선, primary, 2.2) / 시뮬레이션(점선, negative, 1.8) / BTC 홀드(실선, `#f7931a`, 1.5, 알파 0.5) + 위반 마커(반지름 3, 흰 테두리). Y 패딩 `(max-min)×0.1`, 눈금 5등분. 스냅샷 2개 미만이면 그리지 않는다 |
| 시뮬레이션 | `RULE_IMPACT_WEIGHTS = {LOSS_CUT:0.30, CHASE_BUY_BAN:0.25, PROFIT_TAKE:0.20, OVERTRADING_LIMIT:0.15, AVERAGING_DOWN_LIMIT:0.10}`. `sim[i] = (actual[i] + (ruleFollowed[i]-actual[i]) × totalWeight).round()`. **동일 상수·동일 반올림으로 이식해야 웹과 값이 일치한다** |
| BTC 홀드 | 하드코딩 `0%` 대신 `btcHoldValues` 의 `(last/first - 1) × 100` 을 계산해 채운다 |
| 규칙 토글 | 세로 체크박스 → **가로 스크롤 `FilterChip` 행**(규칙 색 + 라벨 + 위반 수 배지). 롱프레스 → 임계값·위반 상세 바텀시트 |
| 위반 목록 | 필터 세그먼트(전체/손실/수익 + 건수, **`profitLoss == 0` 은 수익**). **2행 레이아웃**(1행: 코인·날짜·손익 / 2행: 규칙 태그 가로 스크롤). 정렬하지 않는다(서버 순서 그대로). **감정 배지는 구현하지 않는다**(서버에 필드가 없다) |
| 마커 매칭 | 문자열이 아니라 **`snapshotDate`(DateTime)로 매칭**한다 |
| 집계 전 | "복기 리포트는 매일 밤 집계됩니다. 내일 다시 확인해 주세요." **스켈레톤을 쓰지 않는다**(빈 상태를 로딩으로 오인시킨다) |
| 고지 | 하단에 항상 `* 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다.` |

### 5.9 마이페이지 (`/mypage`, 셸 밖 push)

| 항목 | 내용 |
|---|---|
| 구조 | 세로 1열 카드 스택: 프로필 → 현재 라운드 → 피드백 → 로그아웃 → (회원 탈퇴) |
| 프로필 | 닉네임 행 탭 → **하단 시트**(입력 + 저장). **2~20자를 입력 단계에서 검증**하고 실패를 스낵바로 알린다. 가입일은 `GET /api/users/me` 의 `createdAt`(`2026년 7월 15일`) |
| 현재 라운드 | `라운드 {n}` + 상태 배지. 통계는 **2열 + 1열**(시드머니 전체 폭 / 상한·남은 충전). 금액은 축약하지 않는다(`toLocaleString + "원"` 대응). 투자 원칙 목록은 **5종 전부** 처리 |
| 라운드 종료 | 확인은 **하단 시트 + 파괴적 액션(빨강)**, 완료 안내는 **다이얼로그 유지**. `POST /api/rounds/{id}/end`(바디 없음) → `clearRound()` → `/market`. **실패 시 스낵바** |
| 피드백 | 5행 textarea, `maxLength: 1000`. **`trim().length` 기준 20~1000자**. 카운터는 입력창 우측 하단, 전송 버튼은 키보드 위 고정 바 |
| 로그아웃 | `POST /api/auth/logout`(바디 없음). **API 실패와 무관하게** 로컬 상태를 비운다. 세션·라운드·코인 캐시·티커 구독을 모두 정리한다 |
| 회원 탈퇴 (R11) | `DELETE /api/users/me`. iOS 심사 지침상 필수. 확인 시트 2단계 → 로컬 전량 삭제 → `/login`. 재가입 시 `SIGNUP_RESTRICTED`(403) 안내 |

---

## 6. 구현 순서

각 단위는 **컴파일되고 실행되며, 커밋 하나**다. 외부 의존(제공자 콘솔, 백엔드 `MOBILE` 분기)이 걸린 5단계와 성능 리스크가 걸린 9단계를 앞으로 당겨 위험을 먼저 소진한다.

| # | 단위 | 완료 판정 기준 |
|---|---|---|
| **1** | **스캐폴드** — `flutter create`(android/ios 만), `analysis_options`, 디렉토리 골격, `Env`(`--dart-define`), Android/iOS 플랫폼 설정(§9), 스플래시 | 에뮬레이터에서 스플래시가 뜨고 `flutter analyze` 가 통과한다 |
| **2** | **디자인 시스템** — `ThemeData`(§8.6.1), `TryptoColors`(ThemeExtension), `TextTheme`(§8.6.3), 폰트 3종 번들, `core/widgets/` 공용 위젯, 컴포넌트 카탈로그 화면 1개 | 버튼·배지·카드·입력·세그먼트가 웹과 같아 보인다 |
| **3** | **포매터·한글·시각·상수** — `formatters.dart`(§8.5 전량, **`NumberFormat` 최상위 캐시**), `hangul.dart`, `ServerTime`, `DecimalX`, `ExchangeIds`, `OrderPolicy` | `flutter test` 초록. §8.5 의 예시 표 60여 줄이 전부 통과한다. **포매터 함수 본문에 `NumberFormat(` 생성자가 없다** |
| **4** | **네트워크 코어** — Dio, 인터셉터 3종, `ApiException`, `SessionStore`, 컨버터 3종, `CursorPage`, 쿼리 정제. 디버그 화면에서 `GET /api/exchanges/1/coins` 호출 | `http_mock_adapter` 로 봉투 4경로(`200 SUCCESS`/`201 CREATED`/`201 SUCCESS`/비봉투)·401 2종(`UNAUTHENTICATED` → 폐기, `SOCIAL_LOGIN_FAILED` → **유지**)·`ROUND_NOT_ACTIVE` → `null` 통과. 실 서버에서 코인 600개가 온다 |
| **5** | **인증** ⚠️외부 의존 — PKCE, `AuthConfig`, `flutter_web_auth_2`, `clientType: "MOBILE"`, `AuthController`(부팅 시 `/users/me` 복구), 로그인 화면 + 교환 오버레이. 라우트는 임시 `home:` 분기 | **에뮬레이터에서 카카오·구글 로그인이 실제로 뚫리고, 앱 재시작 후에도 세션이 살아 있다.** 최대 리스크 구간 |
| **6** | **라우터·가드·탭 셸** — `guard()` 순수 함수 + 표 기반 테스트, `GoRouter`(`refreshListenable`), `StatefulShellRoute` 5탭 + `/splash` + `*` 폴백. 임시 `home:` 제거 | 가드 테스트 전량 통과. **콜드 스타트에서 redirect loop 가 나지 않는다.** 로그아웃 → `/login` 자동 이동 |
| **7** | **라운드 상태 + 라운드 생성** — `RoundController`(active/summary 병렬, `ROUND_NOT_ACTIVE`→`null`), `round_rules.dart` + 테스트, 라운드 생성 2단계 화면(상한 100만 강제, 규칙 중복 차단, 서버 오류 노출) | **신규 계정이 로그인 → `/round/new` → 생성 → `/market` 까지 관통한다.** `walletId` 를 확보한다 |
| **8** | **마켓 (정적, REST only)** — 코인 카탈로그 + 캐시, 검색(초성/자모), 필터, 정렬, `ListView.builder(itemExtent: 68)`, 행 3분할 + 고정 폭 셀 | WS 없이 스냅샷만으로 600행 목록이 뜨고, 검색·정렬·필터가 동작한다 |
| **9** | **실시간 (STOMP + TickerStore)** ★성능 게이트 — 백오프·구독 복원·`_intentionalClose`·lifecycle(`paused` 한정)·connectivity 디바운스, 16ms flush, 플래시 스윕, 1초 스로틀 재정렬 + 스크롤 중 동결, 탭 비활성 시 unsubscribe. **+ 원시 틱 관찰자 훅**(`setRawObserver` — §5.4 ②): 티커 소비자는 목록 하나가 아니라 **목록과 캔들 차트 둘**이며, 차트는 프레임 버퍼가 아니라 **매 틱**을 받아야 한다. 훅과 `_chartRevision` 알림 지점까지 이 단위에서 만든다(관찰자는 10단위에서 붙인다) | **아래 §7 측정 기준 3개를 전부 통과한다. 통과하지 못하면 10단계로 넘어가지 않는다.** 성능 계약 테스트 + 행 격리 위젯 테스트 통과. 관찰자를 등록하지 않은 상태에서 목록 성능이 9단위 이전과 동일하다(`_rawObserver == null` 경로) |
| **10** | **코인 상세 + 실시간 캔들 차트** — `CandleScale`·`normalizeCandleTime`·`LiveCandleFolder.fold`·`MergedCandles` **전부 순수 Dart** + 단위 테스트, REST 캔들 `FutureProvider.family`(키 = `CandleRequest`), `TickerStore.setRawObserver` 등록/해제, `_chartRevision` 구독(프레임당 1회 재계산), 15초 재조정 타이머, `CustomPainter` 2레이어, 간격 칩 6종, 패닝·핀치줌·롱프레스 크로스헤어, 가로 풀스크린, 헤더/차트 `RepaintBoundary` | ① 봉 접기 단위 테스트: 한 프레임에 `[100, 130, 90, 110]` 이 들어오면 그 봉이 `high=130, low=90, close=110` 이다(**프레임 버퍼로 접으면 `high=low=close=110` 이 되어 실패한다**). ② `mergeLiveCandles` 3분기(과거 무시 / 같은 봉 = 서버 open + 실시간 high·low·close / 미래 봉 append)와 서버 캔들 0개 경우가 테스트로 고정된다. ③ Timeline: 초당 수백 틱에서 **캔들 레이어 repaint 가 프레임당 1회를 넘지 않는다.** ④ 크로스헤어 드래그 중 캔들 레이어가 재페인트되지 않는다. ⑤ 과거로 패닝해 실시간 봉이 화면 밖이면 **repaint 가 0이다.** ⑥ 새 봉이 열리고 15초 뒤 REST 재조회가 정확히 **1회** 나가고, 보고 있던 구간이 그대로다 |
| **11** | **주문** — `OrderTarget`·`OrderForm`(순수) + 테스트, 주문 바텀시트(4조합), `available` 병렬 조회, `POST /api/orders`, 성공 시 REST 재조회, 거래내역 탭(무한 스크롤 + 취소) | 시장가 매수/매도·지정가 매수/매도 4종이 서버에서 전부 200/201. 시장가 매수 바디에 `volume` 이 없고 `price` 에 총액이 실린다 |
| **12** | **긴급 자금** — 마켓 상단 접힘 배너 + 바텀시트, UUID v4 멱등키, 상한 검증, **실패 스낵바** | 3회 소진 후 버튼이 비활성된다 |
| **13** | **포트폴리오** — 요약(`Decimal`), 도넛 `CustomPainter`, 보유 카드 리스트, 정렬 시트, pull-to-refresh | 보유 0개·현금만 있는 경우에도 도넛이 정상 렌더된다 |
| **14** | **입출금** — 총자산 카드, 자산 리스트(**R9 `price` 결함 수정**), 상세 바텀시트, 송금 3단계 마법사, 전체 내역 페이지(`cursorTransferId` 무한 스크롤) | 송금 성공 후 양쪽 지갑 잔고가 반영되고, 내역 다음 페이지가 실제로 이어진다 |
| **15** | **랭킹** — 기간 세그먼트, 3병렬 조회, 내 랭킹(+스티키 바), 통계, Top3, 무한 스크롤, 랭커 포트폴리오 시트(100위 초과 선제 차단) | 비로그인 상태에서도 목록·통계가 뜨고, 미집계 사용자에서 크래시가 없다 |
| **16** | **투자 복기** — 거래소 선택기, fl_chart 3선 + 마커, 규칙 칩 토글 + 시뮬레이션 보간(+테스트), 위반 목록 2행, BTC 홀드 수익률 계산, 집계 전 안내 | 배치 전 상태에서 빈 리포트가 로딩으로 오인되지 않는다. 시뮬레이션 값이 웹과 일치한다 |
| **17** | **마이페이지 + 마무리** — 프로필·닉네임(2~20자), 라운드 종료 시트 + 완료 다이얼로그, 피드백(20~1000자), 로그아웃, 회원 탈퇴(R11), **R9 의 조용한 실패 5건 전면 제거 점검**, README | 9개 화면 전량 순회에 미처리 실패가 없다. 로그아웃 시 세션·라운드·코인 캐시·티커 구독이 모두 정리된다 |

### 순서의 근거

- **5(인증)를 6(라우터)보다 먼저** 둔다. 가드는 auth/round 를 입력으로 받으므로 상태가 먼저 존재해야 한다. 5단계에서는 임시 `home:` 분기로 로그인 성공을 눈으로 확인하고, 6단계에서 라우터로 교체한다. 두 커밋 모두 독립적으로 동작한다.
- **8(정적 마켓)과 9(실시간)를 분리한다.** 목록·검색·정렬이 REST 만으로 완성된 상태에서 WS 를 얹어야, 프레임이 떨어졌을 때 원인이 배칭 계층임이 **확정**된다. 한 커밋에 넣으면 범인을 좁힐 수 없다.
- **10(차트)은 9(실시간)에 의존한다.** 캔들 차트는 정적 화면이 아니라 `TickerStore` 의 **두 번째 소비자**다(§5.4). 9단위에서 원시 틱 관찰자 훅과 프레임당 1회 알림 지점을 만들어 두지 않으면 10단위는 티커를 다시 구독하는 방향으로 흘러가고, 그 순간 토픽 구독이 둘로 늘어난다(웹의 결함, §4.3.9).
- **11(주문)이 10(차트)보다 뒤인** 이유는 주문이 코인 상세 화면 위에 얹히는 바텀시트이기 때문이다.
- **17 에 회원 탈퇴를 포함한다.** 나중에 붙이면 세션 정리·재가입 제한(`SIGNUP_RESTRICTED`) 처리가 누락되기 쉽다.

---

## 7. 테스트 전략

기준은 하나다 — **틀리면 조용히 틀리고, 눈으로는 못 잡는 것만 테스트한다.** 화면 9개의 레이아웃은 눈으로 본다.

| # | 대상 | 도구 | 왜 여기인가 |
|---|---|---|---|
| **1** | **포매터 11종 · 한글 · 시각 · Decimal** | 순수 단위 | §8.5 의 입출력 표가 그대로 케이스다. `formatKRW(15000) → "2만원"`, `formatKRW(999999999) → "9억 10,000만원"`, `formatChangeRate(0) → "0.00%"`(+ 없음), `formatQuantity(-5) → "-5.0000"` 같은 반직관 케이스는 테스트 없이는 **반드시** 어긋난다. 한글은 초성=`startsWith`/자모=`contains` 비대칭. Decimal 은 `1e-8` 지수 표기 경계 |
| **2** | **봉투 언랩 · 401 분리 · `ROUND_NOT_ACTIVE`** | `http_mock_adapter` | 성공 판정이 3중 조건이다. `401 UNAUTHENTICATED` → 세션 폐기 / **`401 SOCIAL_LOGIN_FAILED` → 세션 유지** / `409 ROUND_NOT_ACTIVE` → 예외가 아니라 `null`. **계약 회귀의 유일한 방어선** |
| **3** | **`TickerStore` 성능 계약** ★ | 순수 단위 | 프레임 드랍은 눈으로 회귀를 잡을 수 없다. 불변식을 테스트로 고정한다: ① 1,000틱 ingest 후 flush 전 알림 **0회**, flush 후 **정확히 1회** ② 같은 프레임의 동일 심볼은 **마지막 값만** ③ 목록 밖 심볼은 **버린다** ④ 값이 안 바뀐 심볼은 **알림이 가지 않는다**(`CoinRowState.==` 계약) ⑤ 리스너 없는 심볼은 **플래시를 계산하지 않는다** ⑥ 플래시는 100ms 후 해제되고, 틱이 끊겨도 잔여 플래시가 꺼진다 ⑦ 거래소 전환 시 버퍼·notifier 가 비워진다 |
| **4** | **`guard()` 라우트 가드** | 순수 단위 | 입력 5개 × 경로의 조합 표. 신규 사용자가 `/round/new` 를 건너뛰거나, 콜드 스타트가 redirect loop 로 터지는 사고가 여기서 난다 |
| **5** | **`OrderForm` 주문 조립** | 순수 단위 | `MARKET+BUY` 는 `volume` 금지·`price` = 총액, `MARKET+SELL` 은 `price` 금지. 4조합 + 각 위반. 틀리면 400 |
| **6** | **`CandleScale` · 복기 시뮬레이션 · 라운드 규칙** | 순수 단위 | `getX`/`getY`/히트테스트/줌 앵커 보존. `RULE_IMPACT_WEIGHTS` 가중치 합 × 선형 보간 + `round()` — "웹과 값이 일치해야 한다"가 명시적 요구다(§6.3.4). 긴급 상한 100만·규칙 중복 차단(R8) |
| **7** | **코인 행 격리** | 위젯 (1개) | 심볼 A 에만 틱을 flush 했을 때 **A 의 숫자 위젯 빌드 카운터만 증가하고 이웃 행 B 는 증가하지 않는다.** 설계의 핵심 주장을 코드로 못 박는 유일한 장치 |
| **8** | **주문 바텀시트** | 위젯 (1개) | 시장가 매수 선택 시 수량 필드가 사라지고, 전송 바디에 `volume` 이 없고 `price` 에 총액이 실린다 |

### 하지 않는 것

- **화면 9개의 위젯 테스트** — 레이아웃 변경마다 깨지고, 눈으로 보면 안다.
- **골든 테스트** — 폰트 렌더링 차이로 환경마다 깨진다.
- **`integration_test` / E2E** — 실 서버 세션 + OAuth 콘솔에 의존해 재현 가능한 픽스처를 만들 수 없다. 로그인 흐름은 에뮬레이터에서 수동 검증한다.
- **Repository 위임 테스트** — `dio.get()` 을 부르는지 검증하는 테스트는 구현을 복사할 뿐이다.
- **커버리지 목표 숫자** — 숫자를 채우려고 의미 없는 테스트가 생긴다.

목표: `flutter test` 가 5초 안에 끝난다. `flutter analyze` 통과를 커밋 조건으로 둔다.

### 성능은 테스트가 아니라 측정으로 확인한다 (9단계 통과 조건)

`flutter run --profile` + DevTools Timeline 에서 **업비트 600코인 + 티커 수신 중 플링 스크롤** 시:

1. UI 스레드 프레임 ≤ 16ms (p99), Raster 스레드 ≤ 16ms (p99)
2. 재정렬이 발생한 프레임(초당 1회)의 UI 시간 ≤ 8ms
3. 마켓 탭을 숨긴 뒤 다른 탭에서 UI 스레드가 **idle 로 떨어진다** (탭 비활성 시 구독 해제가 실제로 동작하는지의 유일한 증거)

---

## 8. Android / iOS 플랫폼 설정

### 8.1 Android

`android/app/build.gradle`
```gradle
defaultConfig {
    applicationId "com.trypto.mobile"
    minSdk 23                 // flutter_secure_storage / flutter_web_auth_2 요구선 이상
    targetSdk 35
}
```

`android/app/src/main/AndroidManifest.xml`
```xml
<application
    android:label="Trypto"
    android:allowBackup="false">          <!-- Auto Backup 으로 세션 ID 가 타 기기에 복원되면 남의 세션을 물고 부팅한다 -->

  <activity android:name=".MainActivity" ... >
      <!-- ★ MainActivity 에는 커스텀 스킴 intent-filter 를 절대 넣지 않는다 -->
      <intent-filter>
          <action android:name="android.intent.action.MAIN"/>
          <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
  </activity>

  <!-- ★ 커스텀 스킴은 flutter_web_auth_2 의 CallbackActivity 에만 선언한다 -->
  <activity
      android:name="com.linusu.flutter_web_auth_2.CallbackActivity"
      android:exported="true">
      <intent-filter android:label="flutter_web_auth_2">
          <action android:name="android.intent.action.VIEW"/>
          <category android:name="android.intent.category.DEFAULT"/>
          <category android:name="android.intent.category.BROWSABLE"/>
          <data android:scheme="trypto"/>
          <!-- 구글/카카오 콘솔이 커스텀 스킴을 거부하면 여기에 스킴을 하나 더 추가한다(§10) -->
      </intent-filter>
  </activity>
</application>
```

`android/app/src/debug/AndroidManifest.xml` — **디버그 전용**. 릴리스 매니페스트를 오염시키지 않는다.
```xml
<application android:usesCleartextTraffic="true"/>
<!-- 에뮬레이터에서 http://10.0.2.2:8080 과 ws://10.0.2.2:8080/ws 에 붙기 위함 -->
```

보안 저장소 옵션과 실패 처리:
```dart
const AndroidOptions(encryptedSharedPreferences: true)
// 복호화 실패(앱 업데이트·키 회전 시 실제로 발생한다)는 try/catch 로 잡아 '세션 없음' 으로 강등한다.
// 부팅 경로에서 예외가 새면 앱이 뜨지 않는다.
```

### 8.2 iOS

`ios/Runner/Info.plist`
```xml
<key>MinimumOSVersion</key><string>13.0</string>   <!-- 방안 B 이므로 17.4 제약이 없다 -->

<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleTypeRole</key><string>Editor</string>
    <key>CFBundleURLName</key><string>com.trypto.mobile</string>
    <key>CFBundleURLSchemes</key><array><string>trypto</string></array>
  </dict>
</array>
```

- **Associated Domains 는 설정하지 않는다.** 방안 A(Universal Links)의 산물이며 방안 B 에서는 불필요하다.
- ATS 예외(`NSAllowsArbitraryLoads`)는 **디버그 스킴 전용 `Info-Debug.plist`** 에만 넣는다. 운영은 `https://` / `wss://` 전제다.
- **Keychain 은 앱 삭제 후에도 살아남는다.** 최초 실행 플래그(`UserDefaults` 의 `first_run`)가 없으면 부팅 시 Keychain 을 비우는 루틴을 넣는다.

### 8.3 빌드 설정 (`--dart-define`)

| 키 | 개발(에뮬레이터) | 운영 |
|---|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8080` | `https://{도메인}` |
| `WS_BASE_URL` | `ws://10.0.2.2:8080/ws` | `wss://{도메인}/ws` |
| `KAKAO_CLIENT_ID` | (REST API 키) | 동일 |
| `GOOGLE_ANDROID_CLIENT_ID` | (Android 클라이언트 ID) | 동일 |
| `GOOGLE_IOS_CLIENT_ID` | (iOS 클라이언트 ID) | 동일 |
| `KAKAO_CALLBACK_SCHEME` / `GOOGLE_CALLBACK_SCHEME` | `trypto` | 콘솔 결과에 따라 변경 가능(§10) |

로컬 백엔드는 **`SESSION_COOKIE_SECURE=false`** 여야 쿠키가 저장·전송된다(R2 부수 조건).

---

## 9. 미해결 항목과 사람이 해줘야 할 일

**아래 1~4 는 구현 5단계(인증) 착수 전에 반드시 닫아야 한다.** 여기가 뚫리지 않으면 나머지 8개 화면은 의미가 없다(라운드·지갑·주문이 전부 세션 뒤에 있다). 코드로 확인할 수 없는 외부 콘솔 정책이므로 사람이 직접 확인해야 한다.

| # | 항목 | 확인·조치 내용 | 실패 시 파급 |
|---|---|---|---|
| **1** | **구글이 `trypto://` 를 받는가** | 구글의 Android/iOS OAuth 클라이언트는 임의 커스텀 스킴을 받지 않을 가능성이 매우 높다. 허용되는 것은 loopback, https 앱링크, **역방향 클라이언트 ID 스킴**(`com.googleusercontent.apps.{ID}:/oauth2redirect`) 이다. 콘솔에서 등록 가능 여부를 확인한다 | 제공자별 콜백 스킴이 달라진다 → `Env.GOOGLE_CALLBACK_SCHEME` 값 변경 + `CallbackActivity` intent-filter 에 `<data android:scheme="...">` 추가 + iOS `CFBundleURLSchemes` 추가 + **백엔드 허용 목록에 같은 문자열 등록**. `AuthConfig` 한 곳만 바뀌도록 설계했으므로 코드 파급은 없다 |
| **2** | **`clientType: "MOBILE"` 하나로 Android/iOS 를 구분할 수 있는가** | 구글의 Android 클라이언트 ID 와 iOS 클라이언트 ID 는 **별개**이고, OAuth 규격상 토큰 교환의 `client_id` 는 인가 요청에 쓴 것과 같아야 한다. 서버가 `MOBILE` 하나로 뭉치면 **안드로이드에서 인가한 코드를 iOS 클라이언트로 교환해 `unauthorized_client` 가 난다** | **백엔드 계약을 `clientType: ANDROID \| IOS` 로 넓히거나, 앱이 `redirectUri` 를 보내고 서버가 허용 목록으로 대조**하도록 백엔드 담당과 지금 확정한다 |
| **3** | **카카오 콘솔이 커스텀 스킴을 등록할 수 있는가** | 카카오 Redirect URI 는 http/https 만 받는 것으로 알려져 있다. 불가하면 Kakao SDK 인가 경로(`kakao{네이티브앱키}://oauth`)로 밀린다 | 콜백 스킴과 **백엔드가 교환에 쓸 redirect URI 값**이 함께 바뀐다. 네이티브 앱키·키 해시(Android)·번들 ID(iOS) 등록이 필요하다 |
| **4** | **백엔드가 구글 토큰 교환에서 `client_secret` 을 빼는 분기를 넣었는가** | 구글 Android/iOS 클라이언트는 `client_secret` 을 발급하지 않는다. 현재 `GoogleOAuthClient` 는 항상 붙인다. `clientType == MOBILE` 일 때 폼에서 제외해야 한다 | 붙여 보내면 구글이 토큰 교환을 거절한다 |
| **5** | **redirect URI 문자열 대조** | 앱의 `AuthConfig.redirectUri(provider)` 가 만드는 문자열과 **백엔드 허용 목록(`app.oauth.{provider}.allowed-redirect-uris`)의 값을 문자 단위로 대조**한다. 한 글자만 달라도 `redirect_uri_mismatch` 다 | 인증이 통째로 실패한다 |
| **6** | 제공자 콘솔 등록 (구글) | Android SHA-1 지문 등록, iOS 번들 ID 등록. 디버그·릴리스 키스토어 지문이 다르므로 둘 다 등록한다 | 에뮬레이터에서 로그인이 실패한다 |
| **7** | 폰트 에셋 확보 | Pretendard(400/500/600/700), Noto Sans KR(800), Roboto Mono(500/700) 를 `assets/fonts/` 에 배치한다. 웹은 CDN 을 쓰지만 앱은 번들이 필요하다 | 폰트가 폴백되어 타이포그래피가 웹과 어긋난다 |
| **8** | 로컬 백엔드 설정 | `SESSION_COOKIE_SECURE=false`(기본값 확인). 에뮬레이터가 `10.0.2.2` 로 호스트에 접근 가능한지 확인 | 쿠키가 저장되지 않아 모든 인증 요청이 401 |

### 서버 수정 없이 남겨 두는 항목 (기능 손실 없음)

- **STOMP 사용자 큐(`/user/queue/events`)** — 웹에서도 미동작이므로 기능 동등성에 영향이 없다(R3). 수신 계층만 만들고 UI 를 걸지 않는다. 서버가 (1) 핸드셰이크에서 `SESSION` 쿠키를 읽어 `Principal` 부착, (2) 페이로드에 `walletId`/`coinId`/`side`/`fee` 추가를 구현하면 그때 구독 한 줄로 켠다.
- **거래소 목록 API** — `ExchangeIds` 상수로 대응한다(R7). 신설하면 앱·웹의 상수 중복이 사라지는 이점만 있다.
- **`DUPLICATE_RULE_TYPE` 메시지 키 누락(500 유발)** — 클라이언트가 중복 전송을 선제 차단해 회피한다(R8).
