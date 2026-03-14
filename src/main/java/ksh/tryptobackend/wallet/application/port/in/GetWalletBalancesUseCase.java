package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.query.GetWalletBalancesQuery;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletBalancesResult;

public interface GetWalletBalancesUseCase {

    WalletBalancesResult getWalletBalances(GetWalletBalancesQuery query);
}
