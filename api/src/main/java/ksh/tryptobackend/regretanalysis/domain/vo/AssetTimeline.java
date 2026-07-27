package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AssetTimeline {

    private final List<DailyAsset> dailyAssets;

    private AssetTimeline(List<DailyAsset> dailyAssets) {
        this.dailyAssets = List.copyOf(dailyAssets);
    }

    public static AssetTimeline of(List<DailyAsset> dailyAssets) {
        return new AssetTimeline(dailyAssets);
    }

    public boolean isEmpty() {
        return dailyAssets.isEmpty();
    }

    public List<LocalDate> getDates() {
        return dailyAssets.stream().map(DailyAsset::date).toList();
    }

    public Optional<BigDecimal> findAssetAt(LocalDate date) {
        return dailyAssets.stream()
                .filter(dailyAsset -> dailyAsset.date().equals(date))
                .findFirst()
                .map(DailyAsset::amount);
    }

    public LocalDate getStartDate() {
        return dailyAssets.getFirst().date();
    }

    public LocalDate getEndDate() {
        return dailyAssets.getLast().date();
    }

    public int calculateTotalDays() {
        if (isEmpty()) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(getStartDate(), getEndDate()) + 1;
    }

    public List<DailyAsset> getDailyAssets() {
        return dailyAssets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetTimeline that)) return false;
        return Objects.equals(dailyAssets, that.dailyAssets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dailyAssets);
    }
}
