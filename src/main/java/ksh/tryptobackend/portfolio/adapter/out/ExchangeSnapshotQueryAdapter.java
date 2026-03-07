package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.portfolio.application.port.out.ExchangeSnapshotQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.ExchangeSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.KrwConversionRate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeSnapshotQueryAdapter implements ExchangeSnapshotQueryPort {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;

    @Override
    public ExchangeSnapshot getExchangeInfo(Long exchangeId) {
        ExchangeDetailResult detail = findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        KrwConversionRate rate = detail.domestic() ? KrwConversionRate.DOMESTIC : KrwConversionRate.OVERSEAS;
        return new ExchangeSnapshot(exchangeId, detail.baseCurrencyCoinId(), rate);
    }
}
