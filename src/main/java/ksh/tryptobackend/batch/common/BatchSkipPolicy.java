package ksh.tryptobackend.batch.common;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.dao.DataIntegrityViolationException;

public class BatchSkipPolicy implements SkipPolicy {

    private static final int MAX_SKIP_COUNT = 100;

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) {
        if (skipCount >= MAX_SKIP_COUNT) {
            return false;
        }
        return isPriceNotAvailable(t) || isDataIntegrityViolation(t);
    }

    private boolean isPriceNotAvailable(Throwable t) {
        return t instanceof CustomException ce
            && ce.getErrorCode() == ErrorCode.PRICE_NOT_AVAILABLE;
    }

    private boolean isDataIntegrityViolation(Throwable t) {
        return t instanceof DataIntegrityViolationException;
    }
}
