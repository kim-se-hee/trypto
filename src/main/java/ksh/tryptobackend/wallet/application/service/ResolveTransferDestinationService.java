package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinChainUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinChainResult;
import ksh.tryptobackend.wallet.application.port.in.FindDepositAddressUseCase;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.ResolveTransferDestinationUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.DepositAddressResult;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferDestinationResult;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResolveTransferDestinationService implements ResolveTransferDestinationUseCase {

    private final FindDepositAddressUseCase findDepositAddressUseCase;
    private final FindWalletUseCase findWalletUseCase;
    private final FindExchangeCoinChainUseCase findExchangeCoinChainUseCase;

    @Override
    @Transactional(readOnly = true)
    public TransferDestinationResult resolveDestination(Long roundId, Long coinId, String chain,
                                                         String toAddress, String toTag) {
        Optional<DepositAddressResult> depositAddress =
            findDepositAddressUseCase.findByRoundIdAndChainAndAddress(roundId, chain, toAddress);
        if (depositAddress.isEmpty()) {
            return TransferDestinationResult.failed("WRONG_ADDRESS");
        }

        DepositAddressResult destAddress = depositAddress.get();
        WalletResult destWallet = findWalletUseCase.findById(destAddress.walletId())
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));

        Optional<ExchangeCoinChainResult> destChainInfo =
            findExchangeCoinChainUseCase.findByExchangeIdAndCoinIdAndChain(
                destWallet.exchangeId(), coinId, chain);
        if (destChainInfo.isEmpty()) {
            return TransferDestinationResult.failed("WRONG_CHAIN");
        }

        if (isMissingRequiredTag(destChainInfo.get(), toTag)) {
            return TransferDestinationResult.failed("MISSING_TAG");
        }

        return TransferDestinationResult.resolved(destAddress.walletId());
    }

    private boolean isMissingRequiredTag(ExchangeCoinChainResult chainResult, String tag) {
        return chainResult.tagRequired() && (tag == null || tag.isBlank());
    }
}
