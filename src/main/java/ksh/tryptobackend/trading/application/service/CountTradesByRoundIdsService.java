package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.CountTradesByRoundIdsUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.vo.FilledOrderCounts;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountTradesByRoundIdsService implements CountTradesByRoundIdsUseCase {

    private final FindWalletUseCase findWalletUseCase;
    private final OrderQueryPort orderQueryPort;

    @Override
    public Map<Long, Integer> countTradesByRoundIds(List<Long> roundIds) {
        List<WalletResult> wallets = findWalletUseCase.findByRoundIds(roundIds);
        List<Long> walletIds = wallets.stream().map(WalletResult::walletId).toList();

        FilledOrderCounts tradeCountByWalletId = orderQueryPort.countFilledGroupByWalletId(walletIds);

        return aggregateByRoundId(wallets, tradeCountByWalletId);
    }

    private Map<Long, Integer> aggregateByRoundId(List<WalletResult> wallets,
                                                   FilledOrderCounts tradeCountByWalletId) {
        return wallets.stream()
            .collect(Collectors.groupingBy(
                WalletResult::roundId,
                Collectors.summingInt(w -> tradeCountByWalletId.getCount(w.walletId()))
            ));
    }
}
