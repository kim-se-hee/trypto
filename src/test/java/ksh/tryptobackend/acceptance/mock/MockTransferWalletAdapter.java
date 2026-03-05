package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockTransferWalletAdapter implements TransferWalletPort {

    private final Map<Long, Long> walletToUserId = new ConcurrentHashMap<>();

    @Override
    public Long getOwnerUserId(Long walletId) {
        Long userId = walletToUserId.get(walletId);
        if (userId == null) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
        return userId;
    }

    public void setOwnerUserId(Long walletId, Long userId) {
        walletToUserId.put(walletId, userId);
    }

    public void clear() {
        walletToUserId.clear();
    }
}
