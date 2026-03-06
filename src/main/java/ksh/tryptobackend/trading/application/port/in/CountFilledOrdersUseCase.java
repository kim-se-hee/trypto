package ksh.tryptobackend.trading.application.port.in;

import java.util.List;
import java.util.Map;

public interface CountFilledOrdersUseCase {

    boolean existsByWalletId(Long walletId);

    int countByWalletId(Long walletId);

    Map<Long, Integer> countGroupByWalletIds(List<Long> walletIds);
}
