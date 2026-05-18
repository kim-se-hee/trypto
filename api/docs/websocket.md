# WebSocket / SSE 인프라

## 개요

클라이언트에게 실시간 데이터를 push 하기 위해 두 개의 단방향 push 채널을 사용한다.

| 데이터 | 프로토콜 | 채널 |
|--------|----------|------|
| 시세 (거래소별 broadcast) | SSE | `GET /api/sse/tickers/{exchangeId}` |
| 체결 통지 (사용자별 1:1) | STOMP over WebSocket | `/user/{userId}/queue/events` |

## 시세 — SSE

거래소당 N 명에게 같은 payload 를 fan-out 할 때, STOMP/topic 는 메시지마다 `subscription:sub-X` 같은 사용자별 헤더가 박혀 frame byte[] 를 N 번 새로 빌드하지만 SSE 는 같은 byte[] 한 벌로 끝난다. 시세 hot path 의 가공 비용을 잘라내기 위해 SSE 를 쓴다.

상세는 [marketdata/live-ticker-streaming.md](marketdata/live-ticker-streaming.md).

## 체결 통지 — STOMP over WebSocket

사용자별 1:1 push 라 fan-out 비용이 없어 SSE 로 옮길 동기가 없다. Spring 의 user destination resolver 와 STOMP 클라이언트 라이브러리(`@stomp/stompjs`) 기능을 그대로 활용한다.

### 기술 선택

| 항목 | 선택 | 이유 |
|------|------|------|
| 프로토콜 | STOMP over WebSocket | Spring 네이티브 지원, user destination 자동 라우팅 |
| STOMP 브로커 | SimpleBroker | 사용자가 연결된 서버에서 직접 push, 크로스 서버 전파 불필요 |
| 폴백 | SockJS 미사용 | 2026년 기준 모든 모던 브라우저가 WebSocket 네이티브 지원 |

### 엔드포인트

```
ws://{host}/ws
```

### 인증

현재 WebSocket 으로 제공하는 데이터는 체결 통지(개인 데이터)이며 사용자 식별은 user destination 으로 처리한다.

### 하트비트

| 방향 | 주기 | 설명 |
|------|------|------|
| 서버 → 클라이언트 | 10초 | 서버가 살아있음을 알림 |
| 클라이언트 → 서버 | 10초 | 클라이언트가 살아있음을 알림 |

- STOMP 프로토콜의 `heart-beat` 헤더로 협상한다
- 3회 연속 하트비트 미수신 시 연결이 끊긴 것으로 판단한다

### 세션 타임아웃

- 비활성 세션 타임아웃: 30분
- 연결 해제 시 서버에서 구독 자원을 정리한다

### 재연결 전략 (클라이언트)

| 시도 | 대기 시간 | 설명 |
|------|----------|------|
| 1회 | 1초 | 즉시 재시도 |
| 2회 | 2초 | |
| 3회 | 4초 | |
| 4회 | 8초 | |
| 5회~ | 30초 | 최대 대기 시간 |

- 지수 백오프(Exponential Backoff)로 재연결을 시도한다
- 재연결 성공 시 이전 구독을 모두 복원한다
- 재연결 성공 시 REST API 로 최신 데이터를 1회 fetch 하여 동기화한다

## 설정

```yaml
# application.yml
app:
  websocket:
    endpoint: /ws
    heartbeat-send: 10000
    heartbeat-receive: 10000
```

## 채널 목록

| 채널 | 전송 방식 | 기능 문서 |
|------|----------|----------|
| `/api/sse/tickers/{exchangeId}` | SSE (거래소별 broadcast) | [live-ticker-streaming.md](marketdata/live-ticker-streaming.md) |
| `/user/{userId}/queue/events` | STOMP (사용자별 1:1) | 체결 통지 |
