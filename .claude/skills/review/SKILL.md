---
description: >
  현재 브랜치 변경분을 5개 전문 리뷰어로 병렬 검증하고, 차단 이슈를
  review.md 의 체크박스 리스트로 적은 뒤 review-applier 에 한 건씩 위임한다.
  /review 단독 또는 /review <scope> <feature> 로 호출한다.
arguments: [scope?, feature?]
---

`/review` 는 현재 브랜치의 변경 코드를 OOP·동시성·성능·계약 관점으로 동시에 점검한다 (api 모듈이 변경된 경우 DDD 관점 추가). 차단 이슈를 별도 마크다운에 체크박스 리스트로 적어두고, 메인은 한 건씩 `review-applier` 에 위임하면서 체크박스를 갱신한다. applier 는 매 호출마다 깨끗한 컨텍스트에서 한 이슈만 처리한다.

## 입력

frontmatter `arguments: [scope?, feature?]` 둘 다 옵셔널.

- 인자 없음 → 현재 브랜치 변경분만 본다. 체크리스트는 `.review/issues.md` 임시 위치에 둔다.
- 둘 다 지정 → ddd-reviewer 에 `docs/<scope>/<feature>/spec.md` 와 `plan.md` 를 같이 넘겨 BR 매핑까지 검증. 체크리스트는 `docs/<scope>/<feature>/review.md` 에 저장한다 (PR 추적용).

예시:
```
/review
/review api/trading place-order
```

scope 만 주거나 feature 만 주는 부분 입력은 거부하고 사용자에게 호출 형태를 안내 후 종료한다.

## 사전 제약

`git diff main...HEAD` 가 비어 있고 미커밋 변경도 없으면 "리뷰할 변경 없음" 알리고 종료한다.

`scope`/`feature` 가 주어졌는데 `docs/<scope>/<feature>/spec.md` 가 없으면 사용자에게 알리고 종료한다.

기능 디렉토리(`docs/<scope>/<feature>/`)에 `review.md` 를 먼저 만든다.  이 파일에 리뷰 사항을 기록한다. (인자 없는 호출은 `.review/issues.md`)

## 흐름

### 1. 1차 리뷰 (병렬)

`git diff --name-only main...HEAD` (미커밋 변경이 있으면 `git status` 도 합쳐) 로 변경 파일 목록을 만든다.

다음 리뷰어들을 **단일 메시지에서 병렬로** 호출한다.

- `ddd-reviewer` — 변경 파일에 `api/` 경로가 하나라도 포함된 경우에만 호출. 그 외엔 스킵
- `oop-reviewer`
- `concurrency-reviewer`
- `performance-reviewer`
- `contract-reviewer`

각 호출 프롬프트에 `scope`, `feature` (있으면) 만 전달한다. 각 리뷰어는 자기 출력 형식대로 마크다운 본문을 메인에 직접 반환한다.

### 2. 이슈 리스트 작성

메인이 5개 반환값을 종합해 `review.md` 에 체크박스 리스트로 저장한다. 양식:

```markdown
## N차 차단 이슈

- [ ] **[파일경로:라인] 이슈 제목** (출처: ddd | oop | 동시성 | 성능 | 계약, CRITICAL | MAJOR)
  - **설명:** ...
  - **수정 제안:** ...

- [ ] **[파일경로:라인] ...**
  - ...

## N차 참고 이슈 (수정 안 함, 보고용)

- [파일:라인] 제목 (출처, MINOR | Pre-existing)
- ...
```

분류 규칙:
- **차단 이슈**: 모든 리뷰어의 CRITICAL + MAJOR 합집합. 단 `[Pre-existing]` 라벨은 제외.
- **참고 이슈**: MINOR + Pre-existing. 자동 적용 대상이 아님.

차단 이슈가 0건이면 4단계로. 1건 이상이면 3단계로.

회차마다 *기존 내용을 지우지 않고* 새 섹션(`## 2차 차단 이슈` 등)을 아래에 추가한다 — 사용자가 어떤 이슈가 나왔다 사라졌는지 한 파일에서 추적할 수 있게.

### 3. 적용 루프

미체크 차단 이슈를 위→아래 순서로 한 건씩 `review-applier` 서브에이전트에 위임한다. 직렬 실행.

각 이슈마다:

1. `review-applier` 호출. 프롬프트에 `scope`, `feature`, `이슈 본문` (체크박스 한 항목 전체) 을 전달.
2. 정상 종료 후 메인이 해당 줄의 `- [ ]` → `- [x]` 로 갱신한다.
3. applier 가 막히면 (해석 불가, 수정 후에도 검증 실패 등) 그 메시지를 사용자에게 그대로 전달하고 루프를 중단한다.

### 4. 재리뷰

차단 이슈가 처음부터 0건이었거나 모두 체크됐으면 1단계를 다시 한다. 단 다음 종료 조건을 추가한다.

- **최대 3회**: 1단계는 최대 3회 실행. 3회째에도 새 차단 이슈가 나오면 사용자에게 잔여 이슈를 보고하고 종료.
- **무한 루프 방지**: 새 회차 차단 이슈 식별자(파일+라인+제목) 집합이 직전 회차와 **완전히 동일**하면 applier 가 같은 자리에서 막힌 것이므로 즉시 사용자에게 에스컬레이션하고 종료한다.
- **0건이면 통과**: 새 회차에서 차단 이슈가 0건이면 5단계로.

### 5. 기능 index.md 갱신

다음 조건이 모두 참일 때만 `docs/<scope>/<feature>/index.md` 의 `단계` 줄을 `단계: review` 로 갱신한다.

- `scope`/`feature` 인자가 주어졌다 (인자 없는 `/review` 는 갱신 대상이 없음).
- 4단계가 "차단 이슈 0건 통과" 로 끝났다 (잔존·에스컬레이션 종료는 미갱신).

기능 디렉토리에 `index.md` 가 아직 없으면 그대로 둔다 — 만들지 않는다. 단계 줄은 "review 단계까지 깔끔히 끝났다" 만 의미한다.

### 6. 보고

```
리뷰 완료: <scope>/<feature> | 현재 브랜치
변경 파일: M개
체크리스트: <review-md-path>
차단 이슈: 통과 (0건) | 잔존 N건 (실패)
참고 이슈: MINOR N건 / Pre-existing N건 (수정하지 않음)
커밋: <applier 가 만든 SHA 목록 또는 "없음">
단계 갱신: review / 미갱신 (사유)
```

차단 이슈가 잔존한 채 종료되면 `review.md` 의 미체크 항목을 그대로 보면 된다.

## 단계 격리

- 메인 세션은 리뷰 디스패치와 `review.md` 갱신(이슈 작성·체크박스 갱신)만 한다. 코드를 직접 수정하지 않는다.
- 코드 수정과 커밋은 `review-applier` 만 한다.
- 메인이 만지는 파일은 `review.md` 뿐이다.
