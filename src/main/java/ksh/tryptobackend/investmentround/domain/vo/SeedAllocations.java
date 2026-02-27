package ksh.tryptobackend.investmentround.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeedAllocations {

    private final List<SeedAllocation> allocations;

    private SeedAllocations(List<SeedAllocation> allocations) {
        this.allocations = allocations;
    }

    public static SeedAllocations of(List<SeedAllocation> allocations) {
        validateNoDuplicate(allocations);
        return new SeedAllocations(allocations);
    }

    public BigDecimal totalAmount() {
        return allocations.stream()
            .map(SeedAllocation::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<SeedAllocation> getAll() {
        return allocations;
    }

    private static void validateNoDuplicate(List<SeedAllocation> allocations) {
        Set<Long> exchangeIds = new HashSet<>();
        for (SeedAllocation allocation : allocations) {
            if (!exchangeIds.add(allocation.exchangeId())) {
                throw new CustomException(ErrorCode.INVALID_SEED_AMOUNT);
            }
        }
    }
}
