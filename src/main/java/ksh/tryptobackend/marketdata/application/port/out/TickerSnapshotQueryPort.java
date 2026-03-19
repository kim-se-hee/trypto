package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.TickerSnapshots;

import java.util.Set;

public interface TickerSnapshotQueryPort {

    TickerSnapshots findByExchangeCoinIds(Set<Long> exchangeCoinIds);
}
