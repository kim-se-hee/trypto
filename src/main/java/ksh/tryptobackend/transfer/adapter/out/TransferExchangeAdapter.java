package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.transfer.application.port.out.TransferExchangePort;
import ksh.tryptobackend.transfer.domain.vo.TransferSourceExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferExchangeAdapter implements TransferExchangePort {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public TransferSourceExchange getExchangeDetail(Long exchangeId) {
        return exchangeQueryPort.findExchangeDetailById(exchangeId)
            .map(detail -> new TransferSourceExchange(
                detail.baseCurrencyCoinId(), detail.domestic()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }
}
