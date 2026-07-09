package ksh.tryptobackend.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Wallet {

    private final Long walletId;
    private final Long roundId;
    private final Long exchangeId;
    private final BigDecimal seedAmount;
    private final LocalDateTime createdAt;

    public static Wallet create(
            Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime createdAt) {
        return Wallet.builder()
                .roundId(roundId)
                .exchangeId(exchangeId)
                .seedAmount(seedAmount)
                .createdAt(createdAt)
                .build();
    }

    public boolean isOwnedBy(Long requesterId, Long ownerId) {
        return ownerId.equals(requesterId);
    }

    public void verifyOwnedBy(Long requesterId, Long ownerId) {
        if (!ownerId.equals(requesterId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_OWNED);
        }
    }

    public void verifySameRoundAs(Wallet other) {
        if (!roundId.equals(other.roundId)) {
            throw new CustomException(ErrorCode.DIFFERENT_ROUND_TRANSFER);
        }
    }
}
