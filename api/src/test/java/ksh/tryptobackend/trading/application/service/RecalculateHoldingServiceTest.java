package ksh.tryptobackend.trading.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecalculateHoldingServiceTest {

    private static final Long WALLET_ID = 1L;
    private static final Long EXCHANGE_COIN_ID = 100L;
    private static final Long COIN_ID = 10L;

    @Mock private OrderQueryPort orderQueryPort;
    @Mock private MarketQueryPort marketQueryPort;
    @Mock private PositionCommandPort positionCommandPort;
    @InjectMocks private RecalculateHoldingService service;

    @Test
    @DisplayName("BUY 단건: avg=체결가, qty=수량, totalBuy=가*수량, averagingDownCount=0")
    void singleBuy() {
        givenFills(List.of(buy(bd("100"), bd("2"))));
        givenNoExistingPosition();

        service.recalculate(WALLET_ID, EXCHANGE_COIN_ID);

        Position saved = capture();
        assertThat(saved.getHolding().avgBuyPrice().value()).isEqualByComparingTo("100");
        assertThat(saved.getHolding().totalQuantity().value()).isEqualByComparingTo("2");
        assertThat(saved.getHolding().totalBuyAmount().value()).isEqualByComparingTo("200");
        assertThat(saved.getAveragingDownCount()).isZero();
    }

    @Test
    @DisplayName("BUY 후 더 낮은 가격으로 BUY: 평단 하락, averagingDownCount 1 증가")
    void averagingDown() {
        givenFills(List.of(buy(bd("100"), bd("2")), buy(bd("80"), bd("2"))));
        givenNoExistingPosition();

        service.recalculate(WALLET_ID, EXCHANGE_COIN_ID);

        Position saved = capture();
        assertThat(saved.getHolding().avgBuyPrice().value()).isEqualByComparingTo("90");
        assertThat(saved.getHolding().totalQuantity().value()).isEqualByComparingTo("4");
        assertThat(saved.getHolding().totalBuyAmount().value()).isEqualByComparingTo("360");
        assertThat(saved.getAveragingDownCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("BUY 후 더 높은 가격으로 BUY: 평단 상승, averagingDownCount 유지")
    void averagingUp() {
        givenFills(List.of(buy(bd("100"), bd("2")), buy(bd("120"), bd("2"))));
        givenNoExistingPosition();

        service.recalculate(WALLET_ID, EXCHANGE_COIN_ID);

        Position saved = capture();
        assertThat(saved.getHolding().avgBuyPrice().value()).isEqualByComparingTo("110");
        assertThat(saved.getAveragingDownCount()).isZero();
    }

    @Test
    @DisplayName("BUY 후 SELL: qty 만 감소, avg 와 totalBuy 는 유지")
    void sellReducesQuantityOnly() {
        givenFills(List.of(buy(bd("100"), bd("5")), sell(bd("150"), bd("2"))));
        givenNoExistingPosition();

        service.recalculate(WALLET_ID, EXCHANGE_COIN_ID);

        Position saved = capture();
        assertThat(saved.getHolding().avgBuyPrice().value()).isEqualByComparingTo("100");
        assertThat(saved.getHolding().totalQuantity().value()).isEqualByComparingTo("3");
        assertThat(saved.getHolding().totalBuyAmount().value()).isEqualByComparingTo("500");
        assertThat(saved.getAveragingDownCount()).isZero();
    }

    @Test
    @DisplayName("기존 포지션이 있으면 그 인스턴스를 replay 해서 저장한다")
    void updatesExistingPosition() {
        Position existing = Position.empty(WALLET_ID, COIN_ID);
        givenFills(List.of(buy(bd("100"), bd("1"))));
        given(positionCommandPort.findByWalletIdAndCoinId(WALLET_ID, COIN_ID))
                .willReturn(Optional.of(existing));

        service.recalculate(WALLET_ID, EXCHANGE_COIN_ID);

        Position saved = capture();
        assertThat(saved).isSameAs(existing);
        assertThat(existing.getHolding().avgBuyPrice().value()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("기존 포지션이 없으면 walletId/coinId 를 가진 새 포지션이 저장된다")
    void createsNewPositionWhenAbsent() {
        givenFills(List.of(buy(bd("100"), bd("1"))));
        givenNoExistingPosition();

        service.recalculate(WALLET_ID, EXCHANGE_COIN_ID);

        Position saved = capture();
        assertThat(saved.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(saved.getCoinId()).isEqualTo(COIN_ID);
    }

    private void givenFills(List<FilledOrder> fills) {
        given(orderQueryPort.findFilledByWalletAndExchangeCoin(WALLET_ID, EXCHANGE_COIN_ID))
                .willReturn(fills);
        given(marketQueryPort.findTradingPair(EXCHANGE_COIN_ID))
                .willReturn(new TradingPair(COIN_ID, 1L));
    }

    private void givenNoExistingPosition() {
        given(positionCommandPort.findByWalletIdAndCoinId(WALLET_ID, COIN_ID))
                .willReturn(Optional.empty());
    }

    private Position capture() {
        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionCommandPort).save(captor.capture());
        return captor.getValue();
    }

    private FilledOrder buy(BigDecimal price, BigDecimal qty) {
        return new FilledOrder(
                1L, WALLET_ID, 1L, Side.BUY, price.multiply(qty), qty, price, LocalDateTime.now());
    }

    private FilledOrder sell(BigDecimal price, BigDecimal qty) {
        return new FilledOrder(
                1L, WALLET_ID, 1L, Side.SELL, price.multiply(qty), qty, price, LocalDateTime.now());
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
