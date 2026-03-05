package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferWalletAdapter implements TransferWalletPort {

    private final WalletQueryPort walletQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    public Long getOwnerUserId(Long walletId) {
        WalletInfo wallet = walletQueryPort.findById(walletId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        InvestmentRoundInfo round = investmentRoundQueryPort.findRoundInfoById(wallet.roundId())
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
        return round.userId();
    }
}
