package ksh.tryptobackend.regretanalysis.domain.strategy;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;

import java.math.BigDecimal;

public enum ViolationLossStrategy {

    BUY {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return switch (ruleType) {
                case CHASE_BUY_BAN, AVERAGING_DOWN_LIMIT -> true;
                case OVERTRADING_LIMIT -> isBuy;
                default -> false;
            };
        }

        @Override
        public BigDecimal calculateLoss(ViolationLossContext context) {
            BigDecimal remainingQty = context.quantity();
            BigDecimal totalLoss = BigDecimal.ZERO;

            for (SoldPortion sell : context.soldPortions()) {
                if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal matchedQty = sell.quantity().min(remainingQty);
                totalLoss = totalLoss.add(
                    context.filledPrice().subtract(sell.price()).multiply(matchedQty));
                remainingQty = remainingQty.subtract(matchedQty);
            }

            if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                totalLoss = totalLoss.add(
                    context.filledPrice().subtract(context.currentPrice()).multiply(remainingQty));
            }

            return totalLoss;
        }
    },

    SELL {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return ruleType == RuleType.OVERTRADING_LIMIT && !isBuy;
        }

        @Override
        public BigDecimal calculateLoss(ViolationLossContext context) {
            return context.currentPrice().subtract(context.filledPrice()).multiply(context.quantity());
        }
    },

    LOSS_CUT {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return ruleType == RuleType.LOSS_CUT;
        }

        @Override
        public BigDecimal calculateLoss(ViolationLossContext context) {
            return context.currentPrice().multiply(context.quantity()).subtract(context.tradeAmount());
        }
    },

    PROFIT_TAKE {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return ruleType == RuleType.PROFIT_TAKE;
        }

        @Override
        public BigDecimal calculateLoss(ViolationLossContext context) {
            return context.tradeAmount().subtract(context.currentPrice().multiply(context.quantity()));
        }
    };

    public abstract boolean supports(RuleType ruleType, boolean isBuy);

    public abstract BigDecimal calculateLoss(ViolationLossContext context);

    public static ViolationLossStrategy resolve(RuleType ruleType, boolean isBuy) {
        for (ViolationLossStrategy strategy : values()) {
            if (strategy.supports(ruleType, isBuy)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("No strategy for " + ruleType + " (isBuy=" + isBuy + ")");
    }
}
