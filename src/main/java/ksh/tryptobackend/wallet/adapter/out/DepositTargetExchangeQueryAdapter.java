package ksh.tryptobackend.wallet.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinChainUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.application.port.out.DepositTargetExchangeQueryPort;
import ksh.tryptobackend.wallet.domain.vo.DepositTargetExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepositTargetExchangeQueryAdapter implements DepositTargetExchangeQueryPort {

    private final WalletJpaRepository walletRepository;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinChainUseCase findExchangeCoinChainUseCase;

    @Override
    public Long getExchangeIdByWalletId(Long walletId) {
        return walletRepository.findById(walletId)
            .map(wallet -> wallet.getExchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    @Override
    public DepositTargetExchange getExchange(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(detail -> DepositTargetExchange.of(
                detail.baseCurrencyCoinId(), detail.domestic()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    @Override
    public boolean isTagRequired(Long exchangeId, Long coinId, String chain) {
        return findExchangeCoinChainUseCase.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .map(result -> result.tagRequired())
            .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_CHAIN));
    }
}
