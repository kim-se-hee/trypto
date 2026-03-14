package ksh.tryptobackend.marketdata.adapter.out;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;
import ksh.tryptobackend.marketdata.domain.model.CandleInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CandleQueryAdapter implements CandleQueryPort {

    private static final int RANGE_MULTIPLIER = 2;

    private final InfluxDBClient influxDBClient;

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

    private String buildFluxQuery(CandleFilter filter) {
        Instant end = filter.cursor() != null ? filter.cursor() : Instant.now();
        Instant start = calculateStart(end, filter.interval(), filter.limit());

        StringBuilder sb = new StringBuilder();
        sb.append("from(bucket: \"").append(bucket).append("\")");
        sb.append(" |> range(start: ").append(start).append(", stop: ").append(end).append(")");
        sb.append(" |> filter(fn: (r) => r._measurement == \"").append(filter.interval().getMeasurement()).append("\"");
        sb.append(" and r.exchange == \"").append(filter.exchange()).append("\"");
        sb.append(" and r.coin == \"").append(filter.coin()).append("\"");
        sb.append(" and (r._field == \"open\" or r._field == \"high\" or r._field == \"low\" or r._field == \"close\"))");
        sb.append(" |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")");
        sb.append(" |> sort(columns: [\"_time\"], desc: true)");
        sb.append(" |> limit(n: ").append(filter.limit()).append(")");
        return sb.toString();
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
                    toBigDecimal(record.getValueByKey("close"), "close")
                ));
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
}
