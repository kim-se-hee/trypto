package ksh.tryptobackend.user.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record FeedbackContent(String value) {

    private static final int MIN_LENGTH = 20;
    private static final int MAX_LENGTH = 1000;

    public FeedbackContent {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_FEEDBACK_LENGTH);
        }
    }

    public static FeedbackContent of(String value) {
        return new FeedbackContent(value == null ? null : value.strip());
    }
}
