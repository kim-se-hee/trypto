package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.ExchangeSnapshot;

public interface ExchangeSnapshotPort {

    ExchangeSnapshot getExchangeInfo(Long exchangeId);
}
