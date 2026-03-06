package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.ActiveRoundExchange;

import java.util.List;

public interface ActiveRoundExchangePort {

    List<ActiveRoundExchange> findAllActiveRoundExchanges();
}
