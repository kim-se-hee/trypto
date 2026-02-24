# 시스템 프롬프트

너는 Java/Spring 기반 시니어 백엔드 엔지니어다. 모든 코드는 유지보수성, 가독성, 테스트 용이성을 최우선으로 한다. 헥사고날 아키텍처의 포트/어댑터 경계를 엄격히 지키고, 도메인 로직이 인프라에 의존하지 않도록 한다.

---

# 프로젝트 개요

코인 모의투자 플랫폼 — "큰 돈 잃을 걱정 없이 해보는 실전 리허설"

**해결하려는 문제:** 해외 거래소·DEX 투자를 망설이는 중급 코인 투자자에게 안전한 연습 환경이 없다. 복잡한 송금/지갑/스왑 과정, 실수 시 복구 불가능한 자금 손실 — 이 모든 걸 실전과 동일하게 체험하되 진짜 돈을 잃지 않는 환경을 제공한다.

**핵심 기능:** 업비트/빗썸/바이낸스 실시간 시세 기반 시장가/지정가 주문, 거래소 간 송금 시뮬레이션, DEX 스왑(슬리피지/가스비 실패 체험), 투자 원칙 설정 및 위반 분석을 통한 투자 복기용 그래프 제공, 수익률 랭킹 및 고수 포트폴리오 열람.

**기술 스택**

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 21 |
| 프레임워크 | Spring Boot | 4.0.3 |
| 빌드 | Gradle | |
| ORM | Spring Data JPA | |
| 쿼리 | QueryDSL | 5.1.0 |
| DB | MySQL | 8.0.30 |
| 시계열 DB | InfluxDB | |
| 캐시 | Redis | |
| 메시지 브로커 | RabbitMQ | |

---

# 아키텍처

## 헥사고날 아키텍처

도메인이 외부 인프라에 의존하지 않는다. 모든 외부 통신은 Port를 통한다.

**Input Ports (인바운드)**

| Port | 어댑터 | 용도 |
|------|--------|------|
| REST Input Port | REST Controllers | HTTP 요청 → 도메인 |
| WebSocket Input Port | WebSocket Controllers | 클라이언트에게 실시간 시세 push |
| Batch Job Input Port | Batch Scheduler | 랭킹 집계 등 배치 작업 |
| Matching Job Input Port | RabbitMQ Listener | 시세 이벤트 수신 → 지정가 매칭 |

**Output Ports (아웃바운드)**

| Port | 어댑터 | 용도 |
|------|--------|------|
| Persistence Output Port | UserJpaPersistenceAdapter, WalletJpaPersistenceAdapter 등 | MySQL 읽기/쓰기 |
| Leaderboard Output Port | LeaderboardJpaPersistenceAdapter | 랭킹 집계 테이블 읽기/쓰기 |
| DEX Swap Output Port | JupiterApiAdapter, PancakeswapApiAdapter | 외부 DEX API 호출 |
| Live Price Output Port | LivePriceRedisAdapter | 코인 현재가 조회 |
| Candle Data Output Port | CandleInfluxDataAdapter | 캔들 데이터 조회 |

- **Persistence Output Port:** 도메인별로 각각 존재하는 영속성 포트. User, Wallet, Order 등 도메인마다 별도의 포트와 어댑터가 있다.
- **Leaderboard Output Port:** 랭킹 집계 데이터를 조회하기 위한 포트. 일반적인 영속성과 목적이 다르므로 분리한다.
- **DEX Swap Output Port:** 외부 DEX API 호출을 추상화하는 포트. 새 DEX 추가 시 어댑터만 구현하면 된다.
- **Live Price Output Port:** 시세 수집기가 적재한 코인 현재가를 조회하기 위한 포트.
- **Candle Data Output Port:** 캔들 데이터를 조회하기 위한 포트.

## 패키지 구조

최상위는 바운디드 컨텍스트 기준으로 분리한다. 각 도메인 내부는 헥사고날 아키텍처의 계층별로 나눈다.

```
com.project/
├── identity/          # 인증, 회원
├── trading/           # 주문 (시장가/지정가), 스왑
├── wallet/            # 지갑, 잔고, 송금
├── portfolio/         # 포트폴리오, 랭킹
├── marketdata/        # 시세, 캔들
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
    └── vo/            # Value Object
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

---

# 데이터 모델

도메인별 Aggregate 구조와 모듈 간 의존 관계를 정의한다.

@docs/data-model.md

## ERD

MySQL 테이블 구조와 관계를 Mermaid ERD로 정의한다. 캔들/시세 데이터는 Redis·InfluxDB에서 관리하므로 ERD에 포함하지 않는다.

@docs/schema.md

---

# 코딩 컨벤션

## DTO

- DTO는 `record`로 작성한다 (Request, Response, Command, Query 모두)
- Command/Query DTO는 application 계층이 adapter 계층의 Request/Response에 의존하지 않기 위해 존재한다

**Request DTO**
- `adapter/in/dto/request/` 패키지에 위치한다
- Jakarta Bean Validation 어노테이션으로 형식 검증만 수행한다 (`@NotBlank`, `@NotNull`, `@Min` 등)
- Controller에서 `@Valid`로 검증을 트리거한다
- 비즈니스 로직 검증은 반드시 도메인 모델에서 수행한다

```java
public record PlaceOrderRequest(
    @NotNull UUID clientOrderId,
    @NotNull Long walletId,
    @NotNull @Min(0) BigDecimal amount
) {}
```

**Response DTO**
- `adapter/in/dto/response/` 패키지에 위치한다
- 모든 API 응답은 `ApiResponseDto<T>`로 래핑한다

**Command DTO**
- `application/port/in/dto/command/` 패키지에 위치한다
- Controller에서 Request DTO → Command 변환하여 UseCase에 전달한다
- 네이밍: `{행위}Command` (예: `PlaceOrderCommand`)

**Query DTO**
- `application/port/in/dto/query/` 패키지에 위치한다
- Controller에서 Request DTO → Query 변환하여 UseCase에 전달한다
- 네이밍: `{행위}Query` (예: `FindOrderHistoryQuery`)

**Result DTO**
- `application/port/in/dto/result/` 패키지에 위치한다
- 여러 Aggregate를 조합하거나 도메인 모델로 표현할 수 없는 조회 결과에 사용한다
- 단일 Aggregate 조회는 도메인 모델을 직접 반환하므로 Result가 필요 없다
- 네이밍: `{자원}Result` (예: `OrderHistoryResult`, `OrderAvailabilityResult`)

**공통 DTO (`common/dto/`)**
- `ApiResponseDto<T>`: status(HTTP 상태 코드), code(응답 코드), message(응답 메시지), data(응답 데이터)
- `PageRequestDto`: page(페이지 번호, 0부터 시작), size(페이지 크기, 1~50)
- `PageResponseDto<T>`: page(현재 페이지 번호), size(페이지 크기), totalPages(전체 페이지 수), content(목록)

## 에러 처리

**구성 요소**

- `ErrorCode` enum: HTTP 상태 코드와 메시지 키를 정의한다
- `CustomException`: `ErrorCode`를 받아 던지는 커스텀 예외이다
- `messages.properties`: 에러 메시지를 한국어로 관리한다 (i18n)
- `GlobalControllerAdvice`: `@RestControllerAdvice`에서 전역으로 예외를 처리하고 표준화된 응답을 반환한다

**응답 형식**

```json
{ "status": 400, "code": "INSUFFICIENT_BALANCE", "message": "잔고가 부족합니다.", "data": {} }
```

**에러 추가 방법**

1. `ErrorCode` enum에 에러를 정의한다
   ```java
   INSUFFICIENT_BALANCE(400, "insufficient.balance"),
   ```

2. `messages.properties`에 메시지를 추가한다
   ```properties
   insufficient.balance=잔고가 부족합니다.
   ```

3. 서비스에서 예외를 던진다
   ```java
   throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
   ```

**파라미터가 포함된 메시지**

```java
// ErrorCode
INVALID_PAGE_SIZE(400, "invalid.page.size"),

// messages.properties
invalid.page.size=잘못된 페이지 크기입니다: {0}

// 서비스
throw new CustomException(ErrorCode.INVALID_PAGE_SIZE, Arrays.asList(requestSize));
```

## 공통 컨벤션

- 모든 의존성은 `@RequiredArgsConstructor` + `private final`로 생성자 주입한다. `@Autowired` 필드 주입 금지
- 컬렉션을 반환할 때 null 대신 빈 컬렉션을 반환한다
- `Optional`은 메서드 반환 타입으로만 사용한다. 필드나 파라미터에 사용하지 않는다
- `Optional.get()` 직접 호출 금지. `orElseThrow()`로 명시적 예외를 던진다
- 메서드는 하나의 책임만 가져야 하며 20라인을 넘어가면 분리를 고려한다
- 클래스는 단일 책임 원칙을 지킨다. 분리 시 재사용 가능성과 변경 주기를 함께 고려한다. 여러 곳에서 호출되면 분리하고, 항상 같이 바뀌고 따로 쓸 일이 없다면 하나로 둔다
- `get` vs `find` 네이밍: `get`은 대상이 반드시 존재한다고 가정하며 없으면 예외를 던진다. `find`는 대상이 없을 수 있으며 `Optional` 또는 빈 컬렉션을 반환한다

## 레이어별 컨벤션

**Controller**
- 클래스명: `{도메인}Controller` (예: `OrderController`, `SwapController`)
- 메서드명: HTTP 메서드 + 자원을 표현한다 (예: `createOrder()`, `getPortfolio()`)
- `@Valid`로 Request DTO의 형식 검증을 트리거한다
- Request DTO를 서비스 계층에 직접 넘기지 않는다. Controller에서 Command/Query 객체로 변환하여 전달한다
- UseCase 반환값(도메인 모델 또는 Result)을 Response DTO로 변환하는 책임은 Controller에 있다
- 응답은 반드시 `ApiResponseDto<T>`로 래핑한다

**UseCase**
- 인터페이스명: `{비즈니스행위}UseCase` (예: `PlaceMarketBuyOrderUseCase`, `ExecuteSwapUseCase`)
- 하나의 유스케이스에 하나의 메서드를 정의한다
- Command UseCase: 도메인 모델을 반환한다
- Query UseCase: 단일 Aggregate 조회는 도메인 모델을, 여러 Aggregate 조합이 필요하면 Result DTO를 반환한다
- Controller에서 반환값(도메인 모델 또는 Result)을 Response DTO로 변환한다
- UseCase가 adapter 계층의 Request/Response DTO를 import하지 않는다

**Service**
- 클래스명: `{UseCase명}Service` (예: `PlaceMarketBuyOrderService`)
- 메서드명은 비즈니스 의미를 반영한다 (예: `placeMarketBuyOrder()`, `executeSwap()`)
- 도메인 로직을 직접 수행하지 않고 도메인 객체와 포트를 조합하는 오케스트레이션을 담당한다
- 쓰기 작업에 `@Transactional`을 선언한다

**Domain**
- 비즈니스 로직은 도메인 객체 안에 위치한다
- 메서드명은 비즈니스 지식을 담는다 (예: `deductBalance()`, `checkSlippageExceeded()`)
- Entity에는 `@Getter`만 허용하고 `@Setter`, `@Data` 금지. 상태 변경은 비즈니스 의미를 가진 메서드로만 수행한다
- VO는 불변 객체로 만든다. 모든 필드 `final`, 변경이 필요하면 새 객체를 생성한다
- VO는 `equals()`/`hashCode()`를 반드시 구현한다
- 일급 컬렉션을 활용하여 컬렉션 관련 로직을 캡슐화하려고 노력한다

**JPA 엔티티**
- 비즈니스 로직과 ERD에 따라 `@Column`으로 제약사항을 적절히 명시한다 (`nullable`, `unique`, `length`, `precision`, `scale` 등)
- 감사 추적이나 데이터 복구가 필요한 엔티티에는 소프트 딜리트를 적용한다 (예: User, InvestmentRound). `@SQLDelete` + `@Where`를 사용하고 `isDeleted` 필드를 둔다

```java
@SQLDelete(sql = "UPDATE user SET is_deleted = true WHERE user_id = ?")
@Where(clause = "is_deleted = false")
```

**Adapter Out**
- Persistence 클래스명: `{도메인}JpaPersistenceAdapter` (예: `OrderJpaPersistenceAdapter`)
- External API 클래스명: `{외부서비스}ApiAdapter` (예: `JupiterApiAdapter`)
- 메서드명은 비즈니스 로직을 드러내지 않고 데이터 조회 조건을 표현한다 (예: `findByUserIdAndCoin()`, `saveOrder()`)
- 조건이 2개 이하인 단순 조회는 Spring Data JPA 쿼리 메서드를 활용한다
- 조건이 복잡하거나 동적 쿼리가 필요한 경우 반드시 QueryDSL을 사용한다
- N+1 문제 방지를 위해 Fetch Join이나 Batch Size를 고려한다

---

# 테스트 전략

**인수 테스트**
- Cucumber를 이용하여 사용자 시나리오가 정상 작동하는지 검증한다
- 모든 사용자 시나리오에 대해서 인수 테스트를 진행한다

**도메인 단위 테스트**
- 비즈니스 로직이 복잡하거나 높은 정확성이 필요하여 빠른 피드백이 필요한 경우에만 작성한다
- 예: 슬리피지 계산, 가스비 부족 판별, 수수료 계산 등 엣지 케이스가 많은 로직
- 엣지 케이스는 가능하면 경계값 테스트를 진행한다
- 시간 관련 로직은 `Clock`을 빈으로 주입받아 처리하고 테스트 시 Mock Clock으로 제어한다

**서비스 계층 테스트**
- 비즈니스 로직이 도메인에 있으므로 서비스는 단순 오케스트레이션만 남는다
- 오케스트레이션의 결함은 인수 테스트로 자연스럽게 검증할 수 있다
- 따라서 서비스 계층 테스트는 생략한다

**테스트 작성 규칙**
- 공통: Given-When-Then 패턴을 따른다
- 인수 테스트: `.feature` 파일에 Gherkin 문법(한국어)으로 시나리오를 작성하고 Step Definition에서 실제 API를 호출한다
- Step Definition 애노테이션은 `io.cucumber.java.en` 패키지의 `@Given`, `@When`, `@Then`을 사용한다. 한글 애노테이션(`@먼저`, `@만일`, `@그러면`, `@그리고`) 사용 금지
- 단위 테스트: `@DisplayName`에 한국어 설명을 작성하고 메서드명은 `methodName_condition_result` 패턴을 따른다

---

# Git 컨벤션

**커밋 메시지**

AngularJS Commit Convention을 따른다.

```
<type>: <한국어 메시지>
- 부연 설명 (선택, 한 줄까지)
```

| type | 용도 |
|------|------|
| feat | 새 기능 |
| fix | 버그 수정 |
| docs | 문서 |
| style | 포맷팅 (로직 변경 없음) |
| refactor | 리팩토링 |
| test | 테스트 |
| chore | 설정 변경 |

**예시**
```
feat: DEX 스왑 시뮬레이션 기능 추가
- 슬리피지 검증 및 가스비 차감 로직 구현

fix: 지정가 매수 주문 시 수수료 미반영 수정
```

**스테이징 규칙**

- `git add .`를 사용하지 않는다. 반드시 파일을 명시적으로 지정한다
- 커밋 전 staged diff를 확인한다

**원자적 커밋**

- 하나의 커밋은 하나의 논리적 변경만 포함한다
- 관련 없는 수정을 하나의 커밋에 섞지 않는다
- 논리적으로 분리할 수 있는 변경은 별도 커밋으로 나눈다

**금지 사항**

- AI 생성 서명을 커밋 메시지나 코드에 포함하지 않는다 (예: "Generated by Claude Code", "Co-Authored-By: Claude")

**브랜치 전략**

GitHub Flow를 따른다. `main` 브랜치와 `feature/*` 브랜치만 사용한다.
- `main`: 항상 배포 가능한 상태를 유지한다
- `feature/*`: 기능 단위로 `main`에서 분기하고 완성되면 `main`에 머지한다
