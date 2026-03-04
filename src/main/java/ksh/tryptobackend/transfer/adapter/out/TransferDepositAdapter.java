package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.transfer.application.port.out.TransferDepositPort;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferDepositAddressInfo;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransferDepositAdapter implements TransferDepositPort {

    private final DepositAddressQueryPort depositAddressQueryPort;

    @Override
    public Optional<TransferDepositAddressInfo> findByRoundIdAndChainAndAddress(Long roundId, String chain, String address) {
        return depositAddressQueryPort.findByRoundIdAndChainAndAddress(roundId, chain, address)
            .map(info -> new TransferDepositAddressInfo(info.walletId(), info.chain(), info.address(), info.tag()));
    }
}
