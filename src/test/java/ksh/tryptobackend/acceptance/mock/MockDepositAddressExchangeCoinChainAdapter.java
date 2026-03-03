package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangeCoinChainPort;
import ksh.tryptobackend.wallet.application.port.out.dto.DepositAddressChainInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDepositAddressExchangeCoinChainAdapter implements DepositAddressExchangeCoinChainPort {

    private final Map<String, DepositAddressChainInfo> chainInfos = new ConcurrentHashMap<>();

    @Override
    public DepositAddressChainInfo getExchangeCoinChain(Long exchangeId, Long coinId, String chain) {
        String key = exchangeId + ":" + coinId + ":" + chain;
        DepositAddressChainInfo info = chainInfos.get(key);
        if (info == null) {
            throw new CustomException(ErrorCode.UNSUPPORTED_CHAIN);
        }
        return info;
    }

    public void addChainInfo(Long exchangeId, Long coinId, String chain, boolean tagRequired) {
        String key = exchangeId + ":" + coinId + ":" + chain;
        chainInfos.put(key, new DepositAddressChainInfo(tagRequired));
    }

    public void clear() {
        chainInfos.clear();
    }
}
