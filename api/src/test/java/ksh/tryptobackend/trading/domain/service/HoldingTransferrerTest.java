package ksh.tryptobackend.trading.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.ExecutedFill;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HoldingTransferrerTest {

    private final HoldingTransferrer holdingTransferrer = new HoldingTransferrer();

    private static Position holdingOf(String avg, String qty) {
        Position position = Position.empty(1L, 1L);
        position.applyFill(new ExecutedFill(Side.BUY, price(avg), Quantity.of(new BigDecimal(qty))), price(avg));
        return position;
    }

    private static Price price(String value) {
        return Price.of(new BigDecimal(value));
    }

    @Test
    @DisplayName("보유 이동 — 출발은 평단 유지 차감, 도착은 취득가로 받는다")
    void transfer() {
        Position source = holdingOf("1000", "10");
        Position destination = Position.empty(2L, 1L);

        holdingTransferrer.transfer(source, destination, Quantity.of(new BigDecimal("4")), price("2000"));

        assertThat(source.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("6"));
        assertThat(source.getHolding().avgBuyPrice().value()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(destination.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(destination.getHolding().avgBuyPrice().value()).isEqualByComparingTo(new BigDecimal("2000"));
    }

    @Test
    @DisplayName("빈 출발 포지션 — 도착 포지션은 아무것도 받지 않는다")
    void emptySourceMovesNothing() {
        Position source = Position.empty(1L, 1L);
        Position destination = Position.empty(2L, 1L);

        holdingTransferrer.transfer(source, destination, Quantity.of(new BigDecimal("5")), price("2000"));

        assertThat(destination.isHolding()).isFalse();
    }

    @Test
    @DisplayName("보유량 초과 요청 — 도착 포지션은 실제 보유량만큼만 받는다")
    void overRequestMovesOnlyHeldQuantity() {
        Position source = holdingOf("1000", "10");
        Position destination = Position.empty(2L, 1L);

        holdingTransferrer.transfer(source, destination, Quantity.of(new BigDecimal("15")), price("2000"));

        assertThat(source.isHolding()).isFalse();
        assertThat(destination.getHolding().totalQuantity().value()).isEqualByComparingTo(new BigDecimal("10"));
    }
}
