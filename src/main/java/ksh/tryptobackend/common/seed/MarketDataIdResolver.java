package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.CoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeCoinJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class MarketDataIdResolver {

    private final CoinJpaRepository coinRepository;
    private final ExchangeJpaRepository exchangeRepository;
    private final ExchangeCoinJpaRepository exchangeCoinRepository;

    void resolve(SeedContext ctx) {
        resolveCoinIds(ctx);
        resolveExchangeIds(ctx);
        resolveExchangeCoinIds(ctx);

        log.info("[Seed] 마켓 데이터 ID 해결 완료 - coin: {}, exchange: {}, exchangeCoin: {}",
            ctx.coinIdBySymbol.size(), ctx.exchangeIdByName.size(), ctx.exchangeCoinIdByKey.size());
    }

    private void resolveCoinIds(SeedContext ctx) {
        List<CoinJpaEntity> coins = coinRepository.findAll();
        coins.forEach(coin -> ctx.coinIdBySymbol.put(coin.getSymbol(), coin.getId()));
    }

    private void resolveExchangeIds(SeedContext ctx) {
        List<ExchangeJpaEntity> exchanges = exchangeRepository.findAll();
        exchanges.forEach(exchange -> ctx.exchangeIdByName.put(exchange.getName(), exchange.getId()));
    }

    private void resolveExchangeCoinIds(SeedContext ctx) {
        for (var entry : ctx.exchangeIdByName.entrySet()) {
            String exchangeName = entry.getKey();
            Long exchangeId = entry.getValue();
            List<ExchangeCoinJpaEntity> exchangeCoins = exchangeCoinRepository.findByExchangeId(exchangeId);
            for (ExchangeCoinJpaEntity ec : exchangeCoins) {
                String coinSymbol = ctx.coinIdBySymbol.entrySet().stream()
                    .filter(e -> e.getValue().equals(ec.getCoinId()))
                    .map(java.util.Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
                if (coinSymbol != null) {
                    ctx.exchangeCoinIdByKey.put(exchangeName + ":" + coinSymbol, ec.getId());
                }
            }
        }
    }
}
