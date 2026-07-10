package ksh.tryptobackend.investmentround.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.out.WalletQueryPort;
import ksh.tryptobackend.investmentround.domain.vo.RoundWallet;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** wallet 컨텍스트 조회를 investmentround 로 번역하는 ACL 어댑터. */
@Component("investmentroundAclWalletQueryAdapter")
@RequiredArgsConstructor
public class AclWalletQueryAdapter implements WalletQueryPort {

    private final FindWalletUseCase findWalletUseCase;

    @Override
    public Long getRoundId(Long walletId) {
        return findWalletUseCase
                .findById(walletId)
                .map(WalletResult::roundId)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    @Override
    public Long getWalletId(Long roundId, Long exchangeId) {
        return findWalletUseCase
                .findByRoundIdAndExchangeId(roundId, exchangeId)
                .map(WalletResult::walletId)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    @Override
    public List<RoundWallet> findWalletsByRoundId(Long roundId) {
        return findWalletUseCase.findByRoundId(roundId).stream().map(this::toRoundWallet).toList();
    }

    private RoundWallet toRoundWallet(WalletResult wallet) {
        return new RoundWallet(wallet.walletId(), wallet.exchangeId());
    }
}
