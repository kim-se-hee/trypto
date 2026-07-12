package ksh.tryptocollector.ingest.upbit;

import java.util.List;
import ksh.tryptocollector.ingest.ExchangeTickerPoller;
import ksh.tryptocollector.ingest.NormalizableTicker;
import ksh.tryptocollector.model.Exchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpbitTickerPoller implements ExchangeTickerPoller {
    private final UpbitRestClient upbitRestClient;

    @Override
    public Exchange exchange() {
        return Exchange.UPBIT;
    }

    @Override
    public List<? extends NormalizableTicker> fetch(List<String> symbolCodes) {
        return upbitRestClient.fetchKrwTickers(symbolCodes);
    }
}
