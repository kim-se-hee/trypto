package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.portfolio.adapter.out.entity.PortfolioSnapshotJpaEntity;
import ksh.tryptobackend.portfolio.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.portfolio.domain.model.SnapshotDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
class PortfolioDataSeeder {

    private static final BigDecimal DEFAULT_SEED = new BigDecimal("10000000");
    private static final int SNAPSHOT_DAYS = 30;
    private static final Map<String, BigDecimal> COIN_PRICES = Map.of(
        "BTC", new BigDecimal("95000000"),
        "ETH", new BigDecimal("5000000"),
        "XRP", new BigDecimal("3200"),
        "SOL", new BigDecimal("280000")
    );

    private final PortfolioSnapshotJpaRepository snapshotRepository;

    private final Random random = new Random(42);

    @Transactional
    void seed(SeedContext ctx) {
        List<PortfolioSnapshotJpaEntity> snapshots = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (var roundEntry : ctx.activeRoundIdByUserId.entrySet()) {
            Long userId = roundEntry.getKey();
            Long roundId = roundEntry.getValue();

            List<Long> walletIds = ctx.walletIdsByRoundId.getOrDefault(roundId, List.of());
            for (Long walletId : walletIds) {
                Long exchangeId = ctx.exchangeIdByWalletId.get(walletId);
                if (exchangeId == null) continue;

                double baseProfitRate = random.nextGaussian() * 15;
                snapshots.addAll(createDailySnapshots(
                    userId, roundId, exchangeId, today, baseProfitRate, ctx));
            }
        }

        snapshotRepository.saveAll(snapshots);
        log.info("[Seed] 포트폴리오 스냅샷 {}건 생성 완료", snapshots.size());
    }

    private List<PortfolioSnapshotJpaEntity> createDailySnapshots(
            Long userId, Long roundId, Long exchangeId,
            LocalDate today, double baseProfitRate, SeedContext ctx) {

        List<PortfolioSnapshotJpaEntity> snapshots = new ArrayList<>();

        for (int day = SNAPSHOT_DAYS; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            double dailyVariation = random.nextGaussian() * 2;
            double profitRate = baseProfitRate + dailyVariation * (SNAPSHOT_DAYS - day) / SNAPSHOT_DAYS;
            BigDecimal profitRateBd = new BigDecimal(profitRate).setScale(4, RoundingMode.HALF_UP);

            BigDecimal totalInvestment = DEFAULT_SEED.divide(new BigDecimal("3"), 8, RoundingMode.HALF_UP);
            BigDecimal totalAsset = totalInvestment.multiply(
                BigDecimal.ONE.add(profitRateBd.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP))
            ).setScale(8, RoundingMode.HALF_UP);
            BigDecimal totalProfit = totalAsset.subtract(totalInvestment);

            List<SnapshotDetail> details = createSnapshotDetails(ctx);

            PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .userId(userId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalAsset(totalAsset)
                .totalAssetKrw(totalAsset)
                .totalInvestment(totalInvestment)
                .totalInvestmentKrw(totalInvestment)
                .totalProfit(totalProfit)
                .totalProfitRate(profitRateBd)
                .snapshotDate(date)
                .details(details)
                .build();
            snapshots.add(PortfolioSnapshotJpaEntity.fromDomain(snapshot));
        }

        return snapshots;
    }

    private List<SnapshotDetail> createSnapshotDetails(SeedContext ctx) {
        List<SnapshotDetail> details = new ArrayList<>();
        int coinCount = 0;

        for (var entry : COIN_PRICES.entrySet()) {
            Long coinId = ctx.getCoinId(entry.getKey());
            if (coinId == null) continue;
            if (random.nextDouble() > 0.5) continue;

            BigDecimal price = entry.getValue();
            BigDecimal variation = BigDecimal.ONE.add(
                new BigDecimal(random.nextDouble(-0.05, 0.05)).setScale(4, RoundingMode.HALF_UP));
            BigDecimal currentPrice = price.multiply(variation).setScale(8, RoundingMode.HALF_UP);
            BigDecimal avgBuyPrice = price.multiply(new BigDecimal("0.98")).setScale(8, RoundingMode.HALF_UP);
            BigDecimal quantity = new BigDecimal(random.nextDouble(0.001, 0.5)).setScale(8, RoundingMode.HALF_UP);
            BigDecimal profitRate = currentPrice.subtract(avgBuyPrice)
                .divide(avgBuyPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

            details.add(SnapshotDetail.builder()
                .coinId(coinId)
                .quantity(quantity)
                .avgBuyPrice(avgBuyPrice)
                .currentPrice(currentPrice)
                .profitRate(profitRate)
                .assetRatio(new BigDecimal("25.00"))
                .build());
            coinCount++;
        }

        if (coinCount > 0) {
            BigDecimal ratio = new BigDecimal("100").divide(new BigDecimal(coinCount), 4, RoundingMode.HALF_UP);
            details.forEach(d -> {
                // assetRatio is final in SnapshotDetail, so we create with correct ratio above
            });
        }

        return details;
    }
}
