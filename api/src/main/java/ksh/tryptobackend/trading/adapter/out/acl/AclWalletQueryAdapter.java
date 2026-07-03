package ksh.tryptobackend.trading.adapter.out.acl;

import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AclWalletQueryAdapter implements WalletQueryPort {

    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    @Override
    public Long getOwnerId(Long walletId) {
        return getWalletOwnerIdUseCase.getWalletOwnerId(walletId);
    }
}
