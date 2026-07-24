package ksh.tryptobackend.trading.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ksh.tryptobackend.trading.application.port.in.MoveHoldingUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.MoveHoldingCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.service.HoldingTransferrer;
import ksh.tryptobackend.trading.domain.vo.CoinExchangeMapping;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveHoldingService implements MoveHoldingUseCase {

    private final PositionCommandPort positionCommandPort;
    private final MarketQueryPort marketQueryPort;

    private final HoldingTransferrer holdingTransferrer;

    @Override
    @Transactional
    public void moveHolding(MoveHoldingCommand command) {
        if (hasNoHolding(command.fromWalletId(), command.coinId())) {
            return;
        }

        Price acquisitionPrice = acquisitionPriceOf(command);
        Map<Long, Position> positions =
                getPositionsInWalletIdOrder(command.fromWalletId(), command.toWalletId(), command.coinId());
        Position source = positions.get(command.fromWalletId());
        Position destination = positions.get(command.toWalletId());
        holdingTransferrer.transfer(source, destination, Quantity.of(command.amount()), acquisitionPrice);

        positionCommandPort.save(source);
        positionCommandPort.save(destination);
    }

    private boolean hasNoHolding(Long walletId, Long coinId) {
        return positionCommandPort
                .findByWalletIdAndCoinId(walletId, coinId)
                .map(position -> !position.isHolding())
                .orElse(true);
    }

    private Price acquisitionPriceOf(MoveHoldingCommand command) {
        CoinExchangeMapping mapping =
                marketQueryPort.findCoinExchangeMapping(command.toExchangeId(), List.of(command.coinId()));
        return marketQueryPort.getCurrentPrice(mapping.getExchangeCoinId(command.coinId()));
    }

    private Map<Long, Position> getPositionsInWalletIdOrder(Long fromWalletId, Long toWalletId, Long coinId) {
        return Stream.of(fromWalletId, toWalletId)
                .sorted()
                .collect(Collectors.toMap(
                        walletId -> walletId,
                        walletId -> positionCommandPort.getOrCreate(walletId, coinId),
                        (first, second) -> first,
                        LinkedHashMap::new));
    }
}
