package ksh.tryptobackend.regretanalysis.domain.vo;

import java.time.LocalDateTime;

public record AnalysisActiveRound(Long roundId, Long userId, LocalDateTime startedAt) {

    public ActiveRoundExchange combineWith(AnalysisWallet wallet) {
        return new ActiveRoundExchange(roundId, userId, wallet.exchangeId(), wallet.walletId(), startedAt);
    }
}
