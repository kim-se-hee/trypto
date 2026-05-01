# 모듈 개요

collector 모듈은 거래소들의 실시간 시세를 수집, 저장, 브로드캐스트 한다. 거래소에서 받은 시세를 통일된 모델로 정규화한 뒤, 현재가 조회용 캐시·차트 집계용 시계열 저장소·실시간 푸시 이벤트 채널·매칭 엔진 입력 큐 네 곳에 동시에 분배한다.

# 토폴로지

거래소에서 수집 → 정규화 → 팬아웃의 단방향 파이프라인이다. 거래소마다 다른 포맷을 통일 모델로 변환해 여러 api 서버에 분배한다.

```
거래소 REST/WebSocket API
        │
        ▼
거래소 어댑터 (Upbit / Bithumb / Binance)
        │ 정규화
        ▼
NormalizedTicker
        │
        ▼
TickerSinkProcessor 
        │
        ├──► InfluxDB 
        ├──► Redis ticker:{EX}:{BASE}/{QUOTE}     
        ├──► RabbitMQ ticker.exchange (Fanout)     (to api)
        └──► RabbitMQ engine.inbox (durable queue) (to engine)
```

- **거래소 격리:** 한 거래소가 죽어도 나머지 거래소 수집은 계속된다
- **이중화:** 여러 인스턴스 중 리더 하나만 실제로 수집하여 시세 수집을 이중화한다.

# 외부 시스템

| 시스템 | 역할 |
|--------|------|
| 거래소 REST/WebSocket API | 마켓 메타 조회, 실시간 시세 수신, REST 폴백 폴링 |
| Redis | 현재가 캐시(TTL 30s), 마켓 메타데이터, Redisson 분산 락 |
| InfluxDB | raw tick 저장 → 서버 사이드 Task가 캔들(OHLC) 집계 |
| RabbitMQ | 실시간 시세 이벤트 발행, 매칭 엔진 입력 전달 |


# 패키지 구조

최상위 패키지는 파이프라인 각 단계를 기준으로 나눈다.

```
ksh.tryptocollector/
├── model/         # 도메인 모델
├── metadata/      # 마켓 마스터 데이터
├── ingest/        # 외부 시세 수신과 정규화
├── distribute/    # 정규화된 시세 팬아웃
├── ha/            # 단일 액티브 인스턴스 보장
└── config/        # 인프라 설정
```

거래소 패키지 내부는 동일한 구조를 따른다.

```
ingest/{exchange}/
├── {Exchange}RestClient.java          # 마켓 목록 + 시세 스냅샷 REST 조회
├── {Exchange}WebSocketHandler.java    # 실시간 시세 WebSocket 수신
├── {Exchange}TickerPoller.java        # WebSocket 폴백용 REST 폴러
├── {Exchange}MarketResponse.java      # REST 응답 DTO
├── {Exchange}TickerResponse.java      # REST 응답 DTO
└── {Exchange}TickerMessage.java       # WebSocket 메시지 DTO
```
