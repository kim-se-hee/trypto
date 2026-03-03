package ksh.tryptobackend.marketdata.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExchangeCoinChain {

    private final Long exchangeCoinChainId;
    private final Long exchangeCoinId;
    private final String chain;
    private final boolean tagRequired;
}
