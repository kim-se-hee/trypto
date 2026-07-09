package ksh.tryptobackend.wallet.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.wallet.domain.model.Wallet;

public interface WalletQueryPort {

    Optional<Wallet> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    Optional<Wallet> findById(Long walletId);

    List<Wallet> findByRoundId(Long roundId);

    List<Wallet> findByRoundIds(List<Long> roundIds);

    List<Wallet> findByExchangeId(Long exchangeId);

    BigDecimal getAvailableBalance(Long walletId, Long coinId);

    BigDecimal getTotalBalance(Long walletId, Long coinId);
}
