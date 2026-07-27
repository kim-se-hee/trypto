package ksh.tryptobackend.acceptance;

import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.mock.MockCandleAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockSocialAuthenticator;
import ksh.tryptobackend.marketdata.application.port.out.BtcPriceHistoryQueryPort;
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

    // PriceChangeRateQueryPort 는 실물 어댑터를 쓴다. 비율→퍼센트 환산이 어댑터에 있어
    // 목으로 대체하면 그 코드가 검증 범위 밖으로 빠진다. 나머지 목 제거는 #329.

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

    @Bean
    @Primary
    public MockSocialAuthenticator socialAuthenticator() {
        return new MockSocialAuthenticator();
    }
}
