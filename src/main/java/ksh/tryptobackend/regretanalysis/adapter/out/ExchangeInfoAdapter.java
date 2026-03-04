package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;
import ksh.tryptobackend.regretanalysis.application.port.out.ExchangeInfoPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.ExchangeInfoRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("regretExchangeInfoAdapter")
@RequiredArgsConstructor
public class ExchangeInfoAdapter implements ExchangeInfoPort {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public ExchangeInfoRecord getExchangeInfo(Long exchangeId) {
        ExchangeDetail detail = exchangeQueryPort.findExchangeDetailById(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        return new ExchangeInfoRecord(exchangeId, detail.name(), detail.domestic() ? "KRW" : "USDT");
    }
}
