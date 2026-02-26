package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.out.*;
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

    private final WalletInfoPort walletInfoPort;
    private final InvestmentRulePort investmentRulePort;
    private final HoldingPersistencePort holdingPersistencePort;
    private final PriceChangeRatePort priceChangeRatePort;
    private final OrderPersistencePort orderPersistencePort;
    private final Clock clock;

    public List<RuleViolation> checkOrderViolations(Order order, Long walletId,
                                                    Long exchangeCoinId, Long coinId,
                                                    BigDecimal currentPrice) {
        Long roundId = walletInfoPort.getRoundIdByWalletId(walletId);
        List<InvestmentRule> rules = investmentRulePort.findByRoundId(roundId);
        if (rules.isEmpty()) {
            return List.of();
        }

        Holding holding = holdingPersistencePort.findByWalletIdAndCoinId(walletId, coinId)
            .orElse(null);

        BigDecimal changeRate = BigDecimal.ZERO;
        if (order.getSide() == Side.BUY) {
            changeRate = priceChangeRatePort.getChangeRate(exchangeCoinId);
        }

        LocalDate today = LocalDate.now(clock);
        long todayOrderCount = orderPersistencePort.countByWalletIdAndCreatedAtBetween(
            walletId, today.atStartOfDay(), today.atTime(LocalTime.MAX));

        ViolationCheckContext context = new ViolationCheckContext(
            order.getSide(), changeRate, holding, currentPrice, todayOrderCount,
            LocalDateTime.now(clock));

        return ViolationChecker.check(rules, context);
    }
}
