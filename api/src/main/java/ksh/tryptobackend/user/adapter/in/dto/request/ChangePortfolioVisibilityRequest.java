package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.user.application.port.in.dto.command.ChangePortfolioVisibilityCommand;

public record ChangePortfolioVisibilityRequest(@NotNull Boolean portfolioPublic) {

    public ChangePortfolioVisibilityCommand toCommand(Long userId) {
        return new ChangePortfolioVisibilityCommand(userId, portfolioPublic);
    }
}
