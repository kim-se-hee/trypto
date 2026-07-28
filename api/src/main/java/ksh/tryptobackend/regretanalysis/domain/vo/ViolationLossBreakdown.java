package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 위반 손익을 실현분과 미실현분으로 나눈 결과. 미실현분은 배치마다 현재가로 다시 매겨져 시장을 따라 움직이지만, 실현분은 매도 시점 금액으로 굳는다. 두 몫은 원칙 준수 시
 * 자산에 반영하는 방법이 달라 따로 들고 다닌다.
 */
public record ViolationLossBreakdown(BigDecimal unrealizedAmount, List<RealizedLoss> realizedLosses) {

    public ViolationLossBreakdown {
        realizedLosses = List.copyOf(realizedLosses);
    }

    public static ViolationLossBreakdown unrealized(BigDecimal amount) {
        return new ViolationLossBreakdown(amount, List.of());
    }

    public BigDecimal totalAmount() {
        return realizedLosses.stream().map(RealizedLoss::amount).reduce(unrealizedAmount, BigDecimal::add);
    }
}
