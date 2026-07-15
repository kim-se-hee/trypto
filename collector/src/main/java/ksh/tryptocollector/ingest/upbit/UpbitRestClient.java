package ksh.tryptocollector.ingest.upbit;

import java.util.Arrays;
import java.util.List;
import ksh.tryptocollector.model.Candle;
import ksh.tryptocollector.model.Exchange;
import ksh.tryptocollector.model.MarketInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UpbitRestClient {
    private final RestClient restClient;
    private final String restUrl;
    private final String tickerUrl;
    private final String candleUrl;

    public UpbitRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${exchange.upbit.rest-url}") String restUrl,
            @Value("${exchange.upbit.ticker-url}") String tickerUrl,
            @Value("${exchange.upbit.candle-url:https://api.upbit.com}") String candleUrl) {
        this.restClient = restClientBuilder.build();
        this.restUrl = restUrl;
        this.tickerUrl = tickerUrl;
        this.candleUrl = candleUrl;
    }

    public List<MarketInfo> fetchKrwMarkets() {
        UpbitMarketResponse[] responses =
                restClient.get().uri(restUrl).retrieve().body(UpbitMarketResponse[].class);
        if (responses == null) {
            return List.of();
        }
        return Arrays.stream(responses)
                .filter(r -> r.market().startsWith("KRW-"))
                .map(r -> {
                    String base = r.market().substring(4);
                    return new MarketInfo(base, "KRW", base + "/KRW", r.koreanName());
                })
                .toList();
    }

    public List<UpbitTickerResponse> fetchKrwTickers(List<String> marketCodes) {
        String markets = String.join(",", marketCodes);
        UpbitTickerResponse[] responses = restClient
                .get()
                .uri(tickerUrl + "?markets=" + markets)
                .retrieve()
                .body(UpbitTickerResponse[].class);
        if (responses == null) {
            return List.of();
        }
        return Arrays.asList(responses);
    }

    public List<Candle> fetchCandles(String candlePath, String base, String toIso, int count) {
        String uri = candleUrl + candlePath + "?market=KRW-" + base + "&count=" + count;
        if (toIso != null) {
            uri += "&to=" + toIso;
        }
        UpbitCandleResponse[] responses = restClient.get().uri(uri).retrieve().body(UpbitCandleResponse[].class);
        if (responses == null) {
            return List.of();
        }
        String symbol = base + "/" + Exchange.UPBIT.getQuote();
        return Arrays.stream(responses)
                .map(r -> new Candle(
                        Exchange.UPBIT.name(),
                        symbol,
                        r.startMs(),
                        r.openingPrice(),
                        r.highPrice(),
                        r.lowPrice(),
                        r.tradePrice()))
                .toList();
    }
}
