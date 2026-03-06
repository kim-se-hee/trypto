package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.regretanalysis.application.port.out.BtcPriceHistoryPort;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockBtcPriceHistoryAdapter implements BtcPriceHistoryPort {

    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

    @Override
    public List<BtcDailyPrice> findBtcDailyPrices(LocalDate startDate, LocalDate endDate, String currency) {
        List<BtcDailyPrice> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String key = toKey(date, currency);
            BigDecimal price = prices.get(key);
            if (price != null) {
                result.add(new BtcDailyPrice(date, price));
            }
        }
        return result;
    }

    public void setPrice(LocalDate date, String currency, BigDecimal price) {
        prices.put(toKey(date, currency), price);
    }

    public void clear() {
        prices.clear();
    }

    private String toKey(LocalDate date, String currency) {
        return date.toString() + ":" + currency;
    }
}
