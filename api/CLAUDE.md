# 시스템 프롬프트

너는 Java/Spring 기반 시니어 백엔드 엔지니어다. 모든 코드는 유지보수성, 가독성, 테스트 용이성을 최우선으로 한다. 헥사고날 아키텍처의 포트/어댑터 경계를 엄격히 지키고, 도메인 로직이 인프라에 의존하지 않도록 한다.

---

# 프로젝트 개요

코인 모의투자 플랫폼 — "큰 돈 잃을 걱정 없이 해보는 실전 리허설"

**해결하려는 문제:** 해외 거래소·DEX 투자를 망설이는 중급 코인 투자자에게 안전한 연습 환경이 없다. 복잡한 송금/지갑/스왑 과정, 실수 시 복구 불가능한 자금 손실 — 이 모든 걸 실전과 동일하게 체험하되 진짜 돈을 잃지 않는 환경을 제공한다.

**핵심 기능:** 업비트/빗썸/바이낸스 실시간 시세 기반 시장가/지정가 주문, 거래소 간 송금 시뮬레이션, DEX 스왑(슬리피지/가스비 실패 체험), 투자 원칙 설정 및 위반 분석을 통한 투자 복기용 그래프 제공, 수익률 랭킹 및 고수 포트폴리오 열람.

---

# 기술 스택

| 분류 | 기술 | 버전 |
|---|---|---|
| 언어 | Java | 21 |
| 프레임워크 | Spring Boot | 4.0.3 |
| 빌드 | Gradle | 1.1.7 |
| ORM | Spring Data JPA (Hibernate) | Spring Boot 관리 |
| 쿼리 | QueryDSL | 5.1.0 |
| 실시간 | Spring WebSocket (STOMP) | Spring Boot 관리 |
| 메시지 큐 | Spring AMQP | Spring Boot 관리 |
| 배치 | Spring Batch | Spring Boot 관리 |
| 시계열 DB | influxdb-client-java | 7.2.0 |
| RDB | MySQL | 8.0.30 |
| 시계열 DB | InfluxDB | 2.x |
| 캐시 | Redis | 7.x |
| 메시지 브로커 | RabbitMQ | 4.x |

---

# 문서 인덱스

작업 시작 전 관련 문서를 확인한다. 컨벤션은 작업 전 통독, 기능/도메인 문서는 필요할 때만 펼친다.

**토대 (작업 전 필독)**
- [docs/architecture.md](docs/architecture.md) — 헥사고날 포트/어댑터, 패키지 구조, 컨텍스트 의존 그래프, Common Shared Kernel
- [docs/conventions.md](docs/conventions.md) — DTO · 에러 처리 · 공통 · 레이어별 코딩 컨벤션

**도메인별 문서** (해당 도메인 작업 시 `index.md` 부터 진입)

각 도메인 디렉토리에는 `index.md`, `aggregate.md`(Aggregate 구조·소유 관계), `dependency.md`(제공/의존 UseCase) 와 기능 명세가 함께 있다.

- `docs/trading/` — 시장가/지정가 주문, 취소, 매칭
- `docs/wallet/` — 지갑·잔고·입금 주소
- `docs/transfer/` — 거래소 간 송금
- `docs/portfolio/` — 보유 자산
- `docs/ranking/` — 수익률 랭킹
- `docs/marketdata/` — 시세, 거래소·코인 메타
- `docs/regretanalysis/` — 후회 그래프, 위반 분석
- `docs/investmentround/` — 투자 라운드, 긴급 충전
- `docs/user/` — 회원, 프로필

**시스템 단위**
- [docs/realtime-ticker.md](docs/realtime-ticker.md) · [docs/websocket.md](docs/websocket.md) · [docs/candle-data.md](docs/candle-data.md)
- `docs/batch/` — 배치 잡 설계
- `docs/screen/` — 화면 단위 통합 명세
- [docs/testing.md](docs/testing.md)
