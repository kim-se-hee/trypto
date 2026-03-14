package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;

public record CoinChainResponse(
    Long exchangeCoinChainId,
    String chain,
    boolean tagRequired
) {

    public static CoinChainResponse from(ExchangeCoinChain exchangeCoinChain) {
        return new CoinChainResponse(
            exchangeCoinChain.getExchangeCoinChainId(),
            exchangeCoinChain.getChain(),
            exchangeCoinChain.isTagRequired()
        );
    }
}
