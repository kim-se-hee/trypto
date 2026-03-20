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
    public Optional<DepositAddressResult> findByRoundIdAndAddress(Long roundId, String address) {
        return depositAddressQueryPort.findByRoundIdAndAddress(roundId, address)
            .map(da -> new DepositAddressResult(da.getWalletId(), da.getAddress()));
    }
}
