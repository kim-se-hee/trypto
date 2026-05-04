package ksh.tryptobackend.marketdata.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLivePriceService implements GetLivePriceUseCase {

    private final LivePriceQueryPort livePriceQueryPort;

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        return livePriceQueryPort.getCurrentPrice(exchangeCoinId);
    }
}
