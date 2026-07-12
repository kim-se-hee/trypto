package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 주문 유스케이스가 대상 지갑이 요청자 소유인지 확인할 때 사용한다. */
@Component
@RequiredArgsConstructor
public class WalletOwnershipVerifier {

    private final WalletQueryPort walletQueryPort;

    public void verify(Long walletId, Long requesterId) {
        Long ownerId = walletQueryPort.getOwnerId(walletId);
        if (!ownerId.equals(requesterId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_OWNED);
        }
    }
}
