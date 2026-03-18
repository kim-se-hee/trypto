package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.LiveTicker;

public interface LivePriceMessagePort {

    void send(Long exchangeId, LiveTicker liveTicker);
}
