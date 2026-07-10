package ksh.tryptobackend.ranking.adapter.out.acl;

import java.util.List;
import java.util.Map;
import ksh.tryptobackend.ranking.application.port.out.TradingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RoundTradeCounts;
import ksh.tryptobackend.trading.application.port.in.CountTradesByRoundIdsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingAclTradingQueryAdapter implements TradingQueryPort {

    private final CountTradesByRoundIdsUseCase countTradesByRoundIdsUseCase;

    @Override
    public RoundTradeCounts countTradesByRoundIds(List<Long> roundIds) {
        Map<Long, Integer> countByRoundId =
                countTradesByRoundIdsUseCase.countTradesByRoundIds(roundIds);
        return new RoundTradeCounts(countByRoundId);
    }
}
