package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisExchangePort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisExchangeAdapter implements AnalysisExchangePort {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;

    @Override
    public AnalysisExchange getExchangeInfo(Long exchangeId) {
        ExchangeDetailResult detail = findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        return new AnalysisExchange(exchangeId, detail.name(), detail.domestic() ? "KRW" : "USDT");
    }
}
