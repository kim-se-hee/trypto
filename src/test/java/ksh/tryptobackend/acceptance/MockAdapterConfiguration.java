package ksh.tryptobackend.acceptance;

import ksh.tryptobackend.acceptance.mock.MockExchangeCoinAdapter;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockInvestmentRuleAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockPriceChangeRateAdapter;
import ksh.tryptobackend.acceptance.mock.MockTradingVenueAdapter;
import ksh.tryptobackend.acceptance.mock.MockViolationPersistenceAdapter;
import ksh.tryptobackend.acceptance.mock.MockWalletBalanceAdapter;
import ksh.tryptobackend.acceptance.mock.MockWalletInfoAdapter;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort;
import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.trading.application.port.out.LivePricePort;
import ksh.tryptobackend.trading.application.port.out.PriceChangeRatePort;
import ksh.tryptobackend.trading.application.port.out.TradingVenuePort;
import ksh.tryptobackend.trading.application.port.out.ViolationPersistencePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.trading.application.port.out.WalletInfoPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockAdapterConfiguration {

    @Bean
    @Primary
    public WalletBalancePort walletBalancePort() {
        return new MockWalletBalanceAdapter();
    }

    @Bean
    @Primary
    public LivePricePort livePricePort() {
        return new MockLivePriceAdapter();
    }

    @Bean
    @Primary
    public TradingVenuePort tradingVenuePort() {
        return new MockTradingVenueAdapter();
    }

    @Bean
    @Primary
    public ExchangeCoinPort exchangeCoinPort() {
        return new MockExchangeCoinAdapter();
    }

    @Bean
    @Primary
    public HoldingPersistencePort holdingPersistencePort() {
        return new MockHoldingAdapter();
    }

    @Bean
    @Primary
    public InvestmentRulePort investmentRulePort() {
        return new MockInvestmentRuleAdapter();
    }

    @Bean
    @Primary
    public PriceChangeRatePort priceChangeRatePort() {
        return new MockPriceChangeRateAdapter();
    }

    @Bean
    @Primary
    public ViolationPersistencePort violationPersistencePort() {
        return new MockViolationPersistenceAdapter();
    }

    @Bean
    @Primary
    public WalletInfoPort walletInfoPort() {
        return new MockWalletInfoAdapter();
    }
}
