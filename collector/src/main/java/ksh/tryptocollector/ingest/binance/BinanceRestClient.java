package ksh.tryptocollector.ingest.binance;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptocollector.model.Candle;
import ksh.tryptocollector.model.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceRestClient {
    private static final String QUOTE_SUFFIX = "USDT";
    private static final String TRADING_STATUS = "TRADING";

    private final RestClient restClient;
    private final String restUrl;
    private final String exchangeInfoUrl;
    private final String candleUrl;

    public BinanceRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${exchange.binance.rest-url}") String restUrl,
            @Value("${exchange.binance.exchange-info-url:https://api.binance.com/api/v3/exchangeInfo}")
                    String exchangeInfoUrl,
            @Value("${exchange.binance.candle-url:https://api.binance.com/api/v3/klines}") String candleUrl) {
        this.restClient = restClientBuilder.build();
        this.restUrl = restUrl;
        this.exchangeInfoUrl = exchangeInfoUrl;
        this.candleUrl = candleUrl;
    }

    public List<BinanceTickerResponse> fetchUsdtTickers() {
        BinanceTickerResponse[] responses =
                restClient.get().uri(restUrl).retrieve().body(BinanceTickerResponse[].class);
        if (responses == null) {
            return List.of();
        }
        return Arrays.stream(responses)
                .filter(r -> r.symbol().endsWith(QUOTE_SUFFIX))
                .toList();
    }

    public List<BinanceTickerResponse> fetchTradingUsdtTickers() {
        Set<String> tradingSymbols = fetchTradingSymbols();
        return fetchUsdtTickers().stream()
                .filter(r -> tradingSymbols.contains(r.symbol()))
                .toList();
    }

    public Set<String> fetchTradingSymbols() {
        BinanceExchangeInfoResponse response = restClient
                .get()
                .uri(exchangeInfoUrl + "?symbolStatus=" + TRADING_STATUS)
                .retrieve()
                .body(BinanceExchangeInfoResponse.class);
        if (response == null || response.symbols() == null) {
            return Set.of();
        }
        return response.symbols().stream()
                .filter(s -> TRADING_STATUS.equals(s.status()))
                .filter(s -> s.symbol().endsWith(QUOTE_SUFFIX))
                .map(BinanceExchangeInfoResponse.Symbol::symbol)
                .collect(Collectors.toUnmodifiableSet());
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
