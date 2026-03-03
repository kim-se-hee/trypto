package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangePort;
import ksh.tryptobackend.wallet.domain.vo.DepositTargetExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepositAddressExchangeAdapter implements DepositAddressExchangePort {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public DepositTargetExchange getExchange(Long exchangeId) {
        return exchangeQueryPort.findExchangeDetailById(exchangeId)
            .map(detail -> DepositTargetExchange.of(
                detail.baseCurrencyCoinId(), "KRW".equals(detail.currency())))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }
}
