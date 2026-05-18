package ksh.tryptocollector.distribute;

import ksh.tryptocollector.model.NormalizedTicker;
import ksh.tryptocollector.distribute.rabbitmq.EngineInboxPublisher;
import ksh.tryptocollector.distribute.rabbitmq.TickerEventConflator;
import ksh.tryptocollector.distribute.redis.TickerRedisRepository;
import ksh.tryptocollector.distribute.tick.TickRawWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TickerSinkProcessor {
    private final TickerRedisRepository tickerRedisRepository;
    private final TickerEventConflator tickerEventConflator;
    private final EngineInboxPublisher engineInboxPublisher;
    private final TickRawWriter tickRawWriter;

    public void process(NormalizedTicker ticker) {
        writeRawTick(ticker);
        saveToRedis(ticker);
        publishEvent(ticker);
        publishToEngine(ticker);
    }

    private void writeRawTick(NormalizedTicker ticker) {
        try {
            tickRawWriter.write(ticker);
        } catch (Exception e) {
            log.debug("InfluxDB raw tick 저장 실패: {}", e.getMessage());
        }
    }

    private void saveToRedis(NormalizedTicker ticker) {
        try {
            tickerRedisRepository.save(ticker);
        } catch (Exception e) {
            log.error("Redis 저장 실패: {}/{}", ticker.exchange(), ticker.base(), e);
        }
    }

    private void publishEvent(NormalizedTicker ticker) {
        try {
            tickerEventConflator.submit(ticker);
        } catch (Exception e) {
            log.error("RabbitMQ conflator 제출 실패: {}/{}", ticker.exchange(), ticker.base(), e);
        }
    }

    private void publishToEngine(NormalizedTicker ticker) {
        try {
            engineInboxPublisher.publish(ticker);
        } catch (Exception e) {
            log.error("engine.inbox 발행 실패: {}/{}", ticker.exchange(), ticker.base(), e);
        }
    }
}
