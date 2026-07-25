package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;

public interface FindSuspendedExchangeCoinIdsUseCase {

    List<Long> findSuspendedExchangeCoinIds();
}
