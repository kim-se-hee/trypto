package ksh.tryptocollector.distribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import ksh.tryptocollector.distribute.rabbitmq.EngineInboxPublisher;
import ksh.tryptocollector.distribute.rabbitmq.TickerEventConflator;
import ksh.tryptocollector.distribute.redis.TickerRedisRepository;
import ksh.tryptocollector.distribute.tick.TickRawWriter;
import ksh.tryptocollector.model.NormalizedTicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TickerSinkProcessorTest {

    @Mock
    private TickerRedisRepository tickerRedisRepository;

    @Mock
    private TickerEventConflator tickerEventConflator;

    @Mock
    private EngineInboxPublisher engineInboxPublisher;

    @Mock
    private TickRawWriter tickRawWriter;

    private TickerSinkProcessor tickerSinkProcessor;

    @BeforeEach
    void setUp() {
        tickerSinkProcessor = new TickerSinkProcessor(
                tickerRedisRepository, tickerEventConflator, engineInboxPublisher, tickRawWriter);
    }

    @Test
    @DisplayName("TickRawWriter 예외가 나도 Redis/conflator/engine.inbox 발행은 계속된다")
    void givenTickRawWriterThrows_whenProcess_thenOtherSinksProceed() {
        NormalizedTicker ticker = new NormalizedTicker(
                "upbit",
                "BTC",
                "KRW",
                "BTC/KRW",
                new BigDecimal("50000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                System.currentTimeMillis());
        willThrow(new RuntimeException("write error")).given(tickRawWriter).write(any());

        tickerSinkProcessor.process(ticker);

        verify(tickerRedisRepository).save(ticker);
        verify(tickerEventConflator).submit(ticker);
        verify(engineInboxPublisher).publish(ticker);
    }
}
