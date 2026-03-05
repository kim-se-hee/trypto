package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.ExchangeSnapshotInfo;

public interface ExchangeInfoQueryPort {

    ExchangeSnapshotInfo getExchangeInfo(Long exchangeId);
}
