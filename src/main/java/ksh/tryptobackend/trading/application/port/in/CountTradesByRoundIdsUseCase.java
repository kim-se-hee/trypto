package ksh.tryptobackend.trading.application.port.in;

import java.util.List;
import java.util.Map;

public interface CountTradesByRoundIdsUseCase {

    Map<Long, Integer> countTradesByRoundIds(List<Long> roundIds);
}
