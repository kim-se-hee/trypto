package ksh.tryptobackend.user.domain.vo;

import java.util.Locale;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public enum Provider {
    KAKAO,
    GOOGLE;

    public static Provider from(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }
    }
}
