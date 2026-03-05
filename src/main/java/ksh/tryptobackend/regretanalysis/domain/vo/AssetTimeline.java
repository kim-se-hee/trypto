package ksh.tryptobackend.regretanalysis.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AssetTimeline {

    private final List<AssetSnapshot> snapshots;

    private AssetTimeline(List<AssetSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public static AssetTimeline of(List<AssetSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            throw new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND);
        }
        return new AssetTimeline(snapshots);
    }

    public List<LocalDate> getDates() {
        return snapshots.stream()
            .map(AssetSnapshot::getSnapshotDate)
            .toList();
    }

    public Optional<BigDecimal> findAssetAt(LocalDate date) {
        return snapshots.stream()
            .filter(s -> s.getSnapshotDate().equals(date))
            .findFirst()
            .map(AssetSnapshot::getTotalAsset);
    }

    public BigDecimal getSeedMoney() {
        return snapshots.getFirst().getTotalAsset();
    }

    public LocalDate getStartDate() {
        return snapshots.getFirst().getSnapshotDate();
    }

    public LocalDate getEndDate() {
        return snapshots.getLast().getSnapshotDate();
    }

    public int calculateTotalDays() {
        return (int) ChronoUnit.DAYS.between(getStartDate(), getEndDate()) + 1;
    }

    public List<AssetSnapshot> getSnapshots() {
        return snapshots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetTimeline that)) return false;
        return Objects.equals(snapshots, that.snapshots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshots);
    }
}
