package ksh.tryptobackend.marketdata.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.model.WithdrawalFee;

public interface WithdrawalFeeQueryPort {

    Optional<WithdrawalFee> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain);
}
