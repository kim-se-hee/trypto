package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;

public interface FindAllExchangeIdsUseCase {

    List<Long> findAllExchangeIds();
}
