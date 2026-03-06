package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.domain.vo.TransferDepositAddress;

import java.util.Optional;

public interface TransferDepositPort {

    Optional<TransferDepositAddress> findByRoundIdAndChainAndAddress(Long roundId, String chain, String address);
}
