package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockWalletBalanceAdapter implements WalletBalancePort {

    private final Map<String, BigDecimal> availableBalances = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lockedBalances = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return availableBalances.getOrDefault(key(walletId, coinId), BigDecimal.ZERO);
    }

    @Override
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        String k = key(walletId, coinId);
        BigDecimal current = availableBalances.getOrDefault(k, BigDecimal.ZERO);
        if (current.compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        availableBalances.put(k, current.subtract(amount));
    }

    @Override
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        String k = key(walletId, coinId);
        BigDecimal current = availableBalances.getOrDefault(k, BigDecimal.ZERO);
        availableBalances.put(k, current.add(amount));
    }

    @Override
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        String k = key(walletId, coinId);
        BigDecimal current = availableBalances.getOrDefault(k, BigDecimal.ZERO);
        if (current.compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        availableBalances.put(k, current.subtract(amount));
        BigDecimal locked = lockedBalances.getOrDefault(k, BigDecimal.ZERO);
        lockedBalances.put(k, locked.add(amount));
    }

    @Override
    public void unlockBalance(Long walletId, Long coinId, BigDecimal amount) {
        String k = key(walletId, coinId);
        BigDecimal locked = lockedBalances.getOrDefault(k, BigDecimal.ZERO);
        lockedBalances.put(k, locked.subtract(amount));
        BigDecimal current = availableBalances.getOrDefault(k, BigDecimal.ZERO);
        availableBalances.put(k, current.add(amount));
    }

    public void setBalance(Long walletId, Long coinId, BigDecimal amount) {
        availableBalances.put(key(walletId, coinId), amount);
    }

    public BigDecimal getLockedBalance(Long walletId, Long coinId) {
        return lockedBalances.getOrDefault(key(walletId, coinId), BigDecimal.ZERO);
    }

    public void clear() {
        availableBalances.clear();
        lockedBalances.clear();
    }

    private String key(Long walletId, Long coinId) {
        return walletId + ":" + coinId;
    }
}
