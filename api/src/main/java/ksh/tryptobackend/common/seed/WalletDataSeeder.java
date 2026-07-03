package ksh.tryptobackend.common.seed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class WalletDataSeeder {

    private static final BigDecimal DEFAULT_SEED = new BigDecimal("10000000");
    private static final BigDecimal LARGE_SEED = new BigDecimal("50000000");

    private final WalletJpaRepository walletRepository;
    private final WalletBalanceJpaRepository balanceRepository;

    @Transactional
    void seed(SeedContext ctx) {
        int walletCount = 0;
        int balanceCount = 0;

        walletCount += seedMainUserWallets(ctx);
        walletCount += seedBackgroundUserWallets(ctx);

        balanceCount += seedBalances(ctx);

        log.info("[Seed] 지갑 {}건, 잔고 {}건 생성 완료", walletCount, balanceCount);
    }

    private int seedMainUserWallets(SeedContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        count += createAllExchangeWallets(ctx, "김비트", "UPBIT", LARGE_SEED, now);

        count += createAllExchangeWallets(ctx, "이더리움", "BITHUMB", DEFAULT_SEED, now);

        count += createAllExchangeWallets(ctx, "박솔라나", "BINANCE", DEFAULT_SEED, now);

        BigDecimal splitSeed =
                LARGE_SEED.divide(new BigDecimal("3"), 8, java.math.RoundingMode.FLOOR);
        count += createWallet(ctx, "최리플", "UPBIT", splitSeed, now);
        count += createWallet(ctx, "최리플", "BITHUMB", splitSeed, now);
        count += createWallet(ctx, "최리플", "BINANCE", splitSeed, now);

        count += createAllExchangeWallets(ctx, "정도지", "UPBIT", DEFAULT_SEED, now);

        count += createAllExchangeWallets(ctx, "한에이다", "UPBIT", DEFAULT_SEED, now);

        count += createAllExchangeWallets(ctx, "강링크", "BITHUMB", DEFAULT_SEED, now);

        count += createAllExchangeWallets(ctx, "윤닷", "UPBIT", DEFAULT_SEED, now);

        count += createAllExchangeWallets(ctx, "송아톰", "UPBIT", DEFAULT_SEED, now);

        count += createAllExchangeWallets(ctx, "임앱트", "BINANCE", LARGE_SEED, now);

        return count;
    }

    private int seedBackgroundUserWallets(SeedContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        String[] exchanges = {"UPBIT", "BITHUMB", "BINANCE"};

        for (int i = 11; i <= 200; i++) {
            String nickname = "투자자" + i;
            Long userId = ctx.userIdByNickname.get(nickname);
            if (userId == null) continue;

            Long roundId = ctx.activeRoundIdByUserId.get(userId);
            if (roundId == null) continue;

            for (String exchange : exchanges) {
                Long exchangeId = ctx.getExchangeId(exchange);
                if (exchangeId == null) continue;

                BigDecimal splitSeed =
                        DEFAULT_SEED.divide(new BigDecimal("3"), 8, java.math.RoundingMode.FLOOR);
                WalletJpaEntity entity = saveWallet(roundId, exchangeId, splitSeed, now);
                ctx.addWalletId(roundId, entity.getId(), exchangeId);
                count++;
            }
        }
        return count;
    }

    private int seedBalances(SeedContext ctx) {
        Long krwCoinId = ctx.getCoinId("KRW");
        if (krwCoinId == null) return 0;

        List<WalletBalanceJpaEntity> balances = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : ctx.walletIdsByRoundId.entrySet()) {
            for (Long walletId : entry.getValue()) {
                balances.add(
                        new WalletBalanceJpaEntity(
                                walletId, krwCoinId, new BigDecimal("5000000"), BigDecimal.ZERO));
            }
        }

        balanceRepository.saveAll(balances);
        return balances.size();
    }

    private static final String[] ALL_EXCHANGES = {"UPBIT", "BITHUMB", "BINANCE"};

    private int createAllExchangeWallets(
            SeedContext ctx,
            String nickname,
            String primaryExchange,
            BigDecimal seedAmount,
            LocalDateTime now) {
        int count = 0;
        for (String exchange : ALL_EXCHANGES) {
            BigDecimal amount = exchange.equals(primaryExchange) ? seedAmount : BigDecimal.ZERO;
            count += createWallet(ctx, nickname, exchange, amount, now);
        }
        return count;
    }

    private int createWallet(
            SeedContext ctx,
            String nickname,
            String exchangeName,
            BigDecimal seedAmount,
            LocalDateTime now) {
        Long userId = ctx.userIdByNickname.get(nickname);
        if (userId == null) return 0;

        Long roundId = ctx.activeRoundIdByUserId.get(userId);
        if (roundId == null) return 0;

        Long exchangeId = ctx.getExchangeId(exchangeName);
        if (exchangeId == null) return 0;

        WalletJpaEntity entity = saveWallet(roundId, exchangeId, seedAmount, now);
        ctx.addWalletId(roundId, entity.getId(), exchangeId);
        return 1;
    }

    private WalletJpaEntity saveWallet(
            Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime now) {
        Wallet wallet =
                Wallet.builder()
                        .roundId(roundId)
                        .exchangeId(exchangeId)
                        .seedAmount(seedAmount)
                        .createdAt(now)
                        .build();
        return walletRepository.save(WalletJpaEntity.fromDomain(wallet));
    }
}
