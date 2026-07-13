package ksh.tryptobackend.common.dto.response;

public record ApiResponseDto<T>(int status, String code, String message, T data) {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String CREATED_CODE = "CREATED";
    private static final int OK_STATUS = 200;
    private static final int CREATED_STATUS = 201;

    public static <T> ApiResponseDto<T> of(int status, String code, String message, T data) {
        return new ApiResponseDto<>(status, code, message, data);
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(OK_STATUS, SUCCESS_CODE, message, data);
    }

    public static <T> ApiResponseDto<T> createdSuccess(String message, T data) {
        return new ApiResponseDto<>(CREATED_STATUS, SUCCESS_CODE, message, data);
    }

    public static <T> ApiResponseDto<T> created(String message, T data) {
        return new ApiResponseDto<>(CREATED_STATUS, CREATED_CODE, message, data);
    }
}
