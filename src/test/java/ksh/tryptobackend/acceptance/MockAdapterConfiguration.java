package ksh.tryptobackend.acceptance;

import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.mock.MockDepositAddressExchangeAdapter;
import ksh.tryptobackend.acceptance.mock.MockDepositAddressExchangeCoinChainAdapter;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockListedCoinAdapter;
import ksh.tryptobackend.acceptance.mock.MockViolationRuleAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockPriceChangeRateAdapter;
import ksh.tryptobackend.acceptance.mock.MockTradingVenueAdapter;
import ksh.tryptobackend.acceptance.mock.MockViolationPersistenceAdapter;
import ksh.tryptobackend.acceptance.mock.MockWalletBalanceAdapter;
import ksh.tryptobackend.acceptance.mock.MockTransferWalletAdapter;
import ksh.tryptobackend.regretanalysis.application.port.out.BtcPriceHistoryPort;
import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.application.port.out.ListedCoinPort;
import ksh.tryptobackend.trading.application.port.out.ViolationRulePort;
import ksh.tryptobackend.trading.application.port.out.LivePricePort;
import ksh.tryptobackend.trading.application.port.out.PriceChangeRatePort;
import ksh.tryptobackend.trading.application.port.out.TradingVenuePort;
import ksh.tryptobackend.trading.application.port.out.ViolationPersistencePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangeCoinChainPort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangePort;
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
    public ListedCoinPort listedCoinPort() {
        return new MockListedCoinAdapter();
    }

    @Bean
    @Primary
    public HoldingPersistencePort holdingPersistencePort() {
        return new MockHoldingAdapter();
    }

    @Bean
    @Primary
    public ViolationRulePort violationRulePort() {
        return new MockViolationRuleAdapter();
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
    public BtcPriceHistoryPort btcPriceHistoryPort() {
        return new MockBtcPriceHistoryAdapter();
    }

    @Bean
    @Primary
    public DepositAddressExchangePort depositAddressExchangePort() {
        return new MockDepositAddressExchangeAdapter();
    }

    @Bean
    @Primary
    public DepositAddressExchangeCoinChainPort depositAddressExchangeCoinChainPort() {
        return new MockDepositAddressExchangeCoinChainAdapter();
    }

    @Bean
    @Primary
    public TransferWalletPort transferWalletPort() {
        return new MockTransferWalletAdapter();
    }
}
