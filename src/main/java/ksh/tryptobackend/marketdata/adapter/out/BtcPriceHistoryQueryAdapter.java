package ksh.tryptobackend.marketdata.adapter.out;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ksh.tryptobackend.marketdata.application.port.out.BtcPriceHistoryQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.DailyClosePrice;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BtcPriceHistoryQueryAdapter implements BtcPriceHistoryQueryPort {

    private static final String MEASUREMENT = "candle_1h";
    private static final String TICKER_KEY_PREFIX = "ticker:";
    private static final Map<String, BtcPriceSource> CURRENCY_SOURCE = Map.of(
        "KRW", new BtcPriceSource("UPBIT", "BTC/KRW"),
        "USD", new BtcPriceSource("BINANCE", "BTC/USDT")
    );

    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Override
    public List<DailyClosePrice> findBtcDailyPrices(LocalDate startDate, LocalDate endDate, String currency) {
        BtcPriceSource source = CURRENCY_SOURCE.get(currency);
        if (source == null) {
            return List.of();
        }

        List<DailyClosePrice> candles = queryDailyCandles(startDate, endDate, source);
        return appendTodayTickerIfNeeded(candles, startDate, endDate, source);
    }

    private List<DailyClosePrice> queryDailyCandles(LocalDate startDate, LocalDate endDate, BtcPriceSource source) {
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant stop = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        String flux = buildFluxQuery(start, stop, source);
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
        return mapToDailyClosePrices(tables);
    }

    private String buildFluxQuery(Instant start, Instant stop, BtcPriceSource source) {
        return "from(bucket: \"" + bucket + "\")" +
            " |> range(start: " + start + ", stop: " + stop + ")" +
            " |> filter(fn: (r) => r._measurement == \"" + MEASUREMENT + "\"" +
            " and r.exchange == \"" + source.exchange() + "\"" +
            " and r.coin == \"" + source.coin() + "\"" +
            " and r._field == \"close\")" +
            " |> aggregateWindow(every: 1d, fn: last, createEmpty: false, timeSrc: \"_start\")" +
            " |> sort(columns: [\"_time\"])";
    }

    private List<DailyClosePrice> mapToDailyClosePrices(List<FluxTable> tables) {
        List<DailyClosePrice> result = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                LocalDate date = record.getTime().atZone(ZoneOffset.UTC).toLocalDate();
                BigDecimal closePrice = toBigDecimal(record.getValue());
                result.add(new DailyClosePrice(date, closePrice));
            }
        }
        return result;
    }

    private List<DailyClosePrice> appendTodayTickerIfNeeded(List<DailyClosePrice> candles,
                                                            LocalDate startDate, LocalDate endDate,
                                                            BtcPriceSource source) {
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            return candles;
        }

        boolean hasTodayCandle = candles.stream().anyMatch(p -> p.date().equals(today));
        if (hasTodayCandle) {
            return candles;
        }

        BigDecimal tickerPrice = queryTickerPrice(source);
        if (tickerPrice == null) {
            return candles;
        }

        List<DailyClosePrice> result = new ArrayList<>(candles);
        result.add(new DailyClosePrice(today, tickerPrice));
        return result;
    }

    private BigDecimal queryTickerPrice(BtcPriceSource source) {
        String key = TICKER_KEY_PREFIX + source.exchange() + ":" + source.coin();
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode priceNode = node.get("lastPrice");
            return priceNode != null ? priceNode.decimalValue() : null;
        } catch (JacksonException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private record BtcPriceSource(String exchange, String coin) {}
}
