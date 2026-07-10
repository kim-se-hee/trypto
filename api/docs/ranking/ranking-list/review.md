# 리뷰 이슈 — ranking-list

리뷰 범위 1차: `3a18566..569d8cd` (리팩토링 5커밋)

## 1차 차단 이슈

- [x] **[api/.../ranking/domain/vo/RankingSummaries.java:7-39] VO 에 equals()/hashCode() 미구현** (출처: 컨벤션) — **완료(`aefbe92`)**
  - **설명:** conventions.md(레이어별-Domain) "VO 는 equals()/hashCode() 를 반드시 구현한다". `RankingSummaries` 는 `summaries`·`hasNext` 를 가진 불변 일급 컬렉션 VO 인데 equals/hashCode 가 없다. 같은 커밋에서 추가된 `UserProfiles` 는 구현해 대비된다. (ranking 도메인의 기존 equals 없는 컬렉션 VO 는 pre-existing 이라 이번 diff 밖.)
  - **수정 제안:** `UserProfiles` 와 동일하게 `summaries`·`hasNext` 기준 `equals()`/`hashCode()` 를 추가한다.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [UserQueryPort.findByUserIds] 포트/메서드명이 CRUD 표현 — 도메인 의도명 고려. 코드베이스 관례 (ddd)
- [UserProfiles vs RankingSummaries] 생성 방식 불일치(public 생성자 vs private+정적 팩토리) — `of(...)` 류로 통일 고려 (oop)
- [RankingSummaries.toList()] 원시 접근자 이름 — `content()` 등 의도 드러나는 이름 고려 (oop)

## 판정 메모

- ddd/concurrency/performance: 조회 전용 조립(§5) 적절, ACL 번역 인프라 경계 내, 닉네임 벌크 조회 1회 유지(N+1 없음), 읽기 전용 트랜잭션 보존.
- 인수 테스트(ranking-list) 4 시나리오 통과.

## 2차 재리뷰 (`569d8cd..aefbe92`)

- 차단 1건 적용(`aefbe92`: `RankingSummaries` equals/hashCode 추가). 재리뷰 convention 차단 0건 — VO 규칙 충족 확인. 통과.
