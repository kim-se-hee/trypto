package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;

public interface MarketStatusNotificationPort {

    void broadcast(MarketStatusNotification notification);
}
