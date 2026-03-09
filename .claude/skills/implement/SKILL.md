---
description: >
  기능 문서 기반 코드 구현. 플랜 모드에서 생성된 docs/{domain}/{feature}.md를 읽고
  CLAUDE.md 컨벤션에 따라 전체 스택을 구현한다.
  TRIGGER: 사용자가 기능 구현을 요청하거나 /implement를 입력할 때.
---

# 기능 문서 기반 코드 구현

기능 문서(`docs/{domain}/{feature}.md`)를 읽고 코드를 구현한다. 에이전트 없이 메인 컨텍스트에서 직접 실행한다.

## 입력

`$ARGUMENTS` = 기능 문서 경로 (예: `docs/wallet/transfer.md`)

---

## Phase 1: 구현

### 사전 읽기

구현 시작 전 아래 파일을 Read로 읽는다:

1. `docs/ai-context/interface-map.json` — 대상 컨텍스트 및 연관 컨텍스트의 UseCase 시그니처와 DTO 확인
2. `docs/data-model.md`의 "모듈 간 의존" 섹션 — 크로스 컨텍스트 의존 파악

단, 기능 문서에서 크로스 컨텍스트 연동이 없는 단일 컨텍스트 작업이면 2번은 생략한다.

### 구현

CLAUDE.md의 코딩 컨벤션과 Git 컨벤션을 따라 구현한다.

- 컴파일 의존 순서대로 구현한다 (domain → application → adapter)
- 논리 단위마다 `./gradlew compileJava` 로 검증 후 커밋한다
- 커밋 단위와 메시지는 CLAUDE.md의 Git 컨벤션을 따른다

구현 완료 후 ArchUnit 테스트를 실행한다.

```bash
./gradlew test --tests "*ArchUnit*"
```

ArchUnit 테스트가 전부 통과할 때까지 코드를 수정하고 재실행한다. 통과하면 Phase 2로 넘어간다.

## Phase 2: 테스트

test-automator 서브에이전트에 위임한다.

프롬프트에 아래 정보를 포함한다:
- 기능 문서 경로
- Phase 1에서 생성/수정한 파일 목록

## Phase 3: 코드 리뷰

아래 4개의 리뷰어 서브에이전트를 병렬로 실행한다.

- architecture-reviewer
- code-quality-reviewer
- performance-reviewer
- concurrency-reviewer

리뷰 결과를 종합하여 사용자에게 보고한다.
