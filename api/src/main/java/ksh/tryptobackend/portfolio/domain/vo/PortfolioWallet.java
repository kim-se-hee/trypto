package ksh.tryptobackend.portfolio.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record PortfolioWallet(Long walletId, Long exchangeId, Long ownerId) {

    public void verifyOwnedBy(Long userId) {
        if (!ownerId.equals(userId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_OWNED);
        }
    }
}
