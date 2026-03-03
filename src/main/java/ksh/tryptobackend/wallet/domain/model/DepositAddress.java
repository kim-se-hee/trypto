package ksh.tryptobackend.wallet.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepositAddress {

    private final Long depositAddressId;
    private final Long walletId;
    private final String chain;
    private final String address;
    private final String tag;

    public static DepositAddress create(Long walletId, String chain, String address, String tag) {
        return DepositAddress.builder()
            .walletId(walletId)
            .chain(chain)
            .address(address)
            .tag(tag)
            .build();
    }
}
