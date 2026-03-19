package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.user.adapter.out.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class DataSeeder {

    private final UserJpaRepository userRepository;
    private final MarketDataIdResolver marketDataIdResolver;
    private final UserDataSeeder userDataSeeder;
    private final InvestmentRoundDataSeeder investmentRoundDataSeeder;
    private final WalletDataSeeder walletDataSeeder;
    private final TradingDataSeeder tradingDataSeeder;
    private final TransferDataSeeder transferDataSeeder;
    private final PortfolioDataSeeder portfolioDataSeeder;
    private final RankingDataSeeder rankingDataSeeder;
    private final RegretAnalysisDataSeeder regretAnalysisDataSeeder;

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (userRepository.count() > 0) {
            log.info("===== DataSeeder 스킵: 이미 데이터가 존재합니다 =====");
            return;
        }

        log.info("===== DataSeeder 시작 =====");

        SeedContext ctx = new SeedContext();

        marketDataIdResolver.resolve(ctx);
        userDataSeeder.seed(ctx);
        investmentRoundDataSeeder.seed(ctx);
        walletDataSeeder.seed(ctx);
        tradingDataSeeder.seed(ctx);
        transferDataSeeder.seed(ctx);
        portfolioDataSeeder.seed(ctx);
        rankingDataSeeder.seed(ctx);
        regretAnalysisDataSeeder.seed(ctx);

        log.info("===== DataSeeder 완료 =====");
    }
}
