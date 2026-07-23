package ksh.tryptoengine.dbwriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptoengine.matching.MarketRefResolver;
import ksh.tryptoengine.matching.OrderDetail;
import ksh.tryptoengine.matching.Side;
import ksh.tryptoengine.metrics.EngineMetrics;
import ksh.tryptoengine.outbox.OrderFilledEvent;
import ksh.tryptoengine.outbox.OutboxPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class FillTransactionExecutor {

    private static final String ORDER_FILLED = "ORDER_FILLED";
    private static final int MONEY_SCALE = 8;

    private final JdbcTemplate jdbc;
    private final HoldingIncrementalUpdater holdingUpdater;
    private final ObjectMapper objectMapper;
    private final EngineMetrics metrics;
    private final OutboxPublisher outboxPublisher;
    private final MarketRefResolver marketRefResolver;

    public FillTransactionExecutor(
            JdbcTemplate jdbc,
            HoldingIncrementalUpdater holdingUpdater,
            @Qualifier("engineObjectMapper") ObjectMapper objectMapper,
            EngineMetrics metrics,
            OutboxPublisher outboxPublisher,
            MarketRefResolver marketRefResolver) {
        this.jdbc = jdbc;
        this.holdingUpdater = holdingUpdater;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.outboxPublisher = outboxPublisher;
        this.marketRefResolver = marketRefResolver;
    }

    @Transactional
    public void executeBatch(List<FillCommand> cmds) {
        if (cmds.isEmpty()) return;

        int[] updated = jdbc.batchUpdate(
                "UPDATE orders SET status='FILLED', filled_price=?, filled_at=?, fee=? "
                        + "WHERE order_id=? AND status='PENDING'",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        FillCommand cmd = cmds.get(i);
                        ps.setBigDecimal(1, cmd.executedPrice());
                        ps.setTimestamp(2, Timestamp.valueOf(cmd.executedAt()));
                        ps.setBigDecimal(3, fee(cmd));
                        ps.setLong(4, cmd.order().orderId());
                    }

                    @Override
                    public int getBatchSize() {
                        return cmds.size();
                    }
                });

        List<FillCommand> succeeded = new ArrayList<>(cmds.size());
        for (int i = 0; i < cmds.size(); i++) {
            if (updated[i] > 0) {
                succeeded.add(cmds.get(i));
            } else {
                log.debug(
                        "fill skipped orderId={} already non-pending",
                        cmds.get(i).order().orderId());
            }
        }
        if (succeeded.isEmpty()) return;

        List<Settlement> settlements = new ArrayList<>(succeeded.size());
        for (FillCommand cmd : succeeded) {
            settlements.add(settle(cmd));
        }

        jdbc.batchUpdate(
                "UPDATE wallet_balance SET locked = locked - ?, available = available + ? "
                        + "WHERE wallet_id = ? AND coin_id = ?",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Settlement s = settlements.get(i);
                        OrderDetail o = s.order();
                        ps.setBigDecimal(1, o.lockedAmount());
                        ps.setBigDecimal(2, s.refund());
                        ps.setLong(3, o.walletId());
                        ps.setLong(4, o.lockedCoinId());
                    }

                    @Override
                    public int getBatchSize() {
                        return settlements.size();
                    }
                });

        jdbc.batchUpdate(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, 0) ON DUPLICATE KEY UPDATE available = available + VALUES(available)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Settlement s = settlements.get(i);
                        ps.setLong(1, s.order().walletId());
                        ps.setLong(2, s.creditCoinId());
                        ps.setBigDecimal(3, s.creditAmount());
                    }

                    @Override
                    public int getBatchSize() {
                        return settlements.size();
                    }
                });

        List<OrderFilledEvent> events = new ArrayList<>(succeeded.size());
        List<String> payloads = new ArrayList<>(succeeded.size());
        for (FillCommand cmd : succeeded) {
            OrderDetail o = cmd.order();
            OrderFilledEvent event = new OrderFilledEvent(
                    o.orderId(), cmd.executedPrice(), o.quantity(), cmd.executedAt(), cmd.matchedAt());
            events.add(event);
            try {
                payloads.add(objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("outbox payload serialization failed", e);
            }
        }

        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());
        List<Long> outboxIds = jdbc.execute((java.sql.Connection conn) -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO outbox (event_type, payload, created_at," + " matched_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < payloads.size(); i++) {
                    ps.setString(1, ORDER_FILLED);
                    ps.setString(2, payloads.get(i));
                    ps.setTimestamp(3, createdAt);
                    ps.setTimestamp(4, Timestamp.valueOf(succeeded.get(i).matchedAt()));
                    ps.addBatch();
                }
                ps.executeBatch();
                List<Long> ids = new ArrayList<>(payloads.size());
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
                return ids;
            }
        });

        holdingUpdater.apply(succeeded);

        metrics.matches().increment(succeeded.size());

        if (outboxIds != null && outboxIds.size() == events.size()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    outboxPublisher.publishAsync(outboxIds, events);
                }
            });
        } else {
            log.warn(
                    "outbox generated key count mismatch ids={} events={}; polling will pick up",
                    outboxIds == null ? -1 : outboxIds.size(),
                    events.size());
        }
    }

    private Settlement settle(FillCommand cmd) {
        OrderDetail o = cmd.order();
        BigDecimal fillAmount = fillAmount(cmd);
        BigDecimal fee = fee(cmd);
        boolean buy = o.side() == Side.BUY;
        BigDecimal refund = buy ? o.lockedAmount().subtract(fillAmount.add(fee)) : BigDecimal.ZERO;
        Long creditCoinId = buy ? o.coinId() : o.baseCoinId();
        BigDecimal creditAmount = buy ? o.quantity() : fillAmount.subtract(fee);
        return new Settlement(o, refund, creditCoinId, creditAmount);
    }

    private BigDecimal fillAmount(FillCommand cmd) {
        return cmd.executedPrice().multiply(cmd.order().quantity()).setScale(MONEY_SCALE, RoundingMode.FLOOR);
    }

    private BigDecimal fee(FillCommand cmd) {
        BigDecimal feeRate = cmd.order().feeRate();
        if (feeRate == null) {
            log.warn("feeRate missing orderId={}, fee=0 처리", cmd.order().orderId());
            feeRate = BigDecimal.ZERO;
        }
        return fillAmount(cmd).multiply(feeRate).setScale(feeScale(cmd), RoundingMode.FLOOR);
    }

    // 수수료는 기축통화 자릿수로 내림 절삭한다 (DOMESTIC=KRW 정수, OVERSEAS=USDT 8자리)
    private int feeScale(FillCommand cmd) {
        MarketRefResolver.MarketRef ref =
                marketRefResolver.resolve(cmd.order().pair().exchangeCoinId());
        if (ref == null) {
            log.warn(
                    "market ref missing orderId={}, feeScale={} 처리", cmd.order().orderId(), MONEY_SCALE);
            return MONEY_SCALE;
        }
        return ref.feeScale();
    }

    private record Settlement(OrderDetail order, BigDecimal refund, Long creditCoinId, BigDecimal creditAmount) {}
}
