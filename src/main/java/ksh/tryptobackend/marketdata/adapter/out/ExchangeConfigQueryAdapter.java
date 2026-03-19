package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.common.config.ExchangeProperties;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeConfigQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExchangeConfigQueryAdapter implements ExchangeConfigQueryPort {

    private final ExchangeProperties exchangeProperties;

    @Override
    public List<ExchangeConfig> findAll() {
        return exchangeProperties.getExchanges().stream()
                .map(config -> new ExchangeConfig(
                        config.getName(),
                        ExchangeMarketType.valueOf(config.getMarketType()),
                        config.getBaseCurrencySymbol(),
                        config.getFeeRate()))
                .toList();
    }
}
