package ksh.tryptobackend.marketdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ExternalTickerCommand;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ResolveLiveTickerCommand;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerBatchResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolveLiveTickerServiceTest {

    @Mock private ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;

    @InjectMocks private ResolveLiveTickerService sut;

    @Test
    @DisplayName("매핑이 존재하면 배치 결과를 반환한다")
    void resolve_withMapping_returnsBatch() {
        // Given
        ExchangeCoinMapping mapping = new ExchangeCoinMapping(10L, 1L, 5L, "BTC");
        when(exchangeCoinMappingCacheQueryPort.resolve("Upbit", "BTC/KRW"))
                .thenReturn(Optional.of(mapping));
        ResolveLiveTickerCommand command =
                new ResolveLiveTickerCommand(
                        "Upbit",
                        List.of(
                                new ExternalTickerCommand(
                                        "BTC/KRW",
                                        new BigDecimal("50000000"),
                                        new BigDecimal("2.3"),
                                        new BigDecimal("1000000000"),
                                        1709913600000L)));

        // When
        Optional<LiveTickerBatchResult> result = sut.resolve(command);

        // Then
        assertThat(result).isPresent();
        LiveTickerBatchResult batch = result.get();
        assertThat(batch.exchangeId()).isEqualTo(1L);
        assertThat(batch.earliestTimestamp()).isEqualTo(1709913600000L);
        assertThat(batch.tickers()).hasSize(1);
        LiveTickerResult ticker = batch.tickers().get(0);
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
        ResolveLiveTickerCommand command =
                new ResolveLiveTickerCommand(
                        "Unknown",
                        List.of(
                                new ExternalTickerCommand(
                                        "XYZ/KRW",
                                        new BigDecimal("1000"),
                                        new BigDecimal("0.1"),
                                        new BigDecimal("500000"),
                                        1709913600000L)));

        // When
        Optional<LiveTickerBatchResult> result = sut.resolve(command);

        // Then
        assertThat(result).isEmpty();
    }
}
