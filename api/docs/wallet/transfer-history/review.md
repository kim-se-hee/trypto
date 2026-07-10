# 리뷰 이슈 — transfer-history

리뷰 범위 1차: `cd8cd35..d195097` (리팩토링 4커밋)

## 1차 차단 이슈

- [x] **[api/.../wallet/application/service/FindTransferHistoryService.java:49-52] 소유권 판정이 도메인을 우회해 서비스에서 인라인 비교** (출처: ddd, oop) — **완료(`e549963`, Wallet.isOwnedBy 술어 위임, WALLET_ACCESS_DENIED 보존)**
  - **설명:** 소유권 판정(`ownerId.equals(userId)` 비교 + 예외)을 응용 서비스가 직접 수행한다. 형제 서비스 `GetWalletBalancesService` 는 `wallet.verifyOwnedBy(...)` 도메인 메소드에 위임한다. 판정 규칙이 도메인 밖으로 새고 형제와 방식이 갈린다.
  - **수정 제안(교정됨):** 소유권 **판정**을 Wallet 도메인 술어로 옮긴다 — `Wallet.isOwnedBy(Long requesterId, Long ownerId)` 불리언 메소드를 추가(또는 재사용)하고, 서비스는 `if (!wallet.isOwnedBy(userId, investmentRoundQueryPort.getOwnerId(wallet.getRoundId()))) throw WALLET_ACCESS_DENIED;` 로 위임한다.
  - **주의 — 에러코드는 보존:** 리뷰어들은 `verifyOwnedBy`(→ `WALLET_NOT_OWNED`)로 통일을 제안했으나, **transfer-history 인수 테스트가 `WALLET_ACCESS_DENIED`(403)를 단정**한다(spec 진실). 에러코드를 `WALLET_NOT_OWNED` 로 바꾸면 안 되고 `WALLET_ACCESS_DENIED` 를 유지한다. 그래서 판정만 도메인 술어로 캡슐화하고 에러코드는 기능별로 서비스가 던진다.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [FindTransferHistoryService buildCursorResult] `resolveCoinSymbols`(외부 조회)와 `TransferHistoryResult.from`(매핑)이 한 메소드에 합쳐져 두 책임 — 심볼 맵을 먼저 구해 넘기는 분리 구조 고려 (oop)
- [MarketDataQueryPort.findCoinSymbols] 연동 대상/CRUD 드러내는 이름 — `resolveCoinSymbols` 등 도메인 의도 이름 고려. 단 코드베이스 관례라 영향 작음 (ddd)

## 판정 메모

- 인수 테스트(transfer-history) 6 시나리오 통과 — 커서 페이징·입출금·심볼 보강·ACL 배선·소유권 403(WALLET_ACCESS_DENIED) 정상. 에러코드는 이 단정을 지켜야 하므로 보존.
- performance: 심볼 보강 벌크(Set) 조회로 N+1 없음 확인. concurrency: 읽기 전용, 이상 없음.

## 2차 재리뷰 (`d195097..e549963`)

- 차단 1건 적용(`e549963`: `Wallet.isOwnedBy` 술어로 판정 캡슐화, 에러코드 `WALLET_ACCESS_DENIED` 보존). 재리뷰 ddd·oop·convention 모두 차단 0건 — 캡슐화 회복 확인. 통과.
- 참고(비차단): `verifyOwnedBy` 를 `isOwnedBy` 에 위임하면 소유권 비교 DRY — wallet-assets 공유 경로라 별도 정리 시 반영 검토.

