package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.ListedCoinRef;

import java.util.Optional;

public interface ListedCoinPort {

    Optional<ListedCoinRef> findById(Long exchangeCoinId);
}
