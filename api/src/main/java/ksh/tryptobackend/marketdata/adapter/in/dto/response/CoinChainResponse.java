package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinChainResult;

public record CoinChainResponse(Long exchangeCoinChainId, String chain, boolean tagRequired) {

    public static CoinChainResponse from(CoinChainResult result) {
        return new CoinChainResponse(
                result.exchangeCoinChainId(), result.chain(), result.tagRequired());
    }
}
