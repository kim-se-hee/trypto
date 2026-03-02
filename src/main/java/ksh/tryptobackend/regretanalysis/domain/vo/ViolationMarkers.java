package ksh.tryptobackend.regretanalysis.domain.vo;

import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ViolationMarkers {

    private final List<Marker> markers;

    private ViolationMarkers(List<Marker> markers) {
        this.markers = markers;
    }

    public static ViolationMarkers from(List<ViolationDetail> violations,
                                         AssetTimeline timeline) {
        Set<LocalDate> violationDates = violations.stream()
            .map(v -> v.getOccurredAt().toLocalDate())
            .collect(Collectors.toSet());

        List<Marker> markers = violationDates.stream()
            .sorted()
            .flatMap(date -> timeline.findAssetAt(date)
                .map(asset -> new Marker(date, asset))
                .stream())
            .toList();

        return new ViolationMarkers(markers);
    }

    public List<Marker> getMarkers() {
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

    public record Marker(LocalDate date, BigDecimal assetValue) {
    }
}
