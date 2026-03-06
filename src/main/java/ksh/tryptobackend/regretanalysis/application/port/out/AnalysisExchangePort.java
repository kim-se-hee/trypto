package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;

public interface AnalysisExchangePort {

    AnalysisExchange getExchangeInfo(Long exchangeId);
}
