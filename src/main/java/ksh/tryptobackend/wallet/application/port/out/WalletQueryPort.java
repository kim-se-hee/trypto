package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletQueryPort {

    Optional<WalletInfo> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    Optional<WalletInfo> findById(Long walletId);

    List<WalletInfo> findByRoundId(Long roundId);

    List<WalletInfo> findByRoundIds(List<Long> roundIds);

    List<WalletInfo> findByExchangeId(Long exchangeId);

    BigDecimal getAvailableBalance(Long walletId, Long coinId);
}
