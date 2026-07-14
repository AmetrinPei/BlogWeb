package com.example.blog.common;

/**
 * 可预期的业务异常，由全局处理器转为统一 Result，不暴露堆栈。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
