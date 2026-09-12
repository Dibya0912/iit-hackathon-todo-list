package dev.levelforge;
public class ApiError extends RuntimeException {
    public final int status; public final String code;
    public ApiError(int status,String code,String message){super(message);this.status=status;this.code=code;}
    public static ApiError missing(){return new ApiError(404,"NOT_FOUND","This record was not found.");}
}

