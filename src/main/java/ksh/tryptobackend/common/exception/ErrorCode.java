package ksh.tryptobackend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(500, "internal.server.error"),
    ;

    private final int status;
    private final String messageKey;
}
