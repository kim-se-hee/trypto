# 모듈 개요

api 모듈은 **사용자 요청 처리와 모의 투자 핵심 비즈니스 로직**을 담당한다. collector가 발행하는 실시간 시세와 engine이 발행하는 매칭 결과를 받아 비즈니스 로직을 처리하며, 프론트엔드와는 REST·STOMP로 통신한다.


# 아키텍쳐

api 모듈은 헥사고날 아키텍처와 DDD를 따른다. 모듈은 여러 도메인으로, 각 도메인은 여러 유스케이스로 구성되며, 외부 세계와는 포트로 소통한다.

- **인바운드 포트** — 사용자 요청 수신, 실시간 시세 수신, 체결 결과 수신, 정기 배치 트리거
- **아웃바운드 포트** — DB·메시지 큐·WebSocket 같은 인프라 호출, 외부 API 호출이

# 외부 시스템

- **MySQL** — 도메인 트랜잭션 쓰기 경로 전부를 담당하는 주 저장소.
- **Redis · InfluxDB** — 시세·캔들 저장소
- **RabbitMQ** — 실시간 시세 수신, 주문 이벤트(접수·취소) 송출, 체결 결과 수신. 채널 스펙은 루트의 `docs/contracts/`에 존재

# 바운디드 컨텍스트 간 상호작용

| From | Depends on |
|------|------------|
| Wallet | MarketData, InvestmentRound |
| InvestmentRound | MarketData, Wallet |
| Trading | Wallet, MarketData, InvestmentRound |
| Transfer | Wallet, InvestmentRound |
| Portfolio | InvestmentRound, Wallet, MarketData, Trading |
| Ranking | InvestmentRound, Portfolio, MarketData, Wallet, Trading |
| RegretAnalysis | InvestmentRound, Trading, MarketData, Wallet, Portfolio |

컨텍스트별 의존관계 상세는 `{domain}/dependency.md` 참고.

# Common Shared Kernel

여러 컨텍스트가 공유하는 최소 모델은 `common` 에만 둔다.

- `RuleType` — 투자 원칙 종류 (LOSS_CUT, PROFIT_TAKE, CHASE_BUY_BAN, AVERAGING_DOWN_LIMIT, OVERTRADING_LIMIT)
- `ProfitRate` — 수익률

# 패키지 구조

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

# 계층별 규약

세부 작성 룰은 [conventions.md](conventions.md) 의 "레이어별 컨벤션" 섹션을 참고한다.
