package ksh.tryptobackend.user.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record Nickname(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;

    public static Nickname of(String value) {
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME_LENGTH);
        }
        return new Nickname(value);
    }

    public boolean hasSameValueAs(String other) {
        return value.equals(other);
    }
}
