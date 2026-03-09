package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindWalletService implements FindWalletUseCase {

    private final WalletQueryPort walletQueryPort;

    @Override
    public Optional<WalletResult> findById(Long walletId) {
        return walletQueryPort.findById(walletId).map(this::toResult);
    }

    @Override
    public Optional<WalletResult> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return walletQueryPort.findByRoundIdAndExchangeId(roundId, exchangeId).map(this::toResult);
    }

    @Override
    public List<WalletResult> findByRoundId(Long roundId) {
        return walletQueryPort.findByRoundId(roundId).stream().map(this::toResult).toList();
    }

    @Override
    public List<WalletResult> findByRoundIds(List<Long> roundIds) {
        return walletQueryPort.findByRoundIds(roundIds).stream().map(this::toResult).toList();
    }

    @Override
    public List<WalletResult> findByExchangeId(Long exchangeId) {
        return walletQueryPort.findByExchangeId(exchangeId).stream().map(this::toResult).toList();
    }

    private WalletResult toResult(WalletInfo info) {
        return new WalletResult(info.walletId(), info.roundId(), info.exchangeId(), info.seedAmount());
    }
}
