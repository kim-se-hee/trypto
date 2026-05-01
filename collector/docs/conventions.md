# 설정 주입

- 외부 설정값은 `@Value`로 주입한다. `@ConfigurationProperties`는 사용하지 않는다
- 기본값을 SpEL로 명시한다: `@Value("${ticker.redis-ttl-seconds:30}")`
- `@Value` 설정값은 `private` 필드에 직접 주입한다 (`final` 제외)

# DTO

- DTO는 `record`로 작성한다
- REST 응답 DTO: `{거래소}MarketResponse`, `{거래소}TickerResponse`
- WebSocket 메시지 DTO: `{거래소}TickerMessage`
- WebSocket 메시지 DTO에 `toNormalized(String displayName)` 변환 메서드를 둔다
- Jackson `FAIL_ON_UNKNOWN_PROPERTIES = false`는 Spring Boot 자동 설정에 의존한다 (별도 ObjectMapperConfig 불필요)

# 동기 패턴

- 모든 I/O는 동기 호출로 처리한다. Reactor(`Mono`/`Flux`) 사용 금지
- Redis: `StringRedisTemplate` 사용
- REST: `RestClient` 사용 (`RestClient.Builder` 자동 구성 주입)
- WebSocket: `java.net.http.HttpClient` + `WebSocket` 사용
- WebSocket 재연결: while 루프 + `Thread.sleep()` 지수 백오프 (최대 60초)
- 거래소별 WebSocket은 별도 스레드에서 블로킹 실행 (`ExecutorService`)

# 네이밍

- REST 클라이언트: `{거래소}RestClient` (예: `UpbitRestClient`)
- WebSocket 핸들러: `{거래소}WebSocketHandler` (예: `UpbitWebSocketHandler`)
- 인터페이스: `ExchangeTickerStream` — `void connect()` 메서드 정의
- 캐시: `MarketInfoCache` — `ConcurrentHashMap` 기반 스레드 안전 캐시
- Redis 저장소: `TickerRedisRepository`
- `get` vs `find`: `get`은 대상이 반드시 존재한다고 가정하며 없으면 예외를 던진다. `find`는 대상이 없을 수 있으며 `Optional` 또는 빈 컬렉션을 반환한다

# 공통

- 모든 의존성은 `@RequiredArgsConstructor` + `private final`로 생성자 주입한다. `@Autowired` 필드 주입 금지
- Entity에는 `@Getter`만 허용하고 `@Setter`, `@Data` 금지
- 컬렉션을 반환할 때 null 대신 빈 컬렉션을 반환한다
- `Optional`은 메서드 반환 타입으로만 사용한다. 필드나 파라미터에 사용하지 않는다
- `Optional.get()` 직접 호출 금지. `orElseThrow()`로 명시적 예외를 던진다
- 매직 넘버/매직 상수를 사용하지 않는다
- 메서드 나열 순서: public 메서드를 먼저, private 메서드를 아래에 배치한다
