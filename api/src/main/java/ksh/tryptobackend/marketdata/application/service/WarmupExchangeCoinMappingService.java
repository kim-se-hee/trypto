package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import ksh.tryptobackend.marketdata.application.port.in.WarmupExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.out.CoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinMappings;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoins;
import ksh.tryptobackend.marketdata.domain.vo.CoinSymbols;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarmupExchangeCoinMappingService implements WarmupExchangeCoinMappingUseCase {

    private final ExchangeQueryPort exchangeQueryPort;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;
    private final CoinQueryPort coinQueryPort;
    private final ExchangeCoinMappingCacheCommandPort exchangeCoinMappingCacheCommandPort;

    @Override
    public void warmup() {
        List<Exchange> exchanges =
                exchangeQueryPort.findAllExchangeIds().stream()
                        .map(exchangeQueryPort::findExchangeDetailById)
                        .flatMap(Optional::stream)
                        .toList();
        CoinSymbols baseCurrencySymbols =
                coinQueryPort.findSymbolsByIds(
                        exchanges.stream()
                                .map(Exchange::getBaseCurrencyCoinId)
                                .collect(Collectors.toSet()));

        ExchangeCoinMappings mappings = ExchangeCoinMappings.empty();
        for (Exchange exchange : exchanges) {
            ExchangeCoins coins = exchangeCoinQueryPort.findByExchangeId(exchange.getExchangeId());
            CoinSymbols coinSymbols = coinQueryPort.findSymbolsByIds(coins.coinIds());
            mappings =
                    mappings.add(
                            exchange,
                            baseCurrencySymbols.getSymbol(exchange.getBaseCurrencyCoinId()),
                            coins,
                            coinSymbols);
        }

        exchangeCoinMappingCacheCommandPort.loadAll(mappings.toMap());
        log.info("거래소-코인 매핑 캐시 로딩 완료: {}건", mappings.size());
    }
}
