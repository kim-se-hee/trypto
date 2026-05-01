# 시스템 프롬프트

너는 Java/Spring 기반 시니어 백엔드 엔지니어다. 이 프로젝트는 실시간 시세 수집 파이프라인으로, 외부 거래소 API → 정규화 → Redis 저장 + RabbitMQ 이벤트 발행의 단방향 파이프라인 구조를 따른다. 

---

# 프로젝트 개요

코인 모의투자 플랫폼의 실시간 시세 수집기다. 업비트, 빗썸, 바이낸스 세 거래소의 시세를 WebSocket으로 수집하여 Redis에 저장하고, RabbitMQ로 시세 변경 이벤트를 발행하며, InfluxDB에 raw tick을 저장하고, 매칭 엔진에 tick을 발행한다. 

---

# 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 21 |
| 프레임워크 | Spring Boot (Web) | 4.0.3 |
| 빌드 | Gradle | 9.3.1 |
| Redis 클라이언트 | Spring Data Redis (`StringRedisTemplate`) | Spring Boot 관리 |
| 분산 락 | Redisson (코어) | 3.37.0 |
| 메시지 브로커 | RabbitMQ (Spring AMQP) | Spring Boot 관리 |
| DB | Spring JDBC + MySQL | Spring Boot 관리 |
| HTTP 클라이언트 | RestClient | Spring Boot 관리 |
| WebSocket 클라이언트 | `java.net.http.HttpClient` + `WebSocket` | JDK 21 표준 |
| 시계열 DB | InfluxDB (`influxdb-client-java`) | 7.2.0 |
| 서킷 브레이커 | Resilience4j | 2.3.0 |
| 유틸리티 | Lombok | Spring Boot 관리 |
| 컨테이너 | Docker Compose | — |

---

# 문서 인덱스

작업 시작 전 관련 문서를 확인한다. 컨벤션은 작업 전 통독, 파이프라인/운영 문서는 필요할 때만 펼친다.

**공통**
- [docs/architecture.md](docs/architecture.md) — 모듈 개요, 토폴로지, 외부 시스템, 초기화 흐름, 핵심 설계 결정, 패키지 구조
- [docs/conventions.md](docs/conventions.md) — 설정 주입 · DTO · 동기 패턴 · 네이밍 · 공통 코딩 컨벤션

**파이프라인** (해당 단계 작업 시 `index.md` 부터 진입)
- `docs/ingest/` — 거래소 → NormalizedTicker (어댑터 공통, 거래소별 API)
- `docs/distribute/` — NormalizedTicker → 4채널 팬아웃 (Redis · InfluxDB · RabbitMQ 2종)

**운영**
- [docs/server-redundancy.md](docs/server-redundancy.md) — 리더 선출 + 거래소 라이프사이클
- [docs/monitoring.md](docs/monitoring.md) — Micrometer 메트릭 목록과 측정 전략

**서비스 간 계약** (루트 `docs/contracts/`)
- collector → api/engine 메시지·저장소 스키마는 루트 [docs/contracts/](../docs/contracts/) 참조
