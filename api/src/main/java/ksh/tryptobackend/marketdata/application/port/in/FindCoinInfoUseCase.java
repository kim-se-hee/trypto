package ksh.tryptobackend.marketdata.application.port.in;

import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;

public interface FindCoinInfoUseCase {

    Map<Long, CoinInfoResult> findByIds(Set<Long> coinIds);
}
