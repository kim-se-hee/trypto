package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.GetLivePricesUseCase;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetLivePricesService implements GetLivePricesUseCase {

    private final LivePriceQueryPort livePriceQueryPort;

    @Override
    public Map<Long, BigDecimal> getCurrentPrices(Set<Long> exchangeCoinIds) {
        return livePriceQueryPort.getCurrentPrices(exchangeCoinIds).toMap();
    }
}
