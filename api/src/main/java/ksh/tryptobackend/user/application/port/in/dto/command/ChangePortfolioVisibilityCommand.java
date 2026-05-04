package ksh.tryptobackend.user.application.port.in.dto.command;

public record ChangePortfolioVisibilityCommand(Long userId, boolean portfolioPublic) {}
