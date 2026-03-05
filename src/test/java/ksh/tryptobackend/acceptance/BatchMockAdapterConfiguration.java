package ksh.tryptobackend.acceptance;

import ksh.tryptobackend.acceptance.mock.MockActiveRoundListAdapter;
import ksh.tryptobackend.acceptance.mock.MockActiveRoundQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockBalanceQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockEligibleRoundQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockEmergencyFundingSnapshotAdapter;
import ksh.tryptobackend.acceptance.mock.MockExchangeInfoQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockSnapshotHoldingQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockRankingLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockTradeViolationQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockWalletSnapshotAdapter;
import ksh.tryptobackend.ranking.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.ranking.application.port.out.EligibleRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.EmergencyFundingSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.ExchangeInfoQueryPort;
import ksh.tryptobackend.ranking.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.LivePricePort;
import ksh.tryptobackend.ranking.application.port.out.WalletSnapshotPort;
import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundListPort;
import ksh.tryptobackend.regretanalysis.application.port.out.TradeViolationQueryPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class BatchMockAdapterConfiguration {

    @Bean
    @Primary
    public ActiveRoundQueryPort activeRoundQueryPort() {
        return new MockActiveRoundQueryAdapter();
    }

    @Bean
    @Primary
    public WalletSnapshotPort walletSnapshotPort() {
        return new MockWalletSnapshotAdapter();
    }

    @Bean
    @Primary
    public ExchangeInfoQueryPort exchangeInfoQueryPort() {
        return new MockExchangeInfoQueryAdapter();
    }

    @Bean
    @Primary
    public HoldingQueryPort holdingQueryPort() {
        return new MockSnapshotHoldingQueryAdapter();
    }

    @Bean
    @Primary
    public BalanceQueryPort balanceQueryPort() {
        return new MockBalanceQueryAdapter();
    }

    @Bean
    @Primary
    public EmergencyFundingSnapshotPort emergencyFundingSnapshotPort() {
        return new MockEmergencyFundingSnapshotAdapter();
    }

    @Bean
    @Primary
    public EligibleRoundQueryPort eligibleRoundQueryPort() {
        return new MockEligibleRoundQueryAdapter();
    }

    @Bean
    @Primary
    public ActiveRoundListPort activeRoundListPort() {
        return new MockActiveRoundListAdapter();
    }

    @Bean
    @Primary
    public TradeViolationQueryPort tradeViolationQueryPort() {
        return new MockTradeViolationQueryAdapter();
    }

    @Bean
    @Primary
    public LivePricePort rankingLivePricePort() {
        return new MockRankingLivePriceAdapter();
    }
}
