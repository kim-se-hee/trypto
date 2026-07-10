package ksh.tryptobackend.investmentround.domain.service;

import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocation;

public interface SeedWalletProvisioner {

    Long provision(Long roundId, SeedAllocation allocation, LocalDateTime createdAt);
}
