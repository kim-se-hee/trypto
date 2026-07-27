package ksh.tryptobackend.regretanalysis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssetTimelineTest {

    private static final LocalDate DAY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2025, 1, 3);

    private DailyAsset assetOn(LocalDate date, BigDecimal amount) {
        return new DailyAsset(date, amount);
    }

    @Nested
    @DisplayName("타임라인 생성")
    class OfTest {

        @Test
        @DisplayName("빈 목록으로 생성하면 빈 타임라인이 된다")
        void of_emptyDailyAssets_createsEmptyTimeline() {
            // Given
            List<DailyAsset> emptyDailyAssets = List.of();

            // When
            AssetTimeline timeline = AssetTimeline.of(emptyDailyAssets);

            // Then
            assertThat(timeline.isEmpty()).isTrue();
            assertThat(timeline.getDates()).isEmpty();
            assertThat(timeline.calculateTotalDays()).isZero();
        }

        @Test
        @DisplayName("일별 자산이 존재하면 타임라인을 생성한다")
        void of_validDailyAssets_createsTimeline() {
            // Given
            List<DailyAsset> dailyAssets = List.of(assetOn(DAY_1, new BigDecimal("1000000")));

            // When
            AssetTimeline timeline = AssetTimeline.of(dailyAssets);

            // Then
            assertThat(timeline.getDailyAssets()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("총 일수 계산")
    class CalculateTotalDaysTest {

        @Test
        @DisplayName("시작일과 종료일이 같으면 1일이다")
        void calculateTotalDays_sameDay_returnsOne() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(assetOn(DAY_1, new BigDecimal("1000000"))));

            // When
            int totalDays = timeline.calculateTotalDays();

            // Then
            assertThat(totalDays).isEqualTo(1);
        }

        @Test
        @DisplayName("3일간의 자산이면 3을 반환한다")
        void calculateTotalDays_threeDays_returnsThree() {
            // Given
            AssetTimeline timeline = AssetTimeline.of(List.of(
                    assetOn(DAY_1, new BigDecimal("1000000")),
                    assetOn(DAY_2, new BigDecimal("1100000")),
                    assetOn(DAY_3, new BigDecimal("1050000"))));

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
            AssetTimeline timeline = AssetTimeline.of(
                    List.of(assetOn(DAY_1, new BigDecimal("1000000")), assetOn(DAY_2, new BigDecimal("1100000"))));

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
            AssetTimeline timeline = AssetTimeline.of(List.of(assetOn(DAY_1, new BigDecimal("1000000"))));

            // When
            Optional<BigDecimal> asset = timeline.findAssetAt(DAY_3);

            // Then
            assertThat(asset).isEmpty();
        }
    }
}
