package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 라운드에 투입된 자금의 내역. 시드머니와 긴급 충전만 자금 투입이며, 거래소 간 송금은 라운드 안에서의 이동이라 포함하지 않는다. 그래프가 시작하는 날보다 앞서 들어온 돈은 첫날에
 * 들어온 것으로 몰아 잡는다.
 */
public final class CapitalInflows {

    private final List<CapitalInflow> inflows;

    private CapitalInflows(List<CapitalInflow> inflows) {
        this.inflows = List.copyOf(inflows);
    }

    public static CapitalInflows of(BigDecimal seedMoney, List<EmergencyCharge> charges, LocalDate firstDate) {
        BigDecimal amountOnFirstDate = seedMoney.add(sumChargesUntil(charges, firstDate));

        List<CapitalInflow> inflows = new ArrayList<>();
        inflows.add(new CapitalInflow(firstDate, amountOnFirstDate));
        charges.stream()
                .filter(charge -> charge.chargedDate().isAfter(firstDate))
                .sorted(Comparator.comparing(EmergencyCharge::chargedDate))
                .forEach(charge -> inflows.add(new CapitalInflow(charge.chargedDate(), charge.amount())));

        return new CapitalInflows(inflows);
    }

    private static BigDecimal sumChargesUntil(List<EmergencyCharge> charges, LocalDate firstDate) {
        return charges.stream()
                .filter(charge -> !charge.chargedDate().isAfter(firstDate))
                .map(EmergencyCharge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<CapitalInflow> values() {
        return inflows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapitalInflows that)) return false;
        return Objects.equals(inflows, that.inflows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inflows);
    }

    public record CapitalInflow(LocalDate date, BigDecimal amount) {}
}
