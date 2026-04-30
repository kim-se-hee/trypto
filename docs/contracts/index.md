서비스 간 메시지 큐 페이로드와 이벤트 스펙을 모아두는 디렉토리. 각 채널의 페이로드 형태·전달 보장·소비자 약속을 기록한다.

## 채널 목록

- [engine-inbox.md](engine-inbox.md) — 매칭 엔진 인바운드 이벤트(주문 접수/취소·시세 tick)를 직렬화하는 단일 큐
- [ticker-exchange.md](ticker-exchange.md) — 거래소 시세 정규화 결과 실시간 브로드캐스트
- [outbox-events.md](outbox-events.md) — 매칭 엔진 체결 확정 후 api로 전달되는 이벤트
