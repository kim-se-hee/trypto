package ksh.tryptobackend.trading.domain.vo;

import java.time.LocalDateTime;

public record Fill(Price filledPrice, Money fee, LocalDateTime filledAt) {}
