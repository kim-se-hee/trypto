# 리뷰 이슈 — pending-order-matching

리뷰 범위 1차: `de56c90..599a3ab` (리팩토링 1커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어 모두 차단 0건·참고 0건. 이 기능은 API 측 이벤트 릴레이(engine.inbox 발행 + 체결 알림 수신→STOMP)로 이미 표준에 가까워, 체결 알림 수신 어댑터를 messaging 패턴에 맞춘 순수 이동/개명(`EngineOrderFilledListener` → `adapter/in/messaging`, 페이로드 DTO → `OrderFilledEngineMessage` 로 발행측 `{이벤트}{대상}Message` 명명과 대칭)으로 완결. 로직·계약 변경 없음(체결 수신 시 DB·크로스컨텍스트 없음 유지).

## 1차 판정 요약

- 유효 차단 0건 — 통과. Trading 컨텍스트 완료(place-order·cancel-order 완료 + pending-order-matching).
- 전용 인수 테스트 없음(엔진 연동 경로) → 컴파일·단위 테스트로 확인.
