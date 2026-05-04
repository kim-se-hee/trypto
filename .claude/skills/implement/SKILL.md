---
description: >
  계획서를 코드로 옮기는 워크플로우. /implement <prefix> <feature> 로 호출하면
  기능별 워크트리·브랜치를 만들고, 인수 테스트를 작성하고, plan.md 의 task 들을
  TDD 로 한 개씩 처리한다.
arguments: [prefix, feature]
---

`/implement` 는 `docs/<prefix>/<feature>/plan.md` 한 장을 동작하는 코드로 변환하는 오케스트레이터다. 메인 세션은 디스패치만 담당하고 실제 작성은 서브에이전트가 한다

## 입력

- `$prefix` — 기능이 속한 위치
  - api 모듈 : `api/<context>` (예: `api/trading`)
  - 그 외 모듈: 모듈명 그대로 (예: `engine`, `collector`, `frontend`)
- `$feature` — 기능 이름 (kebab-case, 예: `place-order`)

예시:
```
/implement api/trading place-order
/implement engine matching
```

인자가 부족하거나 형식이 맞지 않으면 사용자에게 호출 형태를 안내하고 종료한다.

## 사전 제약

`docs/<prefix>/<feature>/plan.md` 가 존재해야 한다. 없으면 종료한다.

## 흐름

### 0. plan.md 검사

`docs/<prefix>/<feature>/plan.md` 를 Read 하여 `## task 목록` 섹션에서 미완료 항목을 위→아래 순서로 추출한다. 모두 완료 상태면 "이미 완료" 알리고 종료.

### 1. 워크트리 + 브랜치 생성

명명 규칙:
- 워크트리 경로: `../trypto-<feature>`
- 브랜치명: `feat/<feature>`

```bash
git worktree add -b feat/<feature> ../trypto-<feature>
```

이미 워크트리·브랜치가 존재하면 사용자에게 알리고 종료 (덮어쓰지 않음).

이후 모든 작업은 새 워크트리 안에서 수행한다.

### 2. 인수 테스트 작성

`acceptance-test-author` 서브에이전트를 호출한다. 프롬프트에 `prefix`, `feature` 만 전달하고 파일 본문은 인용하지 않는다 
서브에이전트가 자기 사전 준비 단계에서 spec.md·plan.md·모듈 testing.md 를 직접 Read 한다. 메인 컨텍스트에 본문이 안 쌓이도록 하기 위함이다.

### 3. task 루프

미완료 task 목록을 위→아래 순서로 한 개씩 `task-implementer` 서브에이전트에 위임한다. 직렬 실행.

각 task 마다:

1. `task-implementer` 호출. 프롬프트에 `prefix`, `feature`, `task 본문` 을 전달.
2. 정상 종료 후 `plan.md` 의 해당 줄을 `- [ ]` → `- [x]` 로 갱신한다.

서브에이전트가 작업 중간에 막혀 에러를 보고하면 (테스트가 계속 그린이라 못 진행, 컴파일 실패 등) 메인은 사용자에게 그 메시지를 그대로 전달하고 루프를 중단한다.

### 4. 인수 테스트 실행

2단계가 "인수 테스트 미작성" 으로 종료했다면 (모듈 정책상 인수 테스트를 두지 않는 경우) 이 단계는 건너뛰고 5단계로 간다.

모든 task 가 끝난 뒤 `acceptance-test-runner` 서브에이전트에 위임한다. 프롬프트에 `prefix`, `feature` 만 전달한다. 러너는 `@<feature>` 태그로 좁혀 인수 테스트를 실행하고, 실패 시 자체적으로 원인 분석·수정·재실행·커밋 루프를 돈 뒤 메인엔 결과 한 줄만 돌려준다 (gradle 출력은 메인 컨텍스트에 쌓이지 않음).

- **통과**: 5단계로.
- **실패**: 러너가 보고한 메시지를 사용자에게 그대로 전달하고 종료.

### 5. 기능 index.md 갱신

다음 조건이 모두 참일 때만 `docs/<prefix>/<feature>/index.md` 의 `단계` 줄을 `단계: implement` 로 갱신한다.

- 3단계 task 루프가 중단 없이 끝났다 (미완료 task 0개).
- 4단계 인수 테스트가 통과했거나 모듈 정책상 미작성으로 건너뛰었다.

하나라도 어긋나면 단계는 갱신하지 않는다 (`plan` 그대로 유지). 부분 진행 상태는 `plan.md` 의 체크박스가 이미 표현하고 있으므로, index.md 의 단계는 "이 단계까지 깔끔히 끝났다" 만 의미한다.

### 6. 보고

```
구현 완료: docs/<prefix>/<feature>/

워크트리: ../trypto-<feature>
브랜치: feat/<feature>
완료 task: <개수>
미완료 task: <목록 또는 "없음">
인수 테스트: 통과 / 실패 (n/m) / 모듈 정책상 미작성
단계 갱신: implement / 미갱신 (사유)

```

## 단계 격리

- 메인 세션은 오케스트레이터만 한다. 직접 코드를 작성·편집하지 않는다.
- 인수 테스트 작성은 `acceptance-test-author`, task 단위 구현·단위 테스트는 `task-implementer`, 인수 테스트 실행·실패 수정은 `acceptance-test-runner` 가 한다.
- 메인이 만지는 파일은 `plan.md` 의 체크박스와 `index.md` 의 단계 줄뿐이다.
