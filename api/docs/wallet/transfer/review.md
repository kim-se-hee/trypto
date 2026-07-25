# 리뷰 이슈 — transfer

리뷰 범위 1차: `cf849b5..69299d0` (리팩토링 3커밋)

## 1차 차단 이슈

- [x] **[api/.../wallet/application/service/TransferCoinService.java:53-54] 잔고 변동을 Transfer 애그리거트 값이 아니라 원본 command 값으로 수행 (기록=실행 불변식 파손)** (출처: oop) — **완료(`9dff12d`)**
  - **설명:** `Transfer.create` 로 송금 기록 애그리거트를 만든 뒤, 실제 잔고 차감/가산(`deductBalance`/`addBalance`)을 `transfer.getX()` 가 아니라 원본 `command` 값으로 수행한다. 삭제된 `planBalanceChanges()` 가 보장하던 "기록된 송금 = 실행된 이체" 불변식이 사라져, Transfer 가 금액을 가공하게 되면 기록과 실제 이체가 어긋날 캡슐화 구멍이 생긴다.
  - **수정 제안:** 잔고 변동에 `transfer.getFromWalletId()`·`getToWalletId()`·`getCoinId()`·`getAmount()` 를 사용해 Transfer 애그리거트를 단일 진실 원천으로 삼는다.

## 반려한 리뷰 지적 (무효)

- **[ddd] `Transfer.create` 가 application 계층 `TransferCoinCommand` 에 의존 — 헥사고날 역전 (차단 주장)** → **무효.** conventions.md(레이어별-Domain) "애그리거트를 생성할 때는 예외적으로 application 계층의 **Command** 객체를 입력 파라미터로 받는다" 가 명시 허용한다. Transfer 는 애그리거트이고 `create` 는 애그리거트 생성이므로 예외 범위 안(place-order `Order.create` 도 동일). convention 리뷰어도 허용으로 판정. (find-candles 사례와 다른 점: 그건 애그리거트가 아닌 필터 VO 가 Query DTO 를 받은 것.)

## 유예한 아키텍처 이슈 (out-of-scope, 별도 이니셔티브)

- **[ddd] 두 지갑 협력을 협력형 도메인 서비스로 응집 + 소스 잔고 불변식이 Transfer 로 샘** — ddd-guideline §8 이 "거래소 간 송금"을 협력형 도메인 서비스 예시로 명시하는 것은 사실이다. 그러나 §8 의 진짜 협력형(매개변수가 애그리거트 인스턴스, 상태 변경이 그 public 메소드 호출)이 성립하려면 잔고 변동을 로드된 `WalletBalance` 애그리거트의 `deductAvailable`/`addAvailable` 로 수행해야 한다. **현재 잔고 변동 경로(전 기능 공통, place-order·emergency-funding 포함)는 `WalletCommandAdapter` 의 원자적 조건부 UPDATE(`WHERE available >= amount`)** 로, 비관락 없이 이중지불을 막는 의도적 동시성 설계다. 이를 로드-변경-저장 + 락으로 바꾸는 것은 wallet 컨텍스트 전역 재설계이며 place-order 레퍼런스의 동시성 설계와 배치되고 이미 완료된 기능들에 파급된다. transfer 만 단독으로 바꾸면 레퍼런스와 불일치하므로, **WalletBalance 애그리거트화 + 송금 도메인 서비스**는 별도 아키텍처 이니셔티브로 분리한다. 현재 구현은 place-order 와 동일한 포트-원자 UPDATE 방식으로 일관된다.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [TransferCoinService 멱등 조회] `isPresent()`+`orElseThrow()` 보다 `findByIdempotencyKey(...).orElseGet(...)` 가 의도(이미 완료 송금이면 그대로 반환)를 더 드러냄 (ddd/oop)
- [TransferCoinService] source/dest 각각 `findById().orElseThrow(WALLET_NOT_FOUND)` 중복 — 포트에 `getById` 존재보장 조회 고려 (oop/convention)
- [WalletTest] 신규 도메인 메소드 `verifySameRoundAs` 단위 테스트 부재 (convention)

## 1차 판정 요약

- 유효 차단 1건(oop) 적용 예정. ddd #2 무효, ddd #1 아키텍처 유예.
- 인수 테스트(transfer) 6 시나리오 통과 — 동작 보존·잔고 정합성 확인. concurrency 리뷰어: 원자 UPDATE 유지로 이중차감 방지 그대로.

## 2차 재리뷰 (`69299d0..9dff12d`)

- 차단 1건 적용(`9dff12d`: 잔고 변동을 transfer 애그리거트 값으로). 재리뷰 oop·ddd·concurrency 모두 차단 0건 — 기록=실행 불변식 회복 확인, 유예된 아키텍처 이슈는 리뷰어도 out-of-scope 동의. 통과.

## 3차 차단 이슈 (보유 내역 이동, `61b34f57..c5642b1e`)

- [x] **[api/src/main/java/ksh/tryptobackend/wallet/adapter/out/service/HoldingMoverImpl.java:3-4] Wallet→Trading 역방향 의존 신설로 컨텍스트 순환 의존 — architecture.md 의존 표와 모순** (출처: oop | 컨벤션) — **완료(`3c2bf3ee`)**
  - **설명:** 이 변경 전 wallet 패키지에는 trading import 가 없었다. `HoldingMoverImpl` 이 trading 의 `MoveHoldingUseCase` 를 호출하면서 기존 Trading→Wallet(`BalanceChangeApplierImpl`) 과 합쳐 양방향 의존이 생겼고, `api/docs/architecture.md` 의 "바운디드 컨텍스트 간 상호작용" 표(`| Wallet | MarketData, InvestmentRound |`)와 코드가 모순된다. `api/docs/wallet/dependency.md` 에는 새 의존을 기록했지만 상위 문서는 갱신하지 않았다.
  - **수정 제안(방향 확정):** 코드 구조는 유지한다. 보유 내역(Position)은 trading 소유이므로 wallet 이 직접 바꾸지 않고 연동형 도메인 서비스 + ACL 로 trading 의 인바운드 포트에 위임하는 현재 구조가 이미 확정된 설계다(`BalanceChangeApplier` 와 대칭). 따라서 이벤트 재구조화가 아니라 **문서 정합화**로 해소한다: ① `architecture.md` 의 Wallet 행을 `MarketData, InvestmentRound, Trading` 으로 갱신하고, ② 같은 문서(또는 wallet/dependency.md 해당 절)에 "Trading↔Wallet 은 컨텍스트 단위로는 상호 의존이지만 유스케이스 경로 단위로는 비순환(BalanceChange: trading→wallet, MoveHolding: wallet→trading, 두 경로는 겹치지 않음)이며, 각 방향 모두 연동형 도메인 서비스 + ACL 로만 접근한다"는 설계 결정을 짧게 기록한다.

- [x] **[api/src/main/java/ksh/tryptobackend/trading/application/service/MoveHoldingService.java:48-69] 애플리케이션 서비스가 private 메서드 3개로 쪼개져 평평한 오케스트레이션 컨벤션 위반** (출처: 컨벤션) — **완료(`2cc43831`)**
  - **설명:** conventions.md 애플리케이션 서비스 규칙("private 메소드를 작성하지 않고 영어 읽히듯 메소드를 구현한다")과 달리 `hasNoHolding`·`acquisitionPriceOf`·`getPositionsInWalletIdOrder` 세 private 헬퍼에 분기·매핑·정렬 로직이 들어 있다. 같은 diff 의 `TransferCoinService` 와 레퍼런스 `PlaceOrderService` 는 평평한 단일 흐름이다.
  - **수정 제안:** 아래 3번 이슈와 **한 번에** 해소한다. 정렬·잠금 로직을 어댑터로 내리면(3번) 서비스 본문은 조회 → 가드 → 현재가 조회 → `holdingTransferrer.transfer` → 저장의 평평한 나열로 정리된다. 보유 없음 가드는 `Optional` 결과를 본문에서 바로 판별해 early return 한다.

- [x] **[api/src/main/java/ksh/tryptobackend/trading/application/service/MoveHoldingService.java:38-45] 지갑 ID 정렬 로드가 실제 잠금·저장 순서를 보장하지 않아 교착 방지가 성립하지 않음** (출처: 동시성 | 컨벤션) — **완료(`f038b09b`)**
  - **설명:** `PositionCommandPort.getOrCreate` 는 `@Lock` 없는 평범한 SELECT 라 읽기 순서를 정렬해도 DB 락이 걸리지 않고, 실제 배타 락이 걸리는 `save()`(UPDATE) 는 정렬과 무관하게 항상 출발→도착 고정 순서다. 반대 방향 동시 송금 시 교착 가능성이 그대로 남는다. 지금은 호출자 `TransferCoinService` 의 `getTransferBalancesWithLock` 이 같은 키 쌍을 먼저 정렬 잠금해 주는 덕에 우연히 재현되지 않을 뿐, `MoveHoldingUseCase` 는 범용 인바운드 포트로 문서화되어 있어 이 전제는 계약이 아니다. `api/docs/wallet/business-rules.md` 의 "(walletId, coinId) 오름차순 잠금" 규약을 Position 에도 실제로 적용해야 한다.
  - **수정 제안:** `WalletBalanceQueryAdapter.getTransferBalancesWithLock` 을 거울로 삼는다. ① `PositionJpaRepository` 에 `@Lock(PESSIMISTIC_WRITE)` 조회(`findWithLockByWalletIdAndCoinId`)를 추가하고, ② 포트에 출발·도착 Position 을 지갑 ID 오름차순으로 잠그며 getOrCreate 하는 조회 메소드를 추가해 정렬·잠금·생성 로직을 어댑터 안에 응집한다. ③ `save` 호출도 같은 정렬 순서를 따르게 한다. 포트 시그니처가 바뀌므로 인수 테스트의 `MockPositionAdapter` 도 함께 갱신한다(모든 getOrCreate 는 save 로 끝나야 한다는 기존 잠금 규약 유지).

## 3차 참고 이슈 (수정 안 함, 보고용)

- [MoveHoldingService.java:33,66] 출발 Position 을 `hasNoHolding` 과 `getOrCreate` 로 두 번 조회 (ddd | oop | 성능 | 컨벤션) — 차단 2·3번 반영 시 자연 해소 여지
- [MoveHoldingService.java:48] `hasNoHolding` 부정형 네이밍 (oop | 컨벤션) — 차단 2번 반영 시 자연 해소
- [MoveHoldingService.java:61-69] `Map<Long, Position>` + `.get()` 간접 구조, 도달 불가능한 병합 함수 (oop) — 차단 2·3번 반영 시 자연 해소
- [MoveHoldingService.java:37,54-58] 취득가 산정을 응용 서비스가 조립 — 현행은 `PlaceOrderService` 패턴과 정합, 향후 판단 분기가 붙으면 도메인 서비스로 이동 (ddd)
- [JpaPositionCommandAdapter.save] 기존 save() 의 재조회 패턴 비용을 새 호출부 2곳이 상속 — 트래픽 증가 시 `saveAll` 배치 포트 고려 (성능)
- [HoldingMover.java:7] `move(Transfer, Long toExchangeId)` — 애그리거트 + 낱개 원시 타입 혼합 파라미터 (oop)
- [TransferCoinService.java:62] 잔고 비관 락 보유 구간 안에서 Position·매핑·현재가 조회가 추가돼 임계 구역 증가 — 취득가 조회를 락 획득 전으로 이동 고려 (동시성)
- [MoveHoldingService 전체] 체결 vs 송금이 같은 Position 행을 두고 경쟁하는 낙관적 락 충돌 경로 신설 — 기존 409 `CONCURRENT_MODIFICATION` 처리로 데이터 손상은 없음 (동시성)
- [Position.java:51-55] release 의 보유량 캡핑 규칙이 spec.md 에 미기재 — 커밋 메시지에만 근거 존재 (컨벤션)

## 4차 차단 이슈 (3차 적용 후 재리뷰, `071a80a7..0b734e10`)

- [x] **[api/src/main/java/ksh/tryptobackend/trading/application/port/out/PositionCommandPort.java:12] `getTransferPositionsWithLock` 이 `get` 네이밍인데 결측 시 예외 없이 `Position.empty()` 를 생성 — 실제 계약은 getOrCreate** (출처: 컨벤션) — **완료(`16b288f6`)**
  - **설명:** conventions.md 의 `get` vs `find` 규칙상 `get` 은 대상이 반드시 존재해야 하고 없으면 예외를 던진다. 그러나 이 메서드 구현(`JpaPositionCommandAdapter.getOrCreateWithLock`)은 행이 없으면 `Position.empty(...)` 를 만들어 돌려준다 — 같은 인터페이스의 `getOrCreate` 와 동일한 "없으면 생성" 의미인데 접미어가 빠졌다. 거울 레퍼런스 `WalletBalanceQueryAdapter.getTransferBalancesWithLock` 은 결측 시 실제로 예외를 던져(`WALLET_BALANCE_NOT_FOUND`) `get` 계약을 지키므로, 이름은 같은 패턴인데 계약이 달라 혼동을 부른다.
  - **수정 제안:** `getTransferPositionsWithLock` → `getOrCreateTransferPositionsWithLock` 으로 리네이밍하고 `PositionCommandPort`·`JpaPositionCommandAdapter`·`MockPositionAdapter`·`MoveHoldingService` 호출부를 함께 바꾼다.

## 4차 참고 이슈 (수정 안 함, 보고용)

- [MoveHoldingService.java:30-36] 보유 없음 가드(무잠금 SELECT)와 실제 이동 대상(잠금 SELECT)이 서로 다른 스냅샷 — TOC-TOU 틈. 3차에서 이월된 항목이고 `release` 의 수량 클램프로 데이터 손상 없음. "보유 없으면 잠금·시세 조회 전 조기 반환"이라는 비용 절감과 트레이드오프 (ddd | oop)
- [PositionCommandPort.java:12] `WithLock` 접미어가 잠금 기술을 노출 — 다만 wallet 의 `getTransferBalancesWithLock` 을 의도적으로 거울 삼은 확립된 대칭 컨벤션이라 현행 유지 권장 (ddd)
- [JpaPositionCommandAdapter.java:24-63] public 메서드 나열이 조회 → 상태 변경 순서라 "상태 변경 → 판별 → 조회" 컨벤션과 어긋남 — 기존 순서를 답습한 것 (oop)
- [JpaPositionCommandAdapter.java:58-63] `saveAll` 의 (walletId, coinId) 정렬은 교착 방지에 실질 효과 없음(락은 이미 `getTransferPositionsWithLock` 에서 획득 완료). 레퍼런스 `WalletBalanceCommandAdapter.saveAll`·`MockPositionAdapter.saveAll` 은 정렬 없이 `forEach(this::save)` 뿐이라 이 구현만 비대칭 — 단순 `forEach` 로 정리 권장 (동시성)
- [JpaPositionCommandAdapter.java:58-63] `saveAll` 이 이름과 달리 배치가 아니라 `save()` 순차 호출이라 이미 잠금 로드한 엔티티를 `findById` 로 재조회 — 트래픽 증가 시 로드된 엔티티 직접 갱신으로 재조회 제거 고려 (성능)
- [JpaPositionCommandAdapter.java:33-39, 65-70] 목적지 Position 최초 생성 시(행 미존재) `FOR UPDATE` 가 잠글 대상이 없어 잠금 공백 — 현재는 호출자 `TransferCoinService` 의 선행 WalletBalance 잠금이 직렬화해 재현 안 됨. `MoveHoldingUseCase` 를 다른 경로에서 호출 시 재검토 필요 (동시성)
- [MoveHoldingService.java:39-47] Position 비관 락이 트랜잭션 커밋까지 유지돼, 동시 체결 통지(`SettleOrderService`)가 즉시 409 대신 행 락 대기로 바뀜 — 채택된 WalletBalance 패턴의 결과, 데이터 손상 없음. 체결 지연이 문제되면 락 타임아웃 힌트 검토 (동시성)
