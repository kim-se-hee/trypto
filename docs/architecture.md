# 아키텍처

## 헥사고날 아키텍처

도메인이 외부 인프라에 의존하지 않는다. 모든 외부 통신은 Port를 통한다.

**Input Ports (인바운드)**

| Port | 어댑터 | 용도 |
|------|--------|------|
| REST Input Port | REST Controllers | HTTP 요청 → 도메인 |
| RabbitMQ Listener (marketdata) | LiveTickerEventListener | 시세 이벤트 수신 → WebSocket 브로드캐스트 |
| RabbitMQ Listener (trading) | TickerEventListener | 시세 이벤트 수신 → 지정가 매칭 |
| Batch Job Input Port | Batch Scheduler | 랭킹 집계 등 배치 작업 |

**Output Ports (아웃바운드)**

| Port | 어댑터 | 용도 |
|------|--------|------|
| Persistence Output Port | UserJpaPersistenceAdapter, WalletJpaPersistenceAdapter 등 | MySQL 읽기/쓰기 |
| Leaderboard Output Port | LeaderboardJpaPersistenceAdapter | 랭킹 집계 테이블 읽기/쓰기 |
| DEX Swap Output Port | JupiterApiAdapter, PancakeswapApiAdapter | 외부 DEX API 호출 |
| Live Price Output Port | LivePriceQueryAdapter | 코인 현재가 조회 (Redis) |
| Candle Data Output Port | CandleInfluxDataAdapter | 캔들 데이터 조회 |

- **Persistence Output Port:** 도메인별로 각각 존재하는 영속성 포트. User, Wallet, Order 등 도메인마다 별도의 포트와 어댑터가 있다.
- **Leaderboard Output Port:** 랭킹 집계 데이터를 조회하기 위한 포트. 일반적인 영속성과 목적이 다르므로 분리한다.
- **DEX Swap Output Port:** 외부 DEX API 호출을 추상화하는 포트. 새 DEX 추가 시 어댑터만 구현하면 된다.
- **Live Price Output Port:** 시세 수집기가 적재한 코인 현재가를 조회하기 위한 포트.
- **Candle Data Output Port:** 캔들 데이터를 조회하기 위한 포트.

## 패키지 구조

최상위는 바운디드 컨텍스트 기준으로 분리한다. 각 도메인 내부는 헥사고날 아키텍처의 계층별로 나눈다.

```
ksh.tryptobackend/
├── user/              # 회원, 프로필
├── trading/           # 주문 (시장가/지정가), 스왑
├── wallet/            # 지갑, 잔고, 입금 주소
├── transfer/          # 거래소 간 송금
├── portfolio/         # 포트폴리오 스냅샷, 보유 자산 조회
├── ranking/           # 수익률 랭킹
├── marketdata/        # 시세, 거래소·코인 정보
├── regretanalysis/    # 후회 그래프, 투자 원칙 위반 분석
├── investmentround/   # 투자 라운드, 투자 원칙
└── common/            # 공통 설정, 예외, DTO
```

각 도메인 내부는 adapter, application, domain 3개 영역으로 나눈다.

```
trading/
├── adapter/
│   ├── in/            # Controller (REST/WebSocket) — 인바운드 어댑터
│   │   └── dto/
│   │       ├── request/   # Request DTO
│   │       └── response/  # Response DTO
│   └── out/           # JpaPersistenceAdapter, ApiAdapter — 아웃바운드 어댑터
│       ├── entity/    # JPA 엔티티 클래스
│       └── repository/ # Spring Data JPA 리포지토리 인터페이스
├── application/
│   ├── port/
│   │   ├── in/        # UseCase 인터페이스 — 인바운드 포트
│   │   │   └── dto/
│   │   │       ├── command/   # Command (쓰기 요청)
│   │   │       ├── query/     # Query (읽기 요청)
│   │   │       └── result/    # Result (읽기 응답)
│   │   └── out/       # Repository/External Port 인터페이스 — 아웃바운드 포트
│   └── service/       # UseCase 구현체
└── domain/
    ├── model/         # Entity, Aggregate Root
    ├── vo/            # Value Object
    └── strategy/      # 도메인 전략 (선택, 필요 시)
```

common 패키지는 공통 설정, 예외, DTO를 관리한다.

```
common/
├── dto/
│   ├── request/       # 공통 Request DTO (PageRequestDto 등)
│   └── response/      # 공통 Response DTO (ApiResponseDto, PageResponseDto 등)
└── exception/         # ErrorCode, CustomException, GlobalControllerAdvice
```

## 계층별 규약

**Controller**
- UseCase 인터페이스에만 의존한다
- 비즈니스 로직 금지, 요청값 검증 + UseCase 위임만 수행한다
- 모든 응답은 공통 응답 DTO로 래핑한다

**UseCase**
- 하나의 유스케이스는 하나의 비즈니스 행위를 표현한다

**Service**
- 외부 연동이 필요한 경우 Output Port 인터페이스에만 의존한다
- 쓰기 작업에 `@Transactional`을 선언한다

**Adapter**
- Output Port 인터페이스를 구현한다

**Domain**
- 외부 의존 없이 순수 비즈니스 로직만 포함한다
- Aggregate 내부의 Entity/VO 변경은 반드시 Aggregate Root를 통해서만 수행한다
- Aggregate 간 참조는 ID로만 한다
