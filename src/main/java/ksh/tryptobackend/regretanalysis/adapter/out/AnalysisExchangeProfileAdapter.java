package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeSummaryUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeSummaryResult;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisExchangeProfilePort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchangeProfile;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisExchangeProfileAdapter implements AnalysisExchangeProfilePort {

    private final FindExchangeSummaryUseCase findExchangeSummaryUseCase;
    private final FindWalletUseCase findWalletUseCase;

    @Override
    public AnalysisExchangeProfile getExchangeProfile(Long exchangeId) {
        ExchangeSummaryResult summary = findExchangeSummaryUseCase.findExchangeSummary(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        return new AnalysisExchangeProfile(summary.exchangeId(), summary.name(), summary.baseCurrencySymbol());
    }

    @Override
    public boolean existsWalletForExchange(Long roundId, Long exchangeId) {
        return findWalletUseCase.findByRoundIdAndExchangeId(roundId, exchangeId).isPresent();
    }
}
