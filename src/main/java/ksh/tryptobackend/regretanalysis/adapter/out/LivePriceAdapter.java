package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.regretanalysis.application.port.out.LivePricePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("regretLivePriceAdapter")
@RequiredArgsConstructor
public class LivePriceAdapter implements LivePricePort {

    private final ksh.tryptobackend.trading.application.port.out.LivePricePort tradingLivePricePort;

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        return tradingLivePricePort.getCurrentPrice(exchangeCoinId);
    }
}
