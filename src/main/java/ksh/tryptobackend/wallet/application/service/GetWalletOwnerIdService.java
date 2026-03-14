package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWalletOwnerIdService implements GetWalletOwnerIdUseCase {

    private final FindWalletUseCase findWalletUseCase;
    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @Override
    @Transactional(readOnly = true)
    public Long getWalletOwnerId(Long walletId) {
        Long roundId = findWalletUseCase.findById(walletId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND))
            .roundId();

        return findRoundInfoUseCase.findById(roundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND))
            .userId();
    }
}
