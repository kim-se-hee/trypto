package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.wallet.application.port.in.FindDepositAddressUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.DepositAddressResult;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindDepositAddressService implements FindDepositAddressUseCase {

    private final DepositAddressQueryPort depositAddressQueryPort;

    @Override
    public Optional<DepositAddressResult> findByRoundIdAndChainAndAddress(Long roundId, String chain, String address) {
        return depositAddressQueryPort.findByRoundIdAndChainAndAddress(roundId, chain, address)
            .map(info -> new DepositAddressResult(info.walletId(), info.chain(), info.address(), info.tag()));
    }
}
