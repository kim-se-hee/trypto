package ksh.tryptobackend.wallet.adapter.in.dto.response;

import ksh.tryptobackend.wallet.domain.model.DepositAddress;

public record DepositAddressResponse(
    Long depositAddressId,
    Long walletId,
    String chain,
    String address,
    String tag
) {

    public static DepositAddressResponse from(DepositAddress depositAddress) {
        return new DepositAddressResponse(
            depositAddress.getDepositAddressId(),
            depositAddress.getWalletId(),
            depositAddress.getChain(),
            depositAddress.getAddress(),
            depositAddress.getTag()
        );
    }
}
