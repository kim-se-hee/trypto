package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundListPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundExchangeInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MockActiveRoundListAdapter implements ActiveRoundListPort {

    private final List<RoundExchangeInfo> roundExchanges = new ArrayList<>();

    @Override
    public List<RoundExchangeInfo> findAllActiveRoundExchanges() {
        return List.copyOf(roundExchanges);
    }

    public void addRoundExchange(Long roundId, Long userId, Long exchangeId,
                                  Long walletId, LocalDateTime startedAt) {
        roundExchanges.add(new RoundExchangeInfo(roundId, userId, exchangeId, walletId, startedAt));
    }

    public void clear() {
        roundExchanges.clear();
    }
}
