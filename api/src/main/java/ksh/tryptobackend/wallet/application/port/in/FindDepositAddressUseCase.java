package ksh.tryptobackend.wallet.application.port.in;

import java.util.Optional;
import ksh.tryptobackend.wallet.application.port.in.dto.result.DepositAddressResult;

public interface FindDepositAddressUseCase {

    Optional<DepositAddressResult> findByRoundIdAndAddress(Long roundId, String address);
}
