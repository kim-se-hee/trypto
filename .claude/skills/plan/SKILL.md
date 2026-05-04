---
description: >
  기능 계획 작성 및 다듬기.
  /plan draft 로 스펙 기반 신규 계획을 작성하고
  /plan refine 으로 사용자 피드백을 반영한다.
arguments: [subcommand, prefix, feature]
---

계획은 스펙의 비즈니스 규칙을 어떻게 구현할지 정리하는 문서다 (how). 코드 한 줄까지 정하지 않는다 — 어느 도메인 모델이 어느 규칙을 책임지는지, 어떤 순서로 작업할지 수준의 결정을 담는다.

## 입력

frontmatter `arguments: [subcommand, prefix, feature]` 로 명명된 위치 인자를 받는다.

- `$subcommand` = `draft` 또는 `refine`
- `$prefix` = 기능이 속한 위치 (spec 스킬과 동일)
  - api 모듈 (DDD 적용): `api/<context>` (예: `api/trading`)
  - 그 외 모듈: 모듈명 그대로 (예: `engine`, `collector`, `frontend`)
- `$feature` = 기능 이름 (kebab-case)

예시:
```
/plan draft api/trading place-order
/plan refine engine matching
```

인자가 부족하거나 형식이 맞지 않으면 사용자에게 호출 형태를 안내하고 종료한다.

## 분기

`$subcommand` 값에 따라 해당 흐름 문서를 Read 하여 그대로 따른다.

- `draft` → [draft.md](draft.md)
- `refine` → [refine.md](refine.md)

## 사전 제약

`docs/<prefix>/<feature>/spec.md` 가 존재해야 한다. 없으면 사용자에게 알리고 종료한다 — 계획은 스펙을 구현 대상으로 삼는다.

## 디렉터리 구조

```
docs/<prefix>/<feature>/
├── index.md         ← 기능 메타 (spec 단계에서 이미 생성)
├── spec.md          ← what/why
└── plan.md          ← how (이 스킬의 산출물)
```

## 양식

### `<feature>/plan.md`

```markdown
## 도메인 모델

### <모델 A> (신규 | 수정 | 신규 VO)
- [<BR번호>] <짧은 본문>
- [<BR번호>] <짧은 본문>

### <모델 B> (...)
- [<BR번호>] <짧은 본문>

## 타 컨텍스트 의존성

- <대상>.<UseCase> — <용도>
- <대상>.<UseCase> [신규 필요] — <왜 필요한지>

## 시퀀스 플로우

```

## task 목록

- [ ] <작업>
- [ ] <작업>
- [ ] <작업>

## API 명세        ← 외부 노출 시에만

## 이벤트 컨트랙트  ← 메시지 큐 사용 시에만

## 규칙 매핑 검증

| BR | 책임 모델 |
|----|-----------|
| 1  | <모델>     |
| 2  | <모델>     |
| ... | ...      |


## 사전 리서치

루트 `CLAUDE.md` 에서 시작해 `CLAUDE.md` 와 `index.md` 에 적힌 목차를 따라 필요한 자료를 탐색한다.

단, api 모듈 기능을 계획할 때는 협력 인터페이스 발견을 위해 **의존 컨텍스트의 `dependency.md` 제공 섹션** 을 반드시 본다.

## BR 참조

도메인 모델이 책임지는 spec 문서의 BR 을 `[<번호>]` 형태로 참조한다.
```markdown
### Order (수정)
- [3] 잔고 기반 주문 가능 검증
- [7] 출고 전 취소

### BalanceSnapshot (신규 VO)
- [5] 시점 잔고 + 미체결 주문 합 묶기
```

## 작성 원칙

- 메서드 시그니처·의사코드는 적지 않는다. 계획 검수가 곧 코드 리뷰가 되면 의미가 사라진다.
- 계획 작성 후 검수자는 다음 셋을 답할 수 있어야 한다:
  1. 각 규칙이 올바른 위치에 들어갔는가
  2. 빠진 규칙은 없는가
  3. 재사용과 신규 추가를 잘 구분했는가
