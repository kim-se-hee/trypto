package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.application.port.out.PriceChangeRatePort;
import ksh.tryptobackend.trading.application.port.out.ViolationRulePort;
import ksh.tryptobackend.trading.domain.model.*;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViolationCheckService {

    private final ViolationRulePort violationRulePort;
    private final HoldingPersistencePort holdingPersistencePort;
    private final PriceChangeRatePort priceChangeRatePort;
    private final OrderPersistencePort orderPersistencePort;
    private final Clock clock;

    public List<RuleViolation> checkOrderViolations(Order order, Long walletId,
                                                    Long exchangeCoinId, Long coinId,
                                                    BigDecimal currentPrice) {
        List<ViolationRule> rules = violationRulePort.findByWalletId(walletId);
        if (rules.isEmpty()) {
            return List.of();
        }

        ViolationCheckContext context = buildContext(
            order, walletId, exchangeCoinId, coinId, currentPrice);

        return ViolationChecker.check(rules, context);
    }

    private ViolationCheckContext buildContext(Order order, Long walletId,
                                               Long exchangeCoinId, Long coinId,
                                               BigDecimal currentPrice) {
        Holding holding = holdingPersistencePort
            .findByWalletIdAndCoinId(walletId, coinId)
            .orElse(null);

        BigDecimal changeRate = resolveChangeRate(order.getSide(), exchangeCoinId);
        long todayOrderCount = countTodayOrders(walletId);

        return new ViolationCheckContext(
            order.getSide(), changeRate, holding, currentPrice, todayOrderCount,
            LocalDateTime.now(clock));
    }

    private BigDecimal resolveChangeRate(Side side, Long exchangeCoinId) {
        if (side == Side.BUY) {
            return priceChangeRatePort.getChangeRate(exchangeCoinId);
        }
        return BigDecimal.ZERO;
    }

    private long countTodayOrders(Long walletId) {
        LocalDate today = LocalDate.now(clock);
        return orderPersistencePort.countByWalletIdAndCreatedAtBetween(
            walletId, today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }
}
