package ksh.tryptobackend.common.seed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.wallet.adapter.out.entity.TransferJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.TransferJpaRepository;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class TransferDataSeeder {

    private final TransferJpaRepository transferRepository;

    @Transactional
    void seed(SeedContext ctx) {
        List<TransferJpaEntity> transfers = new ArrayList<>();

        transfers.addAll(createTransfersForUser(ctx, "최리플", "BTC"));
        transfers.addAll(createTransfersForUser(ctx, "최리플", "XRP"));

        transferRepository.saveAll(transfers);
        log.info("[Seed] 송금 {}건 생성 완료", transfers.size());
    }

    private List<TransferJpaEntity> createTransfersForUser(
            SeedContext ctx, String nickname, String coinSymbol) {
        Long userId = ctx.userIdByNickname.get(nickname);
        if (userId == null) return List.of();

        Long roundId = ctx.activeRoundIdByUserId.get(userId);
        if (roundId == null) return List.of();

        List<Long> walletIds = ctx.walletIdsByRoundId.getOrDefault(roundId, List.of());
        if (walletIds.size() < 2) return List.of();

        Long coinId = ctx.getCoinId(coinSymbol);
        if (coinId == null) return List.of();

        LocalDateTime now = LocalDateTime.now();
        List<TransferJpaEntity> transfers = new ArrayList<>();

        for (int i = 0; i < walletIds.size() - 1; i++) {
            Long fromWalletId = walletIds.get(i);
            Long toWalletId = walletIds.get(i + 1);

            Transfer transfer =
                    Transfer.builder()
                            .fromWalletId(fromWalletId)
                            .toWalletId(toWalletId)
                            .coinId(coinId)
                            .amount(new BigDecimal("0.01"))
                            .status(TransferStatus.SUCCESS)
                            .createdAt(now.minusDays(10 + i))
                            .completedAt(now.minusDays(10 + i).plusMinutes(5))
                            .build();
            transfers.add(TransferJpaEntity.fromDomain(transfer));
        }

        return transfers;
    }
}
