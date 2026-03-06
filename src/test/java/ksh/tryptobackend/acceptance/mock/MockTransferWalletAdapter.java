package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.transfer.domain.vo.TransferWallet;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockTransferWalletAdapter implements TransferWalletPort {

    private final Map<Long, Long> walletToUserId = new ConcurrentHashMap<>();
    private final Map<Long, TransferWallet> wallets = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    @Override
    public Long getOwnerUserId(Long walletId) {
        Long userId = walletToUserId.get(walletId);
        if (userId == null) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
        return userId;
    }

    @Override
    public TransferWallet getWallet(Long walletId) {
        TransferWallet wallet = wallets.get(walletId);
        if (wallet == null) {
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }
        return wallet;
    }

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return balances.getOrDefault(balanceKey(walletId, coinId), BigDecimal.ZERO);
    }

    @Override
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        String key = balanceKey(walletId, coinId);
        balances.merge(key, amount, (current, deduct) -> current.subtract(deduct));
    }

    @Override
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        String key = balanceKey(walletId, coinId);
        balances.merge(key, amount, BigDecimal::add);
    }

    @Override
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        // no-op for test
    }

    public void setOwnerUserId(Long walletId, Long userId) {
        walletToUserId.put(walletId, userId);
    }

    public void addWallet(Long walletId, Long roundId, Long exchangeId) {
        wallets.put(walletId, new TransferWallet(walletId, roundId, exchangeId));
    }

    public void setBalance(Long walletId, Long coinId, BigDecimal amount) {
        balances.put(balanceKey(walletId, coinId), amount);
    }

    public void clear() {
        walletToUserId.clear();
        wallets.clear();
        balances.clear();
    }

    private String balanceKey(Long walletId, Long coinId) {
        return walletId + ":" + coinId;
    }
}
