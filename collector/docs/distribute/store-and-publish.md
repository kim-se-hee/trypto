정규화된 시세(`NormalizedTicker`)를 두 곳에 저장(Redis, InfluxDB)하고 두 채널로 발행(RabbitMQ)한다.

## 오케스트레이션

`TickerSinkProcessor` (@Component, 패키지 `exchange`) — WebSocket 핸들러가 정규화한 시세를 받아 네 채널 모두에 팬아웃한다.

**의존성:** `TickRawWriter`, `TickerRedisRepository`, `TickerEventPublisher`, `EngineInboxPublisher`

| 단계 | 호출 | 채널 |
|------|------|------|
| 1 | `TickRawWriter.write()` | InfluxDB raw tick |
| 2 | `TickerRedisRepository.save()` | Redis 시세 캐시 |
| 3 | `TickerEventConflator.submit()` | RabbitMQ `ticker.exchange` |
| 4 | `EngineInboxPublisher.publish()` | RabbitMQ `engine.inbox` |

**격리:** 각 단계를 try/catch로 감싸 한 채널 실패가 다른 채널로 전이되지 않도록 한다.

## 저장 · Redis 시세 캐시

`TickerRedisRepository` (@Component, 패키지 `redis`) — 정규화된 시세를 Redis에 JSON으로 저장한다.


**설정값:** `@Value`로 `ticker.redis-ttl-seconds`(기본 30)와 `ticker.redis-key-prefix`(기본 `"ticker"`)를 주입한다.

WebSocket이 끊겨 갱신이 중단되면 30초 후 키가 만료되어 소비자가 "시세 없음"을 인지할 수 있다.

> 키 포맷·값 스키마는 [docs/contracts/redis-marketdata.md](../../../docs/contracts/redis-marketdata.md) 의 *정규화 티커* 절 참조.

## 저장 · Redis 마켓 메타데이터

거래소별 마켓 메타데이터를 Redis에 JSON 배열로 저장한다. api가 기동 시 조회하여 `coin`/`exchange_coin` 테이블을 초기화한다.

**설정값:** `@Value`로 `market-meta.redis-key-prefix`(기본 `"market-meta"`)를 주입한다.

TTL 없음. 수집기 재기동 시 덮어쓰기. 

> 키 포맷·값 스키마는 [docs/contracts/redis-marketdata.md](../../../docs/contracts/redis-marketdata.md) 의 *마켓 메타데이터* 절 참조.

## 저장 · InfluxDB raw tick + 캔들 Task

`TickRawWriter` (@Component, 패키지 `tick`) — 매 시세 tick을 InfluxDB `ticker_raw` measurement에 기록한다. InfluxDB Task가 서버 사이드에서 이 데이터를 원본으로 캔들(OHLC)을 계단식으로 집계한다.

### Raw tick 스키마 (`ticker_raw`)

raw tick은 collector 내부 origin 데이터로 외부 계약이 없다. 본 문서가 단일 소스다.

| 구분 | 이름 | 설명 |
|------|------|------|
| tag | `exchange` | 거래소 (UPBIT, BITHUMB, BINANCE) |
| tag | `symbol` | 거래 페어 (BTC/KRW, ETH/USDT) |
| field | `price` | 현재가 (double) |
| timestamp | | 수집 시각 (epoch ms) |

### 캔들 Task 체인

계단식 집계로 상위 타임프레임을 생성한다. 각 Task는 바로 아래 단계의 measurement를 원본으로 사용하여 계산량을 최소화한다.

```
ticker_raw → candle_1m (매 1분)
candle_1m  → candle_5m (매 5분)
candle_1m  → candle_1h (매 1시간)
candle_1h  → candle_4h (매 4시간)
candle_1h  → candle_1d (매 1일)
candle_1d  → candle_1w (매 1주, 월요일 시작)
candle_1d  → candle_1M (매 1개월, 1일 시작)
```

| measurement | source | 집계 주기 | 집계 방식 | 비고 |
|-------------|--------|-----------|-----------|------|
| `candle_1m` | `ticker_raw` | 1분 | `first(price)`, `max(price)`, `min(price)`, `last(price)` | raw tick에서 직접 집계 |
| `candle_5m` | `candle_1m` | 5분 | `first(open)`, `max(high)`, `min(low)`, `last(close)` | |
| `candle_1h` | `candle_1m` | 1시간 | 동일 | |
| `candle_4h` | `candle_1h` | 4시간 | 동일 | |
| `candle_1d` | `candle_1h` | 1일 | 동일 | |
| `candle_1w` | `candle_1d` | 1주 | 동일 | `offset: 4d`로 월요일 시작 |
| `candle_1M` | `candle_1d` | 1개월 | 동일 | calendar duration |

Task 정의는 `influxdb/init-tasks.sh`에 있으며, Docker 초기 setup 시 자동 생성된다.

**실행 순서** — Task 간 offset 체인으로 이전 단계의 write 완료를 보장한다.

| Task | offset | 이유 |
|------|--------|------|
| `candle_1m` | 10s | `TickRawWriter`의 write 전파 대기 |
| `candle_5m` | 30s | `candle_1m` Task 완료 대기 |
| `candle_1h` | 1m | `candle_1m` Task 완료 대기 |
| `candle_4h`, `candle_1d` | 2m | `candle_1h` Task 완료 대기 |
| `candle_1w`, `candle_1M` | 3m | `candle_1d` Task 완료 대기 |

> 결과물인 캔들 measurement 스키마(api 가 읽는 형식)는 [docs/contracts/influx-candle.md](../../../docs/contracts/influx-candle.md) 참조.

## 발행 · RabbitMQ `ticker.exchange`

`NormalizedTicker` 묶음을 `TickerBatchEvent`(거래소 + 동일 거래소의 ticker 리스트) 로 변환하여 Fanout Exchange `ticker.exchange`에 발행한다. api 인스턴스들이 수신하여 batch 단위로 클라이언트에 WebSocket 브로드캐스트한다.

| 항목 | 값 |
|------|-----|
| 발행 단위 | 거래소마다 50ms 마다 1건. 그 사이 들어온 시세들이 한 메시지에 묶여 나간다 |
| Publisher Confirms | `correlated` 모드 (nack 시 로그 경고) |
| 에러 처리 | 직렬화/발행 실패 시 로그 경고 (시세 수집을 중단하지 않음) |

거래소·심볼별로 최신 시세 1건만 유지하다 50ms 마다 flush 한다. 같은 심볼로 윈도우 안에 N건 들어오면 마지막 1건만 살아남고, 거래소별로 묶어 1 메시지로 내보낸다. marketdata 채널만 대상이며, 매칭 엔진은 그대로 매 건 발행한다.

큐 바인딩은 소비자(api)가 담당한다. collector는 Exchange만 선언한다.

> Exchange 토폴로지·페이로드 스키마는 [docs/contracts/ticker-exchange.md](../../../docs/contracts/ticker-exchange.md) 참조.

## 발행 · RabbitMQ `engine.inbox`

`NormalizedTicker`를 tick 페이로드로 직렬화하여 매칭 엔진의 durable queue `engine.inbox`에 발행한다.

| 항목 | 값 |
|------|-----|
| 헤더 | `event_type=TickReceived` (collector가 발행하는 유일한 타입) |
| 에러 처리 | 직렬화/발행 실패 시 로그 경고 (시세 수집을 중단하지 않음) |

큐 선언은 collector가 담당한다. engine은 같은 큐에서 자체 발행한 `OrderPlaced`/`OrderCanceled`도 함께 소비하므로, collector는 `event_type` 헤더로 자기 이벤트를 표시한다. 

**at-least-once 보장** — collector는 Publisher Confirms로 브로커 수신만 확인한다. 그 이후 이벤트 순서·idempotency는 engine 책임(WAL + 주문 상태 PENDING 체크).

> Queue 토폴로지·페이로드 스키마는 [docs/contracts/engine-inbox.md](../../../docs/contracts/engine-inbox.md) 참조.
