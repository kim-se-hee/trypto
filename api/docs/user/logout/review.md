# 로그아웃 리뷰

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·동시성·성능·컨벤션) 모두 차단 이슈 0건. 승인.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [api/.../application/port/out/SessionCommandPort.java:6] 포트 메서드명 `delete` 의 유비쿼터스 언어 — 도메인 의도상 `invalidate` 가 더 명확하나, 기술 아웃바운드 포트이고 기존 `create` 와의 대칭성상 현행도 무방 (출처: ddd)
- [api/.../adapter/in/web/AuthController.java:56-74] 쿠키 빌더 중복 — `buildSessionCookie` 와 `buildExpiredSessionCookie` 가 httpOnly/secure/sameSite/path 4줄 복제. 값만 다르므로 공통 빌더 추출 여지 (출처: oop)
- [frontend/src/contexts/AuthContext.tsx:46-50] 로그아웃 실패를 무음으로 삼킴 — `catch {}` 가 네트워크/5xx 오류까지 흔적 없이 삼킴. 클라이언트 상태 초기화는 유지하되 최소 로깅 권장 (출처: oop)
- [api/.../acceptance/steps/user/LogoutStepDefinition.java:36-44] 동일 구현 스텝 두 개 — `로그아웃을_요청한다` 와 `세션_쿠키_없이_로그아웃을_요청한다` 본문 동일 (출처: oop)
- [frontend/src/contexts/AuthContext.tsx:46-53] 로그아웃 버튼 중복 클릭 시 중복 요청 — 백엔드 멱등이라 정합성 문제는 없음. disabled/로딩 상태로 방어 가능 (출처: 동시성)
