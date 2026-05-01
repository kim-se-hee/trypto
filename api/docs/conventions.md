# DTO

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

# 에러 처리

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

# 공통 컨벤션

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

# 레이어별 컨벤션

- 베스트 프랙티스: `PlaceOrderService`와 trading 도메인을 참고한다. 서비스는 포트 호출과 도메인 객체 생성/위임 등의 오케스트레이션만 수행하고, 비즈니스 로직은 도메인 모델과 VO에 캡슐화한다. 데이터 조합·매핑·필터링 로직이 서비스에 직접 풀어지지 않도록 일급 컬렉션과 VO에 위임한다

**Controller**
- 클래스명: `{도메인}Controller` (예: `OrderController`, `SwapController`)
- 메서드명: HTTP 메서드 + 자원을 표현한다 (예: `createOrder()`, `getPortfolio()`)
- UseCase 인터페이스에만 의존한다. Service 구현체나 Output Port를 직접 주입받지 않는다
- 비즈니스 로직을 두지 않는다. 요청값 검증과 UseCase 위임만 수행한다
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
- 다른 컨텍스트가 소비할 조회 UseCase는 단일 책임으로 설계하고, Result DTO(`application/port/in/dto/result/`)를 반환하여 도메인 모델 유출을 방지한다

**Service**
- 클래스명: `{UseCase명}Service` (예: `PlaceMarketBuyOrderService`)
- 메서드명은 비즈니스 의미를 반영한다 (예: `placeMarketBuyOrder()`, `executeSwap()`)
- 서비스는 순수 오케스트레이션만 담당한다. 검증, 계산, 분기 등 비즈니스 로직은 도메인 모델과 VO에 위임한다
- 의존성 주입 필드 순서: 자기 컨텍스트 Output Port → 크로스 컨텍스트 UseCase (컨텍스트별 그룹) → 인프라(Clock 등). 각 그룹 사이에 빈 줄을 둔다
- 오케스트레이션의 각 단계를 private 메서드로 추출하여 public 메서드의 가독성을 높인다
- private 메서드에는 원시값을 분해하여 넘기지 않고 부모 객체(command, mapping 등)를 직접 전달한다. private 메서드 내부에서 필요한 값을 꺼내 쓴다
- 쓰기 작업에 `@Transactional`을 선언한다
- 생성 시 검증은 도메인 모델이나 VO의 팩토리 메서드(`create`, `of` 등)에서 수행한다. 서비스에서 검증 후 생성하지 않는다
- 컬렉션에 대한 검증이나 계산(`stream`으로 합산, 중복 체크 등)은 일급 컬렉션으로 캡슐화한다
- 다른 바운디드 컨텍스트의 도메인 모델을 직접 import하지 않는다
- 크로스 컨텍스트 의존: 서비스가 다른 컨텍스트의 UseCase(Input Port)를 직접 주입받아 Result DTO를 수신한다. 소비자 쪽에 크로스 컨텍스트 전용 Output Port나 Adapter를 만들지 않는다
- 크로스 컨텍스트 Result DTO를 자기 컨텍스트의 도메인 VO로 변환하는 책임은 서비스에 있다

**Domain**
- 외부 의존 없이 순수 비즈니스 로직만 포함한다. 인프라(JPA, Redis, RabbitMQ 등)나 어댑터·포트를 import하지 않는다
- Aggregate 내부의 Entity/VO 변경은 반드시 Aggregate Root를 통해서만 수행한다. Root를 우회한 직접 변경 금지
- Aggregate 간 참조는 ID로만 한다. 다른 Aggregate Root를 객체 참조로 들지 않는다
- 비즈니스 로직은 도메인 객체 안에 위치한다
- 메서드명은 비즈니스 지식을 담는다 (예: `deductBalance()`, `checkSlippageExceeded()`)
- Entity에는 `@Getter`만 허용하고 `@Setter`, `@Data` 금지. 상태 변경은 비즈니스 의미를 가진 메서드로만 수행한다
- 원시 타입이 단위, 제한, 계산 등 비즈니스 규칙을 가지면 VO로 감싼다 (primitive obsession 방지)
- 원시 타입이나 제네릭 컬렉션(`Map<Long, X>` 등)이 주변 코드의 추상화 수준을 깨뜨리면 VO나 도메인 모델로 감싸서 가독성을 높인다. 비즈니스 규칙이 없더라도 타입 이름이 도메인 의도를 전달하거나 null 처리 같은 사용 패턴을 캡슐화할 수 있다면 감싸는 것이 낫다
- 항상 함께 이동하는 필드 묶음은 VO로 추출한다. 
- 단, 크로스 컨텍스트 경계의 Query/Result DTO에서는 다른 컨텍스트의 도메인 타입을 참조할 수 없으므로 primitive 전달을 해도 된다
- boolean/원시값 → enum/VO 변환은 도메인 팩토리 메서드에 캡슐화한다 (예: `TradingVenue.of(feeRate, baseCurrencyCoinId, domestic)`)
- enum 비교(`getSide() == Side.BUY`)는 도메인 모델의 판별 메서드로 감싼다 (예: `order.isBuyOrder()`)
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

**Output Port**
- Output Port는 `port.out.dto`를 사용하지 않는다. 도메인 모델 또는 도메인 VO만 반환한다
- 조회 컬럼이 JPA 엔티티 필드와 대체로 일치하면 도메인 모델을 직접 반환한다 (Adapter에서 `toDomain()` 활용)
- 집계 쿼리(GROUP BY, COUNT, AVG)나 엔티티와 구조가 크게 다른 프로젝션은 별도 도메인 VO를 만들어 반환한다
- 도메인 VO 이름은 유비쿼터스 언어를 사용한다. `Info`, `Detail`, `Projection` 같은 기술 접미사 대신 도메인 개념을 표현한다 (예: `FilledOrder`, `HoldingSummary`, `RankingStats`)

**Adapter Out**
- Output Port 인터페이스를 구현한다. 다른 어댑터나 도메인이 Adapter 클래스에 직접 의존하지 않는다
- `adapter/out/` 하위에 `entity/`, `repository/` 패키지로 분리한다. Adapter 클래스는 `adapter/out/`에 그대로 둔다
  - `adapter/out/entity/`: JPA 엔티티 클래스 (`{도메인}JpaEntity`)
  - `adapter/out/repository/`: Spring Data JPA 리포지토리 인터페이스 (`{도메인}JpaRepository`)
  - `adapter/out/`: Persistence Adapter, 외부 API Adapter
- Persistence 클래스명: `{도메인}JpaPersistenceAdapter` (예: `OrderJpaPersistenceAdapter`)
- External API 클래스명: `{외부서비스}ApiAdapter` (예: `JupiterApiAdapter`)
- Adapter는 자기 컨텍스트의 인프라(DB, 외부 API)만 담당한다. 크로스 컨텍스트 전용 Adapter를 만들지 않는다
- 메서드명은 비즈니스 로직을 드러내지 않고 데이터 조회 조건을 표현한다 (예: `findByUserIdAndCoin()`, `saveOrder()`)
- 조건이 2개 이하인 단순 조회는 Spring Data JPA 쿼리 메서드를 활용한다
- 조건이 복잡하거나 동적 쿼리가 필요한 경우 반드시 QueryDSL을 사용한다
- N+1 문제 방지를 위해 Fetch Join이나 Batch Size를 고려한다
