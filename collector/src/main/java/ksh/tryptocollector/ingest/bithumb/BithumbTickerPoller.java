package ksh.tryptocollector.ingest.bithumb;

import java.util.List;
import ksh.tryptocollector.ingest.ExchangeTickerPoller;
import ksh.tryptocollector.ingest.NormalizableTicker;
import ksh.tryptocollector.model.Exchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BithumbTickerPoller implements ExchangeTickerPoller {
    private final BithumbRestClient bithumbRestClient;

    @Override
    public Exchange exchange() {
        return Exchange.BITHUMB;
    }

    @Override
    public List<? extends NormalizableTicker> fetch(List<String> symbolCodes) {
        return bithumbRestClient.fetchKrwTickers(symbolCodes);
    }
}
