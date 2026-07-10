package ksh.tryptobackend.trading.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.trading.domain.vo.WalletRef;

public interface WalletQueryPort {

    Long getOwnerId(Long walletId);

    BigDecimal getAvailableBalance(Long walletId, Long coinId);

    List<WalletRef> findByRoundIds(List<Long> roundIds);

    List<Long> findWalletIdsByExchangeId(Long exchangeId);
}
