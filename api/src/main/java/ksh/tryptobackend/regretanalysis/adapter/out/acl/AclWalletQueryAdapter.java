package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import ksh.tryptobackend.regretanalysis.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("regretanalysisAclWalletQueryAdapter")
@RequiredArgsConstructor
public class AclWalletQueryAdapter implements WalletQueryPort {

    private final FindWalletUseCase findWalletUseCase;

    @Override
    public boolean existsWallet(Long roundId, Long exchangeId) {
        return findWalletUseCase.findByRoundIdAndExchangeId(roundId, exchangeId).isPresent();
    }
}
