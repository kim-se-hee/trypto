package ksh.tryptobackend.common.exception;

public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException() {
        super("duplicate request");
    }
}
