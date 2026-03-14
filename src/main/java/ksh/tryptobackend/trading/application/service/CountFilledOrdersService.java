package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.CountFilledOrdersUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CountFilledOrdersService implements CountFilledOrdersUseCase {

    private final OrderQueryPort orderQueryPort;

    @Override
    public boolean existsByWalletId(Long walletId) {
        return orderQueryPort.existsFilledByWalletId(walletId);
    }

    @Override
    public int countByWalletId(Long walletId) {
        return orderQueryPort.countFilledByWalletId(walletId);
    }

    @Override
    public Map<Long, Integer> countGroupByWalletIds(List<Long> walletIds) {
        return orderQueryPort.countFilledGroupByWalletId(walletIds).toMap();
    }
}
