package io.jaspercloud.sdwan.controller;

import io.jaspercloud.sdwan.controller.param.Result;
import io.jaspercloud.sdwan.support.ErrorCode;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ControllerAdvice
@RestController
public class ExceptionAdvice {

    @ExceptionHandler(ProcessCodeException.class)
    public Result exceptionHandler(ProcessCodeException e) {
        log.error(e.getMessage(), e);
        return new Result(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(ProcessException.class)
    public Result exceptionHandler(ProcessException e) {
        log.error(e.getMessage(), e);
        return new Result(ErrorCode.SysError, e.getMessage(), null);
    }
}
