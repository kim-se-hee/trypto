# 리뷰 이슈 — my-holdings

리뷰 범위 1차: `2c2c72e..bf6e030` (리팩토링 3커밋 — 3개 컨텍스트 ACL 분리)

## 1차 차단 이슈

- [x] **[api/.../portfolio/domain/vo/CoinMetadataMap.java / Portfolio 조립] 코인 메타데이터 누락 시 동작이 "부분 제외"→"전체 실패"로 바뀜 (동작 보존 위반)** (출처: oop·concurrency 참고 → 동작 보존 관점에서 차단 승격) — **완료(`b495d49`, hasMetadata 필터로 부분 제외 복원, 단위 테스트 추가)**
  - **설명:** 리팩토링 전 `buildCoinSnapshotMap` 은 `coinInfoMap.containsKey(h.coinId())` 로 필터해, 보유 코인 중 메타데이터가 없는 홀딩만 **조용히 제외**하고 나머지(기축통화 잔고 + 나머지 홀딩)는 정상 반환했다. 리팩토링 후 `CoinMetadataMap.getMetadata` 는 누락 시 `COIN_NOT_FOUND` 를 던져 **전체 포트폴리오 조회가 실패**한다(`AclMarketDataQueryAdapter.findCoinMetadata` 도 누락 coinId 를 맵에서 제거). spec.md/plan.md 는 이 fail-fast 를 규정하지 않는다. 이는 spec 근거 없는 사용자 체감 동작 변화이며, 코인 마스터가 정적이라 인수 테스트(모든 코인 메타데이터 존재)로는 안 잡혔다.
  - **수정 제안:** 리팩토링 전 동작을 복원한다 — 코인 메타데이터가 없는 홀딩은 스냅샷 조립에서 **제외**하고 나머지는 정상 반환(예외를 던지지 않음). 즉 `Portfolio`/`PortfolioHoldings` 조립 시 `CoinMetadataMap` 에 없는 coinId 의 홀딩은 skip. (fail-fast 가 바람직하다면 그것은 refactor 가 아니라 spec 변경으로 별도 결정.)

## 1차 참고 이슈 (수정 안 함, 보고용)

- [MarketDataQueryPort/WalletQueryPort/TradingQueryPort] 포트명이 연동 대상 노출 — 코드베이스 전반 관례 (ddd)
- [Portfolio.java] 식별자·불변식 없는 읽기 모델을 domain/model 에 배치 — 읽기 모델 표시 배치 고려 (ddd)
- [PortfolioWallet/CoinMetadataMap/Portfolio] 새로 캡슐화된 도메인 로직 단위 테스트 부재 — 추가 권장 (oop)

## 판정 메모

- ddd/convention: ACL 3개 어댑터 분리·명명·위치·포트명·소유권 도메인 응집(`PortfolioWallet.verifyOwnedBy`)·컨트롤러 adapter/in/web 이동 모두 표준 부합. performance: 벌크 유지, N+1 없음.
- 인수 테스트(my-holdings) 4 시나리오 통과(단, 메타데이터 누락 케이스는 미포함).

## 2차 재리뷰 (`bf6e030..b495d49`)

- 차단 1건 적용(`b495d49`: `CoinMetadataMap.hasMetadata` + `PortfolioHoldings` 필터로 부분 제외 동작 복원, `PortfolioHoldingsTest` 로 누락/전량 케이스 검증). 재리뷰 oop·concurrency·ddd 모두 차단 0건 — 동작 복원 확인(ddd: plan.md 가 부분 제외 전제임을 확인). 통과.
- 참고(비차단): `hasMetadata`+`getMetadata` 이중 조회를 `findMetadata(Optional)` 로 합칠 여지.
