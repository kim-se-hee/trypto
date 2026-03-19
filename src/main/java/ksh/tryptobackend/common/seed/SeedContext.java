package ksh.tryptobackend.common.seed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SeedContext {

    // MarketDataIdResolver에서 가져온 ID 맵
    final Map<String, Long> coinIdBySymbol = new HashMap<>();
    final Map<String, Long> exchangeIdByName = new HashMap<>();
    final Map<String, Long> exchangeCoinIdByKey = new HashMap<>();

    // 시더가 생성하면서 축적하는 ID
    final Map<String, Long> userIdByNickname = new HashMap<>();
    final Map<Long, Long> activeRoundIdByUserId = new HashMap<>();
    final Map<Long, List<Long>> walletIdsByRoundId = new HashMap<>();
    final Map<Long, Long> exchangeIdByWalletId = new HashMap<>();
    final Map<Long, List<Long>> orderIdsByWalletId = new HashMap<>();
    final Map<Long, List<Long>> ruleIdsByRoundId = new HashMap<>();

    Long getCoinId(String symbol) {
        return coinIdBySymbol.get(symbol);
    }

    Long getExchangeId(String name) {
        return exchangeIdByName.get(name);
    }

    Long getExchangeCoinId(String exchangeName, String coinSymbol) {
        return exchangeCoinIdByKey.get(exchangeName + ":" + coinSymbol);
    }

    void addUserId(String nickname, Long userId) {
        userIdByNickname.put(nickname, userId);
    }

    void addActiveRound(Long userId, Long roundId) {
        activeRoundIdByUserId.put(userId, roundId);
    }

    void addWalletId(Long roundId, Long walletId, Long exchangeId) {
        walletIdsByRoundId.computeIfAbsent(roundId, k -> new ArrayList<>()).add(walletId);
        exchangeIdByWalletId.put(walletId, exchangeId);
    }

    void addOrderId(Long walletId, Long orderId) {
        orderIdsByWalletId.computeIfAbsent(walletId, k -> new ArrayList<>()).add(orderId);
    }

    void addRuleIds(Long roundId, List<Long> ruleIds) {
        ruleIdsByRoundId.put(roundId, ruleIds);
    }
}
