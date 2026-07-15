package ksh.tryptocollector.distribute.tick;

import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LastCandleTimeReader {

    private static final String SYMBOL_COLUMN = "symbol";

    private final QueryApi queryApi;

    @Value("${influxdb.bucket:ticker}")
    private String bucket;

    public Map<String, Long> findLastTimestamps(String measurement, String exchange) {
        String flux = String.format(
                "from(bucket:\"%s\") |> range(start: 0) "
                        + "|> filter(fn:(r)=> r._measurement==\"%s\" and r._field==\"close\" and r.exchange==\"%s\") "
                        + "|> group(columns:[\"%s\"]) |> last()",
                bucket, measurement, exchange, SYMBOL_COLUMN);

        List<FluxTable> tables = queryApi.query(flux);
        Map<String, Long> lastTimes = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Object symbol = record.getValueByKey(SYMBOL_COLUMN);
                Instant time = record.getTime();
                if (symbol != null && time != null) {
                    lastTimes.put(symbol.toString(), time.toEpochMilli());
                }
            }
        }
        return lastTimes;
    }
}
