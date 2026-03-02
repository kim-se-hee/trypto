package ksh.tryptobackend.regretanalysis.domain.vo;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.TradeRecord;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.TradeSide;

import java.math.BigDecimal;
import java.util.List;

public enum ViolationLossType {

    BUY_LOSS {
        @Override
        public BigDecimal calculateLoss(TradeRecord trade,
                                        List<TradeRecord> sellOrdersAfter,
                                        BigDecimal currentPrice) {
            BigDecimal remainingQty = trade.quantity();
            BigDecimal realizedLoss = BigDecimal.ZERO;

            for (TradeRecord sell : sellOrdersAfter) {
                if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal matchQty = sell.quantity().min(remainingQty);
                realizedLoss = realizedLoss.add(
                    trade.filledPrice().subtract(sell.filledPrice()).multiply(matchQty));
                remainingQty = remainingQty.subtract(matchQty);
            }

            if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                realizedLoss = realizedLoss.add(
                    trade.filledPrice().subtract(currentPrice).multiply(remainingQty));
            }

            return realizedLoss;
        }
    },

    SELL_LOSS {
        @Override
        public BigDecimal calculateLoss(TradeRecord trade,
                                        List<TradeRecord> sellOrdersAfter,
                                        BigDecimal currentPrice) {
            return currentPrice.subtract(trade.filledPrice()).multiply(trade.quantity());
        }
    },

    NONE {
        @Override
        public BigDecimal calculateLoss(TradeRecord trade,
                                        List<TradeRecord> sellOrdersAfter,
                                        BigDecimal currentPrice) {
            return BigDecimal.ZERO;
        }
    };

    public abstract BigDecimal calculateLoss(TradeRecord trade,
                                             List<TradeRecord> sellOrdersAfter,
                                             BigDecimal currentPrice);

    public static ViolationLossType from(RuleType ruleType, TradeSide side) {
        return switch (ruleType) {
            case CHASE_BUY_BAN, AVERAGING_DOWN_LIMIT -> BUY_LOSS;
            case OVERTRADING_LIMIT -> side == TradeSide.BUY ? BUY_LOSS : SELL_LOSS;
            case LOSS_CUT, PROFIT_TAKE -> NONE;
        };
    }
}
