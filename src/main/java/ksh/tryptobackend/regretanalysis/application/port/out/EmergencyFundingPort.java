package ksh.tryptobackend.regretanalysis.application.port.out;

import java.math.BigDecimal;

public interface EmergencyFundingPort {

    BigDecimal getTotalFundingAmount(Long roundId);
}
