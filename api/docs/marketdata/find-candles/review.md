# 리뷰 이슈 — find-candles

리뷰 범위 1차: `c52e0ed..1a0fe26` (리팩토링 2커밋)

## 1차 차단 이슈

- [x] **[api/src/main/java/ksh/tryptobackend/marketdata/domain/model/CandleFilter.java:7,17,21] 도메인 모델이 application 계층 Query DTO(`FindCandlesQuery`)를 import·파라미터로 받음 — 헥사고날 의존 방향 역전** (출처: oop, 컨벤션) — **완료(`3cc039e`)**
  - **설명:** `CandleFilter`(domain)가 `application.port.in.dto.query.FindCandlesQuery` 를 import 하고 `validateIdentifiers(FindCandlesQuery)`, `of(FindCandlesQuery, ...)` 로 받는다. conventions.md 의 도메인 예외는 "애그리거트 생성 시 예외적으로 **Command** 객체를 받는다"에 한정된다. `CandleFilter` 는 애그리거트가 아닌 필터 VO 이고 받는 것도 Command 가 아닌 Query 라 예외 범위 밖. 도메인→응용 의존은 헥사고날 경계를 깬다.
  - **수정 제안:** `validateIdentifiers`/`of` 시그니처를 원시값(`exchange`, `coin`, `interval`, `limit`, `cursor`, `baseCurrencySymbol`)을 받도록 바꾸고, `FindCandlesQuery` 언패킹은 `FindCandlesService`(application)에서 수행한다. 이렇게 하면 도메인이 application DTO 를 몰라도 된다.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [CandleFilter.java] `validateIdentifiers` 가 `of()` 와 분리돼 서비스가 호출 순서를 기억해야 함 — `of()` 내부에서 검증까지 수행해 단일 진입점으로 불변식 강제 고려 (oop/ddd)

## 2차 재리뷰 (`1a0fe26..3cc039e`)

- 차단 이슈 1건 적용(`3cc039e`: 도메인이 원시값 수신, `FindCandlesQuery` import 제거). 재리뷰 5개 리뷰어 모두 차단 0건 — 의존 역전 해소 확인. 통과.

