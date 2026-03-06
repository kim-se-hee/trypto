package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundExchangePort;
import ksh.tryptobackend.regretanalysis.domain.vo.ActiveRoundExchange;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockActiveRoundListAdapter implements ActiveRoundExchangePort {

    private final List<ActiveRoundExchange> roundExchanges = new ArrayList<>();

    @Override
    public List<ActiveRoundExchange> findAllActiveRoundExchanges() {
        return List.copyOf(roundExchanges);
    }

    public void addRoundExchange(Long roundId, Long userId, Long exchangeId,
                                  Long walletId, LocalDateTime startedAt) {
        roundExchanges.add(new ActiveRoundExchange(roundId, userId, exchangeId, walletId, startedAt));
    }

    public void clear() {
        roundExchanges.clear();
    }
}
