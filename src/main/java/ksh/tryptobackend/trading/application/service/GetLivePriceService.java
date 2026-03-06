package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.out.LivePricePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetLivePriceService implements GetLivePriceUseCase {

    private final LivePricePort livePricePort;

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        return livePricePort.getCurrentPrice(exchangeCoinId);
    }
}
