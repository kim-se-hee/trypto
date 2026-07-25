package ksh.tryptobackend.trading.application.port.in;

import java.util.List;

public interface FindSuspendedMarketsUseCase {

    List<Long> findSuspendedMarkets();
}
