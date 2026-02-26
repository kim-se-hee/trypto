package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.out.WalletInfoPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockWalletInfoAdapter implements WalletInfoPort {

    private final Map<Long, Long> walletToRound = new ConcurrentHashMap<>();

    @Override
    public Long getRoundIdByWalletId(Long walletId) {
        Long roundId = walletToRound.get(walletId);
        if (roundId == null) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
        return roundId;
    }

    public void setRoundId(Long walletId, Long roundId) {
        walletToRound.put(walletId, roundId);
    }

    public void clear() {
        walletToRound.clear();
    }
}
