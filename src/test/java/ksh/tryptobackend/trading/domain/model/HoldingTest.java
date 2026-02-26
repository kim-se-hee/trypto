package ksh.tryptobackend.trading.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HoldingTest {

    @Nested
    @DisplayName("매수 체결 반영")
    class ApplyBuyTest {

        @Test
        @DisplayName("첫 매수 — 평균 매수가 = 체결가")
        void applyBuy_firstBuy_avgPriceEqualsFilledPrice() {
            Holding holding = Holding.empty(1L, 1L);

            holding.applyBuy(new BigDecimal("50000000"), new BigDecimal("0.01"), new BigDecimal("50000000"));

            assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("50000000"));
            assertThat(holding.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(holding.getTotalBuyAmount()).isEqualByComparingTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("추가 매수 — 가중 평균 매수가 계산")
        void applyBuy_additionalBuy_weightedAveragePrice() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(0)
                .build();

            holding.applyBuy(new BigDecimal("40000000"), new BigDecimal("0.01"), new BigDecimal("40000000"));

            // (500000 + 400000) / (0.01 + 0.01) = 900000 / 0.02 = 45000000
            assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("45000000"));
            assertThat(holding.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("0.02"));
        }

        @Test
        @DisplayName("손실 중 추가 매수 — 물타기 카운트 증가")
        void applyBuy_atLoss_averagingDownCountIncreases() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(0)
                .build();

            // 현재가 40000000 < 평균 매수가 50000000 → 손실 중
            holding.applyBuy(new BigDecimal("40000000"), new BigDecimal("0.01"), new BigDecimal("40000000"));

            assertThat(holding.getAveragingDownCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("이익 중 추가 매수 — 물타기 카운트 미증가")
        void applyBuy_atProfit_averagingDownCountUnchanged() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(0)
                .build();

            // 현재가 60000000 > 평균 매수가 50000000 → 이익 중
            holding.applyBuy(new BigDecimal("60000000"), new BigDecimal("0.01"), new BigDecimal("60000000"));

            assertThat(holding.getAveragingDownCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("매도 체결 반영")
    class ApplySellTest {

        @Test
        @DisplayName("부분 매도 — 수량만 감소, 평균 매수가 유지")
        void applySell_partial_quantityDecreasesAvgPriceUnchanged() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.02"))
                .totalBuyAmount(new BigDecimal("1000000"))
                .averagingDownCount(1)
                .build();

            holding.applySell(new BigDecimal("0.01"));

            assertThat(holding.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("50000000"));
        }

        @Test
        @DisplayName("전량 매도 — 모든 값 리셋")
        void applySell_full_allValuesReset() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(2)
                .build();

            holding.applySell(new BigDecimal("0.01"));

            assertThat(holding.getTotalQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(holding.getTotalBuyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("상태 판별")
    class StateCheckTest {

        @Test
        @DisplayName("보유 중 — 수량 > 0")
        void isHolding_positiveQuantity_true() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(0)
                .build();

            assertThat(holding.isHolding()).isTrue();
        }

        @Test
        @DisplayName("미보유 — 수량 = 0")
        void isHolding_zeroQuantity_false() {
            Holding holding = Holding.empty(1L, 1L);

            assertThat(holding.isHolding()).isFalse();
        }

        @Test
        @DisplayName("손실 중 — 평균 매수가 > 현재가")
        void isAtLoss_avgPriceHigherThanCurrent_true() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(0)
                .build();

            assertThat(holding.isAtLoss(new BigDecimal("40000000"))).isTrue();
        }

        @Test
        @DisplayName("이익 중 — 평균 매수가 < 현재가")
        void isAtLoss_avgPriceLowerThanCurrent_false() {
            Holding holding = Holding.builder()
                .walletId(1L).coinId(1L)
                .avgBuyPrice(new BigDecimal("50000000"))
                .totalQuantity(new BigDecimal("0.01"))
                .totalBuyAmount(new BigDecimal("500000"))
                .averagingDownCount(0)
                .build();

            assertThat(holding.isAtLoss(new BigDecimal("60000000"))).isFalse();
        }
    }
}
