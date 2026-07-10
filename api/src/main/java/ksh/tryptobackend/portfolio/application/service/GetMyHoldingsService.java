package ksh.tryptobackend.portfolio.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.portfolio.application.port.in.GetMyHoldingsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.query.GetMyHoldingsQuery;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.MyHoldingsResult;
import ksh.tryptobackend.portfolio.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.TradingQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.WalletQueryPort;
import ksh.tryptobackend.portfolio.domain.model.Portfolio;
import ksh.tryptobackend.portfolio.domain.vo.CoinMetadataMap;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHoldings;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMyHoldingsService implements GetMyHoldingsUseCase {

    private final WalletQueryPort walletQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final TradingQueryPort tradingQueryPort;

    @Override
    public MyHoldingsResult getMyHoldings(GetMyHoldingsQuery query) {
        PortfolioWallet wallet = walletQueryPort.getWallet(query.walletId());
        wallet.verifyOwnedBy(query.userId());

        Long baseCurrencyCoinId = marketDataQueryPort.getBaseCurrencyCoinId(wallet.exchangeId());
        BigDecimal baseCurrencyBalance =
                walletQueryPort.getBaseCurrencyBalance(query.walletId(), baseCurrencyCoinId);

        PortfolioHoldings holdings =
                tradingQueryPort.findHoldings(query.walletId(), wallet.exchangeId());
        CoinMetadataMap coinMetadata =
                marketDataQueryPort.findCoinMetadata(holdings.coinIdsIncluding(baseCurrencyCoinId));

        Portfolio portfolio =
                new Portfolio(
                        wallet.exchangeId(),
                        baseCurrencyCoinId,
                        baseCurrencyBalance,
                        holdings,
                        coinMetadata);
        return MyHoldingsResult.from(portfolio);
    }
}
