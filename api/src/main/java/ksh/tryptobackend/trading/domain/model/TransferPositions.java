package ksh.tryptobackend.trading.domain.model;

import java.util.List;

public record TransferPositions(Position source, Position destination) {

    public List<Position> toList() {
        return List.of(source, destination);
    }
}
