package ksh.tryptobackend.marketdata.application.service;

import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.FindBtcDailyPricesUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.BtcDailyPriceResult;
import ksh.tryptobackend.marketdata.application.port.out.BtcPriceHistoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindBtcDailyPricesService implements FindBtcDailyPricesUseCase {

    private final BtcPriceHistoryQueryPort btcPriceHistoryQueryPort;

    @Override
    public List<BtcDailyPriceResult> findBtcDailyPrices(
            LocalDate startDate, LocalDate endDate, String currency) {
        return btcPriceHistoryQueryPort.findBtcDailyPrices(startDate, endDate, currency).stream()
                .map(p -> new BtcDailyPriceResult(p.date(), p.closePrice()))
                .toList();
    }
}
