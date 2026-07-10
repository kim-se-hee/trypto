package ksh.tryptobackend.ranking.application.port.out;

import java.util.List;
import ksh.tryptobackend.ranking.domain.vo.RoundTradeCounts;

public interface TradingQueryPort {

    RoundTradeCounts countTradesByRoundIds(List<Long> roundIds);
}
