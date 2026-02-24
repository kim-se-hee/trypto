package ksh.tryptobackend.common.dto.response;

public record ApiResponseDto<T>(
    int status,
    String code,
    String message,
    T data
) {

    public static <T> ApiResponseDto<T> of(int status, String code, String message, T data) {
        return new ApiResponseDto<>(status, code, message, data);
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(200, "SUCCESS", message, data);
    }

    public static <T> ApiResponseDto<T> created(String message, T data) {
        return new ApiResponseDto<>(201, "CREATED", message, data);
    }
}
