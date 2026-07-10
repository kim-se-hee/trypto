package ksh.tryptobackend.trading.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ksh.tryptobackend.trading.application.port.in.CountTradesByRoundIdsUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.trading.domain.vo.FilledOrderCounts;
import ksh.tryptobackend.trading.domain.vo.WalletRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CountTradesByRoundIdsService implements CountTradesByRoundIdsUseCase {

    private final WalletQueryPort walletQueryPort;
    private final OrderQueryPort orderQueryPort;

    @Override
    public Map<Long, Integer> countTradesByRoundIds(List<Long> roundIds) {
        List<WalletRef> wallets = walletQueryPort.findByRoundIds(roundIds);
        List<Long> walletIds = wallets.stream().map(WalletRef::walletId).toList();

        FilledOrderCounts tradeCountByWalletId =
                orderQueryPort.countFilledGroupByWalletId(walletIds);

        return aggregateByRoundId(wallets, tradeCountByWalletId);
    }

    private Map<Long, Integer> aggregateByRoundId(
            List<WalletRef> wallets, FilledOrderCounts tradeCountByWalletId) {
        return wallets.stream()
                .collect(
                        Collectors.groupingBy(
                                WalletRef::roundId,
                                Collectors.summingInt(
                                        w -> tradeCountByWalletId.getCount(w.walletId()))));
    }
}
