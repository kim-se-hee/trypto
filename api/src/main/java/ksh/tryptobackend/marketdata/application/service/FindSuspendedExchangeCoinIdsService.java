package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.FindSuspendedExchangeCoinIdsUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindSuspendedExchangeCoinIdsService implements FindSuspendedExchangeCoinIdsUseCase {

    private final ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Long> findSuspendedExchangeCoinIds() {
        return exchangeCoinQueryPort.findSuspendedExchangeCoinIds();
    }
}
