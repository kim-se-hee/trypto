package ksh.tryptocollector.distribute.tick;

import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import ksh.tryptocollector.model.Candle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandleWriter {

    private final WriteApiBlocking writeApiBlocking;

    public void write(Candle candle, String measurement) {
        Point point = Point.measurement(measurement)
                .addTag("exchange", candle.exchange())
                .addTag("symbol", candle.symbol())
                .addField("open", candle.open().doubleValue())
                .addField("high", candle.high().doubleValue())
                .addField("low", candle.low().doubleValue())
                .addField("close", candle.close().doubleValue())
                .time(candle.startMs(), WritePrecision.MS);
        writeApiBlocking.writePoint(point);
    }
}
