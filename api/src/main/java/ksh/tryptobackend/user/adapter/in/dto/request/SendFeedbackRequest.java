package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.user.application.port.in.dto.command.SendFeedbackCommand;

public record SendFeedbackRequest(@NotNull String content) {

    public SendFeedbackCommand toCommand(Long authorId) {
        return new SendFeedbackCommand(authorId, content);
    }
}
