package ksh.tryptocollector.ingest.binance;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import ksh.tryptocollector.model.Candle;
import ksh.tryptocollector.model.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceRestClient {
    private final RestClient restClient;
    private final String restUrl;
    private final String candleUrl;

    public BinanceRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${exchange.binance.rest-url}") String restUrl,
            @Value("${exchange.binance.candle-url:https://api.binance.com/api/v3/klines}") String candleUrl) {
        this.restClient = restClientBuilder.build();
        this.restUrl = restUrl;
        this.candleUrl = candleUrl;
    }

    public List<BinanceTickerResponse> fetchUsdtTickers() {
        BinanceTickerResponse[] responses =
                restClient.get().uri(restUrl).retrieve().body(BinanceTickerResponse[].class);
        if (responses == null) {
            return List.of();
        }
        return Arrays.stream(responses)
                .filter(r -> r.symbol().endsWith("USDT"))
                .filter(r -> new BigDecimal(r.quoteVolume()).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    public List<Candle> fetchCandles(String interval, String base, Long endTimeMs, int limit) {
        String uri = candleUrl + "?symbol=" + base + "USDT&interval=" + interval + "&limit=" + limit;
        if (endTimeMs != null) {
            uri += "&endTime=" + endTimeMs;
        }
        Object[][] rows = restClient.get().uri(uri).retrieve().body(Object[][].class);
        if (rows == null) {
            return List.of();
        }
        String symbol = base + "/" + Exchange.BINANCE.getQuote();
        return Arrays.stream(rows)
                .map(BinanceCandleResponse::from)
                .map(r -> new Candle(
                        Exchange.BINANCE.name(), symbol, r.openTime(), r.open(), r.high(), r.low(), r.close()))
                .toList();
    }
}
