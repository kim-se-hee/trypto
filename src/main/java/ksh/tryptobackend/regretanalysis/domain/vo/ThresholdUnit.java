package ksh.tryptobackend.regretanalysis.domain.vo;

import ksh.tryptobackend.common.domain.vo.RuleType;

public enum ThresholdUnit {

    PERCENT("%"),
    COUNT("회");

    private final String symbol;

    ThresholdUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public static ThresholdUnit from(RuleType ruleType) {
        return switch (ruleType) {
            case LOSS_CUT, PROFIT_TAKE, CHASE_BUY_BAN -> PERCENT;
            case AVERAGING_DOWN_LIMIT, OVERTRADING_LIMIT -> COUNT;
        };
    }
}
