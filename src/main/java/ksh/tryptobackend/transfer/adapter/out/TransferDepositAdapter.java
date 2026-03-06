package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.transfer.application.port.out.TransferDepositPort;
import ksh.tryptobackend.transfer.domain.vo.TransferDepositAddress;
import ksh.tryptobackend.wallet.application.port.in.FindDepositAddressUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransferDepositAdapter implements TransferDepositPort {

    private final FindDepositAddressUseCase findDepositAddressUseCase;

    @Override
    public Optional<TransferDepositAddress> findByRoundIdAndChainAndAddress(Long roundId, String chain, String address) {
        return findDepositAddressUseCase.findByRoundIdAndChainAndAddress(roundId, chain, address)
            .map(result -> new TransferDepositAddress(result.walletId(), result.chain(), result.address(), result.tag()));
    }
}
