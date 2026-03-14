package ksh.tryptobackend.acceptance;

import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.mock.MockCandleAdapter;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockPriceChangeRateAdapter;
import ksh.tryptobackend.marketdata.application.port.out.BtcPriceHistoryQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.PriceChangeRateQueryPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockAdapterConfiguration {

    @Bean
    @Primary
    public MockLivePriceAdapter livePriceQueryPort() {
        return new MockLivePriceAdapter();
    }

    @Bean
    @Primary
    public MockHoldingAdapter mockHoldingAdapter() {
        return new MockHoldingAdapter();
    }

    @Bean
    @Primary
    public PriceChangeRateQueryPort priceChangeRatePort() {
        return new MockPriceChangeRateAdapter();
    }

    @Bean
    @Primary
    public BtcPriceHistoryQueryPort btcPriceHistoryPort() {
        return new MockBtcPriceHistoryAdapter();
    }

    @Bean
    @Primary
    public MockCandleAdapter mockCandleAdapter() {
        return new MockCandleAdapter();
    }
}
