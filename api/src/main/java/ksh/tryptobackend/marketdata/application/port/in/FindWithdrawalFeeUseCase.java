package ksh.tryptobackend.marketdata.application.port.in;

import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.WithdrawalFeeResult;

public interface FindWithdrawalFeeUseCase {

    Optional<WithdrawalFeeResult> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain);
}
