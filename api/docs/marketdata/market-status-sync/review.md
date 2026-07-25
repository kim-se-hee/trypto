# market-status-sync 리뷰

리뷰 범위: `main...HEAD` (collector 발행 + api 소비 통합 브랜치). 차단 이슈는 모든 리뷰어의 차단 표시 합집합이며 컨벤션 위반은 전부 차단으로 분류한다. 참고 이슈는 보고용이며 자동 적용하지 않는다.

## 1차 차단 이슈

- [x] **[collector/src/main/java/ksh/tryptocollector/distribute/rabbitmq/MarketStatusPublisher.java:25-26,39] 교환기 이름 이중 선언** (출처: 컨벤션)
  - **설명:** `RabbitMQConfig`의 `@Bean`과 `MarketStatusPublisher`의 `@Value`가 동일 프로퍼티 키(`app.rabbitmq.market-status-exchange`)와 기본값(`market.status`)을 각자 하드코딩한다. 한쪽만 바뀌면 선언한 교환기와 발행 대상 교환기가 어긋나 메시지가 존재하지 않는 교환기로 조용히 유실될 수 있다. `TICKER_EXCHANGE`는 상수 단일화로 이미 이 문제를 피한 전례가 있다.
  - **수정 제안:** `RabbitMQConfig`에 상수(또는 `FanoutExchange` 빈)를 단일 소스로 두고, `MarketStatusPublisher`가 문자열을 재주입받지 않고 그 상수/빈을 참조하도록 바꾼다.

- [x] **[api/src/main/java/ksh/tryptobackend/marketdata/application/service/ApplyMarketStatusChangeService.java:49,56,68] 애플리케이션 서비스 private 메서드 3개** (출처: 컨벤션)
  - **설명:** `apply()` 아래 `startTrading()`/`suspendMarket()`/`notificationOf()` private 메서드로 로직을 감췄다. "애플리케이션 서비스는 순수 오케스트레이션, private 메서드 금지, 비즈니스 로직·VO 생성은 도메인에 위임"(`PlaceOrderService` 베스트 프랙티스) 규칙 위반.
  - **수정 제안:** VO 생성은 `MarketStatusNotification.of(...)` 정적 팩토리로 이관(이미 `MarketStatusStompPayload.from(...)` 패턴 사용 중), 등록/정지 흐름은 유스케이스 분리 또는 `apply()` 본문 인라인으로 private 제거.

- [x] **[api/src/main/java/ksh/tryptobackend/marketdata/application/service/ApplyMarketStatusChangeService.java:56-65] 반드시 존재해야 할 대상을 find 후 조용히 무시** (출처: 컨벤션)
  - **설명:** `SUSPENDED` 이벤트는 이미 `거래중`이던(=api에 등록된) 마켓에서만 발생하므로 이 시점 coin·exchangeCoin은 반드시 존재한다. 그런데 `findBySymbol`·`suspend`가 못 찾으면 예외·로그 없이 무시해 collector/api 상태 불일치를 은폐한다. 같은 클래스 `apply()`는 `orElseThrow`로 처리해 방식이 불일치.
  - **수정 제안:** `getBySymbol`(없으면 예외) 조회를 추가하거나 `findBySymbol(...).orElseThrow(() -> new CustomException(ErrorCode.COIN_NOT_FOUND))`로 `apply()`와 통일한다. `suspend`도 동일하게.

- [x] **[api/src/main/java/ksh/tryptobackend/marketdata/application/port/in/GetMarketStatusUseCase.java:5,7] 유스케이스에 메서드 2개 정의** (출처: 컨벤션)
  - **설명:** "하나의 유스케이스에 하나의 메서드" 규칙 위반. `isSuspended(exchangeCoinId)`와 `isSuspended(exchangeId, coinId)`는 조회 조건도, 실패 시 기본값(`orElse(true)` vs `orElse(false)`)도 달라 한 개념으로 묶기 어렵다.
  - **수정 제안:** 두 유스케이스로 분리(예: `GetMarketStatusUseCase` / `GetCoinMarketStatusUseCase`, 구현체는 `GetMarketStatusService`가 둘 다 구현 가능). 분리 시 fail-closed/fail-open 의도가 이름에서 드러나도록.

- [x] **[api/src/main/java/ksh/tryptobackend/wallet/application/service/TransferCoinService.java:52,68-73] 거래지원 종료 송금 거절 규칙이 애플리케이션 서비스로 샘** (출처: 컨벤션, ddd)
  - **설명:** private `rejectWhenSuspended`가 marketdata 데이터로 "송금 거절"을 서비스 계층에서 직접 판정한다. 같은 PR의 `Order.create`(marketInfo.suspended() 검증)는 애그리거트에서 규칙을 처리하고, wallet 자신의 `verifyHandles` 패턴(ACL이 데이터 반환 → 애그리거트가 판정)과도 어긋난다.
  - **수정 제안:** `Wallet`에 `verifyHandles`와 같은 패턴으로 `verifyTradable(boolean suspended)` 도메인 메서드를 추가하고, 서비스는 private 없이 인라인으로 출발·도착 양쪽을 호출한다. (판정 주체를 애그리거트로 옮겨 ddd 지적도 동시 해소.)

- [x] **[api/src/main/java/ksh/tryptobackend/trading/application/service/FindEvaluatedHoldingsService.java:31-40] 보유 코인별 거래지원 종료 여부 N+1 조회** (출처: 성능)
  - **설명:** 바로 위 `findCoinExchangeMapping()`이 보유 코인 전체를 1회 배치 조회하는데, 추가된 `isTradable()`이 코인 수(N)만큼 `marketQueryPort.isSuspended(exchangeCoinId)` 단건 조회를 던진다. 보유 조회 요청과 랭킹 스냅샷 배치(전체 사용자×보유 코인)에서 순차 DB 왕복이 N배로 증가.
  - **수정 제안:** `ExchangeCoinIdMap`/`CoinExchangeMapping`이 `MarketStatus`도 함께 담도록 확장해, `isTradable()`이 추가 쿼리 없이 이미 로드된 맵에서 판정하게 한다.

- [x] **[collector/src/main/java/ksh/tryptocollector/ingest/binance/BinanceRestClient.java:35-46] fetchUsdtTickers()가 호출마다 exchangeInfo 조회 — 200ms 폴백 폴링 회귀** (출처: 성능)
  - **설명:** `fetchUsdtTickers()`는 WS 장애 시 `RestPollingFallback`(200ms 주기)이 부르는 핫패스인데, 이번 변경으로 매 호출마다 `fetchTradingSymbols()`(별도 `exchangeInfo` REST)를 먼저 호출해 왕복이 2배가 됐다. 폴링 경로는 이미 `symbolCodes.contains(...)`로 재필터하므로 상태 필터가 불필요하며, 지연 민감 상황에서 레이트리밋 위험까지 커진다.
  - **수정 제안:** 상태 필터가 필요한 초기 스냅샷·`MarketStatusSynchronizer`용과 이미 알려진 심볼만 고르는 폴링용을 분리하거나, `tradingSymbols`를 캐시(3분 동기화 결과 재사용/짧은 TTL)해 폴링 사이클마다 `exchangeInfo`를 다시 부르지 않게 한다.

- [x] **[api/src/main/java/ksh/tryptobackend/trading/application/service/PlaceOrderService.java:39 · AutoCancelOrdersService.java:27] 주문 생성 vs 자동취소 스캔 TOCTOU로 잠긴 잔고 영구 고착** (출처: 동시성)
  - **설명:** 주문 생성 트랜잭션이 "거래중" 확인 후 잔고를 잠그고 커밋하기 전에, 동시에 도착한 `SUSPENDED` 처리의 자동취소 스캔(`findPendingOrderIdsByExchangeCoinId`, 락 없는 SELECT)이 REPEATABLE READ에서 그 주문을 보지 못한다. 이후 주문이 커밋되면 PENDING·잠금액이 영구 잔존하고, 마켓은 숨겨져 사용자가 취소할 수 없다. 자동취소는 전이 시점 1회성이라 재시도가 없다.
  - **수정 제안:** 주문 생성이 마켓 상태를 조회할 때 `exchange_coin` 로우에 공유 락을 잡아 `suspend()`의 쓰기 락과 상호 배제(먼저 커밋한 쪽에 따라 스캔에 포함되거나 주문이 거절됨)하거나, `SUSPENDED` 마켓의 잔여 PENDING 주문을 재스캔하는 보정 경로를 둔다.

- [x] **[api/src/main/java/ksh/tryptobackend/marketdata/adapter/in/MarketStatusEventListener.java:26-30] 리스너 예외 흡수로 메시지 재시도 없이 영구 유실** (출처: 동시성)
  - **설명:** `catch (Exception)` 후 정상 반환하므로 `@RabbitListener` AUTO ack가 메시지를 성공으로 간주해 ack한다(DLQ·재시도 없음). 락 대기 타임아웃·데드락 같은 일시적 실패가 영구 데이터 유실로 바뀌고, collector는 전이 시 1회만 발행해 재전달이 없어 api 상태가 영구히 어긋난다. SUSPENDED는 자동취소까지 한 트랜잭션이라 실패 표면이 넓다.
  - **수정 제안:** 실패 시 예외를 전파해 컨테이너가 nack/재시도하게 하거나 `RetryOperationsInterceptor` + DLQ를 구성한다(무한 재큐 방지를 위해 재시도 한도·DLQ 필수). 지속 실패 대비 주기적 재동기화(보정)를 별도로 두는 것을 권장.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [api/.../marketdata/adapter/out/ExchangeCoinCommandAdapter.java:18-40] `save()`/`register()` 조회·매핑 중복 → `findOrCreate` 추출 (oop)
- [api/.../trading/domain/model/Order.java:45-48] `marketInfo.suspended()`를 꺼내 Order가 분기 → `marketInfo.assertTradable()` Tell-Don't-Ask (oop) — 위 송금 차단 이슈와 같은 결
- [api/.../marketdata/application/service/GetMarketStatusService.java:18-32] `isSuspended` 오버로드의 상반된 실패 기본값(fail-closed/open) 의도 미문서화 → 주석·명명으로 계약 명시 (oop, ddd)
- [api/.../marketdata/application/service/ApplyMarketStatusChangeService.java:36-50,56-65] `@Transactional` 안에서 STOMP를 commit 전 선발송 + 2단 중첩 Optional 가독성 → AFTER_COMMIT 발송 검토, 중첩 완화 (oop, ddd) — 위 리스너 차단 이슈와 연관
- [api/.../marketdata/adapter/out/persistence/entity/ExchangeCoinJpaEntity.java · ExchangeCoinCommandAdapter.java] 상태 전이·재상장 판정이 JPA 엔티티/어댑터에 위치(기존 참조데이터 관행과는 일관) (ddd)
- [api/.../trading/application/service/FindEvaluatedHoldingsService.java:32,37-39] 종료 코인 필터를 조회 서비스가 판정(read-model 조립이면 허용) (ddd)
- [api/.../trading/adapter/out/persistence/repository/OrderJpaRepository.java:17 · AutoCancelOrdersService.java:14-28] `(exchange_coin_id, status)` 복합 인덱스 존재 확인, 미체결 다수 시 단건 순차 락 트랜잭션 장기 점유 → 인덱스 추가·비동기/청크 검토 (성능)
- [collector/.../metadata/MarketStatusSynchronizer.java:76-81] 회차 중간 실패 시 `tradingBaseline` 미갱신 → 다음 회차 중복 발행 가능(데이터 손상은 없음) (동시성)
- [api/.../marketdata/adapter/out/persistence/entity/ExchangeCoinJpaEntity.java] `@Version` 등 낙관적 락 부재 → 경쟁 컨슈머 처리 순서 역전 시 최신 상태가 덮어써질 이론적 위험(3분 주기라 가능성 낮음) (동시성)
- [collector/.../metadata/MarketInfoCache.java · MarketStatusSynchronizer.java] 리더십 재획득 시 `ExchangeInitializer`(전량 재적재) vs `MarketStatusSynchronizer`(증분 diff) 미조율(다음 회차 자동 회복, 최대 3분 불일치) (동시성)

## 2차 차단 이슈

재리뷰 범위: `1964bb0b..HEAD`(1차 수정 커밋). 5개 리뷰어 차단 0, conv-api 가 수정 과정에서 새로 유입된 위반 1건 발견.

- [ ] **[api/src/main/java/ksh/tryptobackend/trading/domain/vo/CoinExchangeMapping.java:17,21] 판별 메서드가 조회 메서드보다 뒤에 위치** (출처: 컨벤션)
  - **설명:** 이슈6 수정 때 추가한 판별 메서드 `isTradable`이 기존 조회 메서드 `getExchangeCoinId` 뒤에 붙어, "상태 변경 → 판별 → 조회" 나열 순서 컨벤션을 어겼다. 원래 조회 메서드 하나뿐이라 없던 문제가 이번 변경으로 처음 발생.
  - **수정 제안:** `isTradable`을 `getExchangeCoinId` 앞으로 옮긴다.

## 2차 참고 이슈 (수정 안 함, 보고용)

- [api/.../marketdata/adapter/in/MarketStatusEventListener.java ↔ trading/.../SuspendedMarketOrderCancelListener.java · ApplyMarketStatusChangeService.java] **(중요)** 이벤트 자동취소가 `suspend` 트랜잭션과 같은 트랜잭션이라, 이슈9로 예외 삼킴을 걷어낸 뒤에는 자동취소 실패 시 `suspend` 자체가 롤백된다. 그러면 코인이 SUSPENDED로 커밋되지 않아 이슈8 보정 스윕도 그 마켓을 못 찾는다 — 정작 "취소 실패로 잠긴 금액" 케이스가 안전망 사각지대. `SuspendedMarketOrderCancelListener`를 `@TransactionalEventListener(AFTER_COMMIT)` + 자체 트랜잭션으로 분리하면 suspend·알림은 확정되고 회수는 스윕이 보장 (ddd, 선택 수정)
- [api/.../trading/adapter/in/scheduling/SuspendedMarketOrderSweeper.java] 60초 스윕이 "한 번이라도 종료된" 마켓 전체를 무기한 재조회 → 상장 폐지 누적 시 `orders`(쓰기 100 TPS) 반복 조회 증가. `(exchange_coin_id, status)` 인덱스 추가 + 스윕 대상 축소 검토 (성능)
- [api/.../common/config/RabbitMqConfig.java] 상태 리스너 동기 재시도(백오프 최대 ~7초)가 컨슈머 스레드를 블로킹 → 단일 인스턴스/실패 집중 시 다른 마켓 상태 반영 지연. 컨테이너 concurrency 상향 또는 지연 허용 확인 (성능, 동시성)
- [api/.../trading/adapter/in/scheduling/SuspendedMarketOrderSweeper.java · OrderJpaRepository] 다중 인스턴스 동시 스윕 시 `findPendingOrderIdsByExchangeCoinId`에 `ORDER BY id` 부재로 이론적 데드락 → 정렬 명시로 제거 가능(중복 취소는 이미 방지됨) (동시성)
