package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.application.port.out.dto.TransferDepositAddressInfo;

import java.util.Optional;

public interface TransferDepositPort {

    Optional<TransferDepositAddressInfo> findByRoundIdAndChainAndAddress(Long roundId, String chain, String address);
}
