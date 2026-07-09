package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.config.CacheConfig;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWalletOwnerIdService implements GetWalletOwnerIdUseCase {

    private final WalletQueryPort walletQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    @Cacheable(cacheNames = CacheConfig.WALLET_OWNER_CACHE, key = "#walletId")
    @Transactional(readOnly = true)
    public Long getWalletOwnerId(Long walletId) {
        Wallet wallet =
                walletQueryPort
                        .findById(walletId)
                        .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));

        return investmentRoundQueryPort.getOwnerId(wallet.getRoundId());
    }
}
