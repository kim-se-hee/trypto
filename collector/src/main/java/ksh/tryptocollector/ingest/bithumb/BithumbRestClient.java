package ksh.tryptocollector.ingest.bithumb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ksh.tryptocollector.model.Candle;
import ksh.tryptocollector.model.Exchange;
import ksh.tryptocollector.model.MarketInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BithumbRestClient {
    // 마켓 코드를 쿼리스트링에 싣는 구조라 상장 종목이 늘면 URI 길이 한도(4KB)를 넘겨 414 가 난다.
    // KRW 마켓 473개 기준 4,138자로 이미 한도를 초과했으므로 나눠서 요청한다.
    private static final int TICKER_BATCH_SIZE = 100;

    private final RestClient restClient;
    private final String restUrl;
    private final String tickerUrl;
    private final String candleUrl;

    public BithumbRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${exchange.bithumb.rest-url}") String restUrl,
            @Value("${exchange.bithumb.ticker-url}") String tickerUrl,
            @Value("${exchange.bithumb.candle-url:https://api.bithumb.com}") String candleUrl) {
        this.restClient = restClientBuilder.build();
        this.restUrl = restUrl;
        this.tickerUrl = tickerUrl;
        this.candleUrl = candleUrl;
    }

    public List<MarketInfo> fetchKrwMarkets() {
        BithumbMarketResponse[] responses =
                restClient.get().uri(restUrl).retrieve().body(BithumbMarketResponse[].class);
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

    public List<BithumbTickerResponse> fetchKrwTickers(List<String> marketCodes) {
        List<BithumbTickerResponse> tickers = new ArrayList<>();
        for (int from = 0; from < marketCodes.size(); from += TICKER_BATCH_SIZE) {
            int to = Math.min(from + TICKER_BATCH_SIZE, marketCodes.size());
            tickers.addAll(fetchTickerBatch(marketCodes.subList(from, to)));
        }
        return tickers;
    }

    public List<Candle> fetchCandles(String candlePath, String base, String toIso, int count) {
        String uri = candleUrl + candlePath + "?market=KRW-" + base + "&count=" + count;
        if (toIso != null) {
            uri += "&to=" + toIso;
        }
        BithumbCandleResponse[] responses = restClient.get().uri(uri).retrieve().body(BithumbCandleResponse[].class);
        if (responses == null) {
            return List.of();
        }
        String symbol = base + "/" + Exchange.BITHUMB.getQuote();
        return Arrays.stream(responses)
                .map(r -> new Candle(
                        Exchange.BITHUMB.name(),
                        symbol,
                        r.startMs(),
                        r.openingPrice(),
                        r.highPrice(),
                        r.lowPrice(),
                        r.tradePrice()))
                .toList();
    }

    private List<BithumbTickerResponse> fetchTickerBatch(List<String> marketCodes) {
        String markets = String.join(",", marketCodes);
        BithumbTickerResponse[] responses = restClient
                .get()
                .uri(tickerUrl + "?markets=" + markets)
                .retrieve()
                .body(BithumbTickerResponse[].class);
        if (responses == null) {
            return List.of();
        }
        return Arrays.asList(responses);
    }
}
