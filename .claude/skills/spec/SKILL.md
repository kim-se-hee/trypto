---
description: >
  기능 스펙 작성 및 다듬기. 
  /spec draft 로 신규 스펙을 작성하고
  /spec refine 으로 사용자 피드백을 반영한다.
arguments: [subcommand, prefix, feature]
---

스펙은 이번 기능이 무엇이고(what) 어떤 비즈니스 규칙이 있는지(why) 정리하는 문서다. how 는 다루지 않는다.

## 입력

frontmatter `arguments: [subcommand, prefix, feature]` 로 명명된 위치 인자를 받는다.

- `$subcommand` = `draft` 또는 `refine`
- `$prefix` = 기능이 속한 위치
  - api 모듈 (DDD 적용): `api/<context>` (예: `api/trading`)
  - 그 외 모듈: 모듈명 그대로 (예: `engine`, `collector`, `frontend`)
- `$feature` = 기능 이름 (kebab-case, 예: `place-order`)

예시:
```
/spec draft api/trading place-order
/spec refine engine matching
```

인자가 부족하거나 형식이 맞지 않으면 사용자에게 호출 형태를 안내하고 종료한다.

## 분기

`$subcommand` 값에 따라 해당 흐름 문서를 Read 하여 그대로 따른다.

- `draft` → [draft.md](draft.md)
- `refine` → [refine.md](refine.md)

## 디렉터리 구조

```
docs/<prefix>/
├── index.md                    ← 이 prefix 의 기능 목차
└── <feature>/
    ├── index.md                ← 기능 메타 (단계, 회차, 산출물 목록)
    ├── spec.md                 ← what/why (이 스킬의 산출물)
    └── plan.md                 ← how (계획 단계에서 추가, 이 스킬은 만지지 않음)
```

## 파일 책임

| 파일 | 책임 |
|------|------|
| `<feature>/index.md` | 이 기능의 메타 정보 |
| `<feature>/spec.md` | 비즈니스 규칙, 요구사항 (what/why) |
| `<feature>/plan.md` | 구현 전략 (how, 별도 스킬에서 작성) |
| `<prefix>/index.md` | 이 prefix 의 기능 목차 |

## 양식

### `<feature>/index.md`

```markdown
- 단계: spec | plan | implement | review | revise | qa | done
- 리뷰 회차: 0/3
- QA 회차: 0/3

## 산출물
- [spec.md](spec.md) — <한 줄 요약>
- [plan.md](plan.md) — <한 줄 요약>     ← 계획 작성 후 추가
```

### `<prefix>/index.md`

```markdown
## 기능 목록
- [<feature>](<feature>/) — <한 줄 요약>
- ...
```

### `<feature>/spec.md`

```markdown
## 목적
<이 기능이 무엇이고 왜 필요한지>

## 비즈니스 규칙
1. <규칙 본문>
2. <규칙 본문> [ASSUME]
3. <규칙 본문> [BLOCK]
...

## 추측 근거
- [2] <짧은 근거>
- [3] <답이 필요한 이유>
```

## 마커

스펙 본문에서 불확실성을 표시한다.

- `[BLOCK]` — 사용자 답변 없이는 진행이 불가능한 항목
- `[ASSUME]` — 사용자 확인이 필요한 추측

자명한 것에는 붙이지 않는다.

refine 단계 전용 마커(`[CONFLICT]`)와 그 정의는 `refine.md` 참조.

## 비즈니스 규칙(BR) 번호 정책

- BR 번호는 기능별로 독립이다. 다른 기능에 같은 번호가 있어도 서로 무관하다.
- 한 파일 안에서는 1부터 순차 부여한다.

재번호 규칙과 계획 md 동기화는 `refine.md` 참조.

## 비즈니스 규칙이란

도메인이 지켜야 하는 규칙이다. 세부 구현이 바뀌어도 도메인이 존재하는 한 변하지 않는다. 어떻게 구현하는지가 아니라 어떻게 동작해야 하는지를 다룬다.

### 7종 분류 (작성 가이드)

규칙을 빠뜨리지 않기 위한 분류 가이드다. **본문에 분류 태그를 붙이지 않는다.**

1. **용어 정의** — 무엇이 무엇으로 구성되는가
   - 예: 배송지 = 받는 사람 + 전화번호 + 주소
2. **계산** — 값이 어떻게 산출되는가
   - 예: 총 주문 금액 = Σ(상품가 × 개수)
3. **제약 / 검증** — 무엇이 참이어야 하는가 (필수·금지·기한·잔고 등 모두 포함)
   - 배송지 정보 필수
   - 출고 전에만 취소 가능
   - 체결 금액 + 수수료 ≤ 주문 가능 금액
   - 환불은 주문일 기준 7일 이내
4. **상태 전이** — 어떤 상태에서 어떤 상태로 바뀔 수 있는가
   - 주문: `OPEN → PARTIALLY_FILLED → FILLED`, `OPEN → CANCELED`
   - 송금: `요청 → 처리중 → 완료/실패`
   - 체결된 주문은 취소할 수 없다
5. **권한** — 누가 할 수 있는가
   - 예: 본인 주문만 조회 가능
6. **부수 효과** — 어떤 일이 일어나면 무엇이 뒤따르는가 (실패 시 후속 동작 포함)
   - 결제 실패 시 주문 자동 취소
   - 잔고 부족 시 주문 거절
7. **우선순위 (메타)** — 규칙이 충돌할 때 무엇을 먼저 적용하는가
   - 예: 쿠폰과 적립금 동시 사용 시 쿠폰 먼저

모든 BR 이 이 7종에 들어맞는 것은 아니다. 분류는 누락 방지용 체크리스트일 뿐이다.
