package ksh.tryptobackend.wallet.application.port.in;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;

public interface FindWalletUseCase {

    Optional<WalletResult> findById(Long walletId);

    Optional<WalletResult> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<WalletResult> findByRoundId(Long roundId);

    List<WalletResult> findByRoundIds(List<Long> roundIds);

    List<WalletResult> findByExchangeId(Long exchangeId);
}
