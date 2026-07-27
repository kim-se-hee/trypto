package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CumulativeLossTimeline {

    public record DailyLoss(LocalDate date, BigDecimal cumulativeLoss) {}

    private final List<DailyLoss> entries;
    private final Map<LocalDate, DailyLoss> entryByDate;

    private CumulativeLossTimeline(List<DailyLoss> entries) {
        this.entries = entries;
        this.entryByDate = entries.stream().collect(Collectors.toMap(DailyLoss::date, Function.identity()));
    }

    public static CumulativeLossTimeline build(List<ViolationLoss> losses, List<LocalDate> dates) {
        List<ViolationLoss> sortedLosses = losses.stream()
                .sorted(Comparator.comparing(ViolationLoss::occurredDate))
                .toList();

        List<DailyLoss> result = new ArrayList<>();
        BigDecimal cumulativeLoss = BigDecimal.ZERO;
        int lossIndex = 0;

        for (LocalDate date : dates) {
            while (lossIndex < sortedLosses.size()
                    && !sortedLosses.get(lossIndex).occurredDate().isAfter(date)) {
                cumulativeLoss = cumulativeLoss.add(sortedLosses.get(lossIndex).amountKrw());
                lossIndex++;
            }
            result.add(new DailyLoss(date, cumulativeLoss));
        }
        return new CumulativeLossTimeline(result);
    }

    public BigDecimal getLossAt(LocalDate date) {
        DailyLoss entry = entryByDate.get(date);
        return entry != null ? entry.cumulativeLoss() : BigDecimal.ZERO;
    }

    public BigDecimal calculateRuleFollowedAsset(BigDecimal actualAsset, LocalDate date) {
        return actualAsset.add(getLossAt(date));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CumulativeLossTimeline that)) return false;
        if (entries.size() != that.entries.size()) return false;
        for (int i = 0; i < entries.size(); i++) {
            DailyLoss a = entries.get(i);
            DailyLoss b = that.entries.get(i);
            if (!a.date().equals(b.date()) || a.cumulativeLoss().compareTo(b.cumulativeLoss()) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(entries.size());
        for (DailyLoss entry : entries) {
            result = 31 * result
                    + Objects.hash(entry.date(), entry.cumulativeLoss().stripTrailingZeros());
        }
        return result;
    }
}
