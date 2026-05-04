package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;

public final class ViolationMarkers {

    private final List<ViolationMarker> markers;

    private ViolationMarkers(List<ViolationMarker> markers) {
        this.markers = markers;
    }

    public static ViolationMarkers from(List<ViolationDetail> violations, AssetTimeline timeline) {
        Set<LocalDate> violationDates =
                violations.stream()
                        .map(ViolationDetail::getOccurredDate)
                        .collect(Collectors.toSet());

        List<ViolationMarker> markers =
                violationDates.stream()
                        .sorted()
                        .flatMap(
                                date ->
                                        timeline
                                                .findAssetAt(date)
                                                .map(asset -> new ViolationMarker(date, asset))
                                                .stream())
                        .toList();

        return new ViolationMarkers(markers);
    }

    public List<ViolationMarker> getMarkers() {
        return markers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViolationMarkers that)) return false;
        return markers.equals(that.markers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(markers);
    }

    public record ViolationMarker(LocalDate date, BigDecimal assetValue) {}
}
