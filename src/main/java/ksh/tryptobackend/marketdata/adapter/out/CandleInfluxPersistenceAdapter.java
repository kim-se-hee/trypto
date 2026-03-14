package ksh.tryptobackend.marketdata.adapter.out;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleInterval;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CandleInfluxPersistenceAdapter implements CandleQueryPort {

    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Override
    public List<Candle> findByExchangeAndCoinAndInterval(
            String exchange, String coin, CandleInterval interval, int limit, Instant cursor) {
        String flux = buildFluxQuery(exchange, coin, interval, limit, cursor);
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux);
        List<Candle> candles = mapToCandles(tables);
        Collections.reverse(candles);
        return candles;
    }

    private String buildFluxQuery(
            String exchange, String coin, CandleInterval interval, int limit, Instant cursor) {
        StringBuilder sb = new StringBuilder();
        sb.append("from(bucket: \"").append(bucket).append("\")");
        sb.append(" |> range(start: 0)");
        sb.append(" |> filter(fn: (r) => r._measurement == \"").append(interval.getMeasurement()).append("\")");
        sb.append(" |> filter(fn: (r) => r.exchange == \"").append(exchange).append("\")");
        sb.append(" |> filter(fn: (r) => r.coin == \"").append(coin).append("\")");
        sb.append(" |> filter(fn: (r) => r._field == \"open\" or r._field == \"high\" or r._field == \"low\" or r._field == \"close\")");
        if (cursor != null) {
            sb.append(" |> filter(fn: (r) => r._time < ").append(cursor).append(")");
        }
        sb.append(" |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")");
        sb.append(" |> sort(columns: [\"_time\"], desc: true)");
        sb.append(" |> limit(n: ").append(limit).append(")");
        return sb.toString();
    }

    private List<Candle> mapToCandles(List<FluxTable> tables) {
        List<Candle> candles = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                candles.add(new Candle(
                    record.getTime(),
                    toDouble(record.getValueByKey("open")),
                    toDouble(record.getValueByKey("high")),
                    toDouble(record.getValueByKey("low")),
                    toDouble(record.getValueByKey("close"))
                ));
            }
        }
        return candles;
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}
