package io.jaspercloud.sdwan.adapter;

import io.jaspercloud.sdwan.exception.ProcessCodeException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@ControllerAdvice
@RestController
public class ExceptionAdvice {

    @ExceptionHandler(ProcessCodeException.class)
    public Result exceptionHandler(ProcessCodeException e) {
        return new Result(e.getCode(), e.getMessage(), null);
    }
}
