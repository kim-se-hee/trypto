package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.ExchangeMetadata;

public interface ExchangeMetadataPort {

    ExchangeMetadata getExchangeMetadata(Long exchangeId);

    boolean existsWalletForExchange(Long roundId, Long exchangeId);
}
