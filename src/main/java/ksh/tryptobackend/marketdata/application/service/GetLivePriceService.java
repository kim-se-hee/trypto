package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetLivePriceService implements GetLivePriceUseCase {

    private final LivePriceQueryPort livePriceQueryPort;

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        return livePriceQueryPort.getCurrentPrice(exchangeCoinId);
    }
}
