package ksh.tryptocollector.ingest;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import ksh.tryptocollector.model.Exchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 반열림 연결은 수신 전용 소켓에서 영원히 감지되지 않으므로, 주기적 PING 발신과
// 무수신 임계값 검사로 죽은 연결을 강제 폐기해 재연결 루프를 발동시킨다.
@Slf4j
@Component
public class WebSocketLivenessGuard {

    @Value("${websocket.ping-interval-seconds:30}")
    private long pingIntervalSeconds;

    @Value("${websocket.stale-threshold-seconds:60}")
    private long staleThresholdSeconds;

    public void watch(Exchange exchange, WebSocket webSocket, ConnectionLiveness liveness, CountDownLatch closeLatch)
            throws InterruptedException {
        while (!closeLatch.await(pingIntervalSeconds, TimeUnit.SECONDS)) {
            if (liveness.secondsSinceLastReceive() >= staleThresholdSeconds) {
                log.warn("{} WebSocket {}초 이상 무수신, 연결 폐기 후 재연결", exchange, staleThresholdSeconds);
                webSocket.abort();
                return;
            }
            sendPing(webSocket);
        }
    }

    private void sendPing(WebSocket webSocket) {
        try {
            webSocket.sendPing(ByteBuffer.allocate(0));
        } catch (RuntimeException e) {
            log.debug("PING 전송 실패: {}", e.getMessage());
        }
    }
}
