package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.ListedCoinPort;
import ksh.tryptobackend.trading.domain.vo.ListedCoinRef;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockListedCoinAdapter implements ListedCoinPort {

    private final Map<Long, ListedCoinRef> listedCoins = new ConcurrentHashMap<>();

    @Override
    public Optional<ListedCoinRef> findById(Long exchangeCoinId) {
        return Optional.ofNullable(listedCoins.get(exchangeCoinId));
    }

    public void addListedCoin(ListedCoinRef ref) {
        listedCoins.put(ref.exchangeCoinId(), ref);
    }

    public void clear() {
        listedCoins.clear();
    }
}
