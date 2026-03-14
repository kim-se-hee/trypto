package ksh.tryptobackend.portfolio.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WalletSnapshots {

    private final Map<Long, List<WalletSnapshot>> valuesByRoundId;

    public WalletSnapshots(List<WalletSnapshot> wallets) {
        this.valuesByRoundId = wallets.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(WalletSnapshot::roundId),
                Map::copyOf));
    }

    public List<WalletSnapshot> findByRoundId(Long roundId) {
        return valuesByRoundId.getOrDefault(roundId, List.of());
    }
}
