package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceMessagePort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.LiveTicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BroadcastLiveTickerServiceTest {

    @Mock private ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;
    @Mock private LivePriceMessagePort livePriceMessagePort;

    @InjectMocks private BroadcastLiveTickerService sut;

    @Test
    @DisplayName("매핑이 존재하면 LiveTicker를 전송한다")
    void broadcast_withMapping_sendsLiveTicker() {
        // Given
        ExchangeCoinMapping mapping = new ExchangeCoinMapping(10L, 1L, 5L, "BTC");
        when(exchangeCoinMappingCacheQueryPort.resolve("Upbit", "BTC/KRW"))
            .thenReturn(Optional.of(mapping));

        // When
        sut.broadcast("Upbit", "BTC/KRW", new BigDecimal("50000000"),
            new BigDecimal("2.3"), new BigDecimal("1000000000"), 1709913600000L);

        // Then
        ArgumentCaptor<LiveTicker> captor = ArgumentCaptor.forClass(LiveTicker.class);
        verify(livePriceMessagePort).send(eq(1L), captor.capture());

        LiveTicker ticker = captor.getValue();
        assertThat(ticker.coinId()).isEqualTo(5L);
        assertThat(ticker.symbol()).isEqualTo("BTC");
        assertThat(ticker.price()).isEqualByComparingTo(new BigDecimal("50000000"));
        assertThat(ticker.changeRate()).isEqualByComparingTo(new BigDecimal("2.3"));
        assertThat(ticker.quoteTurnover()).isEqualByComparingTo(new BigDecimal("1000000000"));
        assertThat(ticker.timestamp()).isEqualTo(1709913600000L);
    }

    @Test
    @DisplayName("매핑이 없으면 전송하지 않는다")
    void broadcast_withoutMapping_skips() {
        // Given
        when(exchangeCoinMappingCacheQueryPort.resolve("Unknown", "XYZ/KRW"))
            .thenReturn(Optional.empty());

        // When
        sut.broadcast("Unknown", "XYZ/KRW", new BigDecimal("1000"),
            new BigDecimal("0.1"), new BigDecimal("500000"), 1709913600000L);

        // Then
        verify(livePriceMessagePort, never()).send(any(), any());
    }
}
