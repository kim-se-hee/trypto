package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.application.port.out.dto.TransferExchangeCoinChainInfo;

import java.util.Optional;

public interface TransferExchangeCoinChainPort {

    Optional<TransferExchangeCoinChainInfo> findByExchangeIdAndCoinIdAndChain(Long exchangeId, Long coinId, String chain);
}
