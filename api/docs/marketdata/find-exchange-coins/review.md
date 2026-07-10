# 리뷰 이슈 — find-exchange-coins

리뷰 범위 1차: `72664af..612411a` (리팩토링 3커밋 + 빈 이름 충돌 fix 1커밋)

## 1차 차단 이슈

- [x] **[api/src/main/java/ksh/tryptobackend/marketdata/domain/model/ExchangeCoins.java:1] 일급 컬렉션을 domain/model 이 아닌 domain/vo 에 두어야 함** (출처: 컨벤션) — **무효 처리(미적용)**
  - **설명:** `ExchangeCoins` 는 정체성 없는 불변 값 래퍼(불변 `List` + `equals`/`hashCode`)로 VO 요건을 충족한다. marketdata·trading 을 통틀어 동일 성격의 일급 컬렉션(`CoinSymbols`, `TickerSnapshots`, `ExchangeCoinIdMap`, `FilledOrderCounts`)은 예외 없이 `domain/vo/` 에 있고 `domain/model/` 은 개별 도메인 모델만 담는다.
  - **판정: 무효(거짓 양성).** 리뷰어가 든 선례는 전부 VO(`TickerSnapshot`) 또는 원시값(String/Long)을 감싸지만, `ExchangeCoins` 는 **도메인 모델** `ExchangeCoin` 을 감싼다. `domain/vo` 로 옮기면 프로즌 ArchUnit 규칙 `domain_vo_should_not_depend_on_domain_model`("Domain VO should not depend on domain model")을 위반해 빌드가 깨진다. 모델을 감싸는 일급 컬렉션의 올바른 위치는 `domain/model` 이며 현재 위치가 정확하다. 미적용으로 종결.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [api/src/main/java/ksh/tryptobackend/marketdata/application/service/FindExchangeCoinsService.java:37-46] 코인 누락 시 실패 방식이 NPE → coinSymbol=null 로 미세 변화. spec 불변식(모든 상장 코인은 대응 Coin 존재)상 실사용 영향 없음 (ddd)
- [api/src/main/java/ksh/tryptobackend/marketdata/adapter/out/ExchangeCoinQueryAdapter.java:44-48] 지역 변수 `exchangeCoins`(List)와 클래스 `ExchangeCoins` 이름 혼동 소지 (oop)
- [api/src/main/java/ksh/tryptobackend/marketdata/domain/model/ExchangeCoins.java:15-20] `coinIds()`/`exchangeCoinIds()` 가 목록을 두 번 순회. 규모 작아 영향 미미, 기존 로직 이전분 (성능)
- [api/src/main/java/ksh/tryptobackend/marketdata/application/port/in/dto/result/ExchangeCoinListResult.java:13] 정적 팩토리명 `of` — 코드베이스 관례는 `from` (13곳). conventions.md 명문 규칙은 없어 참고 (컨벤션)

## 1차 판정 요약

- 차단 이슈 1건은 무효(거짓 양성)로 종결 — 유효 차단 0건. 코드 변경 없이 리뷰 통과.
- 인수 테스트(marketdata/find-exchange-coins) 3 시나리오 통과 — 동작 보존 확인.
- 참고: ArchUnit `application_should_not_depend_on_adapter`(하드 규칙, 프로즌 아님)가 baseline 부터 red — 타 컨텍스트 UseCase 직접 주입 68건의 **사전 존재 부채**. find-exchange-coins 기여 0건(크로스컨텍스트 소비 없음). 이 부채는 이후 크로스컨텍스트 소비 기능들(my-holdings·ranking·regretanalysis 등)을 place-order 표준(ACL 아웃풋 포트)으로 리팩토링하며 점진 해소 대상.
