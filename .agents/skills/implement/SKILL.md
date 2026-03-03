---
description: >
  기능 문서 기반 코드 구현. docs/{domain}/{feature}.md 기능 문서를 읽고 헥사고날 아키텍처에 따라
  ErrorCode → Domain → Application → Adapter In → Adapter Out 순서로 전체 스택을 구현한다.
  각 Phase마다 컴파일 검증 + 원자적 커밋을 수행한다. 에이전트 없이 메인 컨텍스트에서 직접 실행한다.
  TRIGGER: 사용자가 기능 구현을 요청하거나 /implement를 입력할 때.
---

# 기능 문서 기반 코드 구현

기능 문서(`docs/{domain}/{feature}.md`)를 읽고 헥사고날 아키텍처에 따라 코드를 구현한다. 에이전트 없이 메인 컨텍스트에서 직접 실행한다.

## 입력

`$ARGUMENTS` = 기능 문서 경로 (예: `docs/wallet/transfer.md`)

---

## 워크플로우

### 0. 사전 검증

기능 문서를 읽고 `[TODO]` 마커가 남아있는지 검사한다.

- `[TODO]`가 있으면 목록을 출력하고 **즉시 중단**한다
- `[CONFIRM]`은 무시한다 (이미 사람이 확인한 것으로 간주)

```bash
grep -n "\[TODO\]" {문서경로}
```

### 1. 문서 분석

기능 문서에서 아래 정보를 추출한다:

- **도메인 모델**: Entity, VO, 일급 컬렉션
- **비즈니스 규칙**: 검증 로직, 계산 공식, 상태 전이
- **API 명세**: Method, Path, Request/Response, 에러 코드
- **시퀀스 다이어그램**: 오케스트레이션 흐름
- **크로스 도메인 의존성**: 필요한 Output Port

### 2. 구현 (Phase 1 ~ 5)

**컴파일 의존 방향을 따라** 순서대로 구현한다. 각 Phase 완료 후 컴파일 검증 + 커밋한다.

#### Phase 1: ErrorCode + messages.properties

- `common/exception/ErrorCode.java`에 에러 코드 추가
- `src/main/resources/messages.properties`에 메시지 추가
- 파라미터가 필요한 메시지는 `{0}`, `{1}` 플레이스홀더 사용

```bash
./gradlew compileJava
```

커밋: `feat: {도메인} {기능} ErrorCode 추가`

#### Phase 2: Domain (model + vo)

- Entity: 비즈니스 로직을 포함하는 도메인 모델
- VO: 불변 객체, `equals()`/`hashCode()` 구현
- 일급 컬렉션: 컬렉션 관련 로직 캡슐화
- 팩토리 메서드: `create()`, `of()` 등으로 생성 시 검증

```bash
./gradlew compileJava
```

커밋: `feat: {도메인} {기능} 도메인 모델 구현`

#### Phase 3: Application (port/in, port/out, service)

- **Input Port (UseCase)**: 인터페이스, 하나의 유스케이스에 하나의 메서드
- **Output Port**: 인터페이스, 외부 의존성 추상화
- **Command/Query DTO**: `application/port/in/dto/` 하위
- **Result DTO**: 여러 Aggregate 조합이 필요한 경우만
- **Service**: 순수 오케스트레이션, 각 단계를 private 메서드로 추출

```bash
./gradlew compileJava
```

커밋: `feat: {도메인} {기능} 애플리케이션 계층 구현`

#### Phase 4: Adapter In (controller, request/response DTO)

- **Request DTO**: `adapter/in/dto/request/`, Jakarta Validation
- **Response DTO**: `adapter/in/dto/response/`
- **Controller**: `@Valid` 검증 트리거, Request → Command 변환, `ApiResponseDto<T>` 래핑

```bash
./gradlew compileJava
```

커밋: `feat: {도메인} {기능} 컨트롤러 구현`

#### Phase 5: Adapter Out (entity, repository, adapter)

- **JPA Entity**: `adapter/out/entity/`, `@Column` 제약사항 명시
- **JPA Repository**: `adapter/out/repository/`
- **Persistence Adapter**: `adapter/out/`, Output Port 구현
- 도메인 모델 ↔ JPA Entity 변환 메서드 포함

```bash
./gradlew compileJava
```

커밋: `feat: {도메인} {기능} 영속성 어댑터 구현`

### 3. 최종 검증

```bash
./gradlew compileJava
```
