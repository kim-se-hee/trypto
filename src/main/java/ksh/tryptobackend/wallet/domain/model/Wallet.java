package ksh.tryptobackend.wallet.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class Wallet {

    private final Long walletId;
    private final Long roundId;
    private final Long exchangeId;
    private final BigDecimal seedAmount;
    private final LocalDateTime createdAt;

    public static Wallet create(Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime createdAt) {
        return Wallet.builder()
            .roundId(roundId)
            .exchangeId(exchangeId)
            .seedAmount(seedAmount)
            .createdAt(createdAt)
            .build();
    }
}
