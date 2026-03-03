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

# 코딩 컨벤션

## DTO

- DTO는 `record`로 작성한다 (Request, Response, Command, Query 모두)
- Command/Query DTO는 application 계층이 adapter 계층의 Request/Response에 의존하지 않기 위해 존재한다

**Request DTO**
- `adapter/in/dto/request/` 패키지에 위치한다
- 네이밍: `{행위}Request` (예: `PlaceOrderRequest`, `FindOrderHistoryRequest`)
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
- 네이밍: Command 응답은 `{행위}Response`, Query 응답은 `{자원}Response` (예: `PlaceOrderResponse`, `OrderHistoryResponse`)
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
- 메서드 나열 순서: public 메서드를 먼저, private 메서드를 아래에 배치한다
  - public 메서드: 상태 변경 메서드 → 판별 메서드 → 조회 메서드 순으로 나열한다
  - private 메서드: 사용된 순서대로 나열한다
- 매직 넘버/매직 상수를 사용하지 않는다. 도메인 개념(enum, VO, 상수 클래스)으로 대체한다

## 레이어별 컨벤션

- 베스트 프랙티스: `PlaceOrderService`와 trading 도메인을 참고한다. 서비스는 포트 호출과 도메인 객체 생성/위임만 수행하고, 비즈니스 로직은 도메인 모델(`Order`, `Holding`)과 VO(`BalanceChange`, `OrderAmountPolicy`)에 있다

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
- 서비스는 순수 오케스트레이션만 담당한다. 검증, 계산, 분기 등 비즈니스 로직은 도메인 모델과 VO에 위임한다
- 오케스트레이션의 각 단계를 private 메서드로 추출하여 public 메서드의 가독성을 높인다
- 쓰기 작업에 `@Transactional`을 선언한다
- 생성 시 검증은 도메인 모델이나 VO의 팩토리 메서드(`create`, `of` 등)에서 수행한다. 서비스에서 검증 후 생성하지 않는다
- 컬렉션에 대한 검증이나 계산(`stream`으로 합산, 중복 체크 등)은 일급 컬렉션으로 캡슐화한다
- 다른 바운디드 컨텍스트의 도메인 모델을 직접 import하지 않는다. Output Port를 통해 필요한 정보만 가져온다

**Domain**
- 비즈니스 로직은 도메인 객체 안에 위치한다
- 메서드명은 비즈니스 지식을 담는다 (예: `deductBalance()`, `checkSlippageExceeded()`)
- Entity에는 `@Getter`만 허용하고 `@Setter`, `@Data` 금지. 상태 변경은 비즈니스 의미를 가진 메서드로만 수행한다
- 원시 타입이 단위, 제한, 계산 등 비즈니스 규칙을 가지면 VO로 감싼다 (primitive obsession 방지)
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
- `adapter/out/` 하위에 `entity/`, `repository/` 패키지로 분리한다. Adapter 클래스는 `adapter/out/`에 그대로 둔다
  - `adapter/out/entity/`: JPA 엔티티 클래스 (`{도메인}JpaEntity`)
  - `adapter/out/repository/`: Spring Data JPA 리포지토리 인터페이스 (`{도메인}JpaRepository`)
  - `adapter/out/`: Persistence Adapter, 외부 API Adapter, 크로스 컨텍스트 Adapter
- Persistence 클래스명: `{도메인}JpaPersistenceAdapter` (예: `OrderJpaPersistenceAdapter`)
- External API 클래스명: `{외부서비스}ApiAdapter` (예: `JupiterApiAdapter`)
- 크로스 컨텍스트 Adapter 클래스명: `{소비하는 컨텍스트 관점의 이름}Adapter` (예: `AnalysisRoundAdapter`, `OrderHistoryAdapter`)
  - 다른 모듈이 노출하는 QueryPort를 주입받아 자기 모듈의 Port로 변환한다
  - 타 모듈의 도메인 모델, Output Port, JPA 엔티티(Q 클래스)를 직접 의존하지 않는다
  - 타 모듈의 DTO → 자기 모듈의 DTO 변환 책임은 이 어댑터에 있다
  - 소비하는 컨텍스트의 Port/DTO는 자기 컨텍스트의 유비쿼터스 언어로 이름을 짓는다. 생산하는 컨텍스트의 이름을 그대로 복사하지 않는다 (예: trading의 `OrderInfo`를 regretanalysis에서 소비할 때 → `TradeRecord`)
- 메서드명은 비즈니스 로직을 드러내지 않고 데이터 조회 조건을 표현한다 (예: `findByUserIdAndCoin()`, `saveOrder()`)
- 조건이 2개 이하인 단순 조회는 Spring Data JPA 쿼리 메서드를 활용한다
- 조건이 복잡하거나 동적 쿼리가 필요한 경우 반드시 QueryDSL을 사용한다
- N+1 문제 방지를 위해 Fetch Join이나 Batch Size를 고려한다

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
- 분리 기준:
  - 문서, 기능, 테스트, 버그 수정처럼 커밋 타입이 다르면 분리한다
  - 서로 다른 모듈의 변경은 각각 별도 커밋으로 나눈다
  - 같은 모듈이라도 도메인, 애플리케이션, 어댑터는 별도 커밋으로 나눈다
  - 새 기능을 위해 기존 모듈을 확장한 변경은 새 모듈 커밋과 분리한다. 예를 들어 ErrorCode 추가나 enum 타입 추가는 별도로 먼저 커밋한다

**점진적 커밋**

- 기능 구현 중 논리적 단위가 완성되면 즉시 커밋한다. 모든 구현을 마친 후 한 번에 커밋하지 않는다
- 논리적 단위란 독립적으로 의미를 가지는 연관 변경의 묶음이다
- 커밋 시점에 컴파일이 깨지지 않아야 한다
- 커밋 순서는 컴파일 의존 방향을 따른다. 공통 변경을 먼저, 의존되는 모듈을 먼저 커밋하고, 도메인 → 애플리케이션 → 어댑터 → 테스트 순서로 진행한다

**금지 사항**

- AI 생성 서명을 커밋 메시지나 코드에 포함하지 않는다 (예: "Generated by Claude Code", "Co-Authored-By: Claude")

**브랜치 전략**

GitHub Flow를 따른다. `main` 브랜치와 `feature/*` 브랜치만 사용한다.
- `main`: 항상 배포 가능한 상태를 유지한다
- `feature/*`: 기능 단위로 `main`에서 분기하고 완성되면 `main`에 머지한다

---

# 문서 안내

작업에 필요한 상세 문서는 아래 경로에 있다. 필요할 때 참조한다.

- 공통: `docs/architecture.md`, `docs/testing.md`, `docs/data-model.md`, `docs/schema.md`
- 도메인별: `docs/{domain}/{기능}.md` (예: `docs/trading/cex-order.md`)
