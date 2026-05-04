package ksh.tryptobackend.marketdata.application.port.out;

import java.util.Set;
import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshots;

public interface TickerSnapshotQueryPort {

    TickerSnapshots findByExchangeCoinIds(Set<Long> exchangeCoinIds);
}
