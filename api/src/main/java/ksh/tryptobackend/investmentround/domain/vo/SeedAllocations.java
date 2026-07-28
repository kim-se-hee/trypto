package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public class SeedAllocations {

    private final List<SeedAllocation> allocations;

    private SeedAllocations(List<SeedAllocation> allocations) {
        this.allocations = allocations;
    }

    public static SeedAllocations of(List<SeedAllocation> allocations) {
        validateNoDuplicate(allocations);
        validateAnySeed(allocations);
        return new SeedAllocations(allocations);
    }

    // 시드 총액은 원화 단일 통화로 집계한다. 외화 시드는 라운드 시작 시점 시세로 환산된 값이다.
    public BigDecimal totalKrwAmount() {
        return allocations.stream().map(SeedAllocation::krwAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<SeedAllocation> getAll() {
        return allocations;
    }

    private static void validateNoDuplicate(List<SeedAllocation> allocations) {
        Set<Long> exchangeIds = new HashSet<>();
        for (SeedAllocation allocation : allocations) {
            if (!exchangeIds.add(allocation.exchangeId())) {
                throw new CustomException(ErrorCode.DUPLICATE_EXCHANGE);
            }
        }
    }

    private static void validateAnySeed(List<SeedAllocation> allocations) {
        boolean hasSeed =
                allocations.stream().anyMatch(allocation -> allocation.amount().compareTo(BigDecimal.ZERO) > 0);
        if (!hasSeed) {
            throw new CustomException(ErrorCode.SEED_REQUIRED);
        }
    }
}
