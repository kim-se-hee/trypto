# 리뷰 이슈 — start-round

리뷰 범위 1차: `52b5e4b..806fffd` (리팩토링 4커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건.

핵심 개선: 타 컨텍스트 UseCase 직접 주입(marketdata `FindExchangeDetail`, wallet `CreateWalletWithBalance`·`FindWallet`)을 제거하고 **연동형 도메인 서비스 `SeedWalletProvisioner`(+Impl, wallet UseCase 위임, place-order `WalletBalanceService` 패턴) + `MarketDataQueryPort.getSeedFundingSpec` ACL 포트**로 협력하도록 전환(ArchUnit "must not inject another UseCase" 위반 해소). 시드 판단은 도메인 VO(`SeedAllocation`/`SeedAmountPolicy`)에 응집, ACL 은 marketdata `domestic` 을 도메인 개념으로 번역만. 지갑 생성 반환 walletId 를 바로 사용해 기존 `findByRoundId` 재조회 쿼리 1건 제거(성능 개선). 인수 테스트(start-round) 8 시나리오 통과.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [AclMarketDataQueryAdapter] `getBaseCurrencyCoinId`·`getSeedFundingSpec` 이 거래소 조회+EXCHANGE_NOT_FOUND 중복 — 공통 헬퍼로 묶기 (oop/convention)
- [StartRoundService] `resolveSeedAllocations`/`toRules` 가 필요한 값 대신 `StartRoundCommand` 전체 수신 — 의존 범위 넓음 (oop)
- [StartRoundService] private 메소드 잔존 — place-order 는 0개. 다만 코드베이스 28개 서비스 관행이라 이번만의 이탈 아님 (convention)
- [MarketDataQueryPort] 포트명이 연동 대상 노출 — 코드베이스 전반 관례 (ddd)

## 1차 판정 요약

- 유효 차단 0건 — 통과. Wallet↔InvestmentRound 순환의 InvestmentRound 측이 ACL·연동형 도메인 서비스 표준으로 정리됨.
