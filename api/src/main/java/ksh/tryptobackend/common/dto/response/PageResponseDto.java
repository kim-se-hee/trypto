package ksh.tryptobackend.common.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponseDto<T>(int page, int size, int totalPages, List<T> content) {

    public static <T> PageResponseDto<T> from(Page<T> page) {
        return new PageResponseDto<>(
                page.getNumber(), page.getSize(), page.getTotalPages(), page.getContent());
    }
}
