package ksh.tryptobackend.portfolio.adapter.in.batch;

public record SnapshotInput(Long roundId, Long userId, Long exchangeId, Long walletId) {}
