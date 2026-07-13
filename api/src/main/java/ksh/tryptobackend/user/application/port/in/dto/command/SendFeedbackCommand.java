package ksh.tryptobackend.user.application.port.in.dto.command;

public record SendFeedbackCommand(Long authorId, String content) {}
