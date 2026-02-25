package ksh.tryptobackend.common.dto.response;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponseDto<T>(
    List<T> content,
    Long nextCursor,
    boolean hasNext
) {

    public static <T> CursorPageResponseDto<T> of(List<T> content, Long nextCursor, boolean hasNext) {
        return new CursorPageResponseDto<>(content, nextCursor, hasNext);
    }

    public static <E, T> CursorPageResponseDto<T> fromPage(
        List<E> fetchedItems,
        int requestedSize,
        Function<E, T> mapper,
        Function<E, Long> cursorExtractor
    ) {
        boolean hasNext = fetchedItems.size() > requestedSize;
        List<E> trimmed = hasNext ? fetchedItems.subList(0, requestedSize) : fetchedItems;
        List<T> content = trimmed.stream().map(mapper).toList();
        Long nextCursor = hasNext ? cursorExtractor.apply(trimmed.getLast()) : null;
        return new CursorPageResponseDto<>(content, nextCursor, hasNext);
    }
}
