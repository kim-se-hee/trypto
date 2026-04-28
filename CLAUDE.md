# trypto

코인 모의투자 플랫폼. 실거래소(업비트/빗썸/바이낸스) 시세 기반 모의 매매·송금·투자 랭킹·투자 복기를 제공한다.

## 디렉토리 구조

| 디렉토리 | 역할 |
|---------|------|
| `api/` | 사용자 API 처리 및 모의 투자 핵심 비즈니스 로직 |
| `collector/` | 거래소 시세 수집 및 발행 |
| `engine/` | 주문 매칭 및 체결 |
| `scheduler/` | 누락 주문 보상 배치 |
| `frontend/` | 사용자 웹 UI |
| `docker/` | 인프라 컨테이너 정의 |
| `loadtest/` | 부하 테스트 시나리오 |
| `docs/` | 프로젝트 문서 |


## 문서 인덱스

작업 시작 전 관련 문서 확인. 필요한 것만 읽는다.

**시스템 전체**
- [docs/architecture.md](docs/architecture.md) — 4개 서비스 전체 흐름
- [docs/db-schema.md](docs/db-schema.md) — 시스템 구성 및 모듈 간 데이터 흐름
- [docs/contracts/](docs/contracts/) — 메세지 큐 페이로드 및 이벤트 스펙
  - `engine-inbox.md` — engine.inbox 큐 페이로드
  - `ticker-exchange.md` — ticker.exchange 시세 이벤트
  - `outbox-events.md` — engine → api Outbox 이벤트

**컨벤션**
- [docs/conventions/git.md](docs/conventions/git.md) — Git 커밋/브랜치 규칙
