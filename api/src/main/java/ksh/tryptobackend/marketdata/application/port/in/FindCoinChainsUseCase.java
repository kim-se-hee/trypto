package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinChainResult;

public interface FindCoinChainsUseCase {

    List<CoinChainResult> findCoinChains(Long exchangeId, Long coinId);
}
