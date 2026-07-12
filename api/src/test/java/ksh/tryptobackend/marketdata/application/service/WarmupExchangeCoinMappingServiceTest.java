package ksh.tryptobackend.marketdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoins;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.marketdata.domain.vo.CoinSymbols;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarmupExchangeCoinMappingServiceTest {

    @Mock
    private ExchangeQueryPort exchangeQueryPort;

    @Mock
    private ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Mock
    private CoinQueryPort coinQueryPort;

    @Mock
    private ExchangeCoinMappingCacheCommandPort exchangeCoinMappingCacheCommandPort;

    @InjectMocks
    private WarmupExchangeCoinMappingService sut;

    @Test
    @DisplayName("거래소-코인 매핑 캐시를 로딩한다")
    void warmup_loadsExchangeCoinMappingCache() {
        // Given
        Exchange upbit = Exchange.builder()
                .exchangeId(1L)
                .name("Upbit")
                .marketType(ExchangeMarketType.DOMESTIC)
                .baseCurrencyCoinId(100L)
                .feeRate(new BigDecimal("0.0005"))
                .build();
        when(exchangeQueryPort.findAllExchangeIds()).thenReturn(List.of(1L));
        when(exchangeQueryPort.findExchangeDetailById(1L)).thenReturn(Optional.of(upbit));
        when(coinQueryPort.findSymbolsByIds(Set.of(100L))).thenReturn(new CoinSymbols(Map.of(100L, "KRW")));
        when(exchangeCoinQueryPort.findByExchangeId(1L))
                .thenReturn(new ExchangeCoins(List.of(new ExchangeCoin(10L, 1L, 5L, "Bitcoin"))));
        when(coinQueryPort.findSymbolsByIds(Set.of(5L))).thenReturn(new CoinSymbols(Map.of(5L, "BTC")));

        // When
        sut.warmup();

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<ExchangeSymbolKey, ExchangeCoinMapping>> captor = ArgumentCaptor.forClass(Map.class);
        verify(exchangeCoinMappingCacheCommandPort).loadAll(captor.capture());

        Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings = captor.getValue();
        ExchangeSymbolKey expectedKey = ExchangeSymbolKey.of("Upbit", "BTC", "KRW");
        assertThat(mappings).containsKey(expectedKey);

        ExchangeCoinMapping mapping = mappings.get(expectedKey);
        assertThat(mapping.exchangeCoinId()).isEqualTo(10L);
        assertThat(mapping.exchangeId()).isEqualTo(1L);
        assertThat(mapping.coinId()).isEqualTo(5L);
        assertThat(mapping.coinSymbol()).isEqualTo("BTC");
    }
}
