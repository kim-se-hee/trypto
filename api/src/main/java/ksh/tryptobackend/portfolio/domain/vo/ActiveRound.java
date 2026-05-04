package ksh.tryptobackend.portfolio.domain.vo;

import java.time.LocalDateTime;

public record ActiveRound(Long roundId, Long userId, LocalDateTime startedAt) {}
