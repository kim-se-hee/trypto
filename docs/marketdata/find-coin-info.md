# 개요

코인 ID 목록으로 심볼과 이름을 일괄 조회한다. 기존 FindCoinSymbolsUseCase는 심볼만 반환하므로,
코인 이름(한국어명)까지 필요한 기능(포트폴리오 등)을 위해 추가한다.

# UseCase

FindCoinInfoUseCase.findByIds(Set<Long> coinIds) → Map<Long, CoinInfoResult>

# Result DTO

CoinInfoResult: symbol(String), name(String)

# 크로스 도메인 의존

없음 (자기 컨텍스트 단독)

# 구현 범위

- UseCase 인터페이스: FindCoinInfoUseCase
- Result DTO: CoinInfoResult
- Output Port: CoinQueryPort에 findInfoByIds 메서드 추가
- Service: FindCoinInfoService
- Adapter: CoinJpaPersistenceAdapter (Coin 엔티티의 symbol, name 조회)
