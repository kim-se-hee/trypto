# 리뷰 이슈 — live-ticker-streaming

리뷰 범위 1차: `81e8058..b827e98` (리팩토링 5커밋 + 테스트 갱신 3커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건. 코드 변경 없이 리뷰 통과.

핵심 개선: `WarmupExchangeCoinMappingService` 가 타 UseCase 직접 주입 → 아웃풋 포트 협력으로 전환(ArchUnit "must not inject another UseCase" 위반 해소), 티커 해석을 커맨드/배치 결과 DTO로 정리, 매핑 캐시 저장소(`ExchangeCoinMappingCacheStore`) 분리로 어댑터 간 결합 제거, 매핑 조립을 일급 컬렉션 `ExchangeCoinMappings`(도메인)로 회수.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [ExchangeCoinMappings.java] `add()` 가 루프마다 내부 HashMap 전체 복사 O(n²) — 거래소 수 증가 시 워밍업 비용. 필요 시 mutable builder 누적 후 불변화 (oop/performance)
- [ExchangeCoinMappings.java add] `coinSymbols.getSymbol(coinId)` null 방어 없음 — spec 불변식상 실사용 영향 없음 (convention/ddd)
- [LiveTickerBatchResult.java] earliestTimestamp 별도 스트림 재계산 → resolve 단계에서 함께 누적 시 패스 1회 절감 가능 (performance)
- [ResolveLiveTickerService.java] `Optional.map().flatMap(Optional::stream)` 중첩 → 매핑 실패 다수 시 GC 압력 소폭. 단순 for+continue 대안 (performance)
- [ExchangeCoinMappingCacheStore.loadAll] `clear()+putAll()` 비원자적 — 현재는 웜업이 리스너 기동 전 완료돼 무해, 무중단 재웜업 도입 시 점검 (concurrency)

## 1차 판정 요약

- 유효 차단 0건 — 통과.
- live-ticker-streaming 인수 테스트(.feature) 없음 → 단위 테스트로 동작 보존 확인(워커가 test 커밋 3건으로 갱신·유지).
