package ksh.tryptobackend.marketdata.application.port.in;

import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.BtcDailyPriceResult;

public interface FindBtcDailyPricesUseCase {

    List<BtcDailyPriceResult> findBtcDailyPrices(
            LocalDate startDate, LocalDate endDate, String currency);
}
