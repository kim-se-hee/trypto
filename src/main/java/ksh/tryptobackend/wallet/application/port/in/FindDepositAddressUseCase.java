package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.result.DepositAddressResult;

import java.util.Optional;

public interface FindDepositAddressUseCase {

    Optional<DepositAddressResult> findByRoundIdAndAddress(Long roundId, String address);
}
