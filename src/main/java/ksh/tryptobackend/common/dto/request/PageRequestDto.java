package ksh.tryptobackend.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record PageRequestDto(
    @Min(0) int page,
    @Min(1) @Max(50) int size
) {

    public PageRequestDto() {
        this(0, 20);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
