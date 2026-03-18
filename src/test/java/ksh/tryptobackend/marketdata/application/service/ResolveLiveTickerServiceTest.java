package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveLiveTickerServiceTest {

    @Mock private ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;

    @InjectMocks private ResolveLiveTickerService sut;

    @Test
    @DisplayName("매핑이 존재하면 LiveTickerResult를 반환한다")
    void resolve_withMapping_returnsResult() {
        // Given
        ExchangeCoinMapping mapping = new ExchangeCoinMapping(10L, 1L, 5L, "BTC");
        when(exchangeCoinMappingCacheQueryPort.resolve("Upbit", "BTC/KRW"))
            .thenReturn(Optional.of(mapping));

        // When
        Optional<LiveTickerResult> result = sut.resolve("Upbit", "BTC/KRW",
            new BigDecimal("50000000"), new BigDecimal("2.3"),
            new BigDecimal("1000000000"), 1709913600000L);

        // Then
        assertThat(result).isPresent();
        LiveTickerResult ticker = result.get();
        assertThat(ticker.exchangeId()).isEqualTo(1L);
        assertThat(ticker.coinId()).isEqualTo(5L);
        assertThat(ticker.symbol()).isEqualTo("BTC");
        assertThat(ticker.price()).isEqualByComparingTo(new BigDecimal("50000000"));
        assertThat(ticker.changeRate()).isEqualByComparingTo(new BigDecimal("2.3"));
        assertThat(ticker.quoteTurnover()).isEqualByComparingTo(new BigDecimal("1000000000"));
        assertThat(ticker.timestamp()).isEqualTo(1709913600000L);
    }

    @Test
    @DisplayName("매핑이 없으면 빈 Optional을 반환한다")
    void resolve_withoutMapping_returnsEmpty() {
        // Given
        when(exchangeCoinMappingCacheQueryPort.resolve("Unknown", "XYZ/KRW"))
            .thenReturn(Optional.empty());

        // When
        Optional<LiveTickerResult> result = sut.resolve("Unknown", "XYZ/KRW",
            new BigDecimal("1000"), new BigDecimal("0.1"),
            new BigDecimal("500000"), 1709913600000L);

        // Then
        assertThat(result).isEmpty();
    }
}
