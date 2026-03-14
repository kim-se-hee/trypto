package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferDestinationResult;

public interface ResolveTransferDestinationUseCase {

    TransferDestinationResult resolveDestination(Long roundId, Long coinId, String chain,
                                                  String toAddress, String toTag);
}
