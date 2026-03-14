package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.portfolio.application.port.in.GetMyHoldingsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.query.GetMyHoldingsQuery;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.HoldingSnapshotResult;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.MyHoldingsResult;
import ksh.tryptobackend.portfolio.domain.vo.CoinSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.CoinSnapshotMap;
import ksh.tryptobackend.portfolio.domain.vo.HoldingSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHolding;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHoldings;
import ksh.tryptobackend.trading.application.port.in.FindEvaluatedHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMyHoldingsService implements GetMyHoldingsUseCase {

    private final FindWalletUseCase findWalletUseCase;
    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final FindEvaluatedHoldingsUseCase findEvaluatedHoldingsUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;

    @Override
    public MyHoldingsResult getMyHoldings(GetMyHoldingsQuery query) {
        verifyOwnership(query.walletId(), query.userId());
        WalletResult wallet = findWallet(query.walletId());
        ExchangeDetailResult exchange = findExchangeDetail(wallet.exchangeId());

        BigDecimal baseCurrencyBalance = getAvailableBalanceUseCase.getAvailableBalance(
                query.walletId(), exchange.baseCurrencyCoinId());

        List<EvaluatedHoldingResult> evaluatedHoldings =
                findEvaluatedHoldingsUseCase.findEvaluatedHoldings(query.walletId(), wallet.exchangeId());

        PortfolioHoldings portfolioHoldings = toPortfolioHoldings(evaluatedHoldings);
        Set<Long> allCoinIds = portfolioHoldings.coinIdsIncluding(exchange.baseCurrencyCoinId());
        Map<Long, CoinInfoResult> coinInfoMap = findCoinInfoUseCase.findByIds(allCoinIds);
        String baseCurrencySymbol = getCoinSymbol(coinInfoMap, exchange.baseCurrencyCoinId());

        List<HoldingSnapshotResult> holdingSnapshots = buildHoldingSnapshots(
                evaluatedHoldings, coinInfoMap);

        return new MyHoldingsResult(wallet.exchangeId(), baseCurrencyBalance, baseCurrencySymbol, holdingSnapshots);
    }

    private void verifyOwnership(Long walletId, Long userId) {
        Long ownerId = getWalletOwnerIdUseCase.getWalletOwnerId(walletId);
        if (!ownerId.equals(userId)) {
            throw new CustomException(ErrorCode.WALLET_NOT_OWNED);
        }
    }

    private WalletResult findWallet(Long walletId) {
        return findWalletUseCase.findById(walletId)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private ExchangeDetailResult findExchangeDetail(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private PortfolioHoldings toPortfolioHoldings(List<EvaluatedHoldingResult> evaluatedHoldings) {
        List<PortfolioHolding> converted = evaluatedHoldings.stream()
                .map(h -> new PortfolioHolding(h.coinId(), h.avgBuyPrice(), h.totalQuantity()))
                .toList();
        return new PortfolioHoldings(converted);
    }

    private String getCoinSymbol(Map<Long, CoinInfoResult> coinInfoMap, Long coinId) {
        CoinInfoResult coinInfo = coinInfoMap.get(coinId);
        if (coinInfo == null) {
            throw new CustomException(ErrorCode.COIN_NOT_FOUND);
        }
        return coinInfo.symbol();
    }

    private List<HoldingSnapshotResult> buildHoldingSnapshots(
            List<EvaluatedHoldingResult> evaluatedHoldings,
            Map<Long, CoinInfoResult> coinInfoMap) {
        if (evaluatedHoldings.isEmpty()) {
            return List.of();
        }

        CoinSnapshotMap coinSnapshotMap = buildCoinSnapshotMap(evaluatedHoldings, coinInfoMap);
        PortfolioHoldings portfolioHoldings = toPortfolioHoldings(evaluatedHoldings);
        return portfolioHoldings.toHoldingSnapshots(coinSnapshotMap).stream()
                .map(HoldingSnapshotResult::from)
                .toList();
    }

    private CoinSnapshotMap buildCoinSnapshotMap(
            List<EvaluatedHoldingResult> evaluatedHoldings,
            Map<Long, CoinInfoResult> coinInfoMap) {
        Map<Long, CoinSnapshot> snapshotMap = evaluatedHoldings.stream()
                .filter(h -> coinInfoMap.containsKey(h.coinId()))
                .collect(Collectors.toMap(
                        EvaluatedHoldingResult::coinId,
                        h -> {
                            CoinInfoResult coinInfo = coinInfoMap.get(h.coinId());
                            return new CoinSnapshot(coinInfo.symbol(), coinInfo.name(), h.currentPrice());
                        }
                ));
        return new CoinSnapshotMap(snapshotMap);
    }
}
