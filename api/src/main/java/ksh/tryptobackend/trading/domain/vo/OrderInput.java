package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record OrderInput(BigDecimal volume, BigDecimal price) {

    public BigDecimal requiredVolume() {
        if (volume == null) {
            throw new CustomException(ErrorCode.VOLUME_REQUIRED);
        }
        return volume;
    }

    public BigDecimal requiredPrice() {
        if (price == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED);
        }
        return price;
    }

    public void rejectVolume() {
        if (volume != null) {
            throw new CustomException(ErrorCode.VOLUME_NOT_ALLOWED);
        }
    }

    public void rejectPrice() {
        if (price != null) {
            throw new CustomException(ErrorCode.PRICE_NOT_ALLOWED);
        }
    }
}
