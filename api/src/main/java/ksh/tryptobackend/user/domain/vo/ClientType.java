package ksh.tryptobackend.user.domain.vo;

import java.util.Locale;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public enum ClientType {
    WEB,
    MOBILE;

    private static final ClientType DEFAULT = WEB;

    public static ClientType from(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_CLIENT_TYPE);
        }
    }

    public static ClientType fromNullable(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT;
        }
        return from(name);
    }
}
