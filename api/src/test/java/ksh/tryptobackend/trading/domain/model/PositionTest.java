package ksh.tryptobackend.trading.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.vo.ExecutedFill;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PositionTest {

    private static ExecutedFill buy(String price, String qty) {
        return new ExecutedFill(Side.BUY, Price.of(new BigDecimal(price)), Quantity.of(new BigDecimal(qty)));
    }

    private static ExecutedFill sell(String qty) {
        return new ExecutedFill(Side.SELL, Price.zero(), Quantity.of(new BigDecimal(qty)));
    }

    private static Price price(String value) {
        return Price.of(new BigDecimal(value));
    }

    private static Position holdingOf(String avg, String qty) {
        Position position = Position.empty(1L, 1L);
        position.applyFill(buy(avg, qty), price(avg));
        return position;
    }

    @Nested
    @DisplayName("매수 체결 반영")
    class ApplyBuyTest {

        @Test
        @DisplayName("첫 매수 — 평균 매수가 = 체결가")
        void firstBuy() {
            Position position = Position.empty(1L, 1L);

            position.applyFill(buy("50000000", "0.01"), price("50000000"));

            assertThat(position.getHolding().avgBuyPrice().value()).isEqualByComparingTo(new BigDecimal("50000000"));
            assertThat(position.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(position.getHolding().totalBuyAmount().value()).isEqualByComparingTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("추가 매수 — 가중 평균 매수가 계산")
        void additionalBuy() {
            Position position = holdingOf("50000000", "0.01");

            position.applyFill(buy("40000000", "0.01"), price("40000000"));

            assertThat(position.getHolding().avgBuyPrice().value()).isEqualByComparingTo(new BigDecimal("45000000"));
            assertThat(position.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("0.02"));
        }

        @Test
        @DisplayName("손실 중 추가 매수 — 물타기 카운트 증가")
        void atLossIncrementsAveragingDown() {
            Position position = holdingOf("50000000", "0.01");

            position.applyFill(buy("40000000", "0.01"), price("40000000"));

            assertThat(position.getAveragingDownCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("이익 중 추가 매수 — 물타기 카운트 미증가")
        void atProfitKeepsAveragingDown() {
            Position position = holdingOf("50000000", "0.01");

            position.applyFill(buy("60000000", "0.01"), price("60000000"));

            assertThat(position.getAveragingDownCount()).isZero();
        }
    }

    @Nested
    @DisplayName("매도 체결 반영")
    class ApplySellTest {

        @Test
        @DisplayName("부분 매도 — 수량·매수금액 감소, 평균 매수가 유지")
        void partialSell() {
            Position position = holdingOf("50000000", "0.02");

            position.applyFill(sell("0.01"), price("50000000"));

            assertThat(position.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(position.getHolding().avgBuyPrice().value()).isEqualByComparingTo(new BigDecimal("50000000"));
            assertThat(position.getHolding().totalBuyAmount().value()).isEqualByComparingTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("전량 매도 — 모든 값 리셋")
        void fullSell() {
            Position position = holdingOf("50000000", "0.01");

            position.applyFill(sell("0.01"), price("50000000"));

            assertThat(position.getHolding().totalQuantity().value()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(position.getHolding().avgBuyPrice().value()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(position.getHolding().totalBuyAmount().value()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("부분 매도 후 재매수 — 평균 매수가가 부풀려지지 않는다")
        void partialSellThenBuyKeepsAverageCorrect() {
            Position position = holdingOf("100", "10");

            position.applyFill(sell("5"), price("100"));
            position.applyFill(buy("80", "5"), price("80"));

            assertThat(position.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(position.getHolding().avgBuyPrice().value()).isEqualByComparingTo(new BigDecimal("90"));
        }
    }

    @Nested
    @DisplayName("상태 판별")
    class StateCheckTest {

        @Test
        @DisplayName("보유 중 — 수량 > 0")
        void isHoldingTrue() {
            assertThat(holdingOf("50000000", "0.01").isHolding()).isTrue();
        }

        @Test
        @DisplayName("미보유 — 수량 = 0")
        void isHoldingFalse() {
            assertThat(Position.empty(1L, 1L).isHolding()).isFalse();
        }

        @Test
        @DisplayName("손실 중 — 평균 매수가 > 현재가")
        void isAtLossTrue() {
            assertThat(holdingOf("50000000", "0.01").isAtLoss(price("40000000")))
                    .isTrue();
        }

        @Test
        @DisplayName("이익 중 — 평균 매수가 < 현재가")
        void isAtLossFalse() {
            assertThat(holdingOf("50000000", "0.01").isAtLoss(price("60000000")))
                    .isFalse();
        }
    }
}
