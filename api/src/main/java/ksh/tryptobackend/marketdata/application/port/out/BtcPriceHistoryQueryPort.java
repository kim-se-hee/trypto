package ksh.tryptobackend.marketdata.application.port.out;

import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.marketdata.domain.vo.DailyClosePrice;

public interface BtcPriceHistoryQueryPort {

    List<DailyClosePrice> findBtcDailyPrices(
            LocalDate startDate, LocalDate endDate, String currency);
}
