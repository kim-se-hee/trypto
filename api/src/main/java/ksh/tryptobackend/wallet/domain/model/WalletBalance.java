package ksh.tryptobackend.wallet.domain.model;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletBalance {

    private final Long id;
    private final Long walletId;
    private final Long coinId;
    private BigDecimal available;
    private BigDecimal locked;

    public static WalletBalance zero(Long coinId) {
        return WalletBalance.builder()
                .coinId(coinId)
                .available(BigDecimal.ZERO)
                .locked(BigDecimal.ZERO)
                .build();
    }

    public void deductAvailable(BigDecimal amount) {
        validateSufficient(available, amount);
        this.available = available.subtract(amount);
    }

    public void addAvailable(BigDecimal amount) {
        this.available = available.add(amount);
    }

    public void lock(BigDecimal amount) {
        validateSufficient(available, amount);
        this.available = available.subtract(amount);
        this.locked = locked.add(amount);
    }

    public void unlock(BigDecimal amount) {
        this.locked = locked.subtract(amount);
        this.available = available.add(amount);
    }

    private void validateSufficient(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}
