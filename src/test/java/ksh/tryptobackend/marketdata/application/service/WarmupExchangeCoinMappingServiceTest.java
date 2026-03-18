package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindAllExchangeIdsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarmupExchangeCoinMappingServiceTest {

    @Mock private ExchangeCoinMappingCacheCommandPort exchangeCoinMappingCacheCommandPort;
    @Mock private FindAllExchangeIdsUseCase findAllExchangeIdsUseCase;
    @Mock private FindExchangeDetailUseCase findExchangeDetailUseCase;
    @Mock private FindExchangeCoinsUseCase findExchangeCoinsUseCase;
    @Mock private FindCoinInfoUseCase findCoinInfoUseCase;

    @InjectMocks private WarmupExchangeCoinMappingService sut;

    @Test
    @DisplayName("거래소-코인 매핑 캐시를 로딩한다")
    void warmup_loadsExchangeCoinMappingCache() {
        // Given
        when(findAllExchangeIdsUseCase.findAllExchangeIds()).thenReturn(List.of(1L));
        when(findExchangeDetailUseCase.findExchangeDetail(1L))
            .thenReturn(Optional.of(new ExchangeDetailResult("Upbit", 100L, true, new BigDecimal("0.0005"))));
        when(findCoinInfoUseCase.findByIds(Set.of(100L)))
            .thenReturn(Map.of(100L, new CoinInfoResult("KRW", "Korean Won")));
        when(findExchangeCoinsUseCase.findByExchangeId(1L))
            .thenReturn(List.of(new ExchangeCoinListResult(10L, 5L, "BTC", "Bitcoin")));

        // When
        sut.warmup();

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<ExchangeSymbolKey, ExchangeCoinMapping>> captor =
            ArgumentCaptor.forClass(Map.class);
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
