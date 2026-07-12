package ksh.tryptobackend.trading.adapter.out.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.trading.domain.service.WalletOwnershipVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletOwnershipVerifierImpl implements WalletOwnershipVerifier {

    private final WalletQueryPort walletQueryPort;

    @Override
    public void verify(Long walletId, Long requesterId) {
        Long ownerId = walletQueryPort.getOwnerId(walletId);
        if (!ownerId.equals(requesterId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_OWNED);
        }
    }
}
