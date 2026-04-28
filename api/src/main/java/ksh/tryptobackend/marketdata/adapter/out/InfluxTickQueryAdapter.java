package ksh.tryptobackend.marketdata.adapter.out;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import ksh.tryptobackend.marketdata.application.port.out.TickQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import ksh.tryptobackend.marketdata.domain.vo.Tick;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InfluxTickQueryAdapter implements TickQueryPort {

    private static final String MEASUREMENT = "ticker_raw";
    private static final String PRICE_FIELD = "price";

    private final InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Override
    public List<Tick> findTicks(ExchangeSymbolKey key, Instant from, Instant to) {
        String flux = buildFluxQuery(key, from, to);
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
        return mapToTicks(tables);
    }

    private String buildFluxQuery(ExchangeSymbolKey key, Instant from, Instant to) {
        return new StringBuilder()
            .append("from(bucket: \"").append(bucket).append("\")")
            .append(" |> range(start: ").append(from).append(", stop: ").append(to).append(")")
            .append(" |> filter(fn: (r) => r._measurement == \"").append(MEASUREMENT).append("\"")
            .append(" and r.exchange == \"").append(key.exchange()).append("\"")
            .append(" and r.symbol == \"").append(key.symbol()).append("\"")
            .append(" and r._field == \"").append(PRICE_FIELD).append("\")")
            .append(" |> sort(columns: [\"_time\"])")
            .toString();
    }

    private List<Tick> mapToTicks(List<FluxTable> tables) {
        List<Tick> ticks = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Object value = record.getValue();
                if (value == null) continue;
                ticks.add(new Tick(record.getTime(), new BigDecimal(value.toString())));
            }
        }
        return ticks;
    }
}
