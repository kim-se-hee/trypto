package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ViolationMarkers {

    private final List<ViolationMarker> markers;

    private ViolationMarkers(List<ViolationMarker> markers) {
        this.markers = markers;
    }

    /** 거래소가 다르고 어긴 원칙이 달라도 같은 날이면 마커 하나다. 마커는 그날 원칙을 어겼다는 사실만 알리기 때문이다. */
    public static ViolationMarkers from(List<ViolationLoss> losses, AssetTimeline timeline) {
        Set<LocalDate> violationDates =
                losses.stream().map(ViolationLoss::occurredDate).collect(Collectors.toSet());

        List<ViolationMarker> markers = violationDates.stream()
                .sorted()
                .flatMap(date -> timeline.findAssetAt(date).map(asset -> new ViolationMarker(date, asset)).stream())
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
