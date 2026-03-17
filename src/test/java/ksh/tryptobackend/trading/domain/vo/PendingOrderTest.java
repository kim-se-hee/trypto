package ksh.tryptobackend.trading.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PendingOrderTest {

    @Nested
    @DisplayName("지정가 매수 매칭")
    class BuyMatchingTest {

        @Test
        @DisplayName("현재가 < 지정가 - 매칭 성공")
        void matches_currentPriceBelowLimitPrice_matched() {
            // Given
            PendingOrder order = createBuyOrder(new BigDecimal("50000"));

            // When
            boolean result = order.matches(new BigDecimal("49999"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("현재가 == 지정가 - 매칭 성공 (경계값)")
        void matches_currentPriceEqualsLimitPrice_matched() {
            // Given
            PendingOrder order = createBuyOrder(new BigDecimal("50000"));

            // When
            boolean result = order.matches(new BigDecimal("50000"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("현재가 > 지정가 - 매칭 실패")
        void matches_currentPriceAboveLimitPrice_notMatched() {
            // Given
            PendingOrder order = createBuyOrder(new BigDecimal("50000"));

            // When
            boolean result = order.matches(new BigDecimal("50001"));

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("현재가가 지정가보다 1원 낮음 - 매칭 성공 (경계값)")
        void matches_currentPriceOneWonBelow_matched() {
            // Given
            PendingOrder order = createBuyOrder(new BigDecimal("100000000"));

            // When
            boolean result = order.matches(new BigDecimal("99999999"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("현재가가 지정가보다 1원 높음 - 매칭 실패 (경계값)")
        void matches_currentPriceOneWonAbove_notMatched() {
            // Given
            PendingOrder order = createBuyOrder(new BigDecimal("100000000"));

            // When
            boolean result = order.matches(new BigDecimal("100000001"));

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("지정가 매도 매칭")
    class SellMatchingTest {

        @Test
        @DisplayName("현재가 > 지정가 - 매칭 성공")
        void matches_currentPriceAboveLimitPrice_matched() {
            // Given
            PendingOrder order = createSellOrder(new BigDecimal("50000"));

            // When
            boolean result = order.matches(new BigDecimal("50001"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("현재가 == 지정가 - 매칭 성공 (경계값)")
        void matches_currentPriceEqualsLimitPrice_matched() {
            // Given
            PendingOrder order = createSellOrder(new BigDecimal("50000"));

            // When
            boolean result = order.matches(new BigDecimal("50000"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("현재가 < 지정가 - 매칭 실패")
        void matches_currentPriceBelowLimitPrice_notMatched() {
            // Given
            PendingOrder order = createSellOrder(new BigDecimal("50000"));

            // When
            boolean result = order.matches(new BigDecimal("49999"));

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("현재가가 지정가보다 1원 높음 - 매칭 성공 (경계값)")
        void matches_currentPriceOneWonAbove_matched() {
            // Given
            PendingOrder order = createSellOrder(new BigDecimal("100000000"));

            // When
            boolean result = order.matches(new BigDecimal("100000001"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("현재가가 지정가보다 1원 낮음 - 매칭 실패 (경계값)")
        void matches_currentPriceOneWonBelow_notMatched() {
            // Given
            PendingOrder order = createSellOrder(new BigDecimal("100000000"));

            // When
            boolean result = order.matches(new BigDecimal("99999999"));

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("소수점 가격 매칭")
    class DecimalPriceMatchingTest {

        @Test
        @DisplayName("매수 - 소수점 가격이 정확히 같으면 매칭 성공")
        void matchesBuy_decimalPriceExact_matched() {
            // Given
            PendingOrder order = createBuyOrder(new BigDecimal("0.00012345"));

            // When
            boolean result = order.matches(new BigDecimal("0.00012345"));

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("매도 - 소수점 가격이 정확히 같으면 매칭 성공")
        void matchesSell_decimalPriceExact_matched() {
            // Given
            PendingOrder order = createSellOrder(new BigDecimal("0.00012345"));

            // When
            boolean result = order.matches(new BigDecimal("0.00012345"));

            // Then
            assertThat(result).isTrue();
        }
    }

    private PendingOrder createBuyOrder(BigDecimal price) {
        return new PendingOrder(1L, 100L, Side.BUY, price);
    }

    private PendingOrder createSellOrder(BigDecimal price) {
        return new PendingOrder(1L, 100L, Side.SELL, price);
    }
}
