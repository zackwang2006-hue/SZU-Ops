package com.campusops.common;

import lombok.Getter;

/**
 * 业务异常：携带友好错误信息与业务码，由全局异常处理器转为 Result
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
