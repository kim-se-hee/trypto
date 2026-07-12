package ksh.tryptoengine.outbox;

import jakarta.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OutboxPublisher {

    private final RabbitTemplate rabbit;
    private final JdbcTemplate jdbc;
    private final String fanoutExchange;
    private final ExecutorService pool;

    public OutboxPublisher(
            RabbitTemplate rabbit,
            JdbcTemplate jdbc,
            @Value("${engine.outbox.fanout-exchange}") String fanoutExchange,
            @Value("${engine.outbox.publisher-threads:4}") int threads) {
        this.rabbit = rabbit;
        this.jdbc = jdbc;
        this.fanoutExchange = fanoutExchange;
        AtomicInteger seq = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "outbox-publisher-" + seq.incrementAndGet());
            t.setDaemon(false);
            return t;
        });
    }

    public void publishAsync(List<Long> ids, List<OrderFilledEvent> events) {
        if (ids.isEmpty()) return;
        pool.submit(() -> doPublish(ids, events));
    }

    private void doPublish(List<Long> ids, List<OrderFilledEvent> events) {
        List<Long> sent = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            OrderFilledEvent event = events.get(i);
            try {
                rabbit.convertAndSend(fanoutExchange, "", event);
                sent.add(ids.get(i));
            } catch (Exception e) {
                log.warn("hook publish failed id={}, polling will retry", ids.get(i), e);
            }
        }
        if (sent.isEmpty()) return;

        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            jdbc.batchUpdate(
                    "UPDATE outbox SET sent_at = ? WHERE id = ? AND sent_at IS NULL",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setTimestamp(1, now);
                            ps.setLong(2, sent.get(i));
                        }

                        @Override
                        public int getBatchSize() {
                            return sent.size();
                        }
                    });
        } catch (Exception e) {
            log.warn("hook sent_at update failed size={}, polling will retry remaining", sent.size(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
