package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.out.WalletQueryPort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisWallet;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
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

    @Override
    public List<AnalysisWallet> findWallets(Long roundId) {
        return findWalletUseCase.findByRoundId(roundId).stream()
                .map(this::toAnalysisWallet)
                .toList();
    }

    private AnalysisWallet toAnalysisWallet(WalletResult result) {
        return new AnalysisWallet(result.walletId(), result.exchangeId());
    }
}
