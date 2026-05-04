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
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;

public final class CumulativeLossTimeline {

    public record DailyLoss(LocalDate date, BigDecimal cumulativeLoss) {}

    private final List<DailyLoss> entries;
    private final Map<LocalDate, DailyLoss> entryByDate;

    private CumulativeLossTimeline(List<DailyLoss> entries) {
        this.entries = entries;
        this.entryByDate =
                entries.stream().collect(Collectors.toMap(DailyLoss::date, Function.identity()));
    }

    public static CumulativeLossTimeline build(
            List<ViolationDetail> violations, List<LocalDate> snapshotDates) {
        List<ViolationDetail> sortedViolations =
                violations.stream()
                        .sorted(Comparator.comparing(ViolationDetail::getOccurredDate))
                        .toList();

        List<DailyLoss> result = new ArrayList<>();
        BigDecimal cumulativeLoss = BigDecimal.ZERO;
        int violationIndex = 0;

        for (LocalDate snapshotDate : snapshotDates) {
            while (violationIndex < sortedViolations.size()
                    && !sortedViolations
                            .get(violationIndex)
                            .getOccurredDate()
                            .isAfter(snapshotDate)) {
                cumulativeLoss =
                        cumulativeLoss.add(sortedViolations.get(violationIndex).getLossAmount());
                violationIndex++;
            }
            result.add(new DailyLoss(snapshotDate, cumulativeLoss));
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
            if (!a.date().equals(b.date())
                    || a.cumulativeLoss().compareTo(b.cumulativeLoss()) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(entries.size());
        for (DailyLoss entry : entries) {
            result =
                    31 * result
                            + Objects.hash(
                                    entry.date(), entry.cumulativeLoss().stripTrailingZeros());
        }
        return result;
    }
}
