package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeSummary;
import ksh.tryptobackend.regretanalysis.application.port.out.ExchangeMetadataPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.ExchangeMetadata;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeMetadataAdapter implements ExchangeMetadataPort {

    private final ExchangeQueryPort exchangeQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public ExchangeMetadata getExchangeMetadata(Long exchangeId) {
        ExchangeSummary summary = exchangeQueryPort.findExchangeSummaryById(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        return new ExchangeMetadata(summary.exchangeId(), summary.name(), summary.baseCurrencySymbol());
    }

    @Override
    public boolean existsWalletForExchange(Long roundId, Long exchangeId) {
        return walletQueryPort.findByRoundIdAndExchangeId(roundId, exchangeId).isPresent();
    }
}
