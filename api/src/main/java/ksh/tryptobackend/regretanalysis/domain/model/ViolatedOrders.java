package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptobackend.regretanalysis.domain.vo.CurrentPrices;

public class ViolatedOrders {

    private final List<ViolatedOrder> values;

    public ViolatedOrders(List<ViolatedOrder> values) {
        this.values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Set<Long> exchangeCoinIds() {
        return values.stream().map(ViolatedOrder::getExchangeCoinId).collect(Collectors.toSet());
    }

    public List<ViolationDetail> calculateDetails(CurrentPrices currentPrices) {
        return values.stream().map(v -> toViolationDetail(v, currentPrices)).toList();
    }

    /**
     * 위반 손익(`lossAmount`)은 양수가 손해지만, 거래 손익(`profitLoss`)은 음수가 손실이다. 부호 규약이 서로 반대이므로 뒤집어 담는다.
     */
    private ViolationDetail toViolationDetail(ViolatedOrder violation, CurrentPrices currentPrices) {
        BigDecimal lossAmount = violation.calculateLoss(currentPrices.getPrice(violation.getExchangeCoinId()));
        return ViolationDetail.create(
                violation.getOrderId(),
                violation.getRuleId(),
                violation.getExchangeCoinId(),
                lossAmount,
                lossAmount.negate(),
                violation.getViolatedAt());
    }
}
