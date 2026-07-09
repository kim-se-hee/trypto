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
