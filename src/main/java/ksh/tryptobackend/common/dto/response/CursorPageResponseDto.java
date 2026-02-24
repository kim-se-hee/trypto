package ksh.tryptobackend.common.dto.response;

import java.util.List;

public record CursorPageResponseDto<T>(
    List<T> content,
    Long nextCursor,
    boolean hasNext
) {

    public static <T> CursorPageResponseDto<T> of(List<T> content, Long nextCursor, boolean hasNext) {
        return new CursorPageResponseDto<>(content, nextCursor, hasNext);
    }
}
