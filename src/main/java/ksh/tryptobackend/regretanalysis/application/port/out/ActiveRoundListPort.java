package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundExchangeInfo;

import java.util.List;

public interface ActiveRoundListPort {

    List<RoundExchangeInfo> findAllActiveRoundExchanges();
}
