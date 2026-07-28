package ksh.tryptobackend.regretanalysis.domain.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.vo.RealizedLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossBreakdown;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;

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

        /** 매도와 짝지어진 수량은 그 매도 체결일에 금액이 확정된 실현분이고, 짝이 없어 남은 수량은 조회 시점 현재가로 다시 매기는 미실현분이다. */
        @Override
        public ViolationLossBreakdown calculateLoss(ViolationLossContext context) {
            BigDecimal remainingQty = context.quantity();
            List<RealizedLoss> realizedLosses = new ArrayList<>();

            for (SoldPortion sell : context.soldPortions()) {
                if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal matchedQty = sell.quantity().min(remainingQty);
                realizedLosses.add(new RealizedLoss(
                        sell.soldAt().toLocalDate(),
                        context.filledPrice().subtract(sell.price()).multiply(matchedQty)));
                remainingQty = remainingQty.subtract(matchedQty);
            }

            return new ViolationLossBreakdown(unrealizedLoss(context, remainingQty), realizedLosses);
        }

        private BigDecimal unrealizedLoss(ViolationLossContext context, BigDecimal remainingQty) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return context.filledPrice().subtract(context.currentPrice()).multiply(remainingQty);
        }
    },

    SELL {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return ruleType == RuleType.OVERTRADING_LIMIT && !isBuy;
        }

        @Override
        public ViolationLossBreakdown calculateLoss(ViolationLossContext context) {
            return ViolationLossBreakdown.unrealized(
                    context.currentPrice().subtract(context.filledPrice()).multiply(context.quantity()));
        }
    },

    LOSS_CUT {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return ruleType == RuleType.LOSS_CUT;
        }

        @Override
        public ViolationLossBreakdown calculateLoss(ViolationLossContext context) {
            return ViolationLossBreakdown.unrealized(
                    context.currentPrice().multiply(context.quantity()).subtract(context.tradeAmount()));
        }
    },

    PROFIT_TAKE {
        @Override
        public boolean supports(RuleType ruleType, boolean isBuy) {
            return ruleType == RuleType.PROFIT_TAKE;
        }

        @Override
        public ViolationLossBreakdown calculateLoss(ViolationLossContext context) {
            return ViolationLossBreakdown.unrealized(
                    context.tradeAmount().subtract(context.currentPrice().multiply(context.quantity())));
        }
    };

    public abstract boolean supports(RuleType ruleType, boolean isBuy);

    public abstract ViolationLossBreakdown calculateLoss(ViolationLossContext context);

    public static ViolationLossStrategy resolve(RuleType ruleType, boolean isBuy) {
        for (ViolationLossStrategy strategy : values()) {
            if (strategy.supports(ruleType, isBuy)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("No strategy for " + ruleType + " (isBuy=" + isBuy + ")");
    }
}
