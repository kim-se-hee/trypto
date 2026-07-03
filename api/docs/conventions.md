# DTO

- DTO는 `record`로 작성한다 (Request, Response, Command, Query 모두)
- Command/Query DTO는 application 계층이 adapter 계층의 Request/Response에 의존하지 않기 위해 존재한다

**Request DTO**
- `adapter/in/dto/request/` 패키지에 위치한다
- 네이밍: `{행위}Request` (예: `PlaceOrderRequest`, `FindOrderHistoryRequest`)
- Bean Validation 어노테이션으로 형식 검증만 수행한다 (`@NotBlank`, `@NotNull`, `@Min` 등)
- 비즈니스 로직 검증은 반드시 도메인 모델에서 수행한다

**Response DTO**
- `adapter/in/dto/response/` 패키지에 위치한다
- 네이밍: Command 응답은 `{행위}Response`, Query 응답은 `{자원}Response` (예: `PlaceOrderResponse`, `OrderHistoryResponse`)
- 모든 API 응답은 `ApiResponseDto<T>`로 래핑한다

**Command DTO**
- `application/port/in/dto/command/` 패키지에 위치한다
- Controller에서 Request DTO를 Command DTO로 변환하여 UseCase에 전달한다
- 네이밍: `{행위}Command` (예: `PlaceOrderCommand`)

**Query DTO**
- `application/port/in/dto/query/` 패키지에 위치한다
- Controller에서 Request DTO를 Query DTO로 변환하여 UseCase에 전달한다
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
- `get` vs `find` 네이밍
  - `get`은 대상이 반드시 존재해야 할 때 사용하며 없으면 예외를 던진다. 예) 주문 취소 시 취소 대상 주문은 반드시 존재해야 하므로 `getOrder`처럼 `get`을 쓴다
  - `find`는 대상이 없을 수 있으며 `Optional` 또는 빈 컬렉션을 반환한다 예) 리뷰 조회 시 아직 리뷰가 안 달렸을 수 있으므로 `find`를 쓴다.
- 필드 나열 순서: 상수(static final) → final 필드 → 일반(가변) 필드 순으로 배치한다.
- 메서드 나열 순서: public 메서드를 먼저, private 메서드를 아래에 배치한다
  - public 메서드: 상태 변경 메서드 → 판별 메서드 → 조회 메서드 순으로 나열한다
  - private 메서드: 사용된 순서대로 나열한다
- 매직 넘버/매직 상수를 사용하지 않는다. 도메인 개념(enum, VO, 상수 클래스)으로 대체한다
- early return을 한다.
- 주석은 사용하지 않는다.

# 레이어별 컨벤션

- 베스트 프랙티스: `PlaceOrderService`와 trading 도메인을 참고한다. 서비스는 포트 호출과 도메인 객체 생성/위임 등의 오케스트레이션만 수행하고, 비즈니스 로직이 애플리케이션 서비스에 노출되지 않도록 도메인 모델과 VO에 캡슐화한다.

**Controller**
- 클래스명: `{도메인}Controller` (예: `OrderController`, `SwapController`)
- 메서드명: HTTP 메서드 + 자원을 표현한다 (예: `createOrder()`, `getPortfolio()`)
- UseCase 인터페이스에만 의존한다. Service 구현체나 Output Port를 직접 주입받지 않는다
- Request DTO를 서비스 계층에 직접 넘기지 않는다. Controller에서 Command/Query 객체로 변환하여 전달한다
- 응답은 반드시 `ApiResponseDto<T>`로 래핑한다

**UseCase**
- 인터페이스명: `{비즈니스행위}UseCase` (예: `PlaceMarketBuyOrderUseCase`, `ExecuteSwapUseCase`)
- 하나의 유스케이스에 하나의 메서드를 정의한다
- Command UseCase: 도메인 모델을 반환한다
- Query UseCase: 단일 Aggregate 조회는 도메인 모델을, 여러 Aggregate 조합이 필요하면 Result DTO를 반환한다
- Controller에서 반환값(도메인 모델 또는 Result)을 Response DTO로 변환한다
- UseCase가 adapter 계층의 Request/Response DTO를 import하지 않는다
- 다른 컨텍스트에게 기능을 제공하기 위한 UseCase는 DTO를 반환하여 도메인 모델 유출을 방지한다

**애플리케이션 서비스**
- 클래스명: `{UseCase명}Service` (예: `PlaceMarketBuyOrderService`)
- 메서드명은 비즈니스 의미를 반영한다 (예: `placeMarketBuyOrder()`, `executeSwap()`)
- 애플리케이션 서비스는 순수 오케스트레이션만 담당한다. 비즈니스 로직은 도메인 모델과 VO에 위임한다
- 의존성 주입 필드 순서: 포트 → 도메인 서비스 → 이외 자원
- private 메소드를 작성하지 않고 영어 읽히듯 메소드를 구현한다. 비지니스 로직을 도메인 영역에 넣으면 가능하다.
- 생성 시 검증은 도메인 모델이나 VO의 팩토리 메서드에서 수행한다.
- 다른 바운디드 컨텍스트의 도메인 모델을 직접 import하지 않는다

**Domain**
- 외부 의존 없이 순수 비즈니스 로직만 포함한다. 인프라(JPA, Redis, RabbitMQ 등)나 어댑터·포트를 import하지 않는다
- 애그리거트를 생성할 때는 예외적으로 application 계층의 Command 객체를 입력 파라미터로 받는다.
- 메서드명은 비즈니스를 표현하도록 작성한다 (예: `deductBalance()`, `checkSlippageExceeded()`)
- Entity에는 `@Getter`만 허용하고 `@Setter`, `@Data` 금지. 
- 원시 타입이 단위, 제한, 계산 등 비즈니스 규칙을 가지면 VO로 감싼다 (primitive obsession 방지)
- 원시 타입의 비지니스 규칙이 없더라도 가독성을 위해서 VO로 만드는 것을 고려할 수 있다.
- 컬렉션과 관련된 비지니스 로직이 있거나 구현을 감추고 싶다면 일급 컬렉션을 만들어 가독성과 응집성을 높인다.
- 하나의 도메인 개념을 이루는 필드 묶음은 VO로 추출한다. 필드들이 코드상 항상 함께 이동하지 않아도(로직마다 부분집합으로 흩어져 쓰여도) 개념이 하나면 묶는다.
  - 판별 리트머스: 그 묶음에 도메인 언어로 이름을 붙일 수 있는가. 어떤 판정 메서드가 묶음의 일부와 원시 타입을 나란히 파라미터로 받고 있다면 묶음이 쪼개져 있다는 신호다.
  - 예: 송금의 (chain, toAddress, toTag)는 resolve엔 chain·toAddress만, 태그 판정엔 toTag만 쓰이지만 "도착지 지정"이라는 한 개념이므로 VO로 묶는다.
- VO는 불변 객체로 만든다. 모든 필드 `final`
- VO는 `equals()`/`hashCode()`를 반드시 구현한다
- 외부에 공개할 필요가 없고 애그리거트 내부에서만 사용하는 메소드는 private으로 만든다.

**JPA 엔티티**
- 비즈니스 로직과 ERD에 따라 `@Column`으로 제약사항을 적절히 명시한다 (`nullable`, `unique`, `length`, `precision`, `scale` 등)
- 감사 추적이나 데이터 복구가 필요한 엔티티에는 소프트 딜리트를 적용한다 (예: User, InvestmentRound). `@SQLDelete` + `@Where`를 사용하고 `isDeleted` 필드를 둔다

```java
@SQLDelete(sql = "UPDATE user SET is_deleted = true WHERE user_id = ?")
@Where(clause = "is_deleted = false")
```

**Output Port**
- 조회용 아웃풋 포트는 `{애그리거트}QueryPort` 로 이름 짓는다.
- 자기 컨텍스트의 정보를 조회할 때는 애그리거트 단위 조회 포트만 둔다. 애그리거트 내부 구성요소를 직접 조회하는 포트는 두지 않는다.
- 다른 바운디드 컨텍스트의 데이터를 조회하는 포트는 `{타 컨텍스트 이름}QueryPort` 로 이름 짓는다. (예: `MarketDataQueryPort`)
- 명령용 아웃풋 포트는 `{애그리거트}CommandPort` 로 이름 짓는다.
- 포트 메서드명은 비즈니스 규칙을 담지 않고, 무엇을 어떤 조건으로 조회하는지만 표현한다. (예: `getByIdWithLock()` O / `findByIdForCancellation()` X)

**Adapter Out**
- Output Port 인터페이스를 구현한다.
- 연동 방식에 따라 `adapter/out/` 아래 하위 패키지로 나눈다. 유형별 설명·명명법·컨벤션은 다음과 같다.

- **persistence — `adapter/out/persistence/`**
  - 설명: 저장소 접근 어댑터. RDB뿐 아니라 Redis 등 모든 저장소 접근을 포함한다. 하위에 `entity/`(JPA 엔티티)와 `repository/`(Spring Data JPA 인터페이스)를 둔다
  - 명명법: 어댑터는 `{사용 기술}{애그리거트}{Query|Command}Adapter` (예: `JpaOrderQueryAdapter`, `RedisLivePriceQueryAdapter`), 엔티티는 `{도메인}JpaEntity`, 리포지토리는 `{도메인}JpaRepository`
  - 컨벤션:
    - 조건이 2개 이하인 단순 조회는 Spring Data JPA 쿼리 메서드를, 복잡하거나 동적 쿼리는 QueryDSL을 사용한다. @Query는 절대 사용하지 않는다.
    - QueryDSL은 Spring Data fragment 방식으로 붙인다 
      - `{도메인}JpaRepositoryCustom` 인터페이스에 커스텀 조회 메서드를 선언한다
      - `{도메인}JpaRepository`가 `JpaRepository`와 이 인터페이스를 함께 extends 한다.
      - `{도메인}JpaRepositoryCustomImpl`에서 `JPAQueryFactory`를 주입받아 구현한다.

- **acl — `adapter/out/acl/`**
  - 설명: 다른 컨텍스트의 정보를 조회하는 ACL 어댑터. 저장소에 직접 접근하지 않고 타 컨텍스트 UseCase를 호출해 결과를 받는다는 점이 persistence와 다르다
  - 명명법: `Acl{타 컨텍스트}QueryAdapter` (예: `AclMarketQueryAdapter`)
  - 컨벤션: 타 컨텍스트의 반환값을 자기 컨텍스트 모델로 변환한다. 변환이 복잡하면 별도 `Translator`로 분리한다

- **service — `adapter/out/service/`**
  - 설명: 도메인 서비스 구현체. 
  - 명명법: `{도메인서비스}Impl` (예: `WalletBalanceServiceImpl`, `RuleViolationCheckerImpl`)
  - 컨벤션: acl과 마찬가지로 타 컨텍스트 모델을 자기 컨텍스트로 변환하며, 변환이 복잡하면 `Translator`로 분리한다

- **messaging — `adapter/out/messaging/`**
  - 설명: 메시지 큐로 발행하는 어댑터와 그 페이로드 DTO를 둔다
  - 명명법: 발행기는 `{대상}Publisher` (예: `EngineInboxPublisher`), 페이로드 DTO는 `{이벤트}{대상}Message` (예: `OrderPlacedEngineMessage`)
  - 컨벤션: 페이로드 DTO는 서비스 간 메시지 계약(루트 `docs/contracts/`)을 따른다
