package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.domain.vo.BaseCurrency;

public interface MarketDataQueryPort {

    BaseCurrency getBaseCurrency(Long exchangeId);
}
