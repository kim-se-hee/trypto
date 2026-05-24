---
description: >
  대화로 기능 스펙을 작성·편집한다.
  /spec <scope> <feature> 로 호출하면
  spec.md 가 없으면 신규로 만들고, 있으면 편집한다.
arguments: [scope, feature]
---

스펙은 이번 기능이 무엇이고(what) 어떤 비지니스 규칙이 있는지(why) 정리하는 문서다. how 는 다루지 않는다.

스펙은 사람과 대화하면서 만든다. AI 가 모르는 것은 추측해서 박지 말고 적극적으로 `AskUserQuestion` 툴을 사용해서 그 자리에서 묻는다.

당신의 사용자는 틀린 이야기를 할 때도 많으며 틀린 이야기를 합리적으로 지적당하는 것을 수상할 정도로 기뻐한다. 언제나 사용자의 주장을 비판적으로 사고해라.

## 입력

frontmatter `arguments: [scope, feature]` 로 명명된 위치 인자를 받는다.

- `$scope` = 기능이 속한 위치
  - api 모듈 (DDD 적용): `api/<context>` (예: `api/trading`)
  - 그 외 모듈: 모듈명 그대로 (예: `engine`, `collector`, `frontend`)
- `$feature` = 기능 이름 (kebab-case, 예: `place-order`)

예시:

```
/spec api/trading place-order
/spec engine matching
```

인자가 부족하거나 형식이 맞지 않으면 호출 형태를 안내하고 종료한다.

## feature 디렉터리 구조

`docs` 는 항상 모듈 루트 바로 아래에 있다. api 모듈만 그 안에서 바운디드 컨텍스트로 한 단계 더 나뉜다.

- api (scope `api/<context>`):
  ```
  api/docs/<context>/
  ├── index.md                    ← 이 context 의 기능 목차
  └── <feature>/
      ├── index.md                ← 기능 메타 (단계, 회차, 산출물 목록)
      ├── spec.md                 ← what/why (이 스킬의 산출물)
      └── plan.md                 ← how (계획 단계에서 추가, 이 스킬은 만지지 않음)
  ```
- 그 외 (scope `<module>`):
  ```
  <module>/docs/
  ├── index.md                    ← 이 모듈의 기능 목차
  └── <feature>/
      ├── index.md
      ├── spec.md
      └── plan.md
  ```

## 작업 흐름

기능 폴더의 `spec.md` 가 없으면:
- 폴더와 빈 spec.md / index.md 를 만든다.
- 한 단계 위 `index.md` (api 면 `api/docs/<context>/index.md`, 그 외면 `<module>/docs/index.md`) 의 기능 md 목록에 해당 spec.md를 한 줄 요약과 함께 추가한다.

이후는 사용자 프롬프트에 따라 spec.md 를 채우거나 고친다.

대화 중 모르거나 사용자 확인이 필요한 부분은 반드시 `AskUserQuestion` 으로 그 자리에서 묻는다.

## 양식

### `<feature>/index.md`

```markdown
- 단계: spec | plan | implement | review | qa | done
- 리뷰 회차: 0/3
- QA 회차: 0/3

## 산출물
- [spec.md](spec.md)
```

### 기능 목차 `index.md`

```markdown
## 기능 명세
- [ranking-list.md](ranking-list.md) — 랭킹 목록
- [my-ranking.md](my-ranking.md) — 내 랭킹 조회
```

### `<feature>/spec.md`

```markdown
## 목적
<이 기능이 무엇이고 왜 필요한지>

## 비지니스 규칙
- <규칙 본문>
- <규칙 본문>
...
```

## 비지니스 규칙이란

도메인이 지켜야 하는 규칙이다. 세부 구현이 바뀌어도 도메인이 존재하는 한 변하지 않는다. 어떻게 구현하는지가 아니라 어떻게 동작해야 하는지를 다룬다.

### 예시

비지니스 규칙이 무엇이지 이해하기 위한 가이드다. 모든 비지니스 규칙이 이 7종에 들어맞는 것은 아니다. 예시는 비지니스 로직의 감을 잡기 위한 것일 뿐이다.

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
