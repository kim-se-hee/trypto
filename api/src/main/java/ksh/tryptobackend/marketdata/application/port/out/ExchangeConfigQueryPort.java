package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeConfig;

public interface ExchangeConfigQueryPort {

    List<ExchangeConfig> findAll();
}
