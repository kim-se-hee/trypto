# 개요

여러 exchangeCoinId의 실시간 시세를 한 번의 Redis 호출로 일괄 조회한다.
기존 `GetLivePriceUseCase`(단건 조회)와 ISP 원칙에 따라 별도 UseCase로 분리한다.

# 배경

- Redis 키 형식: `ticker:{exchange}:{pair}` (예: `ticker:UPBIT:BTC/KRW`)
- 값: NormalizedTicker JSON (last_price 필드에 현재가)
- 기존 단건 조회는 N개 코인 보유 시 N번 Redis 왕복이 발생한다
- Redis MGET 명령으로 1번 왕복으로 통합한다

# UseCase

GetLivePricesUseCase.getCurrentPrices(Set<Long> exchangeCoinIds) → Map<Long, BigDecimal>

# 크로스 도메인 의존

없음 (자기 컨텍스트 단독 — exchange_coin, exchange, coin 테이블과 Redis 모두 marketdata 관할)

# 처리 로직

1. exchangeCoinIds 각각에 대해 Redis 키를 구성한다 (기존 ConcurrentHashMap 캐시 활용)
2. Redis MGET으로 모든 키의 값을 한 번에 조회한다
3. 각 결과를 파싱하여 Map<exchangeCoinId, BigDecimal>로 반환한다
4. 시세가 없는 exchangeCoinId가 있으면 PRICE_NOT_AVAILABLE 예외를 던진다

# 구현 범위

- UseCase: GetLivePricesUseCase (신규)
- Service: GetLivePricesService (신규)
- Output Port: LivePriceQueryPort (메서드 추가)
- Adapter: LivePriceQueryAdapter (메서드 추가, Redis MGET 활용)
- 기존 GetLivePriceUseCase/GetLivePriceService는 변경 없음
