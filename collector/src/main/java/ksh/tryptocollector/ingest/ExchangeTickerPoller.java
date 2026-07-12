package ksh.tryptocollector.ingest;

import java.util.List;
import ksh.tryptocollector.model.Exchange;

public interface ExchangeTickerPoller {
    Exchange exchange();

    List<? extends NormalizableTicker> fetch(List<String> symbolCodes);
}
