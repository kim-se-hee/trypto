package ksh.tryptobackend.investmentround.application.port.out;

import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;

import java.util.Optional;

public interface InvestmentRoundQueryPort {

    Optional<InvestmentRoundInfo> findActiveRoundByUserId(Long userId);
}
