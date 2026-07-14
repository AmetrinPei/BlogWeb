package com.example.blog.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：可读 message，不向客户端暴露堆栈（AC-13）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public Result<Void> handleValidation(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException manv) {
            FieldError fieldError = manv.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (ex instanceof BindException bind) {
            FieldError fieldError = bind.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        return Result.fail(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public Result<Void> handleBadRequest(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "请求参数错误";
        return Result.fail(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return Result.fail(ErrorCode.BAD_REQUEST, "文件大小不能超过 8MB");
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipart(MultipartException ex) {
        if (ex instanceof MaxUploadSizeExceededException) {
            return handleMaxUpload((MaxUploadSizeExceededException) ex);
        }
        return Result.fail(ErrorCode.BAD_REQUEST, "请求格式错误");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException ex) {
        return Result.fail(ErrorCode.NOT_FOUND, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(ErrorCode.INTERNAL_ERROR, "服务器内部错误");
    }
}
