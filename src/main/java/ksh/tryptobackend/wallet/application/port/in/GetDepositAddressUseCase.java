package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.query.GetDepositAddressQuery;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;

public interface GetDepositAddressUseCase {

    DepositAddress getDepositAddress(GetDepositAddressQuery query);
}
