package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import ksh.tryptobackend.user.application.port.in.dto.command.SendFeedbackCommand;

public record SendFeedbackRequest(@NotBlank String content) {

    public SendFeedbackCommand toCommand(Long authorId) {
        return new SendFeedbackCommand(authorId, content);
    }
}
