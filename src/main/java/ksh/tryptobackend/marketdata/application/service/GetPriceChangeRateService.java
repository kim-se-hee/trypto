package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.GetPriceChangeRateUseCase;
import ksh.tryptobackend.marketdata.application.port.out.PriceChangeRateQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetPriceChangeRateService implements GetPriceChangeRateUseCase {

    private final PriceChangeRateQueryPort priceChangeRateQueryPort;

    @Override
    public BigDecimal getChangeRate(Long exchangeCoinId) {
        return priceChangeRateQueryPort.getChangeRate(exchangeCoinId);
    }
}
