package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.application.port.out.dto.CoinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CoinQueryPort {

    Map<Long, String> findSymbolsByIds(Set<Long> coinIds);

    List<CoinInfo> findByIds(Set<Long> coinIds);

}
