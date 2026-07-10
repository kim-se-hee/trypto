package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.investmentround.domain.vo.RoundWallet;

public record GetActiveRoundWalletResult(Long walletId, Long exchangeId) {

    public static GetActiveRoundWalletResult from(RoundWallet wallet) {
        return new GetActiveRoundWalletResult(wallet.walletId(), wallet.exchangeId());
    }
}
