# 리뷰 이슈 — active-round

리뷰 범위 1차: `629674f..aade04a` (리팩토링 3커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건.

핵심 개선: 지갑 조회를 wallet `FindWalletUseCase` 직접 주입 → **`WalletQueryPort` + `AclWalletQueryAdapter` 재사용**(ArchUnit `investmentround_isolation` 위반 해소, 8→7), wallet `WalletResult` 를 자기 컨텍스트 `RoundWallet` VO 로 ACL 번역, `RoundInfoResult`/`GetActiveRoundWalletResult` 변환을 정적 팩토리로. 제공 UseCase(`FindRoundInfo`/`FindActiveRounds`/`FindInvestmentRules`)는 도메인 유출 없이 Result DTO 반환 유지. N+1 없음(라운드1+원칙1+지갑1 = 3쿼리). 인수 테스트(active-round) 2 시나리오 통과.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [WalletQueryPort.findWalletsByRoundId / AclWalletQueryAdapter] 포트 메서드명이 CRUD 서술 — 도메인 의도명 `findRoundWallets` 고려. 기존 관례 일관 (ddd)
- [RoundWallet.java] 타 컨텍스트 ID 참조 표시용 VO 배치 — 현재 용도로는 타당 (ddd)
- [GetActiveRoundService] 조회 항목 증가 시 조립 책임을 도메인으로 위임 방향 고려 (oop)

## 1차 판정 요약

- 유효 차단 0건 — 통과. InvestmentRound 컨텍스트 완료(start·end·active + 완료된 emergency-funding).
