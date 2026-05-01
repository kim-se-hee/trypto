다중 인스턴스 환경에서 단일 액티브 인스턴스만 시세를 수집하도록 보장한다. 다운스트림 중복 발행을 방지하면서, 액티브 인스턴스 장애 시 다른 인스턴스가 즉시 인계받는다.

## LeaderElection (@Component)

Redisson 분산 락 기반 리더 선출. 패키지: `config`

| 항목 | 값 |
|------|-----|
| 선출 간격 | 5초 |
| 방식 | Redisson 분산 락 |
| Lease | `tryLock(0, -1)` — leaseTime 무한, Watchdog이 30초마다 자동 연장 |

`isLeader() → boolean` — 현재 노드가 리더인지 반환한다.

리더십 변경 시 `LeadershipAcquiredEvent` / `LeadershipRevokedEvent`를 발행한다.

**ApplicationReadyEvent 시작:** 모든 빈과 `@EventListener` 등록이 완료된 후 스케줄러를 시작하여 초기 리더십 이벤트가 드롭되지 않도록 한다.

## LeaderLifecycleListener

리더십 이벤트를 받아 거래소 어댑터의 시작·정리를 트리거한다.

| 이벤트 | 동작 |
|--------|------|
| `LeadershipAcquiredEvent` | `ExchangeInitializer.start()` |
| `LeadershipRevokedEvent` | `ExchangeInitializer.stop()` |

## ExchangeInitializer (@Component)

리더 노드에서만 거래소 어댑터를 부팅한다. 패키지: `metadata`

**의존성:** 각 거래소 RestClient, 각 거래소 WebSocketHandler, MarketInfoCache, TickerRedisRepository, MarketMetadataRedisRepository, MeterRegistry

`start()`로 `ExecutorService(3)`를 생성하여 각 거래소 초기화를 별도 스레드에서 병렬 실행한다. `stop()`은 스레드풀을 `shutdownNow()`로 정리한다. `start()`/`stop()` 모두 멱등(null 체크로 중복 호출 안전)하며, `@PreDestroy`에서도 `stop()`을 호출하여 non-daemon 스레드의 JVM hang을 방지한다.

| 메서드 | 흐름 |
|--------|------|
| `loadAndConnectUpbit()` | REST 마켓 조회 → 캐시 적재 → Redis 메타데이터 저장 → REST 시세 조회 → Redis 초기 스냅샷 저장 → `upbitWebSocketHandler.connect()` |
| `loadAndConnectBithumb()` | REST 마켓 조회 → 캐시 적재 → Redis 메타데이터 저장 → REST 시세 조회 → Redis 초기 스냅샷 저장 → `bithumbWebSocketHandler.connect()` |
| `loadAndConnectBinance()` | REST 시세 조회 → 캐시 적재 + Redis 초기 스냅샷 → Redis 메타데이터 저장 → `binanceWebSocketHandler.connect()` |

각 초기화는 독립적으로 실행되어 하나가 실패해도 나머지에 영향이 없다. `initWithRetry()`로 감싸 실패 시 무한 재시도(지수 백오프, 최대 60초)로 복구한다.

**초기 시세 스냅샷:** 세 거래소 모두 REST API로 현재 시세를 조회하여 Redis에 저장한다. WebSocket 연결 전에도 시세를 제공하기 위함이다.
