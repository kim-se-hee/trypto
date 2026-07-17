package ksh.tryptocollector.ingest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.http.WebSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import ksh.tryptocollector.model.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebSocketLivenessGuardTest {

    @Mock
    private WebSocket webSocket;

    private WebSocketLivenessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new WebSocketLivenessGuard();
        ReflectionTestUtils.setField(guard, "pingIntervalSeconds", 1L);
        ReflectionTestUtils.setField(guard, "staleThresholdSeconds", 60L);
    }

    @Test
    @DisplayName("연결이 종료되면 abort 없이 반환한다")
    void givenLatchReleased_whenWatch_thenReturnsWithoutAbort() throws Exception {
        CountDownLatch closeLatch = new CountDownLatch(1);
        closeLatch.countDown();

        guard.watch(Exchange.UPBIT, webSocket, new ConnectionLiveness(), closeLatch);

        verify(webSocket, never()).abort();
        verify(webSocket, never()).sendPing(any());
    }

    @Test
    @DisplayName("연결이 살아 있는 동안 주기마다 PING을 보낸다")
    void givenHealthyConnection_whenWatch_thenSendsPingPeriodically() throws Exception {
        CountDownLatch closeLatch = new CountDownLatch(1);
        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            closeLatch.countDown();
        });
        releaser.start();

        guard.watch(Exchange.UPBIT, webSocket, new ConnectionLiveness(), closeLatch);
        releaser.join();

        verify(webSocket, atLeastOnce()).sendPing(any());
        verify(webSocket, never()).abort();
    }

    @Test
    @DisplayName("무수신이 임계값을 넘으면 abort하고 반환한다")
    void givenStaleConnection_whenWatch_thenAbortsAndReturns() throws Exception {
        ReflectionTestUtils.setField(guard, "staleThresholdSeconds", 1L);
        CountDownLatch closeLatch = new CountDownLatch(1);
        ConnectionLiveness liveness = new ConnectionLiveness();
        TimeUnit.MILLISECONDS.sleep(50);

        guard.watch(Exchange.BITHUMB, webSocket, liveness, closeLatch);

        verify(webSocket).abort();
    }

    @Test
    @DisplayName("수신이 이어지는 동안에는 임계값이 지나도 abort하지 않는다")
    void givenContinuousReceive_whenWatch_thenDoesNotAbort() throws Exception {
        ReflectionTestUtils.setField(guard, "staleThresholdSeconds", 2L);
        CountDownLatch closeLatch = new CountDownLatch(1);
        ConnectionLiveness liveness = new ConnectionLiveness();
        Thread receiver = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    liveness.recordReceive();
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            closeLatch.countDown();
        });
        receiver.start();

        guard.watch(Exchange.BINANCE, webSocket, liveness, closeLatch);
        receiver.join();

        verify(webSocket, never()).abort();
    }
}
