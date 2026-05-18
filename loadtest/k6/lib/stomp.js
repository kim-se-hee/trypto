// STOMP 1.2 over WebSocket helpers for k6.
// k6 의 WebSocket 모듈은 STOMP 를 모르므로 프레임을 직접 만들고 파싱한다.
//
// 핵심 함정:
//   - 클라→서버 하트비트는 약속된 주기 안에 안 보내면 서버가 dead 로 판정해 끊는다.
//     이벤트 루프 saturated 하면 setInterval 이 drift 한다. 협상값(server expects=10s) 보다
//     2초 짧게(8s) 보내서 마진을 둔다.
//   - 한 WebSocket frame 에 여러 STOMP frame 이 합쳐져 올 수 있다 (\0 구분).
//   - 서버가 보내는 LF 한 글자는 server→client 하트비트다. STOMP frame 아님.

const NULL = '\0';
const LF = '\n';

const HEARTBEAT_SEND_MS_DEFAULT = 8000;     // 서버 expect 10s, 마진 2s
const HEARTBEAT_RECV_MS_DEFAULT = 10000;
const ACCEPT_VERSION = '1.2';

export function buildConnectFrame(host, heartbeatSendMs, heartbeatRecvMs) {
  return (
    'CONNECT' + LF +
    'accept-version:' + ACCEPT_VERSION + LF +
    'host:' + host + LF +
    'heart-beat:' + heartbeatSendMs + ',' + heartbeatRecvMs + LF +
    LF + NULL
  );
}

export function buildSubscribeFrame(destination, subId) {
  return (
    'SUBSCRIBE' + LF +
    'id:' + subId + LF +
    'destination:' + destination + LF +
    LF + NULL
  );
}

export function buildUnsubscribeFrame(subId) {
  return (
    'UNSUBSCRIBE' + LF +
    'id:' + subId + LF +
    LF + NULL
  );
}

export function buildDisconnectFrame() {
  return 'DISCONNECT' + LF + LF + NULL;
}

// 도착한 텍스트 페이로드 안에 여러 STOMP frame 이 \0 으로 구분되어 들어올 수 있다.
// LF 단독 문자열은 server→client heartbeat 이며 frame 으로 취급하지 않는다.
export function parseFrames(rawText) {
  if (!rawText || rawText === LF) {
    return [];
  }
  const frames = [];
  const chunks = rawText.split(NULL);
  for (let i = 0; i < chunks.length; i++) {
    const chunk = chunks[i];
    if (!chunk || chunk === LF) {
      continue;
    }
    const trimmed = chunk.replace(/^\n+/, ''); // 선행 LF (heartbeat) 제거
    if (!trimmed) {
      continue;
    }
    const frame = parseSingleFrame(trimmed);
    if (frame) {
      frames.push(frame);
    }
  }
  return frames;
}

// (framed text) → { command, headers, body }
function parseSingleFrame(text) {
  const headerEnd = text.indexOf(LF + LF);
  if (headerEnd === -1) {
    return null;
  }
  const headerSection = text.substring(0, headerEnd);
  const body = text.substring(headerEnd + 2);
  const headerLines = headerSection.split(LF);
  const command = headerLines[0];
  const headers = {};
  for (let i = 1; i < headerLines.length; i++) {
    const line = headerLines[i];
    const colon = line.indexOf(':');
    if (colon !== -1) {
      headers[line.substring(0, colon)] = line.substring(colon + 1);
    }
  }
  return { command, headers, body };
}

// host: STOMP CONNECT frame 의 host 헤더값. 서버가 검증하지는 않지만 로깅용.
export function defaultConnectOptions(host) {
  return {
    host: host || 'trypto',
    heartbeatSendMs: HEARTBEAT_SEND_MS_DEFAULT,
    heartbeatRecvMs: HEARTBEAT_RECV_MS_DEFAULT,
  };
}

// 서버 측 LF 한 바이트.
export const HEARTBEAT_FRAME = LF;
