package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.wallet.application.port.in.FindDepositAddressUseCase;
import ksh.tryptobackend.wallet.application.port.in.ResolveTransferDestinationUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.DepositAddressResult;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferDestinationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResolveTransferDestinationService implements ResolveTransferDestinationUseCase {

    private final FindDepositAddressUseCase findDepositAddressUseCase;

    @Override
    @Transactional(readOnly = true)
    public TransferDestinationResult resolveDestination(Long roundId, String toAddress) {
        Optional<DepositAddressResult> depositAddress =
            findDepositAddressUseCase.findByRoundIdAndAddress(roundId, toAddress);

        if (depositAddress.isEmpty()) {
            return TransferDestinationResult.failed("WRONG_ADDRESS");
        }

        return TransferDestinationResult.resolved(depositAddress.get().walletId());
    }
}
