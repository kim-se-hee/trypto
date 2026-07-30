package ksh.tryptocollector.ingest.upbit;

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
public class UpbitRestClient {
    // 마켓 코드를 쿼리스트링에 싣는 구조라 상장 종목이 늘면 URI 길이 한도(4KB)를 넘겨 414 가 난다.
    // 빗썸이 473개에서 이미 한도를 초과했으므로 같은 방식으로 나눠서 요청한다.
    private static final int TICKER_BATCH_SIZE = 100;

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
        List<UpbitTickerResponse> tickers = new ArrayList<>();
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

    private List<UpbitTickerResponse> fetchTickerBatch(List<String> marketCodes) {
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
}
