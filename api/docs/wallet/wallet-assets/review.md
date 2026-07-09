# 리뷰 이슈 — wallet-assets

리뷰 범위 1차: `089438a..d03b8d5` (리팩토링 4커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건.

핵심 개선: 크로스컨텍스트 접근(marketdata `FindCoinInfo`·investmentround `FindRoundInfo`)을 서비스 직접 주입 → **ACL 아웃풋 포트(`MarketDataQueryPort`/`InvestmentRoundQueryPort`) + 어댑터(`AclMarketDataQueryAdapter`/`AclInvestmentRoundQueryAdapter`)** 로 분리(place-order 표준, ArchUnit "must not inject another UseCase" 위반 해소). 지갑 소유권 검증을 `Wallet.verifyOwnedBy` 도메인으로 이동, 기축통화 `BaseCurrency` VO 도입. 인수 테스트(wallet-assets) 4 시나리오 통과 — ACL 배선·동작 보존 확인.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [Wallet.java verifyOwnedBy(requesterId, ownerId)] Long 파라미터 2개 순서 혼동 소지 — 역할이 드러나는 명명/시그니처 고려 (oop)
- [AclMarketDataQueryAdapter] wallet 쪽만 `@Component("walletAclMarketDataQueryAdapter")` 접두사, investmentround 쪽은 기본명 — 빈 충돌은 없으나(접두사로 예방) 명명 비대칭. investmentround 리팩토링 시 통일 검토 (oop/convention)
- [InvestmentRoundQueryPort/MarketDataQueryPort] 포트명이 연동 대상 BC 를 노출 — 다만 코드베이스 전반(`MarketQueryPort`/`WalletQueryPort`)이 동일 관례라 전역 컨벤션 차원 (ddd)

## 1차 판정 요약

- 유효 차단 0건 — 통과. Wallet 클러스터 기준점으로서 ACL 표준 형태 확립.
