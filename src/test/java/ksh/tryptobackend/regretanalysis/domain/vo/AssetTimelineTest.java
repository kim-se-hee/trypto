package ksh.tryptobackend.regretanalysis.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetTimelineTest {

    private static final LocalDate DAY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2025, 1, 3);

    private AssetSnapshot snapshotOn(LocalDate date, BigDecimal totalAsset) {
        return AssetSnapshot.reconstitute(
            1L, 1L, 1L,
            totalAsset, BigDecimal.ZERO, BigDecimal.ZERO,
            date
        );
    }

    @Nested
    @DisplayName("타임라인 생성")
    class OfTest {

        @Test
        @DisplayName("빈 스냅샷 목록으로 생성하면 SNAPSHOT_NOT_FOUND 예외가 발생한다")
        void of_emptySnapshots_throwsException() {
            // Given
            List<AssetSnapshot> emptySnapshots = List.of();

            // When & Then
            assertThatThrownBy(() -> AssetTimeline.of(emptySnapshots))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SNAPSHOT_NOT_FOUND);
        }

        @Test
        @DisplayName("스냅샷이 존재하면 타임라인을 생성한다")
        void of_validSnapshots_createsTimeline() {
            // Given
            List<AssetSnapshot> snapshots = List.of(snapshotOn(DAY_1, new BigDecimal("1000000")));

            // When
            AssetTimeline timeline = AssetTimeline.of(snapshots);

            // Then
            assertThat(timeline.getSnapshots()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("총 일수 계산")
    class CalculateTotalDaysTest {

        @Test
        @DisplayName("시작일과 종료일이 같으면 1일이다")
        void calculateTotalDays_sameDay_returnsOne() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(
                snapshotOn(DAY_1, new BigDecimal("1000000"))
            ));

            // When
            int totalDays = timeline.calculateTotalDays();

            // Then
            assertThat(totalDays).isEqualTo(1);
        }

        @Test
        @DisplayName("3일간의 스냅샷이면 3을 반환한다")
        void calculateTotalDays_threeDays_returnsThree() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(
                snapshotOn(DAY_1, new BigDecimal("1000000")),
                snapshotOn(DAY_2, new BigDecimal("1100000")),
                snapshotOn(DAY_3, new BigDecimal("1050000"))
            ));

            // When
            int totalDays = timeline.calculateTotalDays();

            // Then
            assertThat(totalDays).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("특정 날짜 자산 조회")
    class FindAssetAtTest {

        @Test
        @DisplayName("존재하는 날짜를 조회하면 자산값을 반환한다")
        void findAssetAt_existingDate_returnsAsset() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(
                snapshotOn(DAY_1, new BigDecimal("1000000")),
                snapshotOn(DAY_2, new BigDecimal("1100000"))
            ));

            // When
            Optional<BigDecimal> asset = timeline.findAssetAt(DAY_2);

            // Then
            assertThat(asset).isPresent();
            assertThat(asset.get()).isEqualByComparingTo(new BigDecimal("1100000"));
        }

        @Test
        @DisplayName("존재하지 않는 날짜를 조회하면 빈 Optional을 반환한다")
        void findAssetAt_nonExistentDate_returnsEmpty() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(
                snapshotOn(DAY_1, new BigDecimal("1000000"))
            ));

            // When
            Optional<BigDecimal> asset = timeline.findAssetAt(DAY_3);

            // Then
            assertThat(asset).isEmpty();
        }
    }

    @Nested
    @DisplayName("시드머니 조회")
    class GetSeedMoneyTest {

        @Test
        @DisplayName("첫 번째 스냅샷의 총 자산이 시드머니다")
        void getSeedMoney_returnsFirstSnapshotAsset() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(
                snapshotOn(DAY_1, new BigDecimal("1000000")),
                snapshotOn(DAY_2, new BigDecimal("1200000"))
            ));

            // When
            BigDecimal seedMoney = timeline.getSeedMoney();

            // Then
            assertThat(seedMoney).isEqualByComparingTo(new BigDecimal("1000000"));
        }
    }
}
