package ksh.tryptobackend.marketdata.adapter.out;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;
import ksh.tryptobackend.marketdata.domain.model.CandleInterval;
import ksh.tryptobackend.marketdata.domain.model.CandleWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandleQueryAdapter implements CandleQueryPort {

    private static final int RANGE_MULTIPLIER = 2;

    private static final String CANDLE_1D = "candle_1d";
    private static final String CANDLE_1H = "candle_1h";
    private static final String CANDLE_1M = "candle_1m";
    private static final String TICKER_RAW = "ticker_raw";
    private static final String RAW_PRICE_FIELD = "price";
    private static final String AGG_COLUMN = "agg";
    private static final int OHLC_FIELD_COUNT = 4;

    // 거래소별 캔들 일 경계 타임존. 빗썸만 00:00 KST(Asia/Seoul), 업비트·바이낸스는 00:00 UTC.
    private static final String BITHUMB = "BITHUMB";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final InfluxDBClient influxDBClient;
    private final Clock clock;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Override
    public List<Candle> findByFilter(CandleFilter filter) {
        String flux = buildFluxQuery(filter);
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        List<Candle> candles = mapToCandles(tables);
        Collections.reverse(candles);
        return candles;
    }

    @Override
    public Optional<Candle> findInProgressCandle(CandleFilter filter) {
        Instant now = Instant.now(clock);
        CandleWindow window = CandleWindow.of(filter.interval(), now, candleZone(filter.exchange()));

        List<Ohlc> parts = new ArrayList<>();
        for (Segment segment : buildSegments(window, now)) {
            aggregateSegment(filter, segment).ifPresent(parts::add);
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(combine(window.periodStart(), parts));
    }

    private ZoneId candleZone(String exchange) {
        return BITHUMB.equals(exchange) ? KST : ZoneOffset.UTC;
    }

    // 현재 구간을 코스별로 쪼갠다: 완성된 날은 일봉, 완성된 시간은 1시간봉, 완성된 분은 1분봉, 진행 중인 분은 원본 틱.
    // 앞(과거)부터 순서대로 담는다. 하위 경계가 모두 정시라 어떤 거래소 일 경계든 빈틈 없이 맞물린다.
    private List<Segment> buildSegments(CandleWindow window, Instant now) {
        List<Segment> segments = new ArrayList<>();
        addSegment(segments, CANDLE_1D, window.periodStart(), window.dayStart(), false);
        addSegment(segments, CANDLE_1H, laterOf(window.periodStart(), window.dayStart()), window.hourStart(), false);
        addSegment(segments, CANDLE_1M, laterOf(window.periodStart(), window.hourStart()), window.minuteStart(), false);
        addSegment(segments, TICKER_RAW, laterOf(window.periodStart(), window.minuteStart()), now, true);
        return segments;
    }

    private void addSegment(List<Segment> segments, String measurement, Instant start, Instant stop, boolean rawTick) {
        if (start.isBefore(stop)) {
            segments.add(new Segment(measurement, start, stop, rawTick));
        }
    }

    private Instant laterOf(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private Candle combine(Instant time, List<Ohlc> parts) {
        BigDecimal open = parts.get(0).open();
        BigDecimal close = parts.get(parts.size() - 1).close();
        BigDecimal high =
                parts.stream().map(Ohlc::high).max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal low =
                parts.stream().map(Ohlc::low).min(Comparator.naturalOrder()).orElseThrow();
        return new Candle(time, open, high, low, close);
    }

    private Optional<Ohlc> aggregateSegment(CandleFilter filter, Segment segment) {
        String flux = buildSegmentFlux(filter, segment);
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        Map<String, BigDecimal> values = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Object agg = record.getValueByKey(AGG_COLUMN);
                if (agg != null) {
                    values.put(agg.toString(), toBigDecimal(record.getValue(), agg.toString()));
                }
            }
        }
        if (values.size() < OHLC_FIELD_COUNT) {
            return Optional.empty();
        }
        return Optional.of(new Ohlc(values.get("open"), values.get("high"), values.get("low"), values.get("close")));
    }

    private String buildFluxQuery(CandleFilter filter) {
        Instant end = filter.cursor() != null ? filter.cursor() : Instant.now(clock);
        Instant start = calculateStart(end, filter.interval(), filter.limit());

        StringBuilder sb = new StringBuilder();
        sb.append("from(bucket: \"").append(bucket).append("\")");
        sb.append(" |> range(start: ")
                .append(start)
                .append(", stop: ")
                .append(end)
                .append(")");
        sb.append(" |> filter(fn: (r) => r._measurement == \"")
                .append(filter.interval().getMeasurement())
                .append("\"");
        sb.append(" and r.exchange == \"").append(filter.exchange()).append("\"");
        sb.append(" and r.symbol == \"").append(filter.symbol()).append("\"");
        sb.append(" and (r._field == \"open\" or r._field == \"high\" or r._field == \"low\" or"
                + " r._field == \"close\"))");
        sb.append(" |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")");
        sb.append(" |> sort(columns: [\"_time\"], desc: true)");
        sb.append(" |> limit(n: ").append(filter.limit()).append(")");
        return sb.toString();
    }

    // 한 조각을 시가(first)·고가(max)·저가(min)·종가(last)로 집계해 agg 라벨을 붙여 하나로 합친다.
    private String buildSegmentFlux(CandleFilter filter, Segment segment) {
        String dataFieldFilter = segment.rawTick() ? " and r._field == \"" + RAW_PRICE_FIELD + "\"" : "";
        StringBuilder sb = new StringBuilder();
        sb.append("data = from(bucket: \"").append(bucket).append("\")");
        sb.append(" |> range(start: ")
                .append(segment.start())
                .append(", stop: ")
                .append(segment.stop())
                .append(")");
        sb.append(" |> filter(fn: (r) => r._measurement == \"")
                .append(segment.measurement())
                .append("\"");
        sb.append(" and r.exchange == \"").append(filter.exchange()).append("\"");
        sb.append(" and r.symbol == \"")
                .append(filter.symbol())
                .append("\"")
                .append(dataFieldFilter)
                .append(")\n");
        sb.append(aggLine("o", "first", "open", segment.rawTick()));
        sb.append(aggLine("h", "max", "high", segment.rawTick()));
        sb.append(aggLine("l", "min", "low", segment.rawTick()));
        sb.append(aggLine("c", "last", "close", segment.rawTick()));
        sb.append("union(tables: [o, h, l, c]) |> keep(columns: [\"")
                .append(AGG_COLUMN)
                .append("\", \"_value\"])");
        return sb.toString();
    }

    private String aggLine(String var, String fn, String agg, boolean rawTick) {
        // 캔들 measurement 는 필드별로 걸러 집계하고, 원본 틱은 price 하나뿐이라 걸러낼 필요가 없다.
        String fieldFilter = rawTick ? "" : " |> filter(fn: (r) => r._field == \"" + agg + "\")";
        return var + " = data" + fieldFilter + " |> " + fn + "() |> set(key: \"" + AGG_COLUMN + "\", value: \"" + agg
                + "\")\n";
    }

    private Instant calculateStart(Instant end, CandleInterval interval, int limit) {
        long rangeSeconds = interval.getDuration().getSeconds() * limit * RANGE_MULTIPLIER;
        return end.minusSeconds(rangeSeconds);
    }

    private List<Candle> mapToCandles(List<FluxTable> tables) {
        List<Candle> candles = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                candles.add(new Candle(
                        record.getTime(),
                        toBigDecimal(record.getValueByKey("open"), "open"),
                        toBigDecimal(record.getValueByKey("high"), "high"),
                        toBigDecimal(record.getValueByKey("low"), "low"),
                        toBigDecimal(record.getValueByKey("close"), "close")));
            }
        }
        return candles;
    }

    private BigDecimal toBigDecimal(Object value, String fieldName) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new IllegalStateException("InfluxDB 캔들 필드 '" + fieldName + "'의 값이 유효하지 않습니다: " + value);
    }

    private record Segment(String measurement, Instant start, Instant stop, boolean rawTick) {}

    private record Ohlc(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {}
}
